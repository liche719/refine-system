package com.achobeta.refine.ai.question.infrastructure;

import com.achobeta.refine.ai.provider.RefineEducationAiService;
import com.achobeta.refine.ai.question.application.port.QuestionAiPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LangChain4jQuestionAdapterTest {
    private final RefineEducationAiService assistant = mock(RefineEducationAiService.class);
    private final LangChain4jQuestionAdapter adapter = new LangChain4jQuestionAdapter(assistant, new ObjectMapper());

    @Test
    void mapsStructuredGeneratedQuestionQualityReview() {
        when(assistant.verifyGeneratedQuestion("math", "linear function", "question", "answer", "analysis"))
                .thenReturn("{\"valid\":true,\"reason\":\"conditions are complete\"}");

        QuestionAiPort.QuestionQuality quality = adapter.verify("math", "linear function", "question", "answer", "analysis");

        assertThat(quality).isEqualTo(new QuestionAiPort.QuestionQuality(true, "conditions are complete"));
    }
}
