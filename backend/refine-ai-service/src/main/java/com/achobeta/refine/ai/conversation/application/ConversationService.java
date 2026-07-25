package com.achobeta.refine.ai.conversation.application;

import com.achobeta.refine.ai.conversation.application.port.ConversationAiPort;
import org.springframework.stereotype.Service;

@Service
public class ConversationService {
    private final ConversationAiPort ai;

    public ConversationService(ConversationAiPort ai) {
        this.ai = ai;
    }

    public String send(String userId, String conversationId, String message) {
        return ai.reply(memoryId(userId, conversationId), message);
    }

    public String solveWithContext(String userId, String questionId, String questionContent, String userQuestion) {
        return ai.replyWithQuestion(memoryId(userId, questionId), normalized(questionContent), userQuestion);
    }

    public void streamSend(String userId, String conversationId, String message,
                           java.util.function.Consumer<String> onToken, Runnable onComplete,
                           java.util.function.Consumer<Throwable> onError) {
        ai.streamReply(memoryId(userId, conversationId), message, onToken, onComplete, onError);
    }

    public void streamSolveWithContext(String userId, String questionId, String questionContent, String userQuestion,
                                       java.util.function.Consumer<String> onToken, Runnable onComplete,
                                       java.util.function.Consumer<Throwable> onError) {
        ai.streamReplyWithQuestion(memoryId(userId, questionId), normalized(questionContent), userQuestion,
                onToken, onComplete, onError);
    }

    public boolean delete(String userId, String conversationId) {
        return ai.clearMemory(memoryId(userId, conversationId));
    }

    private String memoryId(String userId, String conversationId) {
        return userId + ":" + conversationId;
    }

    private String normalized(String questionContent) {
        return questionContent == null ? "" : questionContent.trim();
    }
}
