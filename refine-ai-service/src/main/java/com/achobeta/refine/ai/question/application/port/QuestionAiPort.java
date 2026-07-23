package com.achobeta.refine.ai.question.application.port;

import java.util.function.Consumer;

public interface QuestionAiPort {
    String generate(String subject, String knowledgePoint);

    String judge(String question, String expectedAnswer, String userAnswer);

    void streamJudge(String question, String expectedAnswer, String userAnswer,
                     Consumer<String> onToken, Runnable onComplete, Consumer<Throwable> onError);
}
