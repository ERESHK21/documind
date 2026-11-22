package com.documind.search;

import com.documind.document.DocumentChunk;

public record RetrievedChunk(DocumentChunk chunk, double score) {
}
