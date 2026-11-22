package com.documind.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AskRequest(
        @NotBlank(message = "Ask a question first")
        @Size(max = 500, message = "Question is too long")
        String question,

        List<Long> documentIds,

        Long sessionId) {
}
