package com.achobeta.refine.ai.provider;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LangChain4jPromptResourceTest {

    @Test
    void allAiMethodsUseReadableSystemAndUserResources() throws Exception {
        Stream.of(RefineEducationAiService.class, RefineConversationAiService.class, RefineSolveAiService.class)
                .flatMap(type -> Stream.of(type.getDeclaredMethods()))
                .forEach(method -> {
                    var system = method.getAnnotation(dev.langchain4j.service.SystemMessage.class);
                    var user = method.getAnnotation(dev.langchain4j.service.UserMessage.class);

                    assertThat(system).as(method.getName() + " system prompt").isNotNull();
                    assertThat(user).as(method.getName() + " user prompt").isNotNull();
                    try {
                        assertThat(read(system.fromResource())).contains("Refine").doesNotContain("\uFFFD");
                        assertThat(read(user.fromResource())).isNotBlank().doesNotContain("\uFFFD");
                    } catch (Exception exception) {
                        throw new AssertionError("Unable to read prompt resource for " + method.getName(), exception);
                    }
                });
    }

    @Test
    void conversationRendersOnlyTheUserMessageWithoutPreloadedReferences() {
        ChatModel model = mock(ChatModel.class);
        when(model.supportedCapabilities()).thenReturn(Set.of());
        when(model.chat(any(ChatRequest.class))).thenReturn(ChatResponse.builder()
                .aiMessage(AiMessage.from("我是 Refine 的 AI 学习助手。"))
                .build());
        RefineConversationAiService assistant = conversationAssistant(model);

        String answer = assistant.conversation("user-1:conversation-1", "你能做什么？");

        assertThat(answer).contains("Refine");
        ArgumentCaptor<ChatRequest> request = ArgumentCaptor.forClass(ChatRequest.class);
        verify(model).chat(request.capture());
        assertThat(request.getValue().messages()).hasSize(2);
        assertThat(((SystemMessage) request.getValue().messages().get(0)).text())
                .contains("Refine 智能错题学习系统", "search_knowledge_base", "标准 Markdown");
        assertThat(((UserMessage) request.getValue().messages().get(1)).singleText())
                .contains("你能做什么？")
                .doesNotContain("{{references}}", "{{message}}");
    }

    @Test
    void aiServicesExecutesTheKnowledgeToolWhenTheModelRequestsIt() {
        ChatModel model = mock(ChatModel.class);
        when(model.supportedCapabilities()).thenReturn(Set.of());
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .id("tool-call-1")
                .name("search_knowledge_base")
                .arguments("{\"query\":\"二次函数顶点怎么求\"}")
                .build();
        when(model.chat(any(ChatRequest.class))).thenReturn(
                ChatResponse.builder().aiMessage(AiMessage.from(request)).build(),
                ChatResponse.builder().aiMessage(AiMessage.from("顶点横坐标为 -b/(2a)。")) .build());
        ToolProbe probe = new ToolProbe();
        RefineConversationAiService assistant = AiServices.builder(RefineConversationAiService.class)
                .chatModel(model)
                .tools(probe)
                .maxToolCallingRoundTrips(2)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                        .id(memoryId).maxMessages(20).chatMemoryStore(new InMemoryChatMemoryStore()).build())
                .build();

        String answer = assistant.conversation("user-1:conversation-1", "二次函数顶点怎么求？");

        assertThat(answer).contains("顶点横坐标");
        assertThat(probe.calls).hasValue(1);
        assertThat(probe.lastQuery).isEqualTo("二次函数顶点怎么求");
        verify(model, times(2)).chat(any(ChatRequest.class));
    }

    private RefineConversationAiService conversationAssistant(ChatModel model) {
        return AiServices.builder(RefineConversationAiService.class)
                .chatModel(model)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                        .id(memoryId).maxMessages(20).chatMemoryStore(new InMemoryChatMemoryStore()).build())
                .build();
    }

    private static String read(String path) throws Exception {
        try (InputStream input = RefineEducationAiService.class.getResourceAsStream(path)) {
            assertThat(input).as(path).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    static class ToolProbe {
        private final AtomicInteger calls = new AtomicInteger();
        private String lastQuery;

        @Tool(name = "search_knowledge_base", value = "返回匹配的学习资料")
        String search(@P(name = "query", value = "检索问题") String query) {
            calls.incrementAndGet();
            lastQuery = query;
            return "【来源：二次函数】顶点横坐标为 -b/(2a)。";
        }
    }
}
