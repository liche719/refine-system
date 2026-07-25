package com.achobeta.refine.ai.provider;

import com.achobeta.refine.ai.conversation.infrastructure.RedisChatMemoryStore;
import com.achobeta.refine.ai.question.application.port.QuestionAiPort;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LangChain4jEducationAiAdapterQuestionQualityTest {
    private final RefineEducationAiService educationAssistant = mock(RefineEducationAiService.class);
    private final LangChain4jEducationAiAdapter adapter = new LangChain4jEducationAiAdapter(
            educationAssistant, mock(RefineConversationAiService.class), mock(RefineSolveAiService.class),
            mock(RedisChatMemoryStore.class));

    @Test
    void mapsStructuredGenericQuestionQualityReview() {
        when(educationAssistant.verifyGeneratedQuestion("数学", "一次函数", "题目", "答案", "解析"))
                .thenReturn("{\"valid\":true,\"reason\":\"条件完整且答案可推导\"}");

        QuestionAiPort.QuestionQuality quality = adapter.verify("数学", "一次函数", "题目", "答案", "解析");

        assertThat(quality).isEqualTo(new QuestionAiPort.QuestionQuality(true, "条件完整且答案可推导"));
    }
}
