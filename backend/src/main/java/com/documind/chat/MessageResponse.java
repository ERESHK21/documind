package com.documind.chat;

import java.time.Instant;
import java.util.List;

public record MessageResponse(
        Long id,
        String role,
        String content,
        String mode,
        int latencyMs,
        Instant createdAt,
        List<CitationResponse> citations) {
}
