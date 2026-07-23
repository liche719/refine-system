package com.achobeta.refine.learning.overview.infrastructure;

import com.achobeta.refine.learning.overview.application.port.OverviewRepository;
import com.achobeta.refine.learning.overview.application.query.OverviewCounts;
import com.achobeta.refine.learning.overview.application.query.StudyDynamic;
import org.springframework.stereotype.Repository;

@Repository
public class OverviewRepositoryAdapter implements OverviewRepository {
    private final OverviewMapper mapper;
    public OverviewRepositoryAdapter(OverviewMapper mapper) { this.mapper = mapper; }
    @Override public OverviewCounts overview(String userId) { return mapper.overview(userId); }
    @Override public int studyTime(String userId) { Integer value = mapper.studyTime(userId); return value == null ? 0 : value; }
    @Override public StudyDynamic dynamic(String userId) { return mapper.dynamic(userId); }
}
