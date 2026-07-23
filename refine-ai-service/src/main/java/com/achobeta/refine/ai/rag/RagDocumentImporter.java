package com.achobeta.refine.ai.rag;

import com.achobeta.refine.ai.ocr.application.port.DocumentTextPort;
import com.achobeta.refine.ai.rag.application.port.RagRepository;
import com.achobeta.refine.ai.rag.application.query.RagChunkDraft;
import com.achobeta.refine.ai.rag.application.query.RagDocumentMetadata;
import com.achobeta.refine.ai.shared.application.port.TextEmbeddingPort;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Component
@ConditionalOnProperty(prefix = "refine.pgvector", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RagDocumentImporter implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(RagDocumentImporter.class);
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("txt", "md", "pdf", "docx");

    private final PgVectorProperties properties;
    private final RagRepository repository;
    private final DocumentTextPort extractor;
    private final TextEmbeddingPort embeddings;
    private final RagMetadataParser metadataParser;
    private final Counter importedCounter;
    private final Counter skippedCounter;
    private final Counter failureCounter;

    public RagDocumentImporter(PgVectorProperties properties, RagRepository repository, DocumentTextPort extractor,
                               TextEmbeddingPort embeddings, RagMetadataParser metadataParser, MeterRegistry meterRegistry) {
        this.properties = properties;
        this.repository = repository;
        this.extractor = extractor;
        this.embeddings = embeddings;
        this.metadataParser = metadataParser;
        this.importedCounter = meterRegistry.counter("refine.rag.documents", "result", "imported");
        this.skippedCounter = meterRegistry.counter("refine.rag.documents", "result", "skipped");
        this.failureCounter = meterRegistry.counter("refine.rag.documents", "result", "failed");
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            repository.initializeSchema(embeddings.dimensions());
            Path root = Path.of(properties.getDocumentPath()).toAbsolutePath().normalize();
            if (!Files.isDirectory(root)) {
                log.info("RAG document directory does not exist; path={}", root);
                return;
            }
            try (Stream<Path> paths = Files.walk(root)) {
                paths.filter(Files::isRegularFile).filter(this::isSupported).sorted()
                        .forEach(path -> importDocument(root, path));
            }
        } catch (Exception exception) {
            failureCounter.increment();
            log.error("RAG initialization failed; existing embeddings were kept", exception);
        }
    }

    void importDocument(Path root, Path path) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            String source = root.relativize(path.toAbsolutePath().normalize()).toString().replace('\\', '/');
            String content = extractor.extract(bytes, extension(path));
            RagMetadataParser.ParsedRagDocument parsed = metadataParser.parse(Path.of(source), checksum(bytes), content);
            RagDocumentMetadata document = parsed.metadata();
            if (!document.approved()) {
                skippedCounter.increment();
                log.warn("RAG document is not approved; source={}", source);
                return;
            }
            if (parsed.content().isBlank()) {
                skippedCounter.increment();
                log.warn("RAG document has no extractable content; source={}", source);
                return;
            }
            int dimensions = embeddings.dimensions();
            String embeddingModel = embeddings.modelName();
            if (repository.isCurrent(document, embeddingModel, dimensions)) {
                skippedCounter.increment();
                return;
            }
            List<TextSegment> segments = DocumentSplitters.recursive(properties.getChunkSize(), properties.getChunkOverlap())
                    .split(Document.from(parsed.content(), document.toLangChainMetadata())).stream()
                    .filter(segment -> !segment.text().isBlank()).toList();
            List<double[]> vectors = embeddings.embedAll(segments.stream().map(TextSegment::text).toList());
            if (segments.size() != vectors.size()) throw new IllegalStateException("Embedding response count does not match chunks");
            List<RagChunkDraft> chunks = IntStream.range(0, segments.size()).mapToObj(index -> {
                double[] vector = vectors.get(index);
                if (vector.length != dimensions) throw new IllegalStateException("Embedding dimensions do not match configured model");
                String text = segments.get(index).text().strip();
                return new RagChunkDraft(index, text, checksum((document.checksum() + "\n" + index + "\n" + text)
                        .getBytes(StandardCharsets.UTF_8)),
                        vectorLiteral(vector));
            }).toList();
            repository.replaceDocument(document, chunks, embeddingModel, dimensions);
            importedCounter.increment();
            log.info("RAG document imported; source={}, chunks={}", source, chunks.size());
        } catch (Exception exception) {
            failureCounter.increment();
            log.error("RAG document import failed; path={}", path, exception);
        }
    }

    private boolean isSupported(Path path) { return SUPPORTED_EXTENSIONS.contains(extension(path)); }

    private String extension(Path path) {
        String name = path.getFileName().toString();
        int separator = name.lastIndexOf('.');
        return separator < 0 ? "" : name.substring(separator + 1).toLowerCase(java.util.Locale.ROOT);
    }

    private String checksum(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    static String vectorLiteral(double[] vector) {
        return "[" + java.util.Arrays.stream(vector).mapToObj(Double::toString)
                .collect(java.util.stream.Collectors.joining(",")) + "]";
    }
}
