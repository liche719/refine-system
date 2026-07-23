package com.achobeta.refine.ai.rag;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class RagMetadataParserTest {
    private final RagMetadataParser parser = new RagMetadataParser();

    @Test
    void parsesApprovedTextbookFrontMatterAndRemovesItFromContent() {
        var parsed = parser.parse(Path.of("math/linear-equations.md"), "a".repeat(64), """
                ---
                title: Linear equations
                subject: Mathematics
                grade: Grade 7
                textbookVersion: PEP
                chapter: Chapter 3
                section: 3.1
                pageReference: 82
                approved: true
                ---
                An equation has an unknown value.
                """);

        assertThat(parsed.metadata().approved()).isTrue();
        assertThat(parsed.metadata().citation()).isEqualTo("Linear equations · Chapter 3 · 3.1 · 第82");
        assertThat(parsed.content()).isEqualTo("An equation has an unknown value.");
    }

    @Test
    void treatsUnmarkedDocumentsAsUnapproved() {
        var parsed = parser.parse(Path.of("draft.md"), "b".repeat(64), "unreviewed content");

        assertThat(parsed.metadata().approved()).isFalse();
        assertThat(parsed.content()).isEqualTo("unreviewed content");
    }
}
