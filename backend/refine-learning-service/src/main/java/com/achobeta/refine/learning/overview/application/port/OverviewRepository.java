package com.achobeta.refine.learning.overview.application.port;

import com.achobeta.refine.learning.overview.application.query.OverviewCounts;
import com.achobeta.refine.learning.overview.application.query.StudyDynamic;

public interface OverviewRepository {
    OverviewCounts overview(String userId);
    int studyTime(String userId);
    StudyDynamic dynamic(String userId);
}
