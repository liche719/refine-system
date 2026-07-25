package com.achobeta.refine.ai.conversation.infrastructure;

import com.achobeta.refine.ai.provider.RefineConversationAiService;
import com.achobeta.refine.common.api.AppException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LangChain4jConversationAdapterTest {
    private final RefineConversationAiService assistant = mock(RefineConversationAiService.class);
    private final RedisChatMemoryStore memoryStore = mock(RedisChatMemoryStore.class);
    private final LangChain4jConversationAdapter adapter = new LangChain4jConversationAdapter(assistant, memoryStore);

    @Test
    void evictsOnlyJvmMemoryAfterReplyAndKeepsPersistedConversation() {
        when(assistant.conversation("user-1:conversation-1", "hello")).thenReturn("hello");

        assertThat(adapter.reply("user-1:conversation-1", "hello")).isEqualTo("hello");

        verify(assistant).evictChatMemory("user-1:conversation-1");
    }

    @Test
    void evictsJvmMemoryWhenTheProviderFails() {
        when(assistant.conversation("user-1:conversation-1", "hello"))
                .thenThrow(new IllegalStateException("remote failure"));

        assertThatThrownBy(() -> adapter.reply("user-1:conversation-1", "hello"))
                .isInstanceOf(AppException.class);
        verify(assistant).evictChatMemory("user-1:conversation-1");
    }

    @Test
    void forwardsQuestionContextAndDeletesBothMemoryLayersOnExplicitClear() {
        when(assistant.conversationWithQuestion("user-1:question-1", "quadratic function", "how to find vertex"))
                .thenReturn("Use -b/(2a).");
        when(memoryStore.delete("user-1:question-1")).thenReturn(true);

        assertThat(adapter.replyWithQuestion("user-1:question-1", "quadratic function", "how to find vertex"))
                .isEqualTo("Use -b/(2a).");
        assertThat(adapter.clearMemory("user-1:question-1")).isTrue();

        verify(assistant, times(2)).evictChatMemory("user-1:question-1");
        verify(memoryStore).delete("user-1:question-1");
    }
}
