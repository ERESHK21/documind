package com.documind.document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TextDocumentRequest(
        @NotBlank(message = "A title is required")
        @Size(max = 200, message = "Title is too long")
        String name,

        @NotBlank(message = "Paste some text to index")
        @Size(max = 500000, message = "Text is too large")
        String content) {
}
