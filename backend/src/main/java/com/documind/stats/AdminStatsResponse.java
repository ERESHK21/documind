package com.documind.stats;

public record AdminStatsResponse(
        long users,
        long documents,
        long chunks,
        long sessions,
        long questions,
        long aiAnswers,
        long offlineAnswers,
        double avgLatencyMs,
        boolean aiConfigured) {
}
