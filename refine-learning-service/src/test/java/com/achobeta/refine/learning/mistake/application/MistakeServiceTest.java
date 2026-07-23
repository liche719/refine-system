package com.achobeta.refine.learning.mistake.application;

import com.achobeta.refine.contracts.learning.CreateMistakeRequest;
import com.achobeta.refine.common.datasource.ReadReplica;
import com.achobeta.refine.learning.mistake.application.port.LearningActivityPublisher;
import com.achobeta.refine.learning.mistake.application.port.MistakeRepository;
import com.achobeta.refine.learning.mistake.domain.MistakeQuestion;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.Clock;

class MistakeServiceTest {
    @Test
    void createsStableQuestionIdWhenRequestOmitsIt() {
        MistakeRepository repository = mock(MistakeRepository.class);
        LearningActivityPublisher publisher = mock(LearningActivityPublisher.class);
        when(repository.create(any())).thenAnswer(invocation -> {
            MistakeQuestion value = invocation.getArgument(0);
            return new MistakeQuestion(42L, value.userId(), value.questionId(), value.questionContent(), value.subject(),
                    0, 0, 0, 0, 0, null, value.knowledgePointId(), null, 0, value.source(), null, null);
        });
        MistakeService service = new MistakeService(repository, publisher, Clock.systemUTC());

        var result = service.create(new CreateMistakeRequest("u1", null, "1 + 1 = ?", "math", 1, "ocr"));

        assertThat(result.id()).isEqualTo(42L);
        assertThat(result.questionId()).isNotBlank();
    }

    @Test
    void generationContextAlwaysReadsFromPrimaryAfterAnUpload() throws Exception {
        Method method = MistakeService.class.getMethod("generationContext", long.class, String.class);

        assertThat(method.isAnnotationPresent(ReadReplica.class)).isFalse();
    }

    @Test
    void immediateReviewReadsFromPrimaryAfterAnUpload() throws Exception {
        Method method = MistakeService.class.getMethod("reasons", String.class, String.class);

        assertThat(method.isAnnotationPresent(ReadReplica.class)).isFalse();
    }
}
