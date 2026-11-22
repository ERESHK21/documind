package com.documind.document;

import java.time.Instant;

public record DocumentResponse(
        Long id,
        String name,
        String contentType,
        long sizeBytes,
        int pageCount,
        int chunkCount,
        int charCount,
        boolean hasOriginalFile,
        Instant createdAt) {
}
