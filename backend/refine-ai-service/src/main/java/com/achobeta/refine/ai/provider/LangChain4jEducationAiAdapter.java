package com.achobeta.refine.ai.provider;

import com.achobeta.refine.ai.conversation.application.port.ConversationAiPort;
import com.achobeta.refine.ai.conversation.infrastructure.RedisChatMemoryStore;
import com.achobeta.refine.ai.ocr.application.port.OcrQuestionAiPort;
import com.achobeta.refine.ai.ocr.application.port.OcrQuestionClassificationPort;
import com.achobeta.refine.ai.ocr.application.query.QuestionClassification;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.achobeta.refine.ai.question.application.port.QuestionAiPort;
import com.achobeta.refine.ai.solve.application.port.SolveAiPort;
import com.achobeta.refine.ai.suggestion.application.port.SuggestionAiPort;
import com.achobeta.refine.common.api.AppException;
import dev.langchain4j.service.TokenStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Component
public class LangChain4jEducationAiAdapter implements ConversationAiPort, SolveAiPort, QuestionAiPort,
        OcrQuestionAiPort, OcrQuestionClassificationPort, SuggestionAiPort {
    private static final Logger log = LoggerFactory.getLogger(LangChain4jEducationAiAdapter.class);
    private static final String EMPTY_CONTEXT = "（暂无）";

    private final RefineEducationAiService assistant;
    private final RefineConversationAiService conversationAssistant;
    private final RedisChatMemoryStore memoryStore;
    private final ObjectMapper objectMapper;

    @Autowired
    public LangChain4jEducationAiAdapter(
            RefineEducationAiService assistant,
            RefineConversationAiService conversationAssistant,
            RedisChatMemoryStore memoryStore,
            ObjectMapper objectMapper) {
        this.assistant = assistant;
        this.conversationAssistant = conversationAssistant;
        this.memoryStore = memoryStore;
        this.objectMapper = objectMapper;
    }

    LangChain4jEducationAiAdapter(
            RefineEducationAiService assistant,
            RefineConversationAiService conversationAssistant,
            RedisChatMemoryStore memoryStore) {
        this(assistant, conversationAssistant, memoryStore, new ObjectMapper());
    }

    @Override
    public String reply(String memoryId, String references, String message) {
        try {
            return complete(() -> conversationAssistant.conversation(
                    memoryId, context(references), message));
        } finally {
            conversationAssistant.evictChatMemory(memoryId);
        }
    }

    @Override
    public void streamReply(String memoryId, String references, String message, Consumer<String> onToken,
                            Runnable onComplete, Consumer<Throwable> onError) {
        stream(() -> conversationAssistant.streamConversation(memoryId, context(references), message),
                onToken,
                () -> {
                    conversationAssistant.evictChatMemory(memoryId);
                    onComplete.run();
                },
                error -> {
                    conversationAssistant.evictChatMemory(memoryId);
                    onError.accept(error);
                });
    }

    @Override
    public boolean clearMemory(String memoryId) {
        conversationAssistant.evictChatMemory(memoryId);
        return memoryStore.delete(memoryId);
    }

    @Override
    public String solve(String questionContext) {
        return complete(() -> assistant.solve(questionContext));
    }

    @Override
    public void streamSolve(String questionContext, Consumer<String> onToken, Runnable onComplete,
                            Consumer<Throwable> onError) {
        stream(() -> assistant.streamSolve(questionContext), onToken, onComplete, onError);
    }

    @Override
    public String generate(String subject, String knowledgePoint, String referenceContext) {
        return complete(() -> assistant.generateQuestion(context(subject), context(knowledgePoint), context(referenceContext)));
    }

    @Override
    public QuestionAiPort.QuestionQuality verify(String subject, String knowledgePoint, String content,
                                                  String answer, String analysis) {
        try {
            QuestionQualityPayload payload = objectMapper.readValue(jsonObject(complete(() -> assistant.verifyGeneratedQuestion(
                    context(subject), context(knowledgePoint), content, answer, analysis))), QuestionQualityPayload.class);
            return new QuestionAiPort.QuestionQuality(Boolean.TRUE.equals(payload.valid()), trim(payload.reason()));
        } catch (AppException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("AI generated-question verification response was invalid", exception);
            throw new AppException(10001, "AI 生成题校验暂时不可用");
        }
    }

    @Override
    public String judge(String question, String expectedAnswer, String userAnswer) {
        return complete(() -> assistant.judgeAnswer(question, expectedAnswer, userAnswer));
    }

    @Override
    public void streamJudge(String question, String expectedAnswer, String userAnswer,
                            Consumer<String> onToken, Runnable onComplete, Consumer<Throwable> onError) {
        stream(() -> assistant.streamJudgeAnswer(question, expectedAnswer, userAnswer),
                onToken, onComplete, onError);
    }

    @Override
    public String extractFirstQuestion(String rawText) {
        return complete(() -> assistant.extractFirstQuestion(rawText));
    }

    @Override
    public QuestionClassification classify(String question) {
        try {
            ClassificationPayload payload = objectMapper.readValue(jsonObject(complete(() -> assistant.classifyQuestion(question))),
                    ClassificationPayload.class);
            return new QuestionClassification(trim(payload.subject()), trim(payload.knowledgePoint()), trim(payload.description()));
        } catch (Exception exception) {
            log.warn("AI question classification was unavailable; saving OCR question without knowledge point", exception);
            return new QuestionClassification(null, null, null);
        }
    }

    @Override
    public String suggest(String recentKnowledgePoints) {
        return complete(() -> assistant.learningSuggestions(recentKnowledgePoints));
    }

    private String complete(Supplier<String> invocation) {
        try {
            String content = invocation.get();
            if (content == null || content.isBlank()) {
                throw new AppException(10001, "AI 返回内容为空");
            }
            return content;
        } catch (AppException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("LangChain4j education assistant request failed; errorType={}, message={}",
                    exception.getClass().getSimpleName(), exception.getMessage());
            throw new AppException(10001, "AI 服务暂时不可用");
        }
    }

    private void stream(Supplier<TokenStream> invocation, Consumer<String> onToken,
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

    private String context(String value) {
        return value == null || value.isBlank() ? EMPTY_CONTEXT : value;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String jsonObject(String value) {
        int start = value.indexOf('{');
        int end = value.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("classification response does not contain a JSON object");
        }
        return value.substring(start, end + 1);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ClassificationPayload(String subject, String knowledgePoint, String description) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record QuestionQualityPayload(Boolean valid, String reason) { }
}
