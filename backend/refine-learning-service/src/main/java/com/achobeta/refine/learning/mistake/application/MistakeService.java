package com.achobeta.refine.learning.mistake.application;

import com.achobeta.refine.common.api.AppException;
import com.achobeta.refine.contracts.event.LearningActivityPayload;
import com.achobeta.refine.contracts.learning.CreateMistakeRequest;
import com.achobeta.refine.contracts.learning.CreateMistakeResponse;
import com.achobeta.refine.contracts.learning.GenerationContextResponse;
import com.achobeta.refine.learning.mistake.application.port.LearningActivityPublisher;
import com.achobeta.refine.learning.mistake.application.port.MistakeRepository;
import com.achobeta.refine.learning.mistake.application.query.MistakeFilter;
import com.achobeta.refine.learning.mistake.application.query.MistakePage;
import com.achobeta.refine.learning.mistake.application.query.NamedCount;
import com.achobeta.refine.learning.mistake.application.query.ReviewTrend;
import com.achobeta.refine.learning.mistake.application.query.TrickyKnowledge;
import com.achobeta.refine.learning.mistake.domain.MistakeQuestion;
import com.achobeta.refine.learning.mistake.domain.MistakeReason;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class MistakeService {
    private final MistakeRepository repository;
    private final LearningActivityPublisher eventPublisher;
    private final Clock clock;

    public MistakeService(MistakeRepository repository, LearningActivityPublisher eventPublisher, Clock clock) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public CreateMistakeResponse create(CreateMistakeRequest request) {
        String questionId = request.questionId() == null || request.questionId().isBlank()
                ? UUID.randomUUID().toString() : request.questionId();
        MistakeQuestion existing = repository.find(request.userId(), questionId);
        if (existing != null) {
            return new CreateMistakeResponse(existing.id(), existing.questionId());
        }
        MistakeQuestion created = repository.create(new MistakeQuestion(null, request.userId(), questionId,
                request.questionContent(), request.subject(), 0, 0, 0, 0, 0, null,
                request.knowledgePointId(), null, 0, request.source(), null, null));
        eventPublisher.publishAfterCommit(request.userId(), new LearningActivityPayload(questionId,
                "ocr".equalsIgnoreCase(request.source()) ? "upload" : "mistake", request.questionContent(),
                request.subject(), request.knowledgePointId()));
        return new CreateMistakeResponse(created.id(), questionId);
    }

    public GenerationContextResponse generationContext(long id, String userId) {
        GenerationContextResponse context = repository.findGenerationContext(id, userId);
        if (context == null) throw notFound();
        return context;
    }

    public MistakeQuestion reasons(String userId, String questionId) {
        MistakeQuestion question = repository.find(userId, questionId);
        if (question == null) throw notFound();
        return question;
    }

    @Transactional
    public MistakeQuestion toggleReason(String userId, String questionId, String reasonName, String otherReasonText) {
        MistakeQuestion current = reasons(userId, questionId);
        try {
            MistakeQuestion updated = current.toggle(MistakeReason.fromApiValue(reasonName))
                    .changeOtherReasonText(otherReasonText);
            repository.updateReasons(updated);
            return updated;
        } catch (IllegalArgumentException exception) {
            throw new AppException(1001, exception.getMessage());
        }
    }

    @Transactional
    public MistakeQuestion updateOtherReason(String userId, String questionId, String text) {
        try {
            MistakeQuestion updated = reasons(userId, questionId).changeOtherReason(text);
            repository.updateReasons(updated);
            return updated;
        } catch (IllegalArgumentException exception) {
            throw new AppException(1001, exception.getMessage());
        }
    }

    @Transactional
    public void updateNote(String userId, String questionId, String note) {
        try {
            repository.updateNote(reasons(userId, questionId).changeNote(note));
        } catch (IllegalArgumentException exception) {
            throw new AppException(1001, exception.getMessage());
        }
    }

    public MistakePage page(String userId, String keyword, List<String> subjects, List<String> errorTypes,
                            String timeRange, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        MistakeFilter filter = filter(keyword, subjects, errorTypes, timeRange);
        List<MistakeQuestion> content = repository.findPage(userId, filter, safePage * safeSize, safeSize);
        long total = repository.count(userId, filter);
        return MistakePage.of(content, total, (total + safeSize - 1) / safeSize, safeSize, safePage,
                safePage == 0, (long) (safePage + 1) * safeSize >= total);
    }

    public long overdueCount(String userId) { return repository.overdueCount(userId); }
    public List<TrickyKnowledge> trickyKnowledge(String userId) { return repository.trickyKnowledge(userId); }
    public List<NamedCount> subjectStats(String userId) { return repository.subjectStats(userId); }
    public List<NamedCount> knowledgeStats(String userId) { return repository.knowledgeStats(userId); }
    public List<ReviewTrend> reviewTrend(String userId) { return repository.reviewTrend(userId); }

    @Transactional
    public void deleteBatch(String userId, List<Long> ids) {
        if (ids != null && !ids.isEmpty()) repository.deleteBatch(userId, ids);
    }

    private MistakeFilter filter(String keyword, List<String> subjects, List<String> errorTypes, String timeRange) {
        List<MistakeReason> reasons;
        try {
            reasons = errorTypes == null ? List.of() : errorTypes.stream().map(MistakeReason::fromApiValue).toList();
        } catch (IllegalArgumentException exception) {
            throw new AppException(1001, exception.getMessage());
        }
        LocalDate now = LocalDate.now(clock);
        LocalDate start = null;
        LocalDate end = null;
        if (timeRange != null && !timeRange.isBlank()) {
            switch (timeRange.toUpperCase(Locale.ROOT)) {
                case "THIS_WEEK" -> start = now.minusDays(7);
                case "THIS_MONTH" -> start = now.withDayOfMonth(1);
                case "THIS_QUARTER" -> start = now.minusMonths(3).withDayOfMonth(1);
                case "THIS_YEAR" -> start = now.withDayOfYear(1);
                case "MORE_THAN_ONE_WEEK" -> end = now.minusDays(7);
                default -> throw new AppException(1001, "invalid timeRange: " + timeRange);
            }
        }
        return new MistakeFilter(keyword, subjects, reasons, start, end);
    }

    private AppException notFound() { return new AppException(10002, "mistake question not found"); }
}
