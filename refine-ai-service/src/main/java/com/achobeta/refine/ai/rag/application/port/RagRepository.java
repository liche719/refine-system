package com.achobeta.refine.ai.rag.application.port;

import com.achobeta.refine.ai.rag.application.query.RagChunk;
import com.achobeta.refine.ai.rag.application.query.RagChunkDraft;
import com.achobeta.refine.ai.rag.application.query.RagDocumentMetadata;
import com.achobeta.refine.ai.rag.application.query.RagSearchQuery;

import java.util.List;

public interface RagRepository {
    void initializeSchema(int dimensions);
    boolean isCurrent(RagDocumentMetadata document, String embeddingModel, int dimensions);
    void replaceDocument(RagDocumentMetadata document, List<RagChunkDraft> chunks, String embeddingModel, int dimensions);
    List<RagChunk> semanticSearch(RagSearchQuery query);
    List<RagChunk> lexicalSearch(RagSearchQuery query);
    boolean ping();
}
