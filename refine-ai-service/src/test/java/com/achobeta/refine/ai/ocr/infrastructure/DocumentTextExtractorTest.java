package com.achobeta.refine.ai.ocr.infrastructure;

import com.achobeta.refine.ai.ocr.application.port.ImageOcrPort;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class DocumentTextExtractorTest {

    @ParameterizedTest
    @ValueSource(strings = {"md", ".md", "guide.md", "text/markdown"})
    void readsMarkdownAsUtf8TextWithoutCallingOcr(String fileType) {
        ImageOcrPort ocr = mock(ImageOcrPort.class);
        DocumentTextExtractor extractor = new DocumentTextExtractor(ocr);
        String markdown = "# 错题复习\n\n2 + 3 = ?";

        String extracted = extractor.extract(markdown.getBytes(StandardCharsets.UTF_8), fileType);

        assertThat(extracted).isEqualTo(markdown);
        verifyNoInteractions(ocr);
    }
}
