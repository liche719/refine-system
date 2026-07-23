package com.achobeta.refine.ai.ocr.application;

import com.achobeta.refine.ai.learning.application.port.LearningServicePort;
import com.achobeta.refine.ai.ocr.application.port.DocumentTextPort;
import com.achobeta.refine.ai.ocr.application.port.OcrQuestionAiPort;
import com.achobeta.refine.ai.ocr.application.port.OcrQuestionClassificationPort;
import com.achobeta.refine.ai.ocr.application.query.QuestionClassification;
import com.achobeta.refine.common.api.AppException;
import com.achobeta.refine.contracts.learning.CreateMistakeRequest;
import com.achobeta.refine.contracts.learning.CreateMistakeResponse;
import com.achobeta.refine.contracts.learning.EnsureKnowledgePointRequest;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OcrService {
    private final DocumentTextPort extractor;
    private final OcrQuestionAiPort ai;
    private final OcrQuestionClassificationPort classification;
    private final LearningServicePort learningClient;

    public OcrService(DocumentTextPort extractor, OcrQuestionAiPort ai, OcrQuestionClassificationPort classification,
                      LearningServicePort learningClient) {
        this.extractor = extractor;
        this.ai = ai;
        this.classification = classification;
        this.learningClient = learningClient;
    }

    public OcrResult extractFirst(String userId, byte[] bytes, String fileType) {
        String rawText = extractor.extract(bytes, fileType);
        if (rawText == null || rawText.isBlank()) {
            throw new AppException(10003, "no text could be extracted from file");
        }
        String question = ai.extractFirstQuestion(rawText).trim();
        if (question.isBlank()) {
            throw new AppException(10003, "AI did not extract a question");
        }
        String questionId = UUID.randomUUID().toString();
        QuestionClassification classified = classification.classify(question);
        Integer knowledgePointId = null;
        String subject = null;
        String knowledgePoint = null;
        if (classified != null && classified.isUsable()
                && !"未分类".equals(classified.subject()) && !"未分类".equals(classified.knowledgePoint())) {
            subject = classified.subject();
            knowledgePoint = classified.knowledgePoint();
            knowledgePointId = learningClient.ensureKnowledgePoint(new EnsureKnowledgePointRequest(
                    userId, knowledgePoint, classified.description(), subject)).knowledgePointId();
        }
        CreateMistakeResponse saved = learningClient.createMistake(
                new CreateMistakeRequest(userId, questionId, question, subject, knowledgePointId, "ocr"));
        return new OcrResult(question, saved.questionId(), subject, knowledgePoint);
    }

    public record OcrResult(String questionText, String questionId, String subject, String knowledgePoint) {
        public OcrResult(String questionText, String questionId) {
            this(questionText, questionId, null, null);
        }
    }
}
