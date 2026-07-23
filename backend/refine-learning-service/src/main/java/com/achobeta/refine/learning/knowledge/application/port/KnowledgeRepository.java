package com.achobeta.refine.learning.knowledge.application.port;

import com.achobeta.refine.contracts.learning.RecentKnowledgePoint;
import com.achobeta.refine.learning.knowledge.application.query.KnowledgeSummary;
import com.achobeta.refine.learning.knowledge.application.query.KnowledgeTooltip;
import com.achobeta.refine.learning.knowledge.application.query.RelatedQuestion;

import java.util.List;

public interface KnowledgeRepository {
    List<KnowledgeSummary> roots(String userId, String subject);
    List<KnowledgeSummary> children(String userId, int parentId);
    String description(String userId, int id);
    List<RecentKnowledgePoint> recent(String userId, int limit);
    List<RelatedQuestion> relatedQuestions(String userId, int id);
    String note(String userId, int id);
    KnowledgeTooltip tooltip(String userId, int id);
    boolean markMastered(String userId, int id);
    boolean updateNote(String userId, int id, String note);
    boolean rename(String userId, int id, String name);
    int addChild(String userId, int parentId, String name, String description, String subject);
    Integer findRootId(String userId, String subject, String name);
    int addRoot(String userId, String name, String description, String subject);
}
