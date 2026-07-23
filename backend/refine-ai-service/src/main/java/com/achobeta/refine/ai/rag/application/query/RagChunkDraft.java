package com.achobeta.refine.ai.rag.application.query;

public record RagChunkDraft(int chunkIndex, String content, String checksum, String embedding) { }
