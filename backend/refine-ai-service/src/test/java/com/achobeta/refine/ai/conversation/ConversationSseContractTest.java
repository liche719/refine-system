package com.achobeta.refine.ai.conversation;

import com.achobeta.refine.ai.conversation.application.ConversationService;
import com.achobeta.refine.common.security.UserContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ConversationSseContractTest {
    @Test
    @Timeout(10)
    void sendMessageKeepsUrlContentTypeAndTokenOrder() throws Exception {
        ConversationService service = mock(ConversationService.class);
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            var onToken = (java.util.function.Consumer<String>) invocation.getArgument(3);
            var onComplete = (Runnable) invocation.getArgument(4);
            onToken.accept("第一步");
            onToken.accept("second");
            onComplete.run();
            return null;
        }).when(service).streamSend(eq("user-1"), eq("conversation-1"), eq("你好"),
                any(), any(), any());

        ExecutorService executor = Executors.newSingleThreadExecutor();
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ConversationController(service, executor)).build();
        UserContext.set("user-1");
        try {
            MvcResult pending = mvc.perform(post("/api/v1/conversation/send-message")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"conversationId\":\"conversation-1\",\"message\":\"你好\"}"))
                    .andExpect(request().asyncStarted()).andReturn();
            MvcResult completed = mvc.perform(asyncDispatch(pending)).andExpect(status().isOk()).andReturn();
            assertThat(completed.getResponse().getContentType()).startsWith(MediaType.TEXT_EVENT_STREAM_VALUE);
            String body = completed.getResponse().getContentAsString(StandardCharsets.UTF_8);
            assertThat(body).contains("data:第一步", "data:second");
            assertThat(body.indexOf("data:第一步")).isLessThan(body.indexOf("data:second"));
        } finally {
            UserContext.clear();
            executor.shutdownNow();
        }
    }

    @Test
    @Timeout(10)
    void modelFailureStaysInsideSseResponse() throws Exception {
        ConversationService service = mock(ConversationService.class);
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            var onError = (java.util.function.Consumer<Throwable>) invocation.getArgument(5);
            onError.accept(new IllegalStateException("remote failure"));
            return null;
        }).when(service).streamSend(eq("user-1"), eq("conversation-1"), eq("你好"),
                any(), any(), any());

        ExecutorService executor = Executors.newSingleThreadExecutor();
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ConversationController(service, executor)).build();
        UserContext.set("user-1");
        try {
            MvcResult pending = mvc.perform(post("/api/v1/conversation/send-message")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"conversationId\":\"conversation-1\",\"message\":\"你好\"}"))
                    .andExpect(request().asyncStarted()).andReturn();
            MvcResult completed = mvc.perform(asyncDispatch(pending)).andExpect(status().isOk()).andReturn();
            assertThat(completed.getResponse().getContentType()).startsWith(MediaType.TEXT_EVENT_STREAM_VALUE);
            assertThat(completed.getResponse().getContentAsString(StandardCharsets.UTF_8))
                    .contains("event:error", "AI 服务暂时不可用");
        } finally {
            UserContext.clear();
            executor.shutdownNow();
        }
    }
}
