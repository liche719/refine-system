package com.achobeta.refine.learning.mistake.infrastructure;

import com.achobeta.refine.learning.mistake.domain.MistakeQuestion;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class MistakeRepositoryAdapterTest {
    @Test
    void mapsDomainFieldsAndGeneratedIdAcrossTheMyBatisBoundary() {
        MistakeMapper mapper = mock(MistakeMapper.class);
        doAnswer(invocation -> {
            MistakeMapper.MutableMistake row = invocation.getArgument(0);
            row.id = 42L;
            return 1;
        }).when(mapper).insert(any(MistakeMapper.MutableMistake.class));
        MistakeRepositoryAdapter repository = new MistakeRepositoryAdapter(mapper);
        MistakeQuestion input = new MistakeQuestion(null, "user-1", "question-1", "2 + 2", "math",
                0, 0, 0, 0, 0, null, 7, null, 0, "ocr", null, null);

        MistakeQuestion created = repository.create(input);

        assertThat(created.id()).isEqualTo(42L);
        assertThat(created.userId()).isEqualTo("user-1");
        assertThat(created.questionId()).isEqualTo("question-1");
        assertThat(created.knowledgePointId()).isEqualTo(7);
        assertThat(created.source()).isEqualTo("ocr");
    }
}
