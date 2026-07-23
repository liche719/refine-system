package com.achobeta.refine.ai.provider;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class LangChain4jConfigurationContractTest {
    @Test
    void usesOfficialOpenAiPropertyNamesAndSecretOnlyEnvironmentVariables() throws Exception {
        try (var stream = getClass().getResourceAsStream("/application.yml")) {
            assertThat(stream).isNotNull();
            String yaml = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(yaml)
                    .contains("langchain4j:", "open-ai:", "chat-model:", "streaming-chat-model:",
                            "embedding-model:", "${OPENAI_API_KEY", "${OPENAI_EMBEDDING_API_KEY",
                            "https://llm-xtkyazx7u0me9k0m.cn-beijing.maas.aliyuncs.com/compatible-mode/v1",
                            "model-name: qwen3.7-text-embedding", "dimensions: 1536")
                    .doesNotContain("AI_PROVIDER", "AI_BASE_URL", "AI_MODEL", "DASHSCOPE_API_KEY");
        }
    }
}
