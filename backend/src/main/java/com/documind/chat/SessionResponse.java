package com.documind.chat;

import java.time.Instant;

public record SessionResponse(
        Long id,
        String title,
        Instant createdAt,
        Instant lastActivityAt,
        long messageCount,
        String lastQuestion) {
}
