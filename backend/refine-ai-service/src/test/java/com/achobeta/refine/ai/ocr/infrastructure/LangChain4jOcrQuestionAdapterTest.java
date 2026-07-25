package com.achobeta.refine.ai.ocr.infrastructure;

import com.achobeta.refine.ai.ocr.application.query.QuestionClassification;
import com.achobeta.refine.ai.provider.RefineEducationAiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LangChain4jOcrQuestionAdapterTest {
    private final RefineEducationAiService assistant = mock(RefineEducationAiService.class);
    private final LangChain4jOcrQuestionAdapter adapter = new LangChain4jOcrQuestionAdapter(assistant, new ObjectMapper());

    @Test
    void delegatesQuestionExtractionAndMapsClassificationJson() {
        when(assistant.extractFirstQuestion("raw text")).thenReturn("What is x?");
        when(assistant.classifyQuestion("What is x?"))
                .thenReturn("{\"subject\":\"math\",\"knowledgePoint\":\"equation\",\"description\":\"solve a linear equation\"}");

        assertThat(adapter.extractFirstQuestion("raw text")).isEqualTo("What is x?");
        assertThat(adapter.classify("What is x?"))
                .isEqualTo(new QuestionClassification("math", "equation", "solve a linear equation"));
    }
}
