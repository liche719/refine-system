package com.achobeta.refine.ai.ocr.infrastructure;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OcrProviderProperties.class)
public class OcrProviderConfiguration {
    @Bean
    @Qualifier("ocrChatModel")
    ChatModel ocrChatModel(OcrProviderProperties properties) {
        return OpenAiChatModel.builder()
                .baseUrl(properties.baseUrl())
                .apiKey(properties.apiKey())
                .modelName(properties.modelName())
                .timeout(properties.timeout())
                .maxRetries(properties.maxRetries())
                .logRequests(false)
                .logResponses(false)
                .build();
    }

    @Bean
    ImageOcrAiService imageOcrAiService(@Qualifier("ocrChatModel") ChatModel chatModel) {
        return AiServices.builder(ImageOcrAiService.class)
                .chatModel(chatModel)
                .build();
    }
}
