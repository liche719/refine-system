package com.achobeta.refine.ai.provider;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface RefineEducationAiService {

    @SystemMessage(fromResource = "/prompts/question/generate-system.txt")
    @UserMessage(fromResource = "/prompts/question/generate-user.txt")
    String generateQuestion(@V("subject") String subject, @V("knowledgePoint") String knowledgePoint,
                            @V("referenceContext") String referenceContext);

    @SystemMessage(fromResource = "/prompts/question/verify-system.txt")
    @UserMessage(fromResource = "/prompts/question/verify-user.txt")
    String verifyGeneratedQuestion(@V("subject") String subject, @V("knowledgePoint") String knowledgePoint,
                                   @V("content") String content, @V("answer") String answer, @V("analysis") String analysis);

    @SystemMessage(fromResource = "/prompts/question/judge-system.txt")
    @UserMessage(fromResource = "/prompts/question/judge-user.txt")
    String judgeAnswer(@V("question") String question, @V("expectedAnswer") String expectedAnswer,
                       @V("userAnswer") String userAnswer);

    @SystemMessage(fromResource = "/prompts/question/judge-system.txt")
    @UserMessage(fromResource = "/prompts/question/judge-user.txt")
    TokenStream streamJudgeAnswer(@V("question") String question, @V("expectedAnswer") String expectedAnswer,
                                  @V("userAnswer") String userAnswer);

    @SystemMessage(fromResource = "/prompts/ocr/extract-system.txt")
    @UserMessage(fromResource = "/prompts/ocr/extract-user.txt")
    String extractFirstQuestion(@V("rawText") String rawText);

    @SystemMessage(fromResource = "/prompts/ocr/classify-system.txt")
    @UserMessage(fromResource = "/prompts/ocr/classify-user.txt")
    String classifyQuestion(@V("question") String question);

    @SystemMessage(fromResource = "/prompts/suggestion/system.txt")
    @UserMessage(fromResource = "/prompts/suggestion/user.txt")
    String learningSuggestions(@V("recentKnowledgePoints") String recentKnowledgePoints);
}
