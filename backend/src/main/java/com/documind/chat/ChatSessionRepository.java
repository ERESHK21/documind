package com.documind.chat;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    Page<ChatSession> findByOwnerIdOrderByLastActivityAtDesc(Long ownerId, Pageable pageable);

    Optional<ChatSession> findByIdAndOwnerId(Long id, Long ownerId);

    long countByOwnerId(Long ownerId);
}
