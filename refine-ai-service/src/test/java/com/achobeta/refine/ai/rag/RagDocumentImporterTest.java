package com.achobeta.refine.ai.rag;

import com.achobeta.refine.ai.ocr.application.port.DocumentTextPort;
import com.achobeta.refine.ai.rag.application.port.RagRepository;
import com.achobeta.refine.ai.shared.application.port.TextEmbeddingPort;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RagDocumentImporterTest {
    @TempDir
    Path directory;

    @Test
    void importsChangedDocumentWithRemoteEmbedding() throws Exception {
        Path document = Files.writeString(directory.resolve("guide.md"), """
                ---
                title: 间隔重复
                subject: 学习方法
                chapter: 复习策略
                approved: true
                ---
                spaced repetition
                """);
        RagRepository repository = mock(RagRepository.class);
        DocumentTextPort extractor = mock(DocumentTextPort.class);
        when(extractor.extract(any(byte[].class), eq("md"))).thenReturn("""
                ---
                title: 间隔重复
                subject: 学习方法
                chapter: 复习策略
                approved: true
                ---
                spaced repetition
                """);

        importer(repository, extractor).importDocument(directory, document);

        verify(repository).replaceDocument(any(), any(), eq("text-embedding-v4"), eq(2));
    }

    @Test
    void restartSkipsCurrentChecksumAndModelWithoutEmbeddingCall() throws Exception {
        Path document = Files.writeString(directory.resolve("guide.md"), """
                ---
                title: 间隔重复
                approved: true
                ---
                spaced repetition
                """);
        RagRepository repository = mock(RagRepository.class);
        DocumentTextPort extractor = mock(DocumentTextPort.class);
        TextEmbeddingPort embeddings = embeddings();
        when(extractor.extract(any(byte[].class), eq("md"))).thenReturn("""
                ---
                title: 间隔重复
                approved: true
                ---
                spaced repetition
                """);
        when(repository.isCurrent(any(), eq("text-embedding-v4"), eq(2))).thenReturn(true);

        importer(repository, extractor, embeddings).importDocument(directory, document);

        verify(embeddings, never()).embed(anyString());
        verify(repository, never()).replaceDocument(any(), any(), anyString(), org.mockito.ArgumentMatchers.anyInt());
    }

    private RagDocumentImporter importer(RagRepository repository, DocumentTextPort extractor) {
        return importer(repository, extractor, embeddings());
    }

    private RagDocumentImporter importer(RagRepository repository, DocumentTextPort extractor,
                                         TextEmbeddingPort embeddings) {
        PgVectorProperties properties = new PgVectorProperties();
        properties.setDocumentPath(directory.toString());
        return new RagDocumentImporter(properties, repository, extractor, embeddings, new RagMetadataParser(),
                new SimpleMeterRegistry());
    }

    private TextEmbeddingPort embeddings() {
        TextEmbeddingPort embeddings = mock(TextEmbeddingPort.class);
        when(embeddings.modelName()).thenReturn("text-embedding-v4");
        when(embeddings.dimensions()).thenReturn(2);
        when(embeddings.embed(anyString())).thenReturn(new double[]{0.1D, 0.2D});
        when(embeddings.embedAll(anyList())).thenReturn(java.util.List.of(new double[]{0.1D, 0.2D}));
        return embeddings;
    }
}
