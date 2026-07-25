package com.achobeta.refine.ai.conversation.infrastructure;

import com.achobeta.refine.ai.conversation.application.port.ConversationAiPort;
import com.achobeta.refine.ai.provider.RefineConversationAiService;
import com.achobeta.refine.ai.shared.infrastructure.LangChain4jCallSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
public class LangChain4jConversationAdapter implements ConversationAiPort {
    private static final Logger log = LoggerFactory.getLogger(LangChain4jConversationAdapter.class);

    private final RefineConversationAiService assistant;
    private final RedisChatMemoryStore memoryStore;

    public LangChain4jConversationAdapter(RefineConversationAiService assistant, RedisChatMemoryStore memoryStore) {
        this.assistant = assistant;
        this.memoryStore = memoryStore;
    }

    @Override
    public String reply(String memoryId, String message) {
        try {
            return LangChain4jCallSupport.complete("conversation", () -> assistant.conversation(memoryId, message), log);
        } finally {
            assistant.evictChatMemory(memoryId);
        }
    }

    @Override
    public void streamReply(String memoryId, String message, Consumer<String> onToken,
                            Runnable onComplete, Consumer<Throwable> onError) {
        LangChain4jCallSupport.stream(() -> assistant.streamConversation(memoryId, message), onToken,
                () -> complete(memoryId, onComplete), error -> fail(memoryId, error, onError));
    }

    @Override
    public String replyWithQuestion(String memoryId, String questionContent, String message) {
        try {
            return LangChain4jCallSupport.complete("question-context conversation",
                    () -> assistant.conversationWithQuestion(memoryId, LangChain4jCallSupport.context(questionContent), message), log);
        } finally {
            assistant.evictChatMemory(memoryId);
        }
    }

    @Override
    public void streamReplyWithQuestion(String memoryId, String questionContent, String message, Consumer<String> onToken,
                                        Runnable onComplete, Consumer<Throwable> onError) {
        LangChain4jCallSupport.stream(
                () -> assistant.streamConversationWithQuestion(memoryId, LangChain4jCallSupport.context(questionContent), message),
                onToken, () -> complete(memoryId, onComplete), error -> fail(memoryId, error, onError));
    }

    @Override
    public boolean clearMemory(String memoryId) {
        assistant.evictChatMemory(memoryId);
        return memoryStore.delete(memoryId);
    }

    private void complete(String memoryId, Runnable onComplete) {
        assistant.evictChatMemory(memoryId);
        onComplete.run();
    }

    private void fail(String memoryId, Throwable error, Consumer<Throwable> onError) {
        assistant.evictChatMemory(memoryId);
        onError.accept(error);
    }
}
