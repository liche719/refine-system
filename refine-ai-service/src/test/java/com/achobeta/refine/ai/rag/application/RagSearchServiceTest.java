package com.achobeta.refine.ai.rag.application;

import com.achobeta.refine.ai.rag.PgVectorProperties;
import com.achobeta.refine.ai.rag.application.port.RagRepository;
import com.achobeta.refine.ai.rag.application.query.RagChunk;
import com.achobeta.refine.ai.rag.application.query.RagDocumentMetadata;
import com.achobeta.refine.ai.shared.application.port.TextEmbeddingPort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RagSearchServiceTest {
    @Test
    void fusesSemanticAndLexicalRanksAndKeepsCitationMetadata() {
        RagRepository repository = mock(RagRepository.class);
        TextEmbeddingPort embeddings = mock(TextEmbeddingPort.class);
        when(embeddings.embed("equation definition")).thenReturn(new double[]{0.1D, 0.2D});
        when(embeddings.modelName()).thenReturn("embedding-v1");
        RagChunk definition = chunk(1L, "An equation contains an unknown value.");
        RagChunk practice = chunk(2L, "Solve a linear equation by inverse operations.");
        when(repository.semanticSearch(any())).thenReturn(List.of(definition, practice));
        when(repository.lexicalSearch(any())).thenReturn(List.of(practice, definition));

        List<RagChunk> result = new RagSearchService(Optional.of(repository), embeddings, properties())
                .search("equation definition", 3);

        assertThat(result).extracting(RagChunk::id).containsExactly(1L, 2L);
        assertThat(result.getFirst().referenceText()).contains("【来源：Algebra · Chapter 3】");
        verify(repository).semanticSearch(any());
        verify(repository).lexicalSearch(any());
    }

    @Test
    void disablesRetrievalWithoutCallingTheEmbeddingProvider() {
        RagRepository repository = mock(RagRepository.class);
        TextEmbeddingPort embeddings = mock(TextEmbeddingPort.class);
        PgVectorProperties properties = properties();
        properties.setRetrievalEnabled(false);

        assertThat(new RagSearchService(Optional.of(repository), embeddings, properties).search("equation", 3)).isEmpty();

        verifyNoInteractions(repository, embeddings);
    }

    private RagChunk chunk(long id, String content) {
        return new RagChunk(id, content, new RagDocumentMetadata("math.md", "a".repeat(64), "Algebra", "Mathematics",
                "Grade 7", "PEP", "Chapter 3", "", "", true), 0.9D, 0.7D, 0D);
    }

    private PgVectorProperties properties() {
        PgVectorProperties properties = new PgVectorProperties();
        properties.setMinimumFusedScore(0D);
        return properties;
    }
}
