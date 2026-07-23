package com.achobeta.refine.learning.knowledge.infrastructure;

import com.achobeta.refine.contracts.learning.RecentKnowledgePoint;
import com.achobeta.refine.learning.knowledge.application.port.KnowledgeRepository;
import com.achobeta.refine.learning.knowledge.application.query.KnowledgeSummary;
import com.achobeta.refine.learning.knowledge.application.query.KnowledgeTooltip;
import com.achobeta.refine.learning.knowledge.application.query.RelatedQuestion;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class KnowledgeRepositoryAdapter implements KnowledgeRepository {
    private final KnowledgeMapper mapper;
    public KnowledgeRepositoryAdapter(KnowledgeMapper mapper) { this.mapper = mapper; }

    @Override public List<KnowledgeSummary> roots(String userId, String subject) { return mapper.roots(userId, subject); }
    @Override public List<KnowledgeSummary> children(String userId, int parentId) { return mapper.children(userId, parentId); }
    @Override public String description(String userId, int id) { return mapper.description(userId, id); }
    @Override public List<RecentKnowledgePoint> recent(String userId, int limit) { return mapper.recent(userId, limit); }
    @Override public List<RelatedQuestion> relatedQuestions(String userId, int id) { return mapper.relatedQuestions(userId, id); }
    @Override public String note(String userId, int id) { return mapper.note(userId, id); }
    @Override public KnowledgeTooltip tooltip(String userId, int id) { return mapper.tooltip(userId, id); }
    @Override public boolean markMastered(String userId, int id) { return mapper.markMastered(userId, id) > 0; }
    @Override public boolean updateNote(String userId, int id, String note) { return mapper.updateNote(userId, id, note) > 0; }
    @Override public boolean rename(String userId, int id, String name) { return mapper.rename(userId, id, name) > 0; }
    @Override public int addChild(String userId, int parentId, String name, String description, String subject) {
        int id = mapper.nextId(userId);
        mapper.addChild(userId, id, parentId, name, description, subject);
        return id;
    }
    @Override public Integer findRootId(String userId, String subject, String name) {
        return mapper.findRootId(userId, subject, name);
    }
    @Override public int addRoot(String userId, String name, String description, String subject) {
        int id = mapper.nextId(userId);
        mapper.addRoot(userId, id, name, description, subject);
        return id;
    }
}
