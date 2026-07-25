package com.achobeta.refine.ai.conversation.application;

import com.achobeta.refine.ai.conversation.application.port.ConversationAiPort;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConversationServiceTest {
    private final ConversationAiPort ai = mock(ConversationAiPort.class);
    private final ConversationService service = new ConversationService(ai);

    @Test
    void scopesMemoryByAuthenticatedUserAndConversationWithoutPreloadingKnowledge() {
        when(ai.reply("user-7:conversation-2", "你好")).thenReturn("你好，我可以帮助你学习。");

        assertThat(service.send("user-7", "conversation-2", "你好")).isEqualTo("你好，我可以帮助你学习。");

        verify(ai).reply("user-7:conversation-2", "你好");
    }

    @Test
    void keepsTheCurrentQuestionSeparateForToolEnabledQuestionConversation() {
        when(ai.replyWithQuestion("user-7:question-9", "忒修斯之船的木板全部被替换。", "哪艘是真船？"))
                .thenReturn("取决于采用的同一性标准。");

        assertThat(service.solveWithContext("user-7", "question-9", "忒修斯之船的木板全部被替换。", "哪艘是真船？"))
                .isEqualTo("取决于采用的同一性标准。");

        verify(ai).replyWithQuestion("user-7:question-9", "忒修斯之船的木板全部被替换。", "哪艘是真船？");
    }

    @Test
    void streamsQuestionConversationWithTheOriginalQuestionContext() {
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked") Consumer<String> onToken = invocation.getArgument(3);
            onToken.accept("二次函数的顶点");
            invocation.getArgument(4, Runnable.class).run();
            return null;
        }).when(ai).streamReplyWithQuestion(any(), any(), any(), any(), any(), any());

        StringBuilder content = new StringBuilder();
        service.streamSolveWithContext("user-7", "question-9", "求 y=x²-4x+3 的顶点", "如何求？",
                content::append, () -> { }, throwable -> { throw new AssertionError(throwable); });

        assertThat(content).hasToString("二次函数的顶点");
        verify(ai).streamReplyWithQuestion(eq("user-7:question-9"), eq("求 y=x²-4x+3 的顶点"), eq("如何求？"),
                any(), any(), any());
    }

    @Test
    void clearsTheSameUserScopedMemory() {
        when(ai.clearMemory("user-7:conversation-2")).thenReturn(true);

        assertThat(service.delete("user-7", "conversation-2")).isTrue();
        verify(ai).clearMemory("user-7:conversation-2");
    }
}
