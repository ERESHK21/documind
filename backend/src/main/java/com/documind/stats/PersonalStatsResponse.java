package com.documind.stats;

public record PersonalStatsResponse(
        long documents,
        long chunks,
        long sessions,
        long questions,
        long aiAnswers,
        long offlineAnswers,
        double avgLatencyMs) {
}
