package com.achobeta.refine.ai.ocr;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

import java.util.Locale;
import java.util.Set;

@Component
@ConfigurationProperties(prefix = "refine.ocr.upload")
public class OcrUploadProperties {
    private DataSize maxSize = DataSize.ofMegabytes(10);
    private Set<String> allowedTypes = Set.of(
            "txt", "text/plain", "md", "text/markdown",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "pdf", "application/pdf", "png", "image/png", "jpg", "jpeg", "image/jpeg", "webp", "image/webp");

    public DataSize getMaxSize() { return maxSize; }
    public void setMaxSize(DataSize maxSize) { this.maxSize = maxSize; }
    public Set<String> getAllowedTypes() { return allowedTypes; }
    public void setAllowedTypes(Set<String> allowedTypes) { this.allowedTypes = allowedTypes; }

    public boolean supports(String type) {
        if (type == null || type.isBlank()) return false;
        String normalized = type.trim().toLowerCase(Locale.ROOT);
        int queryIndex = normalized.indexOf('?');
        if (queryIndex >= 0) normalized = normalized.substring(0, queryIndex);
        int dotIndex = normalized.lastIndexOf('.');
        String extension = dotIndex >= 0 ? normalized.substring(dotIndex + 1) : normalized;
        return allowedTypes.contains(normalized) || allowedTypes.contains(extension);
    }
}
