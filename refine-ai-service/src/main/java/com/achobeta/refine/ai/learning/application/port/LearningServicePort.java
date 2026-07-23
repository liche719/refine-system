package com.achobeta.refine.ai.learning.application.port;

import com.achobeta.refine.contracts.learning.CreateMistakeRequest;
import com.achobeta.refine.contracts.learning.CreateMistakeResponse;
import com.achobeta.refine.contracts.learning.GenerationContextResponse;
import com.achobeta.refine.contracts.learning.EnsureKnowledgePointRequest;
import com.achobeta.refine.contracts.learning.EnsureKnowledgePointResponse;
import com.achobeta.refine.contracts.learning.RecentKnowledgePoint;

import java.util.List;

public interface LearningServicePort {
    CreateMistakeResponse createMistake(CreateMistakeRequest request);
    EnsureKnowledgePointResponse ensureKnowledgePoint(EnsureKnowledgePointRequest request);
    GenerationContextResponse generationContext(long id, String userId);
    List<RecentKnowledgePoint> recentKnowledge(String userId, int limit);
}
