package com.achobeta.refine.ai.question.infrastructure;

import com.achobeta.refine.ai.provider.RefineEducationAiService;
import com.achobeta.refine.ai.question.application.port.QuestionAiPort;
import com.achobeta.refine.ai.shared.infrastructure.LangChain4jCallSupport;
import com.achobeta.refine.common.api.AppException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
public class LangChain4jQuestionAdapter implements QuestionAiPort {
    private static final Logger log = LoggerFactory.getLogger(LangChain4jQuestionAdapter.class);

    private final RefineEducationAiService assistant;
    private final ObjectMapper objectMapper;

    public LangChain4jQuestionAdapter(RefineEducationAiService assistant, ObjectMapper objectMapper) {
        this.assistant = assistant;
        this.objectMapper = objectMapper;
    }

    @Override
    public String generate(String subject, String knowledgePoint, String referenceContext) {
        return LangChain4jCallSupport.complete("question generation",
                () -> assistant.generateQuestion(LangChain4jCallSupport.context(subject),
                        LangChain4jCallSupport.context(knowledgePoint), LangChain4jCallSupport.context(referenceContext)), log);
    }

    @Override
    public QuestionQuality verify(String subject, String knowledgePoint, String content, String answer, String analysis) {
        try {
            String response = LangChain4jCallSupport.complete("question verification",
                    () -> assistant.verifyGeneratedQuestion(LangChain4jCallSupport.context(subject),
                            LangChain4jCallSupport.context(knowledgePoint), content, answer, analysis), log);
            QuestionQualityPayload payload = objectMapper.readValue(jsonObject(response), QuestionQualityPayload.class);
            return new QuestionQuality(Boolean.TRUE.equals(payload.valid()), trim(payload.reason()));
        } catch (AppException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("AI generated-question verification response was invalid", exception);
            throw new AppException(10001, "AI 生成题校验暂时不可用");
        }
    }

    @Override
    public String judge(String question, String expectedAnswer, String userAnswer) {
        return LangChain4jCallSupport.complete("answer judging",
                () -> assistant.judgeAnswer(question, expectedAnswer, userAnswer), log);
    }

    @Override
    public void streamJudge(String question, String expectedAnswer, String userAnswer,
                            Consumer<String> onToken, Runnable onComplete, Consumer<Throwable> onError) {
        LangChain4jCallSupport.stream(() -> assistant.streamJudgeAnswer(question, expectedAnswer, userAnswer),
                onToken, onComplete, onError);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String jsonObject(String value) {
        int start = value.indexOf('{');
        int end = value.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("response does not contain a JSON object");
        }
        return value.substring(start, end + 1);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record QuestionQualityPayload(Boolean valid, String reason) {
    }
}
