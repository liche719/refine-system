package com.achobeta.refine.ai.shared.application.port;

import java.util.List;

public interface TextEmbeddingPort {
    double[] embed(String text);

    List<double[]> embedAll(List<String> texts);

    String modelName();

    int dimensions();
}
