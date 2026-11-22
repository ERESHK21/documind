package com.documind.chat;

import java.time.Instant;
import java.util.List;

public record SessionDetailResponse(
        Long id,
        String title,
        Instant createdAt,
        Instant lastActivityAt,
        List<MessageResponse> messages) {
}
