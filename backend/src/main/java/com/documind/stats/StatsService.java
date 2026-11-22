package com.documind.stats;

import com.documind.chat.AnswerMode;
import com.documind.chat.ChatMessageRepository;
import com.documind.chat.ChatSessionRepository;
import com.documind.chat.MessageRole;
import com.documind.document.ChunkRepository;
import com.documind.document.DocumentRepository;
import com.documind.user.User;
import com.documind.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatsService {

    private final UserRepository users;
    private final DocumentRepository documents;
    private final ChunkRepository chunks;
    private final ChatSessionRepository sessions;
    private final ChatMessageRepository messages;
    private final boolean aiConfigured;

    public StatsService(
            UserRepository users,
            DocumentRepository documents,
            ChunkRepository chunks,
            ChatSessionRepository sessions,
            ChatMessageRepository messages,
            @Value("${app.ai.api-key:}") String apiKey,
            @Value("${app.ai.provider:auto}") String provider) {
        this.users = users;
        this.documents = documents;
        this.chunks = chunks;
        this.sessions = sessions;
        this.messages = messages;
        this.aiConfigured = apiKey != null && !apiKey.isBlank() && !"offline".equalsIgnoreCase(provider);
    }

    @Transactional(readOnly = true)
    public PersonalStatsResponse personal(User user) {
        Long ownerId = user.getId();
        return new PersonalStatsResponse(
                documents.countByOwnerId(ownerId),
                chunks.countByDocumentOwnerId(ownerId),
                sessions.countByOwnerId(ownerId),
                messages.countBySessionOwnerIdAndRole(ownerId, MessageRole.USER),
                messages.countBySessionOwnerIdAndRoleAndMode(ownerId, MessageRole.ASSISTANT, AnswerMode.AI),
                messages.countBySessionOwnerIdAndRoleAndMode(ownerId, MessageRole.ASSISTANT, AnswerMode.OFFLINE),
                round(messages.averageLatencyForOwner(ownerId, MessageRole.ASSISTANT)));
    }

    @Transactional(readOnly = true)
    public AdminStatsResponse global() {
        return new AdminStatsResponse(
                users.count(),
                documents.count(),
                chunks.count(),
                sessions.count(),
                messages.countByRole(MessageRole.USER),
                messages.countByRoleAndMode(MessageRole.ASSISTANT, AnswerMode.AI),
                messages.countByRoleAndMode(MessageRole.ASSISTANT, AnswerMode.OFFLINE),
                round(messages.averageLatency(MessageRole.ASSISTANT)),
                aiConfigured);
    }

    private static double round(Double value) {
        if (value == null) {
            return 0.0;
        }
        return Math.round(value * 10.0) / 10.0;
    }
}
