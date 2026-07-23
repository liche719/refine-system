package com.achobeta.refine.ai.api;

import com.achobeta.refine.ai.analytics.api.LearningAnalysisController;
import com.achobeta.refine.ai.analytics.application.LearningAnalysisService;
import com.achobeta.refine.ai.ocr.OcrController;
import com.achobeta.refine.ai.ocr.OcrUploadProperties;
import com.achobeta.refine.ai.ocr.application.OcrService;
import com.achobeta.refine.ai.question.QuestionController;
import com.achobeta.refine.ai.question.application.QuestionWorkflowService;
import com.achobeta.refine.common.security.UserContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AiGoldenMasterTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @AfterEach
    void clearUserContext() {
        UserContext.clear();
    }

    @Test
    void ocrIngestionMatchesGoldenMaster() throws Exception {
        OcrService ocr = mock(OcrService.class);
        when(ocr.extractFirst(eq("user-1"), any(byte[].class), eq("txt")))
                .thenReturn(new OcrService.OcrResult("What is 2 + 2?", "question-1"));
        UserContext.set("user-1");
        MockMultipartFile file = new MockMultipartFile("file", "question.txt", "text/plain",
                "What is 2 + 2?".getBytes(StandardCharsets.UTF_8));

        MvcResult result = MockMvcBuilders.standaloneSetup(new OcrController(ocr, new OcrUploadProperties())).build()
                .perform(multipart("/api/v1/ocr/extract-first").file(file).param("fileType", "txt"))
                .andExpect(status().isOk())
                .andReturn();

        assertGolden("contracts/ai/ocr-extract.json", result);
    }

    @Test
    void generatedQuestionMatchesGoldenMaster() throws Exception {
        QuestionWorkflowService questions = mock(QuestionWorkflowService.class);
        when(questions.generate("user-1", 12L))
                .thenReturn(new QuestionWorkflowService.GeneratedQuestion("generated-1", "What is 3 + 4?"));
        UserContext.set("user-1");

        MvcResult result = MockMvcBuilders.standaloneSetup(new QuestionController(questions, Runnable::run)).build()
                .perform(post("/api/question/generation").param("mistakeQuestionId", "12"))
                .andExpect(status().isOk())
                .andReturn();

        assertGolden("contracts/ai/question-generation.json", result);
    }

    @Test
    void learningInsightsMatchGoldenMaster() throws Exception {
        LearningAnalysisService analysis = mock(LearningAnalysisService.class);
        when(analysis.insights("user-1", null)).thenReturn(List.of(
                new LearningAnalysisService.Insight("weakness", "Algebra", "Review equations", 0.8D,
                        List.of("question-1"), null, true)));
        UserContext.set("user-1");

        MvcResult result = MockMvcBuilders.standaloneSetup(new LearningAnalysisController(analysis)).build()
                .perform(get("/api/v1/learning-analysis/insights"))
                .andExpect(status().isOk())
                .andReturn();

        assertGolden("contracts/ai/learning-analysis-insights.json", result);
    }

    @Test
    @Timeout(20)
    void judgeSseMatchesGoldenMasterEventOrderAndCompletion() throws Exception {
        QuestionWorkflowService questions = mock(QuestionWorkflowService.class);
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            var onToken = (java.util.function.Consumer<String>) invocation.getArgument(3);
            var onComplete = (Runnable) invocation.getArgument(4);
            onToken.accept("Correct.");
            onToken.accept("Explain the carry step.");
            onComplete.run();
            return null;
        }).when(questions).streamJudge(eq("user-1"), eq("generated-1"), eq("7"), any(), any(), any());
        UserContext.set("user-1");
        ExecutorService executor = Executors.newSingleThreadExecutor();
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new QuestionController(questions, executor)).build();

        try {
            MvcResult pending = mvc.perform(post("/api/question/judge")
                            .param("questionId", "generated-1")
                            .param("answer", "7"))
                    .andExpect(request().asyncStarted())
                    .andReturn();
            MvcResult completed = mvc.perform(asyncDispatch(pending))
                    .andExpect(status().isOk())
                    .andReturn();
            assertThat(completed.getResponse().getContentType()).startsWith(MediaType.TEXT_EVENT_STREAM_VALUE);

            ObjectNode actual = JSON.createObjectNode();
            actual.put("contentType", MediaType.TEXT_EVENT_STREAM_VALUE);
            actual.set("events", parseEvents(completed.getResponse().getContentAsString(StandardCharsets.UTF_8)));
            actual.put("completed", true);
            assertGolden("contracts/ai/question-judge-sse.json", actual);
        } finally {
            executor.shutdownNow();
        }
    }

    private ArrayNode parseEvents(String body) {
        ArrayNode events = JSON.createArrayNode();
        String eventName = "message";
        StringBuilder data = new StringBuilder();
        for (String line : body.split("\\r?\\n")) {
            if (line.startsWith("event:")) {
                eventName = line.substring("event:".length()).trim();
            } else if (line.startsWith("data:")) {
                if (!data.isEmpty()) data.append('\n');
                data.append(line.substring("data:".length()).trim());
            } else if (line.isEmpty() && !data.isEmpty()) {
                ObjectNode event = events.addObject();
                event.put("event", eventName);
                event.put("data", data.toString());
                eventName = "message";
                data.setLength(0);
            }
        }
        if (!data.isEmpty()) {
            ObjectNode event = events.addObject();
            event.put("event", eventName);
            event.put("data", data.toString());
        }
        return events;
    }

    private void assertGolden(String resource, MvcResult result) throws Exception {
        assertGolden(resource, JSON.readTree(result.getResponse().getContentAsString()));
    }

    private void assertGolden(String resource, JsonNode actual) throws Exception {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertThat(stream).as("golden master %s", resource).isNotNull();
            assertGolden(JSON.readTree(stream), actual);
        }
    }

    private void assertGolden(JsonNode expected, JsonNode actual) {
        if (expected.isObject()) {
            assertThat(actual.isObject()).isTrue();
            expected.properties().forEach(entry -> {
                assertThat(actual.has(entry.getKey())).as("field %s", entry.getKey()).isTrue();
                assertGolden(entry.getValue(), actual.get(entry.getKey()));
            });
            return;
        }
        if (expected.isArray()) {
            assertThat(actual.isArray()).isTrue();
            assertThat(actual.size()).isEqualTo(expected.size());
            for (int index = 0; index < expected.size(); index++) assertGolden(expected.get(index), actual.get(index));
            return;
        }
        if (expected.isTextual() && "$any".equals(expected.asText())) return;
        if (expected.isTextual() && "$any-string".equals(expected.asText())) {
            assertThat(actual.isTextual()).isTrue();
            return;
        }
        assertThat(actual).isEqualTo(expected);
    }
}
