package com.asjad.studygen.controller;

import com.asjad.studygen.dto.document.*;
import com.asjad.studygen.entity.User;
import com.asjad.studygen.service.DocumentProcessingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentProcessingService documentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentDetailResponse> uploadDocument(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "roadmapId", required = false) Long roadmapId) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.processAndStoreDocument(user, file, roadmapId));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentDetailResponse> uploadDocumentLegacy(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "roadmapId", required = false) Long roadmapId) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.processAndStoreDocument(user, file, roadmapId));
    }

    @PostMapping("/url")
    public ResponseEntity<DocumentDetailResponse> uploadDocumentByUrl(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid UrlDocumentRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.processUrlDocument(user, request));
    }

    @GetMapping
    public ResponseEntity<List<DocumentDetailResponse>> getUserDocuments(
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(documentService.getUserDocumentDetails(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentDetailResponse> getDocument(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        return ResponseEntity.ok(documentService.getDocumentDetail(user, id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        documentService.deleteDocument(user, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/summary")
    public ResponseEntity<DocumentSummaryDTO> generateSummary(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        return ResponseEntity.ok(documentService.generateSummary(user, id));
    }

    @PostMapping("/{id}/notes")
    public ResponseEntity<DocumentDetailResponse> generateNotes(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        return ResponseEntity.ok(documentService.generateNotes(user, id));
    }
}
