package com.achobeta.refine.ai.conversation.application.port;

import java.util.function.Consumer;

public interface ConversationAiPort {
    String reply(String memoryId, String references, String message);

    void streamReply(String memoryId, String references, String message,
                     Consumer<String> onToken, Runnable onComplete, Consumer<Throwable> onError);

    boolean clearMemory(String memoryId);
}
