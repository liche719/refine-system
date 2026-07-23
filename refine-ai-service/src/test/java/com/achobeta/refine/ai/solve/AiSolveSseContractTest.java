package com.achobeta.refine.ai.solve;

import com.achobeta.refine.ai.solve.application.port.SolveAiPort;
import com.achobeta.refine.ai.solve.application.AiSolveService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AiSolveSseContractTest {
    @Test
    @Timeout(10)
    void streamKeepsContentTypeAndRemoteTokenOrder() throws Exception {
        SolveAiPort ai = new SolveAiPort() {
            @Override
            public String solve(String questionContext) {
                return "unused";
            }

            @Override
            public void streamSolve(String questionContext, java.util.function.Consumer<String> onToken,
                                    Runnable onComplete, java.util.function.Consumer<Throwable> onError) {
                onToken.accept("first");
                onToken.accept("second");
                onComplete.run();
            }
        };
        ExecutorService executor = Executors.newSingleThreadExecutor();
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new AiSolveController(new AiSolveService(ai), executor)).build();

        try {
            MvcResult pending = mvc.perform(post("/api/v1/solve/stream")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"questionContext\":\"2 + 2\"}"))
                    .andExpect(request().asyncStarted()).andReturn();
            MvcResult completed = mvc.perform(asyncDispatch(pending)).andExpect(status().isOk()).andReturn();
            assertThat(completed.getResponse().getContentType()).startsWith(MediaType.TEXT_EVENT_STREAM_VALUE);
            String body = completed.getResponse().getContentAsString();
            assertThat(body).contains("data:first", "data:second");
            assertThat(body.indexOf("data:first")).isLessThan(body.indexOf("data:second"));
        } finally {
            executor.shutdownNow();
        }
    }
}
