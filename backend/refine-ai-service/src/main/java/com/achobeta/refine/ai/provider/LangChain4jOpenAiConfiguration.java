package com.achobeta.refine.ai.provider;

import com.achobeta.refine.ai.conversation.infrastructure.ChatMemoryProperties;
import com.achobeta.refine.ai.conversation.infrastructure.RedisChatMemoryStore;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@EnableConfigurationProperties({LangChain4jOpenAiProperties.class, ChatMemoryProperties.class})
public class LangChain4jOpenAiConfiguration {
    @Bean(name = "openAiChatModel")
    ChatModel openAiChatModel(LangChain4jOpenAiProperties properties) {
        var model = properties.chatModel();
        return OpenAiChatModel.builder()
                .baseUrl(model.baseUrl()).apiKey(model.apiKey()).modelName(model.modelName())
                .timeout(model.timeout()).maxRetries(retries(model.maxRetries()))
                .logRequests(enabled(model.logRequests())).logResponses(enabled(model.logResponses()))
                .build();
    }

    @Bean(name = "openAiStreamingChatModel")
    StreamingChatModel openAiStreamingChatModel(LangChain4jOpenAiProperties properties) {
        var model = properties.streamingChatModel();
        return OpenAiStreamingChatModel.builder()
                .baseUrl(model.baseUrl()).apiKey(model.apiKey()).modelName(model.modelName())
                .timeout(model.timeout())
                .logRequests(enabled(model.logRequests())).logResponses(enabled(model.logResponses()))
                .build();
    }

    @Bean
    EmbeddingModel openAiEmbeddingModel(LangChain4jOpenAiProperties properties) {
        var model = properties.embeddingModel();
        return OpenAiEmbeddingModel.builder()
                .baseUrl(model.baseUrl()).apiKey(model.apiKey()).modelName(model.modelName())
                .dimensions(model.dimensions()).timeout(model.timeout()).maxRetries(retries(model.maxRetries()))
                .logRequests(enabled(model.logRequests())).logResponses(enabled(model.logResponses()))
                .build();
    }

    @Bean
    RefineEducationAiService refineEducationAiService(
            @Qualifier("openAiChatModel") ChatModel chatModel,
            @Qualifier("openAiStreamingChatModel") StreamingChatModel streamingChatModel) {
        return AiServices.builder(RefineEducationAiService.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .build();
    }

    @Bean
    RedisChatMemoryStore redisChatMemoryStore(
            StringRedisTemplate redis,
            ChatMemoryProperties memoryProperties) {
        return new RedisChatMemoryStore(redis, memoryProperties);
    }

    @Bean
    RefineConversationAiService refineConversationAiService(
            @Qualifier("openAiChatModel") ChatModel chatModel,
            @Qualifier("openAiStreamingChatModel") StreamingChatModel streamingChatModel,
            RedisChatMemoryStore memoryStore,
            ChatMemoryProperties memoryProperties) {
        return AiServices.builder(RefineConversationAiService.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(memoryProperties.maxMessages())
                        .alwaysKeepSystemMessageFirst(true)
                        .chatMemoryStore(memoryStore)
                        .build())
                .build();
    }

    private int retries(Integer value) {
        return value == null ? 2 : Math.min(Math.max(value, 0), 5);
    }

    private boolean enabled(Boolean value) {
        return Boolean.TRUE.equals(value);
    }
}
