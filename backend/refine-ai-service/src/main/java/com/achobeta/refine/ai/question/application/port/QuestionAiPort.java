package com.achobeta.refine.ai.question.application.port;

import java.util.function.Consumer;

public interface QuestionAiPort {
    String generate(String subject, String knowledgePoint, String referenceContext);

    QuestionQuality verify(String subject, String knowledgePoint, String content, String answer, String analysis);

    String judge(String question, String expectedAnswer, String userAnswer);

    void streamJudge(String question, String expectedAnswer, String userAnswer,
                     Consumer<String> onToken, Runnable onComplete, Consumer<Throwable> onError);

    record QuestionQuality(boolean valid, String reason) { }
}
