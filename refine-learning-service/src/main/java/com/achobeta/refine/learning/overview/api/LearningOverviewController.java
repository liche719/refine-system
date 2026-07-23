package com.achobeta.refine.learning.overview.api;

import com.achobeta.refine.common.security.UserContext;
import com.achobeta.refine.learning.overview.application.OverviewService;
import com.achobeta.refine.learning.overview.application.query.LearningOverview;
import com.achobeta.refine.learning.overview.application.query.StudyDynamic;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/overview")
public class LearningOverviewController {
    private final OverviewService service;

    public LearningOverviewController(OverviewService service) { this.service = service; }

    @GetMapping("/get_overview")
    public LearningOverview overview() { return service.overview(UserContext.get()); }

    @GetMapping("/get_study_dynamic")
    public ResponseEntity<StudyDynamic> dynamic() { return ResponseEntity.ok(service.dynamic(UserContext.get())); }
}
