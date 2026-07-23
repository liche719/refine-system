package com.achobeta.refine.ai.conversation.application;

import com.achobeta.refine.ai.conversation.application.port.ConversationAiPort;
import com.achobeta.refine.ai.rag.application.RagSearchService;
import com.achobeta.refine.ai.rag.application.query.RagChunk;
import com.achobeta.refine.ai.rag.application.query.RagDocumentMetadata;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConversationServiceTest {
    private final ConversationAiPort ai = mock(ConversationAiPort.class);
    private final RagSearchService ragSearch = mock(RagSearchService.class);
    private final ConversationService service = new ConversationService(ai, ragSearch);

    @Test
    void scopesMemoryByAuthenticatedUserAndConversation() {
        when(ai.reply("user-7:conversation-2", "", "我上一句说了什么"))
                .thenReturn("你上一句说了你好");

        assertThat(service.send("user-7", "conversation-2", "我上一句说了什么"))
                .isEqualTo("你上一句说了你好");
        verify(ai).reply("user-7:conversation-2", "", "我上一句说了什么");
    }

    @Test
    void clearsTheSameUserScopedMemory() {
        when(ai.clearMemory("user-7:conversation-2")).thenReturn(true);

        assertThat(service.delete("user-7", "conversation-2")).isTrue();
        verify(ai).clearMemory("user-7:conversation-2");
    }

    @Test
    void sendsApprovedKnowledgeReferencesForRegularConversation() {
        RagChunk chunk = new RagChunk(1L, "An equation has an unknown value.",
                new RagDocumentMetadata("math.md", "a".repeat(64), "Algebra", "Mathematics", "Grade 7",
                        "PEP", "Chapter 3", "", "", true), 0.9D, 0.1D, 0.03D);
        when(ragSearch.search("What is an equation?", 3)).thenReturn(java.util.List.of(chunk));
        when(ai.reply("user-7:conversation-2", "【来源：Algebra · Chapter 3】\nAn equation has an unknown value.",
                "What is an equation?")).thenReturn("An equation has an unknown value.");

        service.send("user-7", "conversation-2", "What is an equation?");

        verify(ai).reply("user-7:conversation-2", "【来源：Algebra · Chapter 3】\nAn equation has an unknown value.",
                "What is an equation?");
    }

    @Test
    void prioritizesTheCurrentQuestionWhenAnsweringInQuestionDetail() {
        when(ragSearch.search("The ship has every plank replaced.\nWhich ship is original?", 3))
                .thenReturn(java.util.List.of());
        String context = "当前题目（优先依据）：\nThe ship has every plank replaced.\n\n相关资料：\n";
        when(ai.reply("user-7:question-9", context, "Which ship is original?"))
                .thenReturn("It depends on the identity criterion.");

        service.solveWithContext("user-7", "question-9", "The ship has every plank replaced.",
                "Which ship is original?");

        verify(ai).reply("user-7:question-9", context, "Which ship is original?");
    }
}
