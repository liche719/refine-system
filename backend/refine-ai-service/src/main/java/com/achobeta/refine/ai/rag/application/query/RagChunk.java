package com.achobeta.refine.ai.rag.application.query;

public record RagChunk(long id, String content, RagDocumentMetadata document, double semanticScore,
                       double lexicalScore, double fusedScore) {
    public String referenceText() {
        return "【来源：" + document.citation() + "】\n" + content;
    }
}
