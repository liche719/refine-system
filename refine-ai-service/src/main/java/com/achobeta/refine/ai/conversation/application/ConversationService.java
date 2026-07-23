package com.achobeta.refine.ai.conversation.application;

import com.achobeta.refine.ai.conversation.application.port.ConversationAiPort;
import com.achobeta.refine.ai.rag.application.RagSearchService;
import com.achobeta.refine.ai.rag.application.query.RagChunk;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class ConversationService {
    private final ConversationAiPort ai;
    private final RagSearchService ragSearch;

    public ConversationService(ConversationAiPort ai, RagSearchService ragSearch) {
        this.ai = ai;
        this.ragSearch = ragSearch;
    }

    public String send(String userId, String conversationId, String message) {
        return send(userId, conversationId, message, referencesFor(message));
    }

    public String send(String userId, String conversationId, String message, String referenceContext) {
        return ai.reply(memoryId(userId, conversationId), referenceContext, message);
    }

    public String solveWithContext(String userId, String questionId, String questionContent, String userQuestion) {
        return send(userId, questionId, userQuestion, contextForQuestion(questionContent, userQuestion));
    }

    public void streamSend(String userId, String conversationId, String message,
                           java.util.function.Consumer<String> onToken, Runnable onComplete,
                           java.util.function.Consumer<Throwable> onError) {
        streamSend(userId, conversationId, message, referencesFor(message), onToken, onComplete, onError);
    }

    public void streamSolveWithContext(String userId, String questionId, String questionContent, String userQuestion,
                                       java.util.function.Consumer<String> onToken, Runnable onComplete,
                                       java.util.function.Consumer<Throwable> onError) {
        streamSend(userId, questionId, userQuestion, contextForQuestion(questionContent, userQuestion), onToken, onComplete, onError);
    }

    private void streamSend(String userId, String conversationId, String message, String referenceContext,
                            java.util.function.Consumer<String> onToken, Runnable onComplete,
                            java.util.function.Consumer<Throwable> onError) {
        ai.streamReply(memoryId(userId, conversationId), referenceContext, message,
                onToken, onComplete, onError);
    }

    public boolean delete(String userId, String conversationId) {
        return ai.clearMemory(memoryId(userId, conversationId));
    }

    private String memoryId(String userId, String conversationId) {
        return userId + ":" + conversationId;
    }

    private String referencesFor(String question) {
        return ragSearch.search(question, 3).stream().map(RagChunk::referenceText).collect(Collectors.joining("\n\n"));
    }

    private String contextForQuestion(String questionContent, String userQuestion) {
        String question = questionContent == null ? "" : questionContent.trim();
        String references = referencesFor((question + "\n" + userQuestion).trim());
        if (question.isBlank()) {
            return references;
        }
        return "当前题目（优先依据）：\n" + question + "\n\n相关资料：\n" + references;
    }
}
