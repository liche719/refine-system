package com.achobeta.refine.ai.ocr.application.port;

import com.achobeta.refine.ai.ocr.application.query.QuestionClassification;

public interface OcrQuestionClassificationPort {
    QuestionClassification classify(String question);
}
