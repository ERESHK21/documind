package com.documind.admin;

import com.documind.chat.ChatSessionRepository;
import com.documind.document.DocumentRepository;
import com.documind.stats.AdminStatsResponse;
import com.documind.stats.StatsService;
import com.documind.user.User;
import com.documind.user.UserRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final StatsService stats;
    private final UserRepository users;
    private final DocumentRepository documents;
    private final ChatSessionRepository sessions;

    public AdminController(StatsService stats, UserRepository users,
            DocumentRepository documents, ChatSessionRepository sessions) {
        this.stats = stats;
        this.users = users;
        this.documents = documents;
        this.sessions = sessions;
    }

    @GetMapping("/stats")
    public AdminStatsResponse stats() {
        return stats.global();
    }

    @GetMapping("/users")
    @Transactional(readOnly = true)
    public List<UserSummary> users() {
        return users.findAll().stream()
                .sorted(Comparator.comparing(User::getCreatedAt))
                .map(user -> new UserSummary(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getRole().name(),
                        documents.countByOwnerId(user.getId()),
                        sessions.countByOwnerId(user.getId()),
                        user.getCreatedAt()))
                .toList();
    }
}
