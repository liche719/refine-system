package com.achobeta.refine.learning.knowledge.api;

import com.achobeta.refine.common.security.UserContext;
import com.achobeta.refine.learning.knowledge.application.KnowledgeService;
import com.achobeta.refine.learning.knowledge.application.query.KnowledgeSummary;
import com.achobeta.refine.learning.knowledge.application.query.KnowledgeTooltip;
import com.achobeta.refine.learning.knowledge.application.query.RelatedQuestion;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/keypoints_explanation")
public class KeyPointsExplanationController {
    private final KnowledgeService service;

    public KeyPointsExplanationController(KnowledgeService service) { this.service = service; }

    @GetMapping("/get_key_points")
    public ResponseEntity<List<KnowledgeSummary>> roots(@RequestParam(required = false) String subject) {
        return ResponseEntity.ok(service.roots(UserContext.get(), subject));
    }

    @GetMapping("/get_son_key_points")
    public List<KnowledgeSummary> children(@RequestParam int knowledgeId) {
        return service.children(UserContext.get(), knowledgeId);
    }

    @GetMapping("/{knowledgeId}")
    public ResponseEntity<String> description(@PathVariable int knowledgeId) {
        String value = service.description(UserContext.get(), knowledgeId);
        return ResponseEntity.ok(value == null || value.isBlank() ? "暂无知识点详情" : value);
    }

    @GetMapping("/{knowledgeId}/related-questions-statistic")
    public ResponseEntity<String> relatedStatistic(@PathVariable int knowledgeId) {
        int count = service.relatedQuestions(UserContext.get(), knowledgeId).size();
        return ResponseEntity.ok(count == 0 ? "暂无相关错题" : "该知识点共关联 " + count + " 道错题");
    }

    @GetMapping("/{knowledgeId}/related-questions")
    public ResponseEntity<RelatedQuestionsResponse> relatedQuestions(@PathVariable int knowledgeId) {
        String userId = UserContext.get();
        return ResponseEntity.ok(new RelatedQuestionsResponse(service.relatedQuestions(userId, knowledgeId),
                valueOrEmpty(service.note(userId, knowledgeId))));
    }

    @PostMapping("/{knowledgeId}/mark-as-mastered")
    public ResponseEntity<String> mastered(@PathVariable int knowledgeId) {
        service.markMastered(UserContext.get(), knowledgeId);
        return ResponseEntity.ok("已修改成功");
    }

    @GetMapping("/{knowledgeId}/related-points")
    public ResponseEntity<List<KnowledgeSummary>> relatedPoints(@PathVariable int knowledgeId) {
        return ResponseEntity.ok(service.children(UserContext.get(), knowledgeId));
    }

    @PostMapping("/{knowledgeId}/notes")
    public ResponseEntity<String> note(@PathVariable int knowledgeId, @RequestBody String note) {
        service.updateNote(UserContext.get(), knowledgeId, note);
        return ResponseEntity.ok("笔记更新成功");
    }

    @PostMapping("/{knowledgeId}/rename")
    public ResponseEntity<String> rename(@PathVariable int knowledgeId, @RequestBody String newName) {
        service.rename(UserContext.get(), knowledgeId, newName);
        return ResponseEntity.ok("重命名成功");
    }

    @GetMapping("/{knowledgeId}/show-tooltip")
    public ResponseEntity<TooltipResponse> tooltip(@PathVariable int knowledgeId) {
        KnowledgeTooltip source = service.tooltip(UserContext.get(), knowledgeId);
        long total = source == null ? 0 : source.total();
        long reviewed = source == null ? 0 : source.count();
        return ResponseEntity.ok(new TooltipResponse(reviewed,
                source == null || source.lastReviewTime() == null ? "" : source.lastReviewTime().toString(),
                total == 0 ? 0D : (double) reviewed / total));
    }

    @PostMapping("/{knowledgeId}/add-son-point")
    public ResponseEntity<String> addChild(@PathVariable int knowledgeId, @RequestBody SonPoint request) {
        service.addChild(UserContext.get(), knowledgeId, request.pointName(), request.pointDesc(), request.subject());
        return ResponseEntity.ok("添加成功");
    }

    private String valueOrEmpty(String value) { return value == null ? "" : value; }
    public record RelatedQuestionsResponse(List<RelatedQuestion> questions, String note) { }
    public record TooltipResponse(long count, String lastReviewTime, double degreeOfProficiency) { }
    public record SonPoint(String pointId, String pointName, String pointDesc, String subject, List<SonPoint> sonPoints) { }
}
