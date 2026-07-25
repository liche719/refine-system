package com.achobeta.refine.ai.provider;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface RefineSolveAiService {

    @SystemMessage(fromResource = "/prompts/solve/system.txt")
    @UserMessage(fromResource = "/prompts/solve/user.txt")
    String solve(@V("questionContext") String questionContext);

    @SystemMessage(fromResource = "/prompts/solve/system.txt")
    @UserMessage(fromResource = "/prompts/solve/user.txt")
    TokenStream streamSolve(@V("questionContext") String questionContext);
}
