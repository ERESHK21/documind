package com.documind.admin;

import java.time.Instant;

public record UserSummary(
        Long id,
        String name,
        String email,
        String role,
        long documents,
        long sessions,
        Instant createdAt) {
}
