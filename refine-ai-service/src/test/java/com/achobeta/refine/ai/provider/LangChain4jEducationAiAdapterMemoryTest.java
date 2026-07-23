package com.achobeta.refine.ai.provider;

import com.achobeta.refine.ai.conversation.infrastructure.RedisChatMemoryStore;
import com.achobeta.refine.common.api.AppException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LangChain4jEducationAiAdapterMemoryTest {
    private final RefineEducationAiService educationAssistant = mock(RefineEducationAiService.class);
    private final RefineConversationAiService conversationAssistant = mock(RefineConversationAiService.class);
    private final RedisChatMemoryStore memoryStore = mock(RedisChatMemoryStore.class);
    private final LangChain4jEducationAiAdapter adapter = new LangChain4jEducationAiAdapter(
            educationAssistant, conversationAssistant, memoryStore);

    @Test
    void evictsJvmMemoryAfterSuccessfulSynchronousReply() {
        when(conversationAssistant.conversation("user-1:conversation-1", "reference", "你好"))
                .thenReturn("你好");

        assertThat(adapter.reply("user-1:conversation-1", "reference", "你好")).isEqualTo("你好");
        verify(conversationAssistant).evictChatMemory("user-1:conversation-1");
    }

    @Test
    void evictsJvmMemoryWhenModelInvocationFails() {
        when(conversationAssistant.conversation("user-1:conversation-1", "reference", "你好"))
                .thenThrow(new IllegalStateException("remote failure"));

        assertThatThrownBy(() -> adapter.reply("user-1:conversation-1", "reference", "你好"))
                .isInstanceOf(AppException.class);
        verify(conversationAssistant).evictChatMemory("user-1:conversation-1");
    }

    @Test
    void clearRemovesBothJvmAndRedisMemory() {
        when(memoryStore.delete("user-1:conversation-1")).thenReturn(true);

        assertThat(adapter.clearMemory("user-1:conversation-1")).isTrue();
        verify(conversationAssistant).evictChatMemory("user-1:conversation-1");
        verify(memoryStore).delete("user-1:conversation-1");
    }
}
