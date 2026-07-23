package com.achobeta.refine.learning.knowledge.application;

import com.achobeta.refine.contracts.learning.EnsureKnowledgePointRequest;
import com.achobeta.refine.learning.knowledge.application.port.KnowledgeRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeServiceTest {
    @Test
    void reusesAnExistingRootKnowledgePointForTheSameSubjectAndName() {
        KnowledgeRepository repository = mock(KnowledgeRepository.class);
        when(repository.findRootId("user-1", "哲学", "忒修斯之船悖论")).thenReturn(7);
        KnowledgeService service = new KnowledgeService(repository);

        var result = service.ensureRoot(new EnsureKnowledgePointRequest(
                "user-1", "忒修斯之船悖论", "同一性的判断标准", "哲学"));

        assertThat(result.knowledgePointId()).isEqualTo(7);
        verify(repository, never()).addRoot(any(), any(), any(), any());
    }

    @Test
    void createsARootKnowledgePointWhenClassificationIsNew() {
        KnowledgeRepository repository = mock(KnowledgeRepository.class);
        when(repository.findRootId("user-1", "哲学", "忒修斯之船悖论")).thenReturn(null);
        when(repository.addRoot(eq("user-1"), eq("忒修斯之船悖论"), any(), eq("哲学"))).thenReturn(8);
        KnowledgeService service = new KnowledgeService(repository);

        var result = service.ensureRoot(new EnsureKnowledgePointRequest(
                "user-1", "忒修斯之船悖论", "同一性的判断标准", "哲学"));

        assertThat(result.knowledgePointId()).isEqualTo(8);
        verify(repository).addRoot("user-1", "忒修斯之船悖论", "同一性的判断标准", "哲学");
    }
}
