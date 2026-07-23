package com.achobeta.refine.ai.ocr.infrastructure;

import com.achobeta.refine.ai.ocr.application.port.ImageOcrPort;
import com.achobeta.refine.ai.shared.infrastructure.ProviderErrorSanitizer;
import com.achobeta.refine.common.api.AppException;
import dev.langchain4j.data.message.ImageContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
public class LangChain4jImageOcrProvider implements ImageOcrPort {
    private static final Logger log = LoggerFactory.getLogger(LangChain4jImageOcrProvider.class);
    private final ImageOcrAiService assistant;

    public LangChain4jImageOcrProvider(ImageOcrAiService assistant) {
        this.assistant = assistant;
    }

    @Override
    public String recognize(byte[] imageBytes) {
        try {
            ImageContent image = ImageContent.from(
                    Base64.getEncoder().encodeToString(imageBytes), mediaType(imageBytes));
            String text = assistant.recognize(image);
            if (text == null || text.isBlank()) {
                throw new AppException(10003, "OCR 返回内容为空");
            }
            return text;
        } catch (AppException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("OCR provider call failed; errorType={}, detail={}",
                    exception.getClass().getSimpleName(), ProviderErrorSanitizer.sanitize(exception));
            throw new AppException(10003, "OCR 服务暂时不可用");
        }
    }

    private String mediaType(byte[] bytes) {
        if (bytes.length >= 8 && bytes[0] == (byte) 0x89 && bytes[1] == 0x50) return "image/png";
        if (bytes.length >= 3 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8) return "image/jpeg";
        if (bytes.length >= 6 && bytes[0] == 0x47 && bytes[1] == 0x49) return "image/gif";
        if (bytes.length >= 12 && bytes[8] == 0x57 && bytes[9] == 0x45) return "image/webp";
        return "image/png";
    }
}
