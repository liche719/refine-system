package com.achobeta.refine.ai.stream;

import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class SseSupport {
    private static final String STREAM_ERROR_MESSAGE = "> AI 服务暂时不可用，请稍后重试。";
    private static final MediaType UTF8_TEXT = new MediaType("text", "plain", StandardCharsets.UTF_8);

    private SseSupport() { }

    public static SseEmitter emitStreaming(Executor executor, StreamingOperation operation) {
        SseEmitter emitter = new Utf8SseEmitter(120_000L);
        AtomicBoolean closed = new AtomicBoolean();
        executor.execute(() -> {
            try {
                operation.start(token -> send(emitter, closed, token),
                        () -> complete(emitter, closed), error -> fail(emitter, closed, error));
            } catch (Throwable error) {
                fail(emitter, closed, error);
            }
        });
        emitter.onTimeout(() -> closed.set(true));
        emitter.onError(ignored -> closed.set(true));
        emitter.onCompletion(() -> closed.set(true));
        return emitter;
    }

    private static void send(SseEmitter emitter, AtomicBoolean closed, String token) {
        if (closed.get() || token == null || token.isEmpty()) return;
        try {
            emitter.send(SseEmitter.event().data(token, UTF8_TEXT));
        } catch (Exception exception) {
            closed.set(true);
        }
    }

    private static void complete(SseEmitter emitter, AtomicBoolean closed) {
        if (closed.compareAndSet(false, true)) emitter.complete();
    }

    private static void fail(SseEmitter emitter, AtomicBoolean closed, Throwable error) {
        if (!closed.compareAndSet(false, true)) return;
        try {
            emitter.send(SseEmitter.event().name("error").data(STREAM_ERROR_MESSAGE, UTF8_TEXT));
        } catch (Exception ignored) {
        } finally {
            emitter.complete();
        }
    }

    @FunctionalInterface
    public interface StreamingOperation {
        void start(Consumer<String> onToken, Runnable onComplete, Consumer<Throwable> onError);
    }

    private static final class Utf8SseEmitter extends SseEmitter {
        private Utf8SseEmitter(Long timeout) {
            super(timeout);
        }

        @Override
        protected void extendResponse(ServerHttpResponse outputMessage) {
            super.extendResponse(outputMessage);
            outputMessage.getHeaders().setContentType(
                    new MediaType("text", "event-stream", StandardCharsets.UTF_8));
        }
    }
}
