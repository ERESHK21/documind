package com.documind.document;

public record ChunkResponse(
        Long id,
        int pageNumber,
        int chunkIndex,
        int charCount,
        String content) {
}
