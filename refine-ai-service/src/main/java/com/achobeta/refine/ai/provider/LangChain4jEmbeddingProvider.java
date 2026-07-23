package com.achobeta.refine.ai.provider;

import com.achobeta.refine.ai.shared.application.port.TextEmbeddingPort;
import com.achobeta.refine.ai.shared.infrastructure.ProviderErrorSanitizer;
import com.achobeta.refine.common.api.AppException;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LangChain4jEmbeddingProvider implements TextEmbeddingPort {
    private static final Logger log = LoggerFactory.getLogger(LangChain4jEmbeddingProvider.class);
    private final EmbeddingModel model;

    public LangChain4jEmbeddingProvider(EmbeddingModel model) {
        this.model = model;
    }

    @Override
    public double[] embed(String text) {
        try {
            return require(model.embed(text).content()).vectorAsDoubleArray();
        } catch (Exception exception) {
            logFailure(exception);
            throw new AppException(10001, "Embedding 服务暂时不可用");
        }
    }

    @Override
    public List<double[]> embedAll(List<String> texts) {
        try {
            List<TextSegment> segments = texts.stream().map(TextSegment::from).toList();
            return model.embedAll(segments).content().stream().map(this::require)
                    .map(Embedding::vectorAsDoubleArray).toList();
        } catch (Exception exception) {
            logFailure(exception);
            throw new AppException(10001, "Embedding 服务暂时不可用");
        }
    }

    @Override
    public String modelName() {
        return model.modelName();
    }

    @Override
    public int dimensions() {
        return model.dimension();
    }

    private Embedding require(Embedding embedding) {
        if (embedding == null || embedding.dimension() == 0) {
            throw new IllegalStateException("Embedding response is empty");
        }
        return embedding;
    }

    private void logFailure(Exception exception) {
        log.error("Embedding provider call failed; errorType={}, detail={}",
                exception.getClass().getSimpleName(), ProviderErrorSanitizer.sanitize(exception));
    }
}
