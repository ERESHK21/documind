package com.documind.document;

import com.documind.common.ApiException;
import com.documind.common.PageResponse;
import com.documind.user.User;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentService {

    private static final int MAX_PAGES = 300;
    private static final int MAX_CHARS = 500_000;

    private final DocumentRepository documents;
    private final ChunkRepository chunks;
    private final TextExtractor extractor;
    private final Chunker chunker;
    private final Path uploadRoot;

    public DocumentService(
            DocumentRepository documents,
            ChunkRepository chunks,
            TextExtractor extractor,
            Chunker chunker,
            @Value("${app.upload-dir:uploads}") String uploadDir) throws IOException {
        this.documents = documents;
        this.chunks = chunks;
        this.extractor = extractor;
        this.chunker = chunker;
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(this.uploadRoot);
    }

    @Transactional
    public DocumentResponse upload(MultipartFile file, User owner) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("Choose a PDF, TXT or Markdown file to upload");
        }
        String originalName = file.getOriginalFilename() == null ? "document" : file.getOriginalFilename();
        String name = originalName.replaceAll("[\\\\/]", "").trim();
        if (name.isEmpty()) {
            name = "document";
        }
        String extension = extensionOf(name);
        if (!List.of("pdf", "txt", "md", "markdown").contains(extension)) {
            throw ApiException.badRequest("Only PDF, TXT and Markdown files are supported");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw ApiException.badRequest("Could not read the uploaded file");
        }
        if (bytes.length == 0) {
            throw ApiException.badRequest("That file is empty");
        }

        TextExtractor.Extracted extracted = extractor.extract(bytes, name, file.getContentType());
        Path stored = storeOriginal(bytes, extension);

        try {
            return index(name, file.getContentType() == null ? contentTypeFor(extension) : file.getContentType(),
                    bytes.length, extracted, stored.toString(), owner);
        } catch (RuntimeException ex) {
            try {
                Files.deleteIfExists(stored);
            } catch (IOException ignored) {
                // best effort cleanup
            }
            throw ex;
        }
    }

    @Transactional
    public DocumentResponse createFromText(String name, String content, User owner) {
        String cleanName = name == null || name.isBlank() ? "Pasted note" : name.trim();
        String text = content == null ? "" : content;
        TextExtractor.Extracted extracted = new TextExtractor.Extracted(List.of(text));
        return index(cleanName, "text/plain", text.getBytes(java.nio.charset.StandardCharsets.UTF_8).length,
                extracted, null, owner);
    }

    private DocumentResponse index(String name, String contentType, long sizeBytes,
            TextExtractor.Extracted extracted, String filePath, User owner) {
        String joined = extracted.joined();
        if (joined.isBlank()) {
            throw ApiException.badRequest("No readable text found in this document");
        }
        if (extracted.pages().size() > MAX_PAGES) {
            throw ApiException.badRequest("Document is too large to index (max " + MAX_PAGES + " pages)");
        }
        if (joined.length() > MAX_CHARS) {
            throw ApiException.badRequest("Document is too large to index (max 500,000 characters)");
        }

        Document document = new Document();
        document.setName(name);
        document.setContentType(contentType == null ? "application/octet-stream" : contentType);
        document.setSizeBytes(sizeBytes);
        document.setPageCount(extracted.pages().size());
        document.setCharCount(joined.length());
        document.setFilePath(filePath);
        document.setOwner(owner);
        document.setCreatedAt(Instant.now());

        int index = 0;
        int total = 0;
        for (int page = 0; page < extracted.pages().size(); page++) {
            for (String part : chunker.split(extracted.pages().get(page))) {
                DocumentChunk chunk = new DocumentChunk();
                chunk.setDocument(document);
                chunk.setPageNumber(page + 1);
                chunk.setChunkIndex(index++);
                chunk.setContent(part);
                document.getChunks().add(chunk);
                total++;
            }
        }
        if (total == 0) {
            throw ApiException.badRequest("No readable text found in this document");
        }
        document.setChunkCount(total);
        documents.save(document);
        return toResponse(document);
    }

    @Transactional(readOnly = true)
    public PageResponse<DocumentResponse> list(String q, int page, int size, User owner) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), clampSize(size),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Document> result = q == null || q.isBlank()
                ? documents.findByOwnerId(owner.getId(), pageable)
                : documents.findByOwnerIdAndNameContainingIgnoreCase(owner.getId(), q.trim(), pageable);
        return PageResponse.of(result, this::toResponse);
    }

    @Transactional(readOnly = true)
    public DocumentResponse get(Long id, User owner) {
        return toResponse(loadOwned(id, owner));
    }

    @Transactional(readOnly = true)
    public PageResponse<ChunkResponse> chunks(Long id, int page, int size, User owner) {
        Document document = loadOwned(id, owner);
        Page<DocumentChunk> result = chunks.findByDocumentId(document.getId(),
                PageRequest.of(Math.max(page, 0), clampSize(size), Sort.by("chunkIndex")));
        return PageResponse.of(result,
                chunk -> new ChunkResponse(chunk.getId(), chunk.getPageNumber(), chunk.getChunkIndex(),
                        chunk.getContent().length(), chunk.getContent()));
    }

    @Transactional
    public void delete(Long id, User owner) {
        Document document = loadOwned(id, owner);
        documents.delete(document);
        if (document.getFilePath() != null) {
            try {
                Files.deleteIfExists(Paths.get(document.getFilePath()));
            } catch (IOException ignored) {
                // best effort
            }
        }
    }

    @Transactional(readOnly = true)
    public Path originalFile(Long id, User owner) {
        Document document = loadOwned(id, owner);
        if (document.getFilePath() == null) {
            throw ApiException.notFound("Original file");
        }
        Path path = Paths.get(document.getFilePath());
        if (!Files.exists(path)) {
            throw ApiException.notFound("Original file");
        }
        return path;
    }

    private Document loadOwned(Long id, User owner) {
        return documents.findByIdAndOwnerId(id, owner.getId())
                .orElseThrow(() -> ApiException.notFound("Document"));
    }

    private Path storeOriginal(byte[] bytes, String extension) {
        String filename = UUID.randomUUID() + "." + extension;
        Path target = uploadRoot.resolve(filename);
        try (InputStream in = new java.io.ByteArrayInputStream(bytes)) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not store the uploaded file");
        }
        return target;
    }

    private static String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String contentTypeFor(String extension) {
        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "md", "markdown" -> "text/markdown";
            default -> "text/plain";
        };
    }

    private DocumentResponse toResponse(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getName(),
                document.getContentType(),
                document.getSizeBytes(),
                document.getPageCount(),
                document.getChunkCount(),
                document.getCharCount(),
                document.getFilePath() != null,
                document.getCreatedAt());
    }

    private static int clampSize(int size) {
        return Math.min(Math.max(size, 1), 50);
    }
}
