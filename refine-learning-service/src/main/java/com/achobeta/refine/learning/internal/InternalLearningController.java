package com.achobeta.refine.learning.internal;

import com.achobeta.refine.contracts.learning.CreateMistakeRequest;
import com.achobeta.refine.contracts.learning.CreateMistakeResponse;
import com.achobeta.refine.contracts.learning.GenerationContextResponse;
import com.achobeta.refine.contracts.learning.RecentKnowledgePoint;
import com.achobeta.refine.contracts.learning.EnsureKnowledgePointRequest;
import com.achobeta.refine.contracts.learning.EnsureKnowledgePointResponse;
import com.achobeta.refine.learning.knowledge.application.KnowledgeService;
import com.achobeta.refine.learning.mistake.application.MistakeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/v1")
public class InternalLearningController {
    private final MistakeService mistakeService;
    private final KnowledgeService knowledgeService;

    public InternalLearningController(MistakeService mistakeService, KnowledgeService knowledgeService) {
        this.mistakeService = mistakeService;
        this.knowledgeService = knowledgeService;
    }

    @PostMapping("/mistakes")
    public CreateMistakeResponse create(@Valid @RequestBody CreateMistakeRequest request) {
        return mistakeService.create(request);
    }

    @PostMapping("/knowledge-points")
    public EnsureKnowledgePointResponse ensureKnowledgePoint(@Valid @RequestBody EnsureKnowledgePointRequest request) {
        return knowledgeService.ensureRoot(request);
    }

    @GetMapping("/mistakes/{id}/generation-context")
    public GenerationContextResponse generationContext(@PathVariable long id, @RequestParam String userId) {
        return mistakeService.generationContext(id, userId);
    }

    @GetMapping("/knowledge-points/recent")
    public List<RecentKnowledgePoint> recent(@RequestParam String userId, @RequestParam(defaultValue = "10") int limit) {
        return knowledgeService.recent(userId, limit);
    }
}
