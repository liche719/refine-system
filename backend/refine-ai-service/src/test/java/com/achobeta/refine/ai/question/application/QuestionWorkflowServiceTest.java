package com.achobeta.refine.ai.question.application;

import com.achobeta.refine.ai.learning.application.port.LearningServicePort;
import com.achobeta.refine.ai.question.application.port.QuestionAiPort;
import com.achobeta.refine.ai.question.application.port.QuestionCache;
import com.achobeta.refine.ai.rag.application.RagSearchService;
import com.achobeta.refine.ai.rag.application.query.RagChunk;
import com.achobeta.refine.ai.rag.application.query.RagDocumentMetadata;
import com.achobeta.refine.common.api.AppException;
import com.achobeta.refine.contracts.learning.GenerationContextResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuestionWorkflowServiceTest {
    private final LearningServicePort learning = mock(LearningServicePort.class);
    private final QuestionAiPort ai = mock(QuestionAiPort.class);
    private final QuestionCache cache = mock(QuestionCache.class);
    private final RagSearchService ragSearch = mock(RagSearchService.class);
    private final QuestionWorkflowService service = new QuestionWorkflowService(learning, ai, cache, ragSearch, new ObjectMapper());

    @Test
    void rejectsAnyGeneratedQuestionThatFailsTheGenericQualityReviewBeforeItReachesTheCache() {
        when(learning.generationContext(12L, "user-1"))
                .thenReturn(new GenerationContextResponse(12L, "历史", 1, "史料论证", "原始错题"));
        when(ragSearch.search("历史 史料论证", 3)).thenReturn(java.util.List.of());
        when(ai.generate("历史", "史料论证", ""))
                .thenReturn("""
                        {"content":"请评价这段史料。",
                         "answer":"无法确定",
                         "analysis":"题干没有给出史料内容和评价标准。"}
                        """);
        when(ai.verify(any(), any(), any(), any(), any()))
                .thenReturn(new QuestionAiPort.QuestionQuality(false, "题干条件不足"));

        assertThatThrownBy(() -> service.generate("user-1", 12L))
                .isInstanceOf(AppException.class)
                .hasMessage("生成的练习题未通过完整性校验，请重试");

        verify(ragSearch).search("历史 史料论证", 3);
        verify(cache, never()).save(any(), any());
    }

    @Test
    void persistsAnySubjectQuestionAfterTheGenericQualityReviewPasses() {
        when(learning.generationContext(12L, "user-1"))
                .thenReturn(new GenerationContextResponse(12L, "语文", 1, "论证方法", "原始错题"));
        RagChunk reference = new RagChunk(1L, "举例论证使用具体事例证明观点。", metadata(), 0.9D, 0.8D, 0.1D);
        when(ragSearch.search("语文 论证方法", 3)).thenReturn(java.util.List.of(reference));
        when(ai.generate("语文", "论证方法", reference.referenceText()))
                .thenReturn("""
                        {"content":"阅读给出的论据，判断其主要论证方法。",
                         "answer":"举例论证",
                         "analysis":"论据通过具体事例证明观点，属于举例论证。"}
                        """);
        when(ai.verify(any(), any(), any(), any(), any()))
                .thenReturn(new QuestionAiPort.QuestionQuality(true, "条件完整"));

        QuestionWorkflowService.GeneratedQuestion generated = service.generate("user-1", 12L);

        assertThat(generated.content()).isEqualTo("阅读给出的论据，判断其主要论证方法。");
        verify(ragSearch).search("语文 论证方法", 3);
        verify(cache).save(any(), any());
    }

    private RagDocumentMetadata metadata() {
        return new RagDocumentMetadata("language.md", "a".repeat(64), "论证方法", "语文", "九年级",
                "人教版", "议论文", "", "", true);
    }
}
