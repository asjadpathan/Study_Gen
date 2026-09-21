package com.asjad.studygen.service;

import com.asjad.studygen.dto.document.DocumentResponse;
import com.asjad.studygen.entity.DocumentChunk;
import com.asjad.studygen.entity.UploadedDocument;
import com.asjad.studygen.entity.User;
import com.asjad.studygen.repository.UploadedDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentProcessingService {

    private final UploadedDocumentRepository documentRepository;

    @Transactional
    public DocumentResponse processAndStoreDocument(User user, MultipartFile file) {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document.txt";
        String contentType = file.getContentType() != null ? file.getContentType() : "text/plain";
        long size = file.getSize();

        String content;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            content = reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            content = "File content could not be read: " + e.getMessage();
        }

        UploadedDocument document = new UploadedDocument(user, filename, contentType, size);

        // Chunk text into segments (~500 chars with small overlap)
        List<String> chunks = chunkText(content, 500, 50);
        int index = 0;
        for (String chunkText : chunks) {
            DocumentChunk chunk = new DocumentChunk(document, index++, chunkText);
            document.addChunk(chunk);
        }

        UploadedDocument saved = documentRepository.save(document);

        return new DocumentResponse(
                saved.getId(),
                saved.getFilename(),
                saved.getFileType(),
                saved.getFileSize(),
                saved.getChunks().size(),
                saved.getUploadedAt().toString()
        );
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> getUserDocuments(User user) {
        return documentRepository.findByUserIdOrderByUploadedAtDesc(user.getId())
                .stream()
                .map(d -> new DocumentResponse(
                        d.getId(),
                        d.getFilename(),
                        d.getFileType(),
                        d.getFileSize(),
                        d.getChunks().size(),
                        d.getUploadedAt().toString()
                )).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> searchRelevantChunks(Long userId, String query, int topK) {
        List<UploadedDocument> docs = documentRepository.findByUserIdOrderByUploadedAtDesc(userId);
        List<String> matches = new ArrayList<>();

        String lowerQuery = query.toLowerCase();
        for (UploadedDocument doc : docs) {
            for (DocumentChunk chunk : doc.getChunks()) {
                if (chunk.getContentText().toLowerCase().contains(lowerQuery)) {
                    matches.add(chunk.getContentText());
                    if (matches.size() >= topK) {
                        return matches;
                    }
                }
            }
        }

        // If no direct substring match, return first few chunks as context
        if (matches.isEmpty() && !docs.isEmpty() && !docs.get(0).getChunks().isEmpty()) {
            matches.add(docs.get(0).getChunks().get(0).getContentText());
        }

        return matches;
    }

    private List<String> chunkText(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }

        int start = 0;
        int length = text.length();

        while (start < length) {
            int end = Math.min(start + chunkSize, length);
            chunks.add(text.substring(start, end));
            if (end == length) break;
            start += (chunkSize - overlap);
        }

        return chunks;
    }
}
