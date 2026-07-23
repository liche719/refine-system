package com.achobeta.refine.ai.ocr.infrastructure;

import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

interface ImageOcrAiService {
    @SystemMessage(fromResource = "/prompts/ocr/image-system.txt")
    @UserMessage(fromResource = "/prompts/ocr/image-user.txt")
    String recognize(@UserMessage ImageContent image);
}
