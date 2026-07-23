package com.achobeta.refine.ai.analytics.application;

import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class VectorCodec {
    public String serialize(double[] vector) {
        return Arrays.stream(vector).mapToObj(Double::toString)
                .collect(java.util.stream.Collectors.joining(","));
    }

    public double[] parse(String value) {
        if (value == null || value.isBlank()) return new double[0];
        try {
            return Arrays.stream(value.split(",")).mapToDouble(Double::parseDouble).toArray();
        } catch (NumberFormatException exception) {
            return new double[0];
        }
    }

    public double cosine(double[] left, double[] right) {
        if (left.length == 0 || left.length != right.length) return 0D;
        double dot = 0D;
        double leftNorm = 0D;
        double rightNorm = 0D;
        for (int index = 0; index < left.length; index++) {
            dot += left[index] * right[index];
            leftNorm += left[index] * left[index];
            rightNorm += right[index] * right[index];
        }
        return leftNorm == 0D || rightNorm == 0D ? 0D : dot / Math.sqrt(leftNorm * rightNorm);
    }
}
