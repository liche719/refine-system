package com.achobeta.refine.ai.provider;

import com.github.tomakehurst.wiremock.WireMockServer;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

class LangChain4jOpenAiConfigurationTest {
    @Test
    void createsStandardOpenAiCompatibleModels() {
        var configuration = new LangChain4jOpenAiConfiguration();
        var properties = properties("https://chat.test/v1");

        assertThat(configuration.openAiChatModel(properties)).isInstanceOf(OpenAiChatModel.class);
        assertThat(configuration.openAiStreamingChatModel(properties)).isInstanceOf(OpenAiStreamingChatModel.class);
        assertThat(configuration.openAiEmbeddingModel(properties)).isInstanceOf(OpenAiEmbeddingModel.class);
    }

    @Test
    void synchronousChatUsesChatCompletionsEndpoint() {
        WireMockServer server = new WireMockServer(options().dynamicPort());
        server.start();
        try {
            server.stubFor(post(urlPathEqualTo("/v1/chat/completions"))
                    .willReturn(aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                    {
                                      "id": "chatcmpl-1",
                                      "object": "chat.completion",
                                      "model": "gpt-5.4-mini",
                                      "choices": [{
                                        "index": 0,
                                        "message": {"role": "assistant", "content": "你好"},
                                        "finish_reason": "stop"
                                      }],
                                      "usage": {"prompt_tokens": 1, "completion_tokens": 1, "total_tokens": 2}
                                    }
                                    """)));
            var configuration = new LangChain4jOpenAiConfiguration();

            assertThat(configuration.openAiChatModel(properties(server.baseUrl() + "/v1")).chat("你好"))
                    .isEqualTo("你好");
            server.verify(postRequestedFor(urlEqualTo("/v1/chat/completions"))
                    .withRequestBody(matchingJsonPath("$.model", equalTo("gpt-5.4-mini"))));
        } finally {
            server.stop();
        }
    }

    private LangChain4jOpenAiProperties properties(String baseUrl) {
        var chat = new LangChain4jOpenAiProperties.ChatModelProperties(
                baseUrl, "key", "gpt-5.4-mini", "low", Duration.ofSeconds(30), 2, false, false);
        var streaming = new LangChain4jOpenAiProperties.ChatModelProperties(
                baseUrl, "key", "gpt-5.4-mini", "low", Duration.ofSeconds(120), 0, false, false);
        var embedding = new LangChain4jOpenAiProperties.EmbeddingModelProperties(
                baseUrl, "key", "embedding", 1536, Duration.ofSeconds(30), 2, false, false);
        return new LangChain4jOpenAiProperties(chat, streaming, embedding);
    }
}
