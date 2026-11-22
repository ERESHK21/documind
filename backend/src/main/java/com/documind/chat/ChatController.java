package com.documind.chat;

import com.documind.auth.AuthFacade;
import com.documind.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final AuthFacade auth;

    public ChatController(ChatService chatService, AuthFacade auth) {
        this.chatService = chatService;
        this.auth = auth;
    }

    @PostMapping("/ask")
    public AskResponse ask(@Valid @RequestBody AskRequest request) {
        return chatService.ask(request, auth.current());
    }

    @GetMapping("/sessions")
    public PageResponse<SessionResponse> sessions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return chatService.list(page, size, auth.current());
    }

    @GetMapping("/sessions/{id}")
    public SessionDetailResponse session(@PathVariable Long id) {
        return chatService.detail(id, auth.current());
    }

    @DeleteMapping("/sessions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        chatService.delete(id, auth.current());
    }
}
