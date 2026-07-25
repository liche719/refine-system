package com.achobeta.refine.ai.ocr.infrastructure;

import com.achobeta.refine.ai.ocr.application.port.OcrQuestionAiPort;
import com.achobeta.refine.ai.ocr.application.port.OcrQuestionClassificationPort;
import com.achobeta.refine.ai.ocr.application.query.QuestionClassification;
import com.achobeta.refine.ai.provider.RefineEducationAiService;
import com.achobeta.refine.ai.shared.infrastructure.LangChain4jCallSupport;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LangChain4jOcrQuestionAdapter implements OcrQuestionAiPort, OcrQuestionClassificationPort {
    private static final Logger log = LoggerFactory.getLogger(LangChain4jOcrQuestionAdapter.class);

    private final RefineEducationAiService assistant;
    private final ObjectMapper objectMapper;

    public LangChain4jOcrQuestionAdapter(RefineEducationAiService assistant, ObjectMapper objectMapper) {
        this.assistant = assistant;
        this.objectMapper = objectMapper;
    }

    @Override
    public String extractFirstQuestion(String rawText) {
        return LangChain4jCallSupport.complete("OCR question extraction", () -> assistant.extractFirstQuestion(rawText), log);
    }

    @Override
    public QuestionClassification classify(String question) {
        try {
            String response = LangChain4jCallSupport.complete("OCR question classification",
                    () -> assistant.classifyQuestion(question), log);
            ClassificationPayload payload = objectMapper.readValue(jsonObject(response), ClassificationPayload.class);
            return new QuestionClassification(trim(payload.subject()), trim(payload.knowledgePoint()), trim(payload.description()));
        } catch (Exception exception) {
            log.warn("AI question classification was unavailable; saving OCR question without knowledge point", exception);
            return new QuestionClassification(null, null, null);
        }
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
    private record ClassificationPayload(String subject, String knowledgePoint, String description) {
    }
}
