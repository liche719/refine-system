package com.achobeta.refine.learning.mistake.application.port;

import com.achobeta.refine.contracts.event.LearningActivityPayload;

public interface LearningActivityPublisher {
    void publishAfterCommit(String userId, LearningActivityPayload payload);
}
