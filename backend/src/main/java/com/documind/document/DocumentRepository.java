package com.documind.document;

import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    Page<Document> findByOwnerId(Long ownerId, Pageable pageable);

    Page<Document> findByOwnerIdAndNameContainingIgnoreCase(Long ownerId, String name, Pageable pageable);

    Optional<Document> findByIdAndOwnerId(Long id, Long ownerId);

    long countByOwnerId(Long ownerId);

    long countByOwnerIdAndIdIn(Long ownerId, Collection<Long> ids);
}
