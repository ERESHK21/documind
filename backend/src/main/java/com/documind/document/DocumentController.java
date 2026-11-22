package com.documind.document;

import com.documind.auth.AuthFacade;
import com.documind.common.ApiException;
import com.documind.common.PageResponse;
import com.documind.user.User;
import jakarta.validation.Valid;
import java.nio.file.Path;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final AuthFacade auth;

    public DocumentController(DocumentService documentService, AuthFacade auth) {
        this.documentService = documentService;
        this.auth = auth;
    }

    @GetMapping
    public PageResponse<DocumentResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return documentService.list(q, page, size, auth.current());
    }

    @GetMapping("/{id}")
    public DocumentResponse get(@PathVariable Long id) {
        return documentService.get(id, auth.current());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse upload(@RequestParam("file") MultipartFile file) {
        return documentService.upload(file, auth.current());
    }

    @PostMapping("/text")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse createFromText(@Valid @RequestBody TextDocumentRequest request) {
        return documentService.createFromText(request.name(), request.content(), auth.current());
    }

    @GetMapping("/{id}/chunks")
    public PageResponse<ChunkResponse> chunks(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return documentService.chunks(id, page, size, auth.current());
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        User owner = auth.current();
        DocumentResponse document = documentService.get(id, owner);
        Path path = documentService.originalFile(id, owner);
        try {
            Resource resource = new UrlResource(path.toUri());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(document.contentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment().filename(document.name()).build().toString())
                    .body(resource);
        } catch (java.net.MalformedURLException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not open that file");
        }
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        documentService.delete(id, auth.current());
    }
}
