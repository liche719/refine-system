package com.achobeta.refine.ai.rag;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "refine.pgvector")
public class PgVectorProperties {
    private boolean enabled = true;
    private boolean retrievalEnabled = true;
    private String url;
    private String username;
    private String password;
    private String documentPath = "./docs/rag";
    private int chunkSize = 450;
    private int chunkOverlap = 80;
    private int semanticCandidates = 12;
    private int lexicalCandidates = 12;
    private int resultLimit = 3;
    private double minimumFusedScore = 0.016;
    private int reciprocalRankConstant = 60;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isRetrievalEnabled() { return retrievalEnabled; }
    public void setRetrievalEnabled(boolean retrievalEnabled) { this.retrievalEnabled = retrievalEnabled; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getDocumentPath() { return documentPath; }
    public void setDocumentPath(String documentPath) { this.documentPath = documentPath; }
    public int getChunkSize() { return chunkSize; }
    public void setChunkSize(int chunkSize) { this.chunkSize = positive(chunkSize, "chunkSize"); }
    public int getChunkOverlap() { return chunkOverlap; }
    public void setChunkOverlap(int chunkOverlap) { this.chunkOverlap = positive(chunkOverlap, "chunkOverlap"); }
    public int getSemanticCandidates() { return semanticCandidates; }
    public void setSemanticCandidates(int semanticCandidates) { this.semanticCandidates = positive(semanticCandidates, "semanticCandidates"); }
    public int getLexicalCandidates() { return lexicalCandidates; }
    public void setLexicalCandidates(int lexicalCandidates) { this.lexicalCandidates = positive(lexicalCandidates, "lexicalCandidates"); }
    public int getResultLimit() { return resultLimit; }
    public void setResultLimit(int resultLimit) { this.resultLimit = positive(resultLimit, "resultLimit"); }
    public double getMinimumFusedScore() { return minimumFusedScore; }
    public void setMinimumFusedScore(double minimumFusedScore) { this.minimumFusedScore = minimumFusedScore; }
    public int getReciprocalRankConstant() { return reciprocalRankConstant; }
    public void setReciprocalRankConstant(int reciprocalRankConstant) { this.reciprocalRankConstant = positive(reciprocalRankConstant, "reciprocalRankConstant"); }

    private int positive(int value, String name) {
        if (value < 1) throw new IllegalArgumentException("refine.pgvector." + name + " must be positive");
        return value;
    }
}
