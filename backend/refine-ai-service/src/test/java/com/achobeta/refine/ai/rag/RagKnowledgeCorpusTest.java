package com.achobeta.refine.ai.rag;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class RagKnowledgeCorpusTest {
    private final RagMetadataParser parser = new RagMetadataParser();

    @Test
    void approvedKnowledgeDocumentsHaveTraceableMetadataAndContent() throws Exception {
        Path root = knowledgeRoot();
        try (Stream<Path> paths = Files.walk(root)) {
            List<Path> documents = paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".md"))
                    .toList();

            assertThat(documents).hasSizeGreaterThanOrEqualTo(7);
            for (Path document : documents) {
                String content = Files.readString(document, StandardCharsets.UTF_8);
                RagMetadataParser.ParsedRagDocument parsed = parser.parse(root.relativize(document), "test-checksum", content);
                assertThat(parsed.metadata().approved()).as(document.toString()).isTrue();
                assertThat(parsed.metadata().title()).as(document.toString()).isNotBlank();
                assertThat(parsed.metadata().subject()).as(document.toString()).isNotBlank();
                assertThat(parsed.metadata().chapter()).as(document.toString()).isNotBlank();
                assertThat(parsed.content()).as(document.toString()).hasSizeGreaterThan(200);
            }
        }
    }

    private Path knowledgeRoot() {
        Path moduleRelative = Path.of("..", "docs", "rag").toAbsolutePath().normalize();
        if (Files.isDirectory(moduleRelative)) {
            return moduleRelative;
        }
        return Path.of("backend", "docs", "rag").toAbsolutePath().normalize();
    }
}
