package com.achobeta.refine.learning.mistake.infrastructure;

import com.achobeta.refine.contracts.learning.GenerationContextResponse;
import com.achobeta.refine.learning.mistake.application.port.MistakeRepository;
import com.achobeta.refine.learning.mistake.application.query.MistakeFilter;
import com.achobeta.refine.learning.mistake.application.query.NamedCount;
import com.achobeta.refine.learning.mistake.application.query.ReviewTrend;
import com.achobeta.refine.learning.mistake.application.query.TrickyKnowledge;
import com.achobeta.refine.learning.mistake.domain.MistakeQuestion;
import com.achobeta.refine.learning.mistake.domain.MistakeReason;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MistakeRepositoryAdapter implements MistakeRepository {
    private final MistakeMapper mapper;

    public MistakeRepositoryAdapter(MistakeMapper mapper) {
        this.mapper = mapper;
    }

    @Override public MistakeQuestion find(String userId, String questionId) { return mapper.findByUserAndQuestion(userId, questionId); }

    @Override
    public MistakeQuestion create(MistakeQuestion question) {
        MistakeMapper.MutableMistake row = new MistakeMapper.MutableMistake();
        row.userId = question.userId(); row.questionId = question.questionId();
        row.questionContent = question.questionContent(); row.subject = question.subject();
        row.knowledgePointId = question.knowledgePointId(); row.source = question.source();
        mapper.insert(row);
        return new MistakeQuestion(row.id, question.userId(), question.questionId(), question.questionContent(),
                question.subject(), 0, 0, 0, 0, 0, null, question.knowledgePointId(), null, 0,
                question.source(), null, null);
    }

    @Override public GenerationContextResponse findGenerationContext(long id, String userId) { return mapper.findGenerationContext(id, userId); }
    @Override public void updateReasons(MistakeQuestion question) { mapper.updateReasons(question); }
    @Override public void updateNote(MistakeQuestion question) { mapper.updateNote(question.userId(), question.questionId(), question.studyNote()); }

    @Override
    public List<MistakeQuestion> findPage(String userId, MistakeFilter filter, int offset, int size) {
        return mapper.findPage(userId, filter.keyword(), filter.subjects(), has(filter, MistakeReason.CARELESS),
                has(filter, MistakeReason.UNFAMILIAR), has(filter, MistakeReason.CALCULATION_ERROR),
                has(filter, MistakeReason.TIME_SHORTAGE), has(filter, MistakeReason.OTHER),
                filter.startDate(), filter.endDate(), offset, size);
    }

    @Override
    public long count(String userId, MistakeFilter filter) {
        return mapper.count(userId, filter.keyword(), filter.subjects(), has(filter, MistakeReason.CARELESS),
                has(filter, MistakeReason.UNFAMILIAR), has(filter, MistakeReason.CALCULATION_ERROR),
                has(filter, MistakeReason.TIME_SHORTAGE), has(filter, MistakeReason.OTHER), filter.startDate(), filter.endDate());
    }

    @Override public long overdueCount(String userId) { return mapper.overdueCount(userId); }
    @Override public List<TrickyKnowledge> trickyKnowledge(String userId) { return mapper.trickyKnowledge(userId); }
    @Override public List<NamedCount> subjectStats(String userId) { return mapper.subjectStats(userId); }
    @Override public List<NamedCount> knowledgeStats(String userId) { return mapper.knowledgeStats(userId); }
    @Override public List<ReviewTrend> reviewTrend(String userId) { return mapper.reviewTrend(userId); }
    @Override public void deleteBatch(String userId, List<Long> ids) { mapper.deleteBatch(userId, ids); }

    private boolean has(MistakeFilter filter, MistakeReason reason) { return filter.reasons().contains(reason); }
}
