package com.achobeta.refine.ai.question.application;

import com.achobeta.refine.ai.learning.application.port.LearningServicePort;
import com.achobeta.refine.ai.question.application.port.QuestionCache;
import com.achobeta.refine.ai.question.application.port.QuestionAiPort;
import com.achobeta.refine.common.api.AppException;
import com.achobeta.refine.contracts.learning.CreateMistakeRequest;
import com.achobeta.refine.contracts.learning.GenerationContextResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class QuestionWorkflowService {
    private final LearningServicePort learning;
    private final QuestionAiPort ai;
    private final QuestionCache cache;
    private final ObjectMapper objectMapper;
    private final QuadraticQuestionSolvabilityGuard solvabilityGuard = new QuadraticQuestionSolvabilityGuard();

    public QuestionWorkflowService(LearningServicePort learning, QuestionAiPort ai,
                                   QuestionCache cache, ObjectMapper objectMapper) {
        this.learning = learning; this.ai = ai;
        this.cache = cache; this.objectMapper = objectMapper;
    }

    public GeneratedQuestion generate(String userId, long mistakeQuestionId) {
        GenerationContextResponse context = learning.generationContext(mistakeQuestionId, userId);
        String output = ai.generate(context.subject(), context.knowledgePointName());
        QuestionCandidate candidate = parse(output, context);
        String questionId = UUID.randomUUID().toString();
        QuestionCandidate stored = new QuestionCandidate(questionId, userId, candidate.content(), candidate.answer(),
                candidate.analysis(), context.subject(), context.knowledgePointId());
        try { cache.save(stored, Duration.ofHours(4)); }
        catch (Exception exception) { throw new AppException(10001, "generated question cache failed"); }
        return new GeneratedQuestion(questionId, stored.content());
    }

    public String judge(String userId, String questionId, String answer) {
        QuestionCandidate candidate = get(userId, questionId);
        return ai.judge(candidate.content(), candidate.answer(), answer);
    }

    public void streamJudge(String userId, String questionId, String answer,
                            java.util.function.Consumer<String> onToken, Runnable onComplete,
                            java.util.function.Consumer<Throwable> onError) {
        QuestionCandidate candidate = get(userId, questionId);
        ai.streamJudge(candidate.content(), candidate.answer(), answer, onToken, onComplete, onError);
    }

    public void record(String userId, String questionId) {
        QuestionCandidate candidate = get(userId, questionId);
        learning.createMistake(new CreateMistakeRequest(userId, questionId, candidate.content(), candidate.subject(),
                candidate.knowledgePointId(), "generated"));
        cache.delete(questionId);
    }

    private QuestionCandidate get(String userId, String questionId) {
        try {
            QuestionCandidate candidate = cache.find(questionId);
            if (candidate == null) throw new AppException(10002, "question expired or not found");
            if (!userId.equals(candidate.userId())) throw new AppException(1005, "question access denied");
            return candidate;
        } catch (AppException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AppException(10002, "question cache is corrupted");
        }
    }

    private QuestionCandidate parse(String output, GenerationContextResponse context) {
        try {
            String json = output.substring(output.indexOf('{'), output.lastIndexOf('}') + 1);
            JsonNode node = objectMapper.readTree(json);
            String content = node.path("content").asText();
            String answer = node.path("answer").asText();
            if (content.isBlank() || answer.isBlank()) throw new IllegalArgumentException("missing fields");
            solvabilityGuard.verify(content);
            return new QuestionCandidate(null, null, content, answer, node.path("analysis").asText(),
                    context.subject(), context.knowledgePointId());
        } catch (AppException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AppException(10001, "AI did not return valid question JSON");
        }
    }

    public record GeneratedQuestion(String questionId, String content) { }
}
