package com.achobeta.refine.learning.mistake.domain;

import java.util.Locale;

public enum MistakeReason {
    CARELESS,
    UNFAMILIAR,
    CALCULATION_ERROR,
    TIME_SHORTAGE,
    OTHER;

    public static MistakeReason fromApiValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("reason name is required");
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "careless", "iscareless", "\u7c97\u5fc3\u9a6c\u864e" -> CARELESS;
            case "unfamiliar", "isunfamiliar", "\u77e5\u8bc6\u70b9\u4e0d\u719f\u6089" -> UNFAMILIAR;
            case "calculateerr", "iscalculateerr", "calculationerror", "\u8ba1\u7b97\u9519\u8bef" -> CALCULATION_ERROR;
            case "timeshortage", "istimeshortage", "\u65f6\u95f4\u4e0d\u591f" -> TIME_SHORTAGE;
            case "otherreason", "other" -> OTHER;
            default -> throw new IllegalArgumentException("unknown mistake reason: " + value);
        };
    }
}
