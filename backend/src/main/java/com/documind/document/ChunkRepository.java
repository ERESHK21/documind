package com.documind.document;

import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChunkRepository extends JpaRepository<DocumentChunk, Long> {

    Page<DocumentChunk> findByDocumentId(Long documentId, Pageable pageable);

    long countByDocumentId(Long documentId);

    long countByDocumentOwnerId(Long ownerId);

    List<DocumentChunk> findByDocumentOwnerId(Long ownerId);

    List<DocumentChunk> findByDocumentOwnerIdAndDocumentIdIn(Long ownerId, Collection<Long> documentIds);

    void deleteByDocumentId(Long documentId);
}
