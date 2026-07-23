package com.achobeta.refine.ai.conversation.infrastructure;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisChatMemoryStoreTest {
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final ChatMemoryProperties properties = new ChatMemoryProperties(20, Duration.ofHours(24));
    private RedisChatMemoryStore store;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(values);
        store = new RedisChatMemoryStore(redis, properties);
    }

    @Test
    void serializesMessagesAndAppliesSlidingTtl() {
        List<ChatMessage> messages = List.of(UserMessage.from("你好"), AiMessage.from("你好，有什么可以帮你？"));

        store.updateMessages("user-1:conversation-1", messages);

        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(values).set(
                org.mockito.ArgumentMatchers.eq("ai:chat-memory:v1:user-1:conversation-1"),
                json.capture(),
                org.mockito.ArgumentMatchers.eq(Duration.ofHours(24)));
        when(values.get(anyString())).thenReturn(json.getValue());
        List<ChatMessage> restored = store.getMessages("user-1:conversation-1");
        assertThat(restored).hasSize(2);
        assertThat(restored.get(0)).isInstanceOf(UserMessage.class);
        assertThat(restored.get(1)).isInstanceOf(AiMessage.class);
    }

    @Test
    void returnsEmptyHistoryWhenKeyDoesNotExist() {
        when(values.get(anyString())).thenReturn(null);

        assertThat(store.getMessages("missing")).isEmpty();
    }

    @Test
    void reportsWhetherStoredMemoryWasDeleted() {
        when(redis.delete("ai:chat-memory:v1:user-1:conversation-1")).thenReturn(true);

        assertThat(store.delete("user-1:conversation-1")).isTrue();
    }
}
