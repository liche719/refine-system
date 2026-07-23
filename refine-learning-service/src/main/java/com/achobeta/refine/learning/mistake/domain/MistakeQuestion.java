package com.achobeta.refine.learning.mistake.domain;

import java.time.LocalDateTime;

public record MistakeQuestion(
        Long id,
        String userId,
        String questionId,
        String questionContent,
        String subject,
        Integer isCareless,
        Integer isUnfamiliar,
        Integer isCalculateErr,
        Integer isTimeShortage,
        Integer otherReasonFlag,
        String otherReason,
        Integer knowledgePointId,
        String studyNote,
        Integer questionStatus,
        String source,
        LocalDateTime createTime,
        LocalDateTime updateTime) {

    public MistakeQuestion toggle(MistakeReason reason) {
        return switch (reason) {
            case CARELESS -> copy(flip(isCareless), isUnfamiliar, isCalculateErr, isTimeShortage,
                    otherReasonFlag, otherReason, studyNote, questionStatus);
            case UNFAMILIAR -> copy(isCareless, flip(isUnfamiliar), isCalculateErr, isTimeShortage,
                    otherReasonFlag, otherReason, studyNote, questionStatus);
            case CALCULATION_ERROR -> copy(isCareless, isUnfamiliar, flip(isCalculateErr), isTimeShortage,
                    otherReasonFlag, otherReason, studyNote, questionStatus);
            case TIME_SHORTAGE -> copy(isCareless, isUnfamiliar, isCalculateErr, flip(isTimeShortage),
                    otherReasonFlag, otherReason, studyNote, questionStatus);
            case OTHER -> {
                int nextFlag = flip(otherReasonFlag);
                yield copy(isCareless, isUnfamiliar, isCalculateErr, isTimeShortage,
                        nextFlag, nextFlag == 0 ? null : otherReason, studyNote, questionStatus);
            }
        };
    }

    public MistakeQuestion changeOtherReason(String text) {
        if (otherReasonFlag == null || otherReasonFlag != 1) {
            throw new IllegalArgumentException("other reason must be selected before updating its text");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("other reason text must not be blank");
        }
        String normalized = text.trim();
        return copy(isCareless, isUnfamiliar, isCalculateErr, isTimeShortage,
                1, normalized, studyNote, questionStatus);
    }

    public MistakeQuestion changeOtherReasonText(String text) {
        if (text == null || otherReasonFlag == null || otherReasonFlag != 1) {
            return this;
        }
        if (text.isBlank()) {
            throw new IllegalArgumentException("other reason text must not be blank");
        }
        return copy(isCareless, isUnfamiliar, isCalculateErr, isTimeShortage,
                otherReasonFlag, text.trim(), studyNote, questionStatus);
    }

    public MistakeQuestion changeNote(String note) {
        if (note == null || note.isBlank()) {
            throw new IllegalArgumentException("study note must not be blank");
        }
        return copy(isCareless, isUnfamiliar, isCalculateErr, isTimeShortage,
                otherReasonFlag, otherReason, note.trim(), questionStatus);
    }

    public MistakeQuestion markReviewed() {
        return questionStatus == 1 ? this : copy(isCareless, isUnfamiliar, isCalculateErr, isTimeShortage,
                otherReasonFlag, otherReason, studyNote, 1);
    }

    private MistakeQuestion copy(Integer careless, Integer unfamiliar, Integer calculationError,
                                 Integer timeShortage, Integer otherFlag, String otherText,
                                 String note, Integer status) {
        return new MistakeQuestion(id, userId, questionId, questionContent, subject, careless, unfamiliar,
                calculationError, timeShortage, otherFlag, otherText, knowledgePointId, note, status,
                source, createTime, updateTime);
    }

    private static int flip(Integer value) {
        return value != null && value == 1 ? 0 : 1;
    }
}
