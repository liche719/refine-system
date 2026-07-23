package com.achobeta.refine.ai.conversation.infrastructure;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

public class RedisChatMemoryStore implements ChatMemoryStore {
    private static final String KEY_PREFIX = "ai:chat-memory:v1:";

    private final StringRedisTemplate redis;
    private final ChatMemoryProperties properties;

    public RedisChatMemoryStore(StringRedisTemplate redis, ChatMemoryProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String json = redis.opsForValue().get(key(memoryId));
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return ChatMessageDeserializer.messagesFromJson(json);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Cannot deserialize chat memory", exception);
        }
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        redis.opsForValue().set(
                key(memoryId),
                ChatMessageSerializer.messagesToJson(messages),
                properties.idleTtl());
    }

    @Override
    public void deleteMessages(Object memoryId) {
        delete(memoryId);
    }

    public boolean delete(Object memoryId) {
        return Boolean.TRUE.equals(redis.delete(key(memoryId)));
    }

    String key(Object memoryId) {
        return KEY_PREFIX + memoryId;
    }
}
