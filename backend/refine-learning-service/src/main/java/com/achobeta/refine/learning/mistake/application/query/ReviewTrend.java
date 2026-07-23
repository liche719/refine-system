package com.achobeta.refine.learning.mistake.application.query;

public record ReviewTrend(String month, long total, long reviewed) {
    public double completionRate() {
        return total == 0 ? 0D : (double) reviewed / total;
    }
}
