package com.achobeta.refine.ai.rag;

import com.achobeta.refine.ai.rag.application.port.RagRepository;
import com.achobeta.refine.ai.rag.application.query.RagChunk;
import com.achobeta.refine.ai.rag.application.query.RagChunkDraft;
import com.achobeta.refine.ai.rag.application.query.RagDocumentMetadata;
import com.achobeta.refine.ai.rag.application.query.RagSearchQuery;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@ConditionalOnProperty(prefix = "refine.pgvector", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RagDocumentRepository implements RagRepository {
    private final JdbcTemplate jdbcTemplate;

    public RagDocumentRepository(@Qualifier("pgVectorJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void initializeSchema(int dimensions) {
        if (dimensions < 1 || dimensions > 2_000) {
            throw new IllegalArgumentException("Embedding dimensions must be between 1 and 2000");
        }
        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS pg_trgm");
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS rag_documents (
                    id BIGSERIAL PRIMARY KEY,
                    source_path TEXT NOT NULL UNIQUE,
                    checksum CHAR(64) NOT NULL,
                    title TEXT NOT NULL,
                    subject TEXT NOT NULL DEFAULT '',
                    grade TEXT NOT NULL DEFAULT '',
                    textbook_version TEXT NOT NULL DEFAULT '',
                    chapter TEXT NOT NULL DEFAULT '',
                    section TEXT NOT NULL DEFAULT '',
                    page_reference TEXT NOT NULL DEFAULT '',
                    embedding_model TEXT NOT NULL,
                    embedding_dimensions INT NOT NULL,
                    indexed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                    UNIQUE(checksum, embedding_model, embedding_dimensions)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS rag_chunks (
                    id BIGSERIAL PRIMARY KEY,
                    document_id BIGINT NOT NULL REFERENCES rag_documents(id) ON DELETE CASCADE,
                    chunk_index INT NOT NULL,
                    checksum CHAR(64) NOT NULL,
                    content TEXT NOT NULL,
                    embedding vector(%d) NOT NULL,
                    embedding_model TEXT NOT NULL,
                    embedding_dimensions INT NOT NULL,
                    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                    UNIQUE(document_id, chunk_index),
                    UNIQUE(checksum, embedding_model, embedding_dimensions)
                )
                """.formatted(dimensions));
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_rag_chunks_embedding_hnsw "
                + "ON rag_chunks USING hnsw (embedding vector_cosine_ops)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_rag_chunks_content_trgm "
                + "ON rag_chunks USING gin (content gin_trgm_ops)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_rag_chunks_model "
                + "ON rag_chunks (embedding_model, embedding_dimensions)");
    }

    @Override
    public boolean isCurrent(RagDocumentMetadata document, String embeddingModel, int dimensions) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*) FROM rag_documents
                        WHERE checksum=? AND embedding_model=? AND embedding_dimensions=?
                        """, Integer.class, document.checksum(), embeddingModel, dimensions);
        return count != null && count > 0;
    }

    @Override
    public void replaceDocument(RagDocumentMetadata document, List<RagChunkDraft> chunks,
                                String embeddingModel, int dimensions) {
        Long documentId = jdbcTemplate.queryForObject("""
                        INSERT INTO rag_documents(source_path,checksum,title,subject,grade,textbook_version,chapter,section,
                                                  page_reference,embedding_model,embedding_dimensions)
                        VALUES(?,?,?,?,?,?,?,?,?,?,?)
                        ON CONFLICT(source_path) DO UPDATE SET
                          checksum=EXCLUDED.checksum, title=EXCLUDED.title, subject=EXCLUDED.subject, grade=EXCLUDED.grade,
                          textbook_version=EXCLUDED.textbook_version, chapter=EXCLUDED.chapter, section=EXCLUDED.section,
                          page_reference=EXCLUDED.page_reference, embedding_model=EXCLUDED.embedding_model,
                          embedding_dimensions=EXCLUDED.embedding_dimensions, updated_at=NOW()
                        RETURNING id
                        """, Long.class, document.sourcePath(), document.checksum(), document.title(), document.subject(),
                document.grade(), document.textbookVersion(), document.chapter(), document.section(), document.pageReference(),
                embeddingModel, dimensions);
        if (documentId == null) throw new IllegalStateException("RAG document id was not returned");
        jdbcTemplate.update("DELETE FROM rag_chunks WHERE document_id=?", documentId);
        jdbcTemplate.batchUpdate("""
                        INSERT INTO rag_chunks(document_id,chunk_index,checksum,content,embedding,embedding_model,embedding_dimensions)
                        VALUES(?,?,?,?,CAST(? AS vector),?,?)
                        """, chunks, 100, (statement, chunk) -> {
            statement.setLong(1, documentId);
            statement.setInt(2, chunk.chunkIndex());
            statement.setString(3, chunk.checksum());
            statement.setString(4, chunk.content());
            statement.setString(5, chunk.embedding());
            statement.setString(6, embeddingModel);
            statement.setInt(7, dimensions);
        });
    }

    @Override
    public List<RagChunk> semanticSearch(RagSearchQuery query) {
        return jdbcTemplate.query("""
                        SELECT c.id,c.content,d.source_path,d.checksum,d.title,d.subject,d.grade,d.textbook_version,
                               d.chapter,d.section,d.page_reference,
                               1-(c.embedding <=> CAST(? AS vector)) AS semantic_score
                        FROM rag_chunks c JOIN rag_documents d ON d.id=c.document_id
                        WHERE c.embedding_model=? AND c.embedding_dimensions=?
                        ORDER BY c.embedding <=> CAST(? AS vector)
                        LIMIT ?
                        """, (resultSet, rowNum) -> map(resultSet.getLong("id"), resultSet.getString("content"),
                        document(resultSet), resultSet.getDouble("semantic_score"), 0D),
                query.vector(), query.embeddingModel(), query.dimensions(), query.vector(), query.candidateLimit());
    }

    @Override
    public List<RagChunk> lexicalSearch(RagSearchQuery query) {
        return jdbcTemplate.query("""
                        SELECT c.id,c.content,d.source_path,d.checksum,d.title,d.subject,d.grade,d.textbook_version,
                               d.chapter,d.section,d.page_reference,
                               GREATEST(similarity(c.content, ?), word_similarity(?, c.content)) AS lexical_score
                        FROM rag_chunks c JOIN rag_documents d ON d.id=c.document_id
                        WHERE c.embedding_model=? AND c.embedding_dimensions=?
                        ORDER BY lexical_score DESC
                        LIMIT ?
                        """, (resultSet, rowNum) -> map(resultSet.getLong("id"), resultSet.getString("content"),
                        document(resultSet), 0D, resultSet.getDouble("lexical_score")),
                query.query(), query.query(), query.embeddingModel(), query.dimensions(), query.candidateLimit());
    }

    @Override
    public boolean ping() {
        Integer value = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        return value != null && value == 1;
    }

    private RagChunk map(long id, String content, RagDocumentMetadata document, double semanticScore, double lexicalScore) {
        return new RagChunk(id, content, document, semanticScore, lexicalScore, 0D);
    }

    private RagDocumentMetadata document(java.sql.ResultSet resultSet) throws java.sql.SQLException {
        return new RagDocumentMetadata(resultSet.getString("source_path"), resultSet.getString("checksum"),
                resultSet.getString("title"), resultSet.getString("subject"), resultSet.getString("grade"),
                resultSet.getString("textbook_version"), resultSet.getString("chapter"), resultSet.getString("section"),
                resultSet.getString("page_reference"), true);
    }
}
