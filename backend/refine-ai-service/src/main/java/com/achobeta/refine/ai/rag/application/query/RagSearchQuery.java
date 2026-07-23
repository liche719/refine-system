package com.achobeta.refine.ai.rag.application.query;

public record RagSearchQuery(String query, String vector, String embeddingModel, int dimensions,
                             int candidateLimit) { }
