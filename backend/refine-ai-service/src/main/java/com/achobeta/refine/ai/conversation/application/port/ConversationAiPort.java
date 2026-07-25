package com.achobeta.refine.ai.conversation.application.port;

import java.util.function.Consumer;

public interface ConversationAiPort {
    String reply(String memoryId, String message);

    void streamReply(String memoryId, String message,
                     Consumer<String> onToken, Runnable onComplete, Consumer<Throwable> onError);

    String replyWithQuestion(String memoryId, String questionContent, String message);

    void streamReplyWithQuestion(String memoryId, String questionContent, String message,
                                 Consumer<String> onToken, Runnable onComplete, Consumer<Throwable> onError);

    boolean clearMemory(String memoryId);
}
