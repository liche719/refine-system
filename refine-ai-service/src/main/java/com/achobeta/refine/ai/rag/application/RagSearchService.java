package com.achobeta.refine.ai.rag.application;

import com.achobeta.refine.ai.rag.PgVectorProperties;
import com.achobeta.refine.ai.rag.application.port.RagRepository;
import com.achobeta.refine.ai.rag.application.query.RagChunk;
import com.achobeta.refine.ai.rag.application.query.RagSearchQuery;
import com.achobeta.refine.ai.shared.application.port.TextEmbeddingPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class RagSearchService {
    private static final Logger log = LoggerFactory.getLogger(RagSearchService.class);
    private final Optional<RagRepository> repository;
    private final TextEmbeddingPort embeddings;
    private final PgVectorProperties properties;

    public RagSearchService(Optional<RagRepository> repository, TextEmbeddingPort embeddings, PgVectorProperties properties) {
        this.repository = repository;
        this.embeddings = embeddings;
        this.properties = properties;
    }

    public List<RagChunk> search(String query, int requestedLimit) {
        if (!properties.isRetrievalEnabled() || repository.isEmpty() || query == null || query.isBlank()) return List.of();
        try {
            int resultLimit = Math.min(Math.max(requestedLimit, 1), properties.getResultLimit());
            double[] vector = embeddings.embed(query);
            RagSearchQuery semantic = new RagSearchQuery(query, vectorLiteral(vector), embeddings.modelName(), vector.length,
                    properties.getSemanticCandidates());
            RagSearchQuery lexical = new RagSearchQuery(query, semantic.vector(), semantic.embeddingModel(), semantic.dimensions(),
                    properties.getLexicalCandidates());
            return fuse(repository.orElseThrow().semanticSearch(semantic), repository.orElseThrow().lexicalSearch(lexical))
                    .stream().filter(chunk -> chunk.fusedScore() >= properties.getMinimumFusedScore())
                    .limit(resultLimit).toList();
        } catch (RuntimeException exception) {
            log.warn("RAG search degraded; queryLength={}", query.length(), exception);
            return List.of();
        }
    }

    private List<RagChunk> fuse(List<RagChunk> semantic, List<RagChunk> lexical) {
        Map<Long, RankedChunk> candidates = new LinkedHashMap<>();
        rank(semantic, true, candidates);
        rank(lexical, false, candidates);
        return candidates.values().stream().map(RankedChunk::toChunk)
                .sorted(Comparator.comparingDouble(RagChunk::fusedScore).reversed()).toList();
    }

    private void rank(List<RagChunk> source, boolean semantic, Map<Long, RankedChunk> candidates) {
        for (int index = 0; index < source.size(); index++) {
            RagChunk chunk = source.get(index);
            double rrf = 1D / (properties.getReciprocalRankConstant() + index + 1D);
            RankedChunk ranked = candidates.computeIfAbsent(chunk.id(), ignored -> new RankedChunk(chunk));
            ranked.add(semantic ? rrf : 0D, semantic ? chunk.semanticScore() : 0D,
                    semantic ? 0D : rrf, semantic ? 0D : chunk.lexicalScore());
        }
    }

    private String vectorLiteral(double[] vector) {
        return "[" + java.util.Arrays.stream(vector).mapToObj(Double::toString)
                .collect(java.util.stream.Collectors.joining(",")) + "]";
    }

    private static final class RankedChunk {
        private final RagChunk source;
        private double semanticRrf;
        private double lexicalRrf;
        private double semanticScore;
        private double lexicalScore;

        private RankedChunk(RagChunk source) { this.source = source; }

        private void add(double semanticRrf, double semanticScore, double lexicalRrf, double lexicalScore) {
            this.semanticRrf += semanticRrf;
            this.lexicalRrf += lexicalRrf;
            this.semanticScore = Math.max(this.semanticScore, semanticScore);
            this.lexicalScore = Math.max(this.lexicalScore, lexicalScore);
        }

        private RagChunk toChunk() {
            return new RagChunk(source.id(), source.content(), source.document(), semanticScore, lexicalScore,
                    semanticRrf + lexicalRrf);
        }
    }
}
