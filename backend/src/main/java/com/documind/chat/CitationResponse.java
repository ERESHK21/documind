package com.documind.chat;

import java.time.Instant;

public record CitationResponse(
        Long documentId,
        String documentName,
        int pageNumber,
        String snippet,
        double score) {
}
