package com.achobeta.refine.ai.provider;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LangChain4jPromptResourceTest {

    @Test
    void allEducationMethodsUseReadableSystemAndUserResources() throws Exception {
        for (var method : Stream.concat(
                Stream.of(RefineEducationAiService.class.getDeclaredMethods()),
                Stream.of(RefineConversationAiService.class.getDeclaredMethods())).toList()) {
            if (method.getDeclaringClass() != RefineEducationAiService.class
                    && method.getDeclaringClass() != RefineConversationAiService.class) {
                continue;
            }
            var system = method.getAnnotation(dev.langchain4j.service.SystemMessage.class);
            var user = method.getAnnotation(dev.langchain4j.service.UserMessage.class);

            assertThat(system).as(method.getName() + " system prompt").isNotNull();
            assertThat(user).as(method.getName() + " user prompt").isNotNull();
            assertThat(read(system.fromResource())).contains("Refine").doesNotContain("锛", "銆");
            assertThat(read(user.fromResource())).isNotBlank().doesNotContain("锛", "銆");
        }
    }

    @Test
    void aiServicesRendersConversationVariablesIntoAnnotatedMessages() {
        ChatModel model = mock(ChatModel.class);
        when(model.supportedCapabilities()).thenReturn(Set.<Capability>of());
        when(model.chat(any(ChatRequest.class))).thenReturn(ChatResponse.builder()
                .aiMessage(AiMessage.from("我是 Refine 的 AI 学习助手。"))
                .build());
        RefineConversationAiService assistant = AiServices.builder(RefineConversationAiService.class)
                .chatModel(model)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(20)
                        .chatMemoryStore(new InMemoryChatMemoryStore())
                        .build())
                .build();

        String answer = assistant.conversation("user-1:conversation-1", "判别式相关资料", "你能做什么");

        assertThat(answer).contains("Refine");
        ArgumentCaptor<ChatRequest> request = ArgumentCaptor.forClass(ChatRequest.class);
        verify(model).chat(request.capture());
        assertThat(request.getValue().messages()).hasSize(2);
        assertThat(((SystemMessage) request.getValue().messages().get(0)).text())
                .contains("Refine 智能错题学习系统", "不要自称编程助手", "标准 Markdown");
        assertThat(((UserMessage) request.getValue().messages().get(1)).singleText())
                .contains("判别式相关资料", "你能做什么")
                .doesNotContain("{{history}}", "{{references}}", "{{message}}");
    }

    private String read(String path) throws Exception {
        try (InputStream input = RefineEducationAiService.class.getResourceAsStream(path)) {
            assertThat(input).as(path).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
