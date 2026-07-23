package com.achobeta.refine.ai.ocr.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "refine.ocr")
public record OcrProviderProperties(String provider, String baseUrl, String modelName,
                                    String apiKey, Duration timeout, int maxRetries) {
    public OcrProviderProperties {
        if (!"openai".equalsIgnoreCase(provider) && !"openai-compatible".equalsIgnoreCase(provider)) {
            throw new IllegalStateException("refine.ocr.provider must be openai or openai-compatible");
        }
        require(baseUrl, "refine.ocr.base-url");
        require(modelName, "refine.ocr.model-name");
        require(apiKey, "OCR_API_KEY");
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalStateException("refine.ocr.timeout must be positive");
        }
        if (maxRetries < 0 || maxRetries > 5) {
            throw new IllegalStateException("refine.ocr.max-retries must be between 0 and 5");
        }
    }

    private static void require(String value, String variable) {
        if (value == null || value.isBlank()) throw new IllegalStateException(variable + " is required");
    }
}
