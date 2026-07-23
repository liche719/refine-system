package com.achobeta.refine.ai.rag;

import com.achobeta.refine.ai.rag.application.query.RagDocumentMetadata;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
class RagMetadataParser {
    ParsedRagDocument parse(Path sourcePath, String checksum, String extractedContent) {
        Map<String, String> fields = new LinkedHashMap<>();
        String content = extractedContent == null ? "" : extractedContent.strip();
        if (content.startsWith("---")) {
            int end = content.indexOf("\n---", 3);
            if (end > 0) {
                String header = content.substring(3, end).strip();
                for (String line : header.split("\\R")) {
                    int separator = line.indexOf(':');
                    if (separator > 0) fields.put(line.substring(0, separator).trim(), line.substring(separator + 1).trim());
                }
                content = content.substring(end + 4).strip();
            }
        }
        String fallbackTitle = sourcePath.getFileName().toString().replaceFirst("\\.[^.]+$", "");
        RagDocumentMetadata metadata = new RagDocumentMetadata(
                sourcePath.toString().replace('\\', '/'), checksum,
                fields.getOrDefault("title", fallbackTitle), fields.get("subject"), fields.get("grade"),
                fields.get("textbookVersion"), fields.get("chapter"), fields.get("section"),
                fields.get("pageReference"), Boolean.parseBoolean(fields.getOrDefault("approved", "false")));
        return new ParsedRagDocument(metadata, content);
    }

    record ParsedRagDocument(RagDocumentMetadata metadata, String content) { }
}
