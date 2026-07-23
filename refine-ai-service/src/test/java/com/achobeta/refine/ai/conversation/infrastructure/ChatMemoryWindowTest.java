package com.achobeta.refine.ai.conversation.infrastructure;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChatMemoryWindowTest {
    @Test
    void keepsOnlyTheConfiguredRecentMessages() {
        RecordingStore store = new RecordingStore();
        var memory = MessageWindowChatMemory.builder()
                .id("user-1:conversation-1")
                .maxMessages(4)
                .chatMemoryStore(store)
                .build();

        memory.add(UserMessage.from("first"));
        memory.add(AiMessage.from("first-answer"));
        memory.add(UserMessage.from("second"));
        memory.add(AiMessage.from("second-answer"));
        memory.add(UserMessage.from("third"));

        assertThat(store.messages).hasSizeLessThanOrEqualTo(4);
        assertThat(store.messages.get(store.messages.size() - 1).toString()).contains("third");
    }

    private static final class RecordingStore implements ChatMemoryStore {
        private List<ChatMessage> messages = new ArrayList<>();

        @Override
        public List<ChatMessage> getMessages(Object memoryId) {
            return new ArrayList<>(messages);
        }

        @Override
        public void updateMessages(Object memoryId, List<ChatMessage> messages) {
            this.messages = new ArrayList<>(messages);
        }

        @Override
        public void deleteMessages(Object memoryId) {
            messages.clear();
        }
    }
}
