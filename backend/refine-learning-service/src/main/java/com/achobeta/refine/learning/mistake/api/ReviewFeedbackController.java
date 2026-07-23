package com.achobeta.refine.learning.mistake.api;

import com.achobeta.refine.common.api.Response;
import com.achobeta.refine.common.security.UserContext;
import com.achobeta.refine.learning.mistake.application.MistakeService;
import com.achobeta.refine.learning.mistake.application.query.MistakePage;
import com.achobeta.refine.learning.mistake.application.query.NamedCount;
import com.achobeta.refine.learning.mistake.application.query.ReviewTrend;
import com.achobeta.refine.learning.mistake.application.query.TrickyKnowledge;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/feedback/review")
public class ReviewFeedbackController {
    private final MistakeService service;

    public ReviewFeedbackController(MistakeService service) { this.service = service; }

    @GetMapping("/overdue-count")
    public Response<OverdueResponse> overdue() {
        return Response.success(new OverdueResponse(service.overdueCount(UserContext.get()),
                "\u8d85\u8fc7\u4e00\u5468\u672a\u590d\u4e60\u7684\u9898\u76ee"));
    }

    @GetMapping("/tricky_knowledge")
    public Response<List<TrickyKnowledge>> tricky() {
        return Response.success(service.trickyKnowledge(UserContext.get()));
    }

    @GetMapping("/list")
    public ResponseEntity<MistakePage> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<String> subject,
            @RequestParam(required = false) List<String> errorType,
            @RequestParam(required = false) String timeRange,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(service.page(UserContext.get(), keyword, subject, errorType, timeRange, page, size));
    }

    @GetMapping("/detail")
    public Response<MistakeDetailResponse> detail(@RequestParam String questionId) {
        var question = service.reasons(UserContext.get(), questionId);
        return Response.success(new MistakeDetailResponse(question.questionId(), question.questionContent(), question.subject()));
    }

    @DeleteMapping("/deleteBatch")
    public ResponseEntity<String> delete(@RequestParam List<Long> questionIds) {
        service.deleteBatch(UserContext.get(), questionIds);
        return ResponseEntity.ok("\u5220\u9664\u6210\u529f");
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        String userId = UserContext.get();
        List<DistributionEntry> subjects = service.subjectStats(userId).stream().map(DistributionEntry::from).toList();
        List<DistributionEntry> knowledge = service.knowledgeStats(userId).stream().map(DistributionEntry::from).toList();
        List<ReviewTrendResponse> trends = service.reviewTrend(userId).stream().map(ReviewTrendResponse::from).toList();
        return ResponseEntity.ok(new StatisticsResponse(subjects, knowledge, trends));
    }

    public record OverdueResponse(long count, String description) { }
    public record MistakeDetailResponse(String questionId, String questionText, String subject) { }
    public record StatisticsResponse(List<DistributionEntry> subjectDistribution,
                                     List<DistributionEntry> knowledgeDistribution,
                                     List<ReviewTrendResponse> reviewTrend) { }
    public record ReviewTrendResponse(String month, long total, long reviewed, double completionRate) {
        static ReviewTrendResponse from(ReviewTrend value) {
            return new ReviewTrendResponse(value.month(), value.total(), value.reviewed(), value.completionRate());
        }
    }
    public record DistributionEntry(String name, long count) {
        static DistributionEntry from(NamedCount value) { return new DistributionEntry(value.name(), value.count()); }
        @JsonValue public Map<String, Long> asJson() { return Map.of(name == null ? "" : name, count); }
    }
}
