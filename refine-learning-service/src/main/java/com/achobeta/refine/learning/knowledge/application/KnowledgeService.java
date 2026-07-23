package com.achobeta.refine.learning.knowledge.application;

import com.achobeta.refine.common.api.AppException;
import com.achobeta.refine.contracts.learning.RecentKnowledgePoint;
import com.achobeta.refine.contracts.learning.EnsureKnowledgePointRequest;
import com.achobeta.refine.contracts.learning.EnsureKnowledgePointResponse;
import com.achobeta.refine.learning.knowledge.application.port.KnowledgeRepository;
import com.achobeta.refine.learning.knowledge.application.query.KnowledgeSummary;
import com.achobeta.refine.learning.knowledge.application.query.KnowledgeTooltip;
import com.achobeta.refine.learning.knowledge.application.query.RelatedQuestion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class KnowledgeService {
    private final KnowledgeRepository repository;
    public KnowledgeService(KnowledgeRepository repository) { this.repository = repository; }

    public List<KnowledgeSummary> roots(String userId, String subject) { return repository.roots(userId, subject); }
    public List<KnowledgeSummary> children(String userId, int parentId) { return repository.children(userId, parentId); }
    public String description(String userId, int id) { return repository.description(userId, id); }
    public List<RecentKnowledgePoint> recent(String userId, int limit) { return repository.recent(userId, Math.min(Math.max(limit, 1), 100)); }
    public List<RelatedQuestion> relatedQuestions(String userId, int id) { return repository.relatedQuestions(userId, id); }
    public String note(String userId, int id) { return repository.note(userId, id); }
    public KnowledgeTooltip tooltip(String userId, int id) { return repository.tooltip(userId, id); }
    @Transactional public void markMastered(String userId, int id) { requireChanged(repository.markMastered(userId, id)); }
    @Transactional public void updateNote(String userId, int id, String note) { requireChanged(repository.updateNote(userId, id, note)); }
    @Transactional public void rename(String userId, int id, String name) { requireChanged(repository.rename(userId, id, name)); }
    @Transactional public int addChild(String userId, int parentId, String name, String description, String subject) {
        return repository.addChild(userId, parentId, name, description, subject);
    }
    @Transactional
    public EnsureKnowledgePointResponse ensureRoot(EnsureKnowledgePointRequest request) {
        Integer existing = repository.findRootId(request.userId(), request.subject(), request.name());
        int knowledgePointId = existing == null
                ? repository.addRoot(request.userId(), request.name(), request.description(), request.subject())
                : existing;
        return new EnsureKnowledgePointResponse(knowledgePointId);
    }
    private void requireChanged(boolean changed) { if (!changed) throw new AppException(10002, "knowledge point not found"); }
}
