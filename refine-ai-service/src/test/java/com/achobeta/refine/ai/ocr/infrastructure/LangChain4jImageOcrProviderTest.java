package com.achobeta.refine.ai.ocr.infrastructure;

import com.achobeta.refine.ai.shared.infrastructure.ProviderErrorSanitizer;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LangChain4jImageOcrProviderTest {

    @Test
    void combinesPromptResourcesAndImageContentInOneUserMessage() {
        ChatModel model = mock(ChatModel.class);
        when(model.supportedCapabilities()).thenReturn(Set.<Capability>of());
        when(model.chat(any(ChatRequest.class))).thenReturn(ChatResponse.builder()
                .aiMessage(AiMessage.from("识别结果"))
                .build());
        ImageOcrAiService assistant = AiServices.builder(ImageOcrAiService.class)
                .chatModel(model)
                .build();

        String result = assistant.recognize(ImageContent.from("aGVsbG8=", "image/png"));

        assertThat(result).isEqualTo("识别结果");
        ArgumentCaptor<ChatRequest> request = ArgumentCaptor.forClass(ChatRequest.class);
        verify(model).chat(request.capture());
        assertThat(request.getValue().messages()).hasSize(2);
        assertThat(((SystemMessage) request.getValue().messages().get(0)).text())
                .contains("Refine", "OCR");
        UserMessage user = (UserMessage) request.getValue().messages().get(1);
        assertThat(user.contents()).anyMatch(TextContent.class::isInstance)
                .anyMatch(ImageContent.class::isInstance);
        assertThat(user.contents().stream()
                .filter(TextContent.class::isInstance)
                .map(TextContent.class::cast)
                .map(TextContent::text))
                .anyMatch(text -> text.contains("逐字识别"));
    }

    @Test
    void removesCredentialsAndImageDataFromProviderErrors() {
        String message = "Authorization: Bearer sk-secret-token "
                + "image=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAE=";

        String sanitized = ProviderErrorSanitizer.sanitize(
                new IllegalStateException(message));

        assertThat(sanitized)
                .contains("Bearer [REDACTED]")
                .contains("data:image/[REDACTED]")
                .doesNotContain("sk-secret-token", "iVBORw0KGgoAAAANSUhEUgAAAAE=");
    }
}
