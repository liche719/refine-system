package com.achobeta.refine.ai.ocr;

import com.achobeta.refine.ai.learning.application.port.LearningServicePort;
import com.achobeta.refine.ai.ocr.application.port.DocumentTextPort;
import com.achobeta.refine.ai.ocr.application.port.OcrQuestionAiPort;
import com.achobeta.refine.ai.ocr.application.port.OcrQuestionClassificationPort;
import com.achobeta.refine.ai.ocr.application.OcrService;
import com.achobeta.refine.ai.ocr.application.query.QuestionClassification;
import com.achobeta.refine.common.api.AppException;
import com.achobeta.refine.contracts.learning.CreateMistakeResponse;
import com.achobeta.refine.contracts.learning.EnsureKnowledgePointResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import org.mockito.ArgumentCaptor;
import com.achobeta.refine.contracts.learning.CreateMistakeRequest;

class OcrUploadBoundaryTest {
    @Test
    void rejectsOversizedAndUnsupportedFilesBeforeReadingTheirBytes() {
        OcrUploadProperties properties = new OcrUploadProperties();
        properties.setMaxSize(DataSize.ofBytes(4));
        OcrController controller = new OcrController(mock(OcrService.class), properties);

        assertThatThrownBy(() -> controller.extract(
                new MockMultipartFile("file", "large.txt", "text/plain", new byte[5]), "txt"))
                .isInstanceOfSatisfying(AppException.class, exception -> assertThat(exception.getCode()).isEqualTo(1001));
        assertThatThrownBy(() -> controller.extract(
                new MockMultipartFile("file", "script.exe", "application/octet-stream", new byte[1]), "exe"))
                .isInstanceOfSatisfying(AppException.class, exception -> assertThat(exception.getCode()).isEqualTo(1001));
    }

    @Test
    void acceptsMarkdownByExtensionAndContentType() {
        OcrUploadProperties properties = new OcrUploadProperties();

        assertThat(properties.supports("lesson.md")).isTrue();
        assertThat(properties.supports("text/markdown")).isTrue();
    }

    @Test
    void rejectsBlankExtractionBeforeCallingTheAiModel() {
        DocumentTextPort extractor = mock(DocumentTextPort.class);
        OcrQuestionAiPort model = mock(OcrQuestionAiPort.class);
        OcrQuestionClassificationPort classification = mock(OcrQuestionClassificationPort.class);
        LearningServicePort learning = mock(LearningServicePort.class);
        when(extractor.extract(any(byte[].class), eq("txt"))).thenReturn("  ");
        OcrService service = new OcrService(extractor, model, classification, learning);

        assertThatThrownBy(() -> service.extractFirst("user-1", new byte[]{1}, "txt"))
                .isInstanceOfSatisfying(AppException.class, exception -> assertThat(exception.getCode()).isEqualTo(10003));
    }

    @Test
    void classifiesAnOcrQuestionAndLinksTheEnsuredKnowledgePoint() {
        DocumentTextPort extractor = mock(DocumentTextPort.class);
        OcrQuestionAiPort model = mock(OcrQuestionAiPort.class);
        OcrQuestionClassificationPort classification = mock(OcrQuestionClassificationPort.class);
        LearningServicePort learning = mock(LearningServicePort.class);
        when(extractor.extract(any(byte[].class), eq("txt"))).thenReturn("题目原文");
        when(model.extractFirstQuestion("题目原文")).thenReturn("忒修斯之船哪个是真船？");
        when(classification.classify("忒修斯之船哪个是真船？"))
                .thenReturn(new QuestionClassification("哲学", "忒修斯之船悖论", "同一性的判断标准"));
        when(learning.ensureKnowledgePoint(any())).thenReturn(new EnsureKnowledgePointResponse(8));
        when(learning.createMistake(any())).thenReturn(new CreateMistakeResponse(9L, "question-1"));

        OcrService service = new OcrService(extractor, model, classification, learning);
        OcrService.OcrResult result = service.extractFirst("user-1", new byte[]{1}, "txt");

        ArgumentCaptor<CreateMistakeRequest> request = ArgumentCaptor.forClass(CreateMistakeRequest.class);
        verify(learning).createMistake(request.capture());
        assertThat(request.getValue().subject()).isEqualTo("哲学");
        assertThat(request.getValue().knowledgePointId()).isEqualTo(8);
        assertThat(result.knowledgePoint()).isEqualTo("忒修斯之船悖论");
    }
}
