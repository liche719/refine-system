package com.achobeta.refine.ai.ocr;

import com.achobeta.refine.ai.ocr.infrastructure.OcrProviderProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OcrProviderPropertiesTest {
    @Test
    void rejectsStubAndMissingRemoteCredentials() {
        assertThatThrownBy(() -> new OcrProviderProperties("stub", "https://ocr.example", "vision-model",
                        "key", java.time.Duration.ofSeconds(30), 2))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("provider");
        assertThatThrownBy(() -> new OcrProviderProperties("openai", "https://ocr.example", "vision-model",
                        "", java.time.Duration.ofSeconds(30), 2))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OCR_API_KEY");
    }
}
