package com.achobeta.refine.learning.overview.application;

import com.achobeta.refine.learning.overview.application.port.OverviewRepository;
import com.achobeta.refine.learning.overview.application.query.LearningOverview;
import com.achobeta.refine.learning.overview.application.query.OverviewCounts;
import com.achobeta.refine.learning.overview.application.query.StudyDynamic;
import org.springframework.stereotype.Service;

@Service
public class OverviewService {
    private final OverviewRepository repository;
    public OverviewService(OverviewRepository repository) { this.repository = repository; }
    public LearningOverview overview(String userId) {
        OverviewCounts counts = repository.overview(userId);
        return new LearningOverview(Math.toIntExact(counts.questionsNum()), counts.reviewRate(),
                Math.toIntExact(counts.hardQuestions()), repository.studyTime(userId));
    }
    public StudyDynamic dynamic(String userId) { return repository.dynamic(userId); }
}
