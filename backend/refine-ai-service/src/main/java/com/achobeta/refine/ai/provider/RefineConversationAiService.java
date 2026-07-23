package com.achobeta.refine.ai.provider;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.memory.ChatMemoryAccess;

interface RefineConversationAiService extends ChatMemoryAccess {

    @SystemMessage(fromResource = "/prompts/conversation/system.txt")
    @UserMessage(fromResource = "/prompts/conversation/user.txt")
    String conversation(@MemoryId String memoryId, @V("references") String references,
                        @V("message") String message);

    @SystemMessage(fromResource = "/prompts/conversation/system.txt")
    @UserMessage(fromResource = "/prompts/conversation/user.txt")
    TokenStream streamConversation(@MemoryId String memoryId, @V("references") String references,
                                   @V("message") String message);
}
