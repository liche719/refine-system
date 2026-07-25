package com.achobeta.refine.ai.shared.infrastructure;

import com.achobeta.refine.common.api.AppException;
import dev.langchain4j.service.TokenStream;
import org.slf4j.Logger;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** Shared error and streaming semantics for outbound LangChain4j adapters. */
public final class LangChain4jCallSupport {
    private LangChain4jCallSupport() {
    }

    public static String complete(String capability, Supplier<String> invocation, Logger log) {
        try {
            String content = invocation.get();
            if (content == null || content.isBlank()) {
                throw new AppException(10001, "AI 返回内容为空");
            }
            return content;
        } catch (AppException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("{} AI request failed; errorType={}, detail={}", capability,
                    exception.getClass().getSimpleName(), ProviderErrorSanitizer.sanitize(exception));
            throw new AppException(10001, "AI 服务暂时不可用");
        }
    }

    public static void stream(Supplier<TokenStream> invocation, Consumer<String> onToken,
                              Runnable onComplete, Consumer<Throwable> onError) {
        try {
            invocation.get()
                    .onPartialResponse(onToken)
                    .onCompleteResponse(ignored -> onComplete.run())
                    .onError(onError)
                    .start();
        } catch (Throwable error) {
            onError.accept(error);
        }
    }

    public static String context(String value) {
        return value == null || value.isBlank() ? "（暂无）" : value;
    }
}
