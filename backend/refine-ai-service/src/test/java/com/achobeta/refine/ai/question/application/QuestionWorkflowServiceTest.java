package com.achobeta.refine.ai.question.application;

import com.achobeta.refine.ai.learning.application.port.LearningServicePort;
import com.achobeta.refine.ai.question.application.port.QuestionAiPort;
import com.achobeta.refine.ai.question.application.port.QuestionCache;
import com.achobeta.refine.common.api.AppException;
import com.achobeta.refine.contracts.learning.GenerationContextResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuestionWorkflowServiceTest {
    private final LearningServicePort learning = mock(LearningServicePort.class);
    private final QuestionAiPort ai = mock(QuestionAiPort.class);
    private final QuestionCache cache = mock(QuestionCache.class);
    private final QuestionWorkflowService service = new QuestionWorkflowService(learning, ai, cache, new ObjectMapper());

    @Test
    void rejectsAnUnderdeterminedGeneratedQuestionBeforeItReachesTheCache() {
        when(learning.generationContext(12L, "user-1"))
                .thenReturn(new GenerationContextResponse(12L, "数学", 1, "二次函数", "原始错题"));
        when(ai.generate("数学", "二次函数"))
                .thenReturn("""
                        {"content":"已知二次函数 y=ax²+bx+c 经过点(0,1)和(1,3)，求其解析式。",
                         "answer":"不存在唯一答案",
                         "analysis":"只有两个独立条件，无法唯一确定三个系数。"}
                        """);

        assertThatThrownBy(() -> service.generate("user-1", 12L))
                .isInstanceOf(AppException.class)
                .hasMessage("AI 生成的二次函数题目条件不足");

        verify(cache, never()).save(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
