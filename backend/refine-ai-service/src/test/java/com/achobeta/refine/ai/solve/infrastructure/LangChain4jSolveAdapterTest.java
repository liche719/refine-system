package com.achobeta.refine.ai.solve.infrastructure;

import com.achobeta.refine.ai.provider.RefineSolveAiService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LangChain4jSolveAdapterTest {
    @Test
    void delegatesSolvingToTheToolEnabledSolveAssistant() {
        RefineSolveAiService assistant = mock(RefineSolveAiService.class);
        when(assistant.solve("question context")).thenReturn("solution");

        assertThat(new LangChain4jSolveAdapter(assistant).solve("question context")).isEqualTo("solution");
    }
}
