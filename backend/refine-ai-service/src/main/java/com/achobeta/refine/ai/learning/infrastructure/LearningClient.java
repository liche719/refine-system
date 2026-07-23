package com.achobeta.refine.ai.learning.infrastructure;

import com.achobeta.refine.contracts.learning.CreateMistakeRequest;
import com.achobeta.refine.contracts.learning.CreateMistakeResponse;
import com.achobeta.refine.contracts.learning.GenerationContextResponse;
import com.achobeta.refine.contracts.learning.EnsureKnowledgePointRequest;
import com.achobeta.refine.contracts.learning.EnsureKnowledgePointResponse;
import com.achobeta.refine.contracts.learning.RecentKnowledgePoint;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "refine-learning-service", configuration = LearningClientConfiguration.class,
        fallbackFactory = LearningClientFallbackFactory.class)
public interface LearningClient {
    @PostMapping("/internal/v1/mistakes")
    CreateMistakeResponse createMistake(@RequestBody CreateMistakeRequest request);

    @PostMapping("/internal/v1/knowledge-points")
    EnsureKnowledgePointResponse ensureKnowledgePoint(@RequestBody EnsureKnowledgePointRequest request);

    @GetMapping("/internal/v1/mistakes/{id}/generation-context")
    GenerationContextResponse generationContext(@PathVariable("id") long id, @RequestParam("userId") String userId);

    @GetMapping("/internal/v1/knowledge-points/recent")
    List<RecentKnowledgePoint> recentKnowledge(@RequestParam("userId") String userId, @RequestParam("limit") int limit);
}
