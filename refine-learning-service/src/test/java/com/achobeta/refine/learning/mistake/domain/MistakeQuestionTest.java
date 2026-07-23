package com.achobeta.refine.learning.mistake.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MistakeQuestionTest {
    private final MistakeQuestion question = new MistakeQuestion(1L, "u1", "q1", "content", "math",
            0, 0, 0, 0, 0, null, 1, null, 0, "manual", null, null);

    @Test
    void togglesReasonsWithoutChangingOtherState() {
        MistakeQuestion updated = question.toggle(MistakeReason.CARELESS)
                .toggle(MistakeReason.CALCULATION_ERROR);

        assertThat(updated.isCareless()).isEqualTo(1);
        assertThat(updated.isCalculateErr()).isEqualTo(1);
        assertThat(updated.isUnfamiliar()).isZero();
        assertThat(updated.questionId()).isEqualTo("q1");
    }

    @Test
    void otherReasonRequiresSelectionAndClearsTextWhenDisabled() {
        assertThatThrownBy(() -> question.changeOtherReason("missed condition"))
                .isInstanceOf(IllegalArgumentException.class);
        MistakeQuestion withReason = question.toggle(MistakeReason.OTHER)
                .changeOtherReason("  missed condition  ");
        MistakeQuestion cleared = withReason.toggle(MistakeReason.OTHER);

        assertThat(withReason.otherReasonFlag()).isEqualTo(1);
        assertThat(withReason.otherReason()).isEqualTo("missed condition");
        assertThat(cleared.otherReasonFlag()).isZero();
        assertThat(cleared.otherReason()).isNull();
        assertThatThrownBy(() -> withReason.changeOtherReason(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void noteMustNotBeBlankAndReviewedTransitionIsIdempotent() {
        assertThatThrownBy(() -> question.changeNote(" ")).isInstanceOf(IllegalArgumentException.class);
        MistakeQuestion reviewed = question.changeNote(" revise formula ").markReviewed();
        assertThat(reviewed.studyNote()).isEqualTo("revise formula");
        assertThat(reviewed.questionStatus()).isEqualTo(1);
        assertThat(reviewed.markReviewed()).isSameAs(reviewed);
    }
}
