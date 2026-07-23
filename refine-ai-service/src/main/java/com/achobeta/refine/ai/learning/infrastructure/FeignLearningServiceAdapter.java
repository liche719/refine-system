package com.achobeta.refine.ai.learning.infrastructure;

import com.achobeta.refine.ai.learning.application.port.LearningServicePort;
import com.achobeta.refine.contracts.learning.CreateMistakeRequest;
import com.achobeta.refine.contracts.learning.CreateMistakeResponse;
import com.achobeta.refine.contracts.learning.GenerationContextResponse;
import com.achobeta.refine.contracts.learning.EnsureKnowledgePointRequest;
import com.achobeta.refine.contracts.learning.EnsureKnowledgePointResponse;
import com.achobeta.refine.contracts.learning.RecentKnowledgePoint;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FeignLearningServiceAdapter implements LearningServicePort {
    private final LearningClient client;
    public FeignLearningServiceAdapter(LearningClient client) { this.client = client; }
    @Override public CreateMistakeResponse createMistake(CreateMistakeRequest request) { return client.createMistake(request); }
    @Override public EnsureKnowledgePointResponse ensureKnowledgePoint(EnsureKnowledgePointRequest request) { return client.ensureKnowledgePoint(request); }
    @Override public GenerationContextResponse generationContext(long id, String userId) { return client.generationContext(id, userId); }
    @Override public List<RecentKnowledgePoint> recentKnowledge(String userId, int limit) { return client.recentKnowledge(userId, limit); }
}
