package com.achobeta.refine.learning.mistake.application.port;

import com.achobeta.refine.contracts.learning.GenerationContextResponse;
import com.achobeta.refine.learning.mistake.application.query.MistakeFilter;
import com.achobeta.refine.learning.mistake.application.query.NamedCount;
import com.achobeta.refine.learning.mistake.application.query.ReviewTrend;
import com.achobeta.refine.learning.mistake.application.query.TrickyKnowledge;
import com.achobeta.refine.learning.mistake.domain.MistakeQuestion;

import java.util.List;

public interface MistakeRepository {
    MistakeQuestion find(String userId, String questionId);
    MistakeQuestion create(MistakeQuestion question);
    GenerationContextResponse findGenerationContext(long id, String userId);
    void updateReasons(MistakeQuestion question);
    void updateNote(MistakeQuestion question);
    List<MistakeQuestion> findPage(String userId, MistakeFilter filter, int offset, int size);
    long count(String userId, MistakeFilter filter);
    long overdueCount(String userId);
    List<TrickyKnowledge> trickyKnowledge(String userId);
    List<NamedCount> subjectStats(String userId);
    List<NamedCount> knowledgeStats(String userId);
    List<ReviewTrend> reviewTrend(String userId);
    void deleteBatch(String userId, List<Long> ids);
}
