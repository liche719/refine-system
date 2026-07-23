package com.achobeta.refine.ai.shared.infrastructure;

public final class ProviderErrorSanitizer {
    private static final int MAX_DETAIL_LENGTH = 1000;

    private ProviderErrorSanitizer() {
    }

    public static String sanitize(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) return "<no detail>";
        String sanitized = message
                .replaceAll("(?i)Bearer\\s+[^\\s,;]+", "Bearer [REDACTED]")
                .replaceAll("sk-[A-Za-z0-9_-]+", "sk-[REDACTED]")
                .replaceAll("data:image/[A-Za-z0-9.+-]+;base64,[A-Za-z0-9+/=]+",
                        "data:image/[REDACTED]");
        return sanitized.length() <= MAX_DETAIL_LENGTH
                ? sanitized
                : sanitized.substring(0, MAX_DETAIL_LENGTH) + "...";
    }
}
