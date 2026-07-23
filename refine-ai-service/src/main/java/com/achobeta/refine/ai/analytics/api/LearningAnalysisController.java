package com.achobeta.refine.ai.analytics.api;

import com.achobeta.refine.ai.analytics.application.LearningAnalysisService;
import com.achobeta.refine.common.api.Response;
import com.achobeta.refine.common.security.UserContext;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/learning-analysis")
public class LearningAnalysisController {
    private final LearningAnalysisService service;

    public LearningAnalysisController(LearningAnalysisService service) {
        this.service = service;
    }

    @GetMapping("/insights")
    public Response<List<LearningAnalysisService.Insight>> insights() {
        return Response.success(service.insights(UserContext.get(), null));
    }

    @GetMapping("/weaknesses")
    public Response<List<LearningAnalysisService.Insight>> weaknesses() {
        return Response.success(service.insights(UserContext.get(), "weakness"));
    }

    @GetMapping("/recommendations")
    public Response<List<LearningAnalysisService.Insight>> recommendations() {
        return Response.success(service.insights(UserContext.get(), "recommendation"));
    }

    @GetMapping("/similar-questions")
    public Response<List<LearningAnalysisService.SimilarQuestion>> similarQuestions(
            @RequestParam @NotBlank String queryText,
            @RequestParam(defaultValue = "5") @Min(1) @Max(20) int limit) {
        return Response.success(service.similarQuestions(UserContext.get(), queryText, limit));
    }

    @PostMapping("/trigger-analysis")
    public Response<Boolean> trigger() {
        return Response.success(service.trigger(UserContext.get()));
    }

    @GetMapping("/dynamics")
    public Response<List<LearningAnalysisService.LearningDynamic>> dynamics() {
        return Response.success(service.dynamics(UserContext.get()));
    }
}
