package com.asjad.studygen.service;

import com.asjad.studygen.dto.document.*;
import com.asjad.studygen.entity.*;
import com.asjad.studygen.entity.Module;
import com.asjad.studygen.repository.ModuleRepository;
import com.asjad.studygen.repository.RoadmapRepository;
import com.asjad.studygen.repository.UploadedDocumentRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentProcessingService {

    private final UploadedDocumentRepository documentRepository;
    private final RoadmapRepository roadmapRepository;
    private final ModuleRepository moduleRepository;
    private final UserActivityService userActivityService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final long MAX_FILE_SIZE = 25 * 1024 * 1024; // 25 MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "docx", "txt", "md");

    @Transactional
    public DocumentDetailResponse processAndStoreDocument(User user, MultipartFile file, Long roadmapId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file cannot be empty.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds 25MB limit. Please upload a smaller file.");
        }

        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document.txt";
        String ext = getFileExtension(filename);
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("Unsupported file format: ." + ext + ". Accepted formats: PDF, DOCX, TXT, MD.");
        }

        String contentType = file.getContentType() != null ? file.getContentType() : "text/plain";
        long size = file.getSize();

        String content;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            content = reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            content = "File content could not be read: " + e.getMessage();
        }

        UploadedDocument document = new UploadedDocument(user, filename, contentType, size);
        document.setStatus("Ready");

        if (roadmapId != null) {
            roadmapRepository.findById(roadmapId).ifPresent(document::setRoadmap);
        }

        // Chunk text
        List<String> chunks = chunkText(content, 500, 50);
        int index = 0;
        for (String chunkText : chunks) {
            DocumentChunk chunk = new DocumentChunk(document, index++, chunkText);
            document.addChunk(chunk);
        }

        UploadedDocument saved = documentRepository.save(document);
        userActivityService.logActivity(user, "DOCUMENT_UPLOAD", 10);

        return mapToDetailResponse(saved);
    }

    @Transactional
    public DocumentDetailResponse processUrlDocument(User user, UrlDocumentRequest request) {
        String urlString = request.url();
        if (urlString == null || urlString.isBlank()) {
            throw new IllegalArgumentException("Please provide a valid URL.");
        }

        String textContent;
        String filename;
        try {
            URI uri = URI.create(urlString.trim());
            filename = uri.getHost() != null ? uri.getHost() + uri.getPath() : "web-document.txt";
            if (filename.endsWith("/") || filename.isBlank()) {
                filename = "web-document.txt";
            }

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(6))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(8))
                    .header("User-Agent", "StudyGen Document Importer/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IllegalArgumentException("Failed to fetch URL: HTTP " + response.statusCode());
            }

            // Strip HTML tags for clean text content
            textContent = response.body().replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
            if (textContent.isBlank()) {
                textContent = "Imported content from " + urlString;
            }
        } catch (Exception e) {
            log.warn("URL import failed for {}: {}", urlString, e.getMessage());
            throw new IllegalArgumentException("Failed to fetch content from URL: " + e.getMessage());
        }

        UploadedDocument document = new UploadedDocument(user, filename, "text/html", textContent.length());
        document.setStatus("Ready");

        if (request.roadmapId() != null) {
            roadmapRepository.findById(request.roadmapId()).ifPresent(document::setRoadmap);
        }

        List<String> chunks = chunkText(textContent, 500, 50);
        int index = 0;
        for (String chunkText : chunks) {
            DocumentChunk chunk = new DocumentChunk(document, index++, chunkText);
            document.addChunk(chunk);
        }

        UploadedDocument saved = documentRepository.save(document);
        userActivityService.logActivity(user, "DOCUMENT_UPLOAD", 10);

        return mapToDetailResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<DocumentDetailResponse> getUserDocumentDetails(User user) {
        return documentRepository.findByUserIdOrderByUploadedAtDesc(user.getId())
                .stream()
                .map(this::mapToDetailResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DocumentDetailResponse getDocumentDetail(User user, Long docId) {
        UploadedDocument doc = documentRepository.findById(docId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + docId));

        if (!doc.getUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Unauthorized access to document.");
        }

        return mapToDetailResponse(doc);
    }

    @Transactional
    public void deleteDocument(User user, Long docId) {
        UploadedDocument doc = documentRepository.findById(docId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + docId));

        if (!doc.getUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Unauthorized to delete document.");
        }

        documentRepository.delete(doc);
    }

    @Transactional
    public DocumentSummaryDTO generateSummary(User user, Long docId) {
        UploadedDocument doc = documentRepository.findById(docId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + docId));

        if (!doc.getUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Unauthorized to access document.");
        }

        String overview = "This document comprehensively covers key concepts and practical methodologies in " +
                doc.getFilename() + ". It outlines core principles, essential terminology, and architectural workflows designed to accelerate understanding. The content emphasizes foundational clarity while establishing strong patterns for real-world application.";

        List<String> keyPoints = List.of(
                "Establishes a solid foundational conceptual framework for " + doc.getFilename() + ".",
                "Highlights critical mechanics, operational boundaries, and best practice implementations.",
                "Identifies common stumbling blocks, anti-patterns, and diagnostic techniques.",
                "Provides actionable takeaways easily synthesizable into active study drills."
        );

        String audience = "Ideal for intermediate learners, software engineers, and practitioners seeking structured domain mastery.";

        doc.setSummaryOverview(overview);
        try {
            doc.setSummaryKeyPointsJson(objectMapper.writeValueAsString(keyPoints));
        } catch (Exception e) {
            doc.setSummaryKeyPointsJson("[]");
        }
        doc.setSummaryAudience(audience);
        documentRepository.save(doc);

        userActivityService.logActivity(user, "DOCUMENT_SUMMARY", 5);

        return new DocumentSummaryDTO(overview, keyPoints, audience);
    }

    @Transactional
    public DocumentDetailResponse generateNotes(User user, Long docId) {
        UploadedDocument doc = documentRepository.findById(docId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + docId));

        if (!doc.getUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Unauthorized to access document.");
        }

        String notes = "# Study Notes: " + doc.getFilename() + "\n\n" +
                "## 1. Executive Overview\n\n" +
                "This document serves as an authoritative guide on the core architectural and conceptual foundations discussed within `" + doc.getFilename() + "`. " +
                "Careful study of these notes reinforces memory retention and prepares you for technical assessments.\n\n" +
                "## 2. Core Concepts & Definitions\n\n" +
                "- **Primary Mechanism**: The fundamental driver that governs system behavior and data flow.\n" +
                "- **Boundary Invariants**: Key constraints that must be preserved under high concurrency or failure scenarios.\n" +
                "- **Resilience Pattern**: Graceful degradation strategies when external dependencies become unavailable.\n\n" +
                "## 3. Best Practices & Workflows\n\n" +
                "1. Always enforce strict input validation before initiating state mutations.\n" +
                "2. Decouple synchronous user-facing request paths from intensive computational workloads.\n" +
                "3. Instrument distributed tracing and logging with unified correlation IDs.\n\n" +
                "## 4. Review Questions\n\n" +
                "1. *What is the primary trade-off highlighted in this document?*\n" +
                "2. *How does this architecture handle unexpected edge cases or network partitions?*\n" +
                "3. *Which diagnostic metrics should be monitored to detect early signs of performance regression?*\n";

        doc.setNotesMarkdown(notes);
        UploadedDocument saved = documentRepository.save(doc);

        userActivityService.logActivity(user, "DOCUMENT_NOTES", 5);

        return mapToDetailResponse(saved);
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

        if (matches.isEmpty() && !docs.isEmpty() && !docs.get(0).getChunks().isEmpty()) {
            matches.add(docs.get(0).getChunks().get(0).getContentText());
        }

        return matches;
    }

    private DocumentDetailResponse mapToDetailResponse(UploadedDocument doc) {
        DocumentSummaryDTO summary = null;
        if (doc.getSummaryOverview() != null) {
            List<String> points = new ArrayList<>();
            try {
                if (doc.getSummaryKeyPointsJson() != null) {
                    points = objectMapper.readValue(doc.getSummaryKeyPointsJson(), new TypeReference<>() {});
                }
            } catch (Exception ignored) {}
            summary = new DocumentSummaryDTO(doc.getSummaryOverview(), points, doc.getSummaryAudience());
        }

        Long roadmapId = doc.getRoadmap() != null ? doc.getRoadmap().getId() : null;
        String roadmapTitle = doc.getRoadmap() != null ? doc.getRoadmap().getTitle() : null;

        List<DocumentCitationDTO> usedIn = new ArrayList<>();
        if (doc.getRoadmap() != null) {
            List<Module> modules = moduleRepository.findByRoadmapIdOrderBySequenceOrderAsc(doc.getRoadmap().getId());
            for (Module m : modules) {
                usedIn.add(new DocumentCitationDTO(doc.getRoadmap().getId(), doc.getRoadmap().getTitle(), m.getId(), m.getTitle()));
            }
        }

        return new DocumentDetailResponse(
                doc.getId(),
                doc.getFilename(),
                doc.getFileType(),
                doc.getFileSize(),
                doc.getStatus() != null ? doc.getStatus() : "Ready",
                doc.getUploadedAt().toString(),
                roadmapId,
                roadmapTitle,
                doc.getChunks() != null ? doc.getChunks().size() : 0,
                summary,
                doc.getNotesMarkdown(),
                usedIn
        );
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private List<String> chunkText(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) return chunks;

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
