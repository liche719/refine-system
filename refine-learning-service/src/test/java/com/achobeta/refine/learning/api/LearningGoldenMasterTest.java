package com.achobeta.refine.learning.api;

import com.achobeta.refine.common.security.UserContext;
import com.achobeta.refine.learning.mistake.api.MistakeReasonController;
import com.achobeta.refine.learning.mistake.api.ReviewFeedbackController;
import com.achobeta.refine.learning.mistake.application.MistakeService;
import com.achobeta.refine.learning.mistake.application.query.MistakePage;
import com.achobeta.refine.learning.mistake.domain.MistakeQuestion;
import com.achobeta.refine.learning.overview.api.LearningOverviewController;
import com.achobeta.refine.learning.overview.application.OverviewService;
import com.achobeta.refine.learning.overview.application.query.LearningOverview;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LearningGoldenMasterTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @AfterEach
    void clearUserContext() {
        UserContext.clear();
    }

    @Test
    void reviewListMatchesGoldenMasterIncludingPaginationAndFilters() throws Exception {
        MistakeService mistakes = mock(MistakeService.class);
        MistakeQuestion question = question();
        when(mistakes.page("user-1", "2", List.of("math"), List.of("careless"), "THIS_WEEK", 1, 5))
                .thenReturn(MistakePage.of(List.of(question), 6, 2, 5, 1, false, false));
        UserContext.set("user-1");

        MvcResult result = MockMvcBuilders.standaloneSetup(new ReviewFeedbackController(mistakes)).build()
                .perform(get("/api/v1/feedback/review/list")
                        .param("keyword", "2")
                        .param("subject", "math")
                        .param("errorType", "careless")
                        .param("timeRange", "THIS_WEEK")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andReturn();

        assertGolden("contracts/learning/review-list.json", result);
    }

    @Test
    void mistakeDetailMatchesGoldenMaster() throws Exception {
        MistakeService mistakes = mock(MistakeService.class);
        when(mistakes.reasons("user-1", "question-1")).thenReturn(question());
        UserContext.set("user-1");

        MvcResult result = MockMvcBuilders.standaloneSetup(new MistakeReasonController(mistakes)).build()
                .perform(get("/api/v1/mistake-reason/get").param("questionId", "question-1"))
                .andExpect(status().isOk())
                .andReturn();

        assertGolden("contracts/learning/mistake-detail.json", result);
    }

    @Test
    void reviewDetailRestoresQuestionById() throws Exception {
        MistakeService mistakes = mock(MistakeService.class);
        when(mistakes.reasons("user-1", "question-1")).thenReturn(question());
        UserContext.set("user-1");

        MvcResult result = MockMvcBuilders.standaloneSetup(new ReviewFeedbackController(mistakes)).build()
                .perform(get("/api/v1/feedback/review/detail").param("questionId", "question-1"))
                .andExpect(status().isOk())
                .andReturn();

        assertGolden("contracts/learning/review-detail.json", result);
    }

    @Test
    void mistakeReasonSwitchMatchesGoldenMaster() throws Exception {
        MistakeService mistakes = mock(MistakeService.class);
        MistakeQuestion switched = new MistakeQuestion(12L, "user-1", "question-1", "2 + 2", "math",
                0, 0, 0, 0, 0, null, 7, "review addition", 0, "ocr", null, null);
        when(mistakes.toggleReason("user-1", "question-1", "careless", null)).thenReturn(switched);
        UserContext.set("user-1");

        MvcResult result = MockMvcBuilders.standaloneSetup(new MistakeReasonController(mistakes)).build()
                .perform(post("/api/v1/mistake-reason/toggle/careless")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"forged-user\",\"questionId\":\"question-1\"}"))
                .andExpect(status().isOk())
                .andReturn();

        verify(mistakes).toggleReason(eq("user-1"), eq("question-1"), eq("careless"), eq(null));
        assertGolden("contracts/learning/mistake-reason-switch.json", result);
    }

    @Test
    void studyNoteUpdateMatchesGoldenMaster() throws Exception {
        MistakeService mistakes = mock(MistakeService.class);
        doNothing().when(mistakes).updateNote("user-1", "question-1", "review addition");
        UserContext.set("user-1");

        MvcResult result = MockMvcBuilders.standaloneSetup(new MistakeReasonController(mistakes)).build()
                .perform(post("/api/v1/mistake-reason/study-note/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"forged-user\",\"questionId\":\"question-1\",\"studyNote\":\"review addition\"}"))
                .andExpect(status().isOk())
                .andReturn();

        verify(mistakes).updateNote("user-1", "question-1", "review addition");
        assertGolden("contracts/learning/study-note-update.json", result);
    }

    @Test
    void overviewMatchesGoldenMaster() throws Exception {
        OverviewService overview = mock(OverviewService.class);
        when(overview.overview("user-1")).thenReturn(new LearningOverview(12, 0.75D, 3, 45));
        UserContext.set("user-1");

        MvcResult result = MockMvcBuilders.standaloneSetup(new LearningOverviewController(overview)).build()
                .perform(get("/api/v1/overview/get_overview"))
                .andExpect(status().isOk())
                .andReturn();

        assertGolden("contracts/learning/overview.json", result);
    }

    private MistakeQuestion question() {
        return new MistakeQuestion(12L, "user-1", "question-1", "2 + 2", "math",
                1, 0, 0, 0, 0, null, 7, "review addition", 0, "ocr", null, null);
    }

    private void assertGolden(String resource, MvcResult result) throws Exception {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertThat(stream).as("golden master %s", resource).isNotNull();
            assertGolden(JSON.readTree(stream), JSON.readTree(result.getResponse().getContentAsString()));
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
