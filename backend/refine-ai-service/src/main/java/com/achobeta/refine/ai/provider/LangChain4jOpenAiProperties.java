package com.achobeta.refine.ai.provider;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "langchain4j.open-ai")
public record LangChain4jOpenAiProperties(
        ChatModelProperties chatModel,
        ChatModelProperties streamingChatModel,
        EmbeddingModelProperties embeddingModel) {

    public LangChain4jOpenAiProperties {
        if (chatModel == null || streamingChatModel == null || embeddingModel == null) {
            throw new IllegalStateException("LangChain4j OpenAI chat, streaming and embedding configuration is required");
        }
    }

    public record ChatModelProperties(String baseUrl, String apiKey, String modelName,
                                      String reasoningEffort,
                                      Duration timeout, Integer maxRetries,
                                      Boolean logRequests, Boolean logResponses) {
        public ChatModelProperties {
            require(baseUrl, "base-url");
            require(apiKey, "api-key");
            require(modelName, "model-name");
            require(reasoningEffort, "reasoning-effort");
            requireTimeout(timeout);
        }
    }

    public record EmbeddingModelProperties(String baseUrl, String apiKey, String modelName,
                                           Integer dimensions, Duration timeout, Integer maxRetries,
                                           Boolean logRequests, Boolean logResponses) {
        public EmbeddingModelProperties {
            require(baseUrl, "embedding base-url");
            require(apiKey, "embedding api-key");
            require(modelName, "embedding model-name");
            if (dimensions == null || dimensions <= 0) {
                throw new IllegalStateException("embedding dimensions must be positive");
            }
            requireTimeout(timeout);
        }
    }

    private static void require(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalStateException(name + " is required");
    }

    private static void requireTimeout(Duration timeout) {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalStateException("timeout must be positive");
        }
    }
}
