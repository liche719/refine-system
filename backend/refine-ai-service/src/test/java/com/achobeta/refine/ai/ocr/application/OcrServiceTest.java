package com.achobeta.refine.ai.ocr.application;

import com.achobeta.refine.ai.learning.application.port.LearningServicePort;
import com.achobeta.refine.ai.ocr.application.port.DocumentTextPort;
import com.achobeta.refine.ai.ocr.application.port.OcrQuestionAiPort;
import com.achobeta.refine.ai.ocr.application.port.OcrQuestionClassificationPort;
import com.achobeta.refine.contracts.learning.CreateMistakeRequest;
import com.achobeta.refine.contracts.learning.CreateMistakeResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OcrServiceTest {

    private final DocumentTextPort extractor = mock(DocumentTextPort.class);
    private final OcrQuestionAiPort ai = mock(OcrQuestionAiPort.class);
    private final OcrQuestionClassificationPort classification = mock(OcrQuestionClassificationPort.class);
    private final LearningServicePort learningClient = mock(LearningServicePort.class);
    private final OcrService service = new OcrService(extractor, ai, classification, learningClient);

    @Test
    void retryOfTheSameUploadReusesTheSameQuestionId() {
        byte[] file = "same-photo-bytes".getBytes(StandardCharsets.UTF_8);
        when(extractor.extract(any(), any())).thenReturn("question text");
        when(ai.extractFirstQuestion(any())).thenReturn("question text");
        when(classification.classify(any())).thenReturn(null);
        when(learningClient.createMistake(any()))
                .thenReturn(new CreateMistakeResponse(1L, "ignored"))
                .thenReturn(new CreateMistakeResponse(1L, "ignored"));

        service.extractFirst("user-1", file, "image/png");
        service.extractFirst("user-1", file, "image/png");

        ArgumentCaptor<CreateMistakeRequest> captor = ArgumentCaptor.forClass(CreateMistakeRequest.class);
        verify(learningClient, times(2)).createMistake(captor.capture());
        // Feign 超时重试会重复提交同一份文件，questionId 必须稳定，否则唯一键拦不住重复入库
        assertThat(captor.getAllValues().get(0).questionId())
                .isEqualTo(captor.getAllValues().get(1).questionId());
    }

    @Test
    void differentUploadsProduceDifferentQuestionIds() {
        when(extractor.extract(any(), any())).thenReturn("question text");
        when(ai.extractFirstQuestion(any())).thenReturn("question text");
        when(classification.classify(any())).thenReturn(null);
        when(learningClient.createMistake(any())).thenReturn(new CreateMistakeResponse(1L, "ignored"));

        service.extractFirst("user-1", "photo-a".getBytes(StandardCharsets.UTF_8), "image/png");
        service.extractFirst("user-1", "photo-b".getBytes(StandardCharsets.UTF_8), "image/png");

        ArgumentCaptor<CreateMistakeRequest> captor = ArgumentCaptor.forClass(CreateMistakeRequest.class);
        verify(learningClient, times(2)).createMistake(captor.capture());
        assertThat(captor.getAllValues().get(0).questionId())
                .isNotEqualTo(captor.getAllValues().get(1).questionId());
    }
}
