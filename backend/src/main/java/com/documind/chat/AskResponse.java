package com.documind.chat;

public record AskResponse(
        Long sessionId,
        String sessionTitle,
        MessageResponse userMessage,
        MessageResponse answer) {
}
