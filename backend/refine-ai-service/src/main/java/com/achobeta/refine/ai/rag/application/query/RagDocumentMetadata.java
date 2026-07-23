package com.achobeta.refine.ai.rag.application.query;

import dev.langchain4j.data.document.Metadata;

import java.util.Objects;

public record RagDocumentMetadata(String sourcePath, String checksum, String title, String subject, String grade,
                                  String textbookVersion, String chapter, String section, String pageReference,
                                  boolean approved) {
    public RagDocumentMetadata {
        sourcePath = required(sourcePath, "sourcePath");
        checksum = required(checksum, "checksum");
        title = required(title, "title");
        subject = value(subject);
        grade = value(grade);
        textbookVersion = value(textbookVersion);
        chapter = value(chapter);
        section = value(section);
        pageReference = value(pageReference);
    }

    public Metadata toLangChainMetadata() {
        return Metadata.from(java.util.Map.of(
                "sourcePath", sourcePath,
                "title", title,
                "subject", subject,
                "grade", grade,
                "textbookVersion", textbookVersion,
                "chapter", chapter,
                "section", section,
                "pageReference", pageReference));
    }

    public String citation() {
        String location = section.isBlank() ? chapter : chapter + " · " + section;
        if (!pageReference.isBlank()) location = location.isBlank() ? "第" + pageReference : location + " · 第" + pageReference;
        return location.isBlank() ? title : title + " · " + location;
    }

    private static String required(String value, String name) {
        String normalized = value(value);
        if (normalized.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return normalized;
    }

    private static String value(String value) {
        return Objects.requireNonNullElse(value, "").trim();
    }
}
