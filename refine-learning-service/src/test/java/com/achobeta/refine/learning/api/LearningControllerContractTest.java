package com.achobeta.refine.learning.api;

import com.achobeta.refine.common.security.UserContext;
import com.achobeta.refine.learning.mistake.api.ReviewFeedbackController;
import com.achobeta.refine.learning.mistake.application.MistakeService;
import com.achobeta.refine.learning.mistake.application.query.NamedCount;
import com.achobeta.refine.learning.mistake.application.query.MistakePage;
import com.achobeta.refine.learning.mistake.application.query.ReviewTrend;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LearningControllerContractTest {
    @AfterEach
    void clearContext() {
        UserContext.clear();
    }

    @Test
    void reviewListKeepsLegacyPageImplJsonShape() throws Exception {
        MistakeService service = mock(MistakeService.class);
        when(service.page("user-1", null, null, null, null, 0, 10))
                .thenReturn(MistakePage.of(List.of(), 0, 0, 10, 0, true, true));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ReviewFeedbackController(service)).build();
        UserContext.set("user-1");

        mvc.perform(get("/api/v1/feedback/review/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.pageable.pageSize").value(10))
                .andExpect(jsonPath("$.pageable.offset").value(0))
                .andExpect(jsonPath("$.pageable.paged").value(true))
                .andExpect(jsonPath("$.pageable.unpaged").value(false))
                .andExpect(jsonPath("$.pageable.sort.empty").value(true))
                .andExpect(jsonPath("$.sort.sorted").value(false))
                .andExpect(jsonPath("$.sort.unsorted").value(true))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.numberOfElements").value(0))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true))
                .andExpect(jsonPath("$.empty").value(true));
    }

    @Test
    void statisticsKeepsDynamicDistributionJsonShape() throws Exception {
        MistakeService service = mock(MistakeService.class);
        when(service.subjectStats("user-1")).thenReturn(List.of(new NamedCount("math", 2)));
        when(service.knowledgeStats("user-1")).thenReturn(List.of(new NamedCount("algebra", 1)));
        when(service.reviewTrend("user-1")).thenReturn(List.of(new ReviewTrend("2026-07", 4, 3)));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ReviewFeedbackController(service)).build();
        UserContext.set("user-1");

        mvc.perform(get("/api/v1/feedback/review/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjectDistribution[0].math").value(2))
                .andExpect(jsonPath("$.knowledgeDistribution[0].algebra").value(1))
                .andExpect(jsonPath("$.reviewTrend[0].month").value("2026-07"))
                .andExpect(jsonPath("$.reviewTrend[0].completionRate").value(0.75));
    }

}
