package com.documind.chat;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    long countBySessionId(Long sessionId);

    Optional<ChatMessage> findFirstBySessionIdAndRoleOrderByCreatedAtDesc(Long sessionId, MessageRole role);

    long countBySessionOwnerIdAndRole(Long ownerId, MessageRole role);

    long countBySessionOwnerIdAndRoleAndMode(Long ownerId, MessageRole role, AnswerMode mode);

    long countByRole(MessageRole role);

    long countByRoleAndMode(MessageRole role, AnswerMode mode);

    @Query("select coalesce(avg(m.latencyMs), 0) from ChatMessage m "
            + "where m.session.owner.id = :ownerId and m.role = :role")
    Double averageLatencyForOwner(@Param("ownerId") Long ownerId, @Param("role") MessageRole role);

    @Query("select coalesce(avg(m.latencyMs), 0) from ChatMessage m where m.role = :role")
    Double averageLatency(@Param("role") MessageRole role);
}
