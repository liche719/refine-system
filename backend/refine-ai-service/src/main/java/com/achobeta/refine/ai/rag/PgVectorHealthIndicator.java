package com.achobeta.refine.ai.rag;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component("pgVector")
@ConditionalOnProperty(prefix = "refine.pgvector", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PgVectorHealthIndicator implements HealthIndicator {
    private final RagDocumentRepository repository;

    public PgVectorHealthIndicator(RagDocumentRepository repository) {
        this.repository = repository;
    }

    @Override
    public Health health() {
        try {
            return repository.ping() ? Health.up().build() : Health.down().build();
        } catch (RuntimeException exception) {
            return Health.down(exception).build();
        }
    }
}
