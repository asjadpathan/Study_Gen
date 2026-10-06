package com.asjad.studygen.service;

import com.asjad.studygen.dto.roadmap.CreateRoadmapRequest;
import com.asjad.studygen.dto.roadmap.ModuleResponse;
import com.asjad.studygen.dto.roadmap.RoadmapResponse;
import com.asjad.studygen.entity.Module;
import com.asjad.studygen.entity.Roadmap;
import com.asjad.studygen.entity.User;
import com.asjad.studygen.repository.RoadmapRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoadmapService {

    private final RoadmapRepository roadmapRepository;
    private final com.asjad.studygen.repository.ConceptRepository conceptRepository;
    private final TeachingContentService teachingContentService;

    @Transactional
    public RoadmapResponse createRoadmap(User user, CreateRoadmapRequest request) {
        Roadmap roadmap = new Roadmap();
        roadmap.setUser(user);
        roadmap.setTitle(request.title());
        roadmap.setTargetRole(request.targetRole());
        roadmap.setActive(true);

        if (request.modules() != null) {
            request.modules().forEach(mReq -> {
                Module module = new Module();
                module.setTitle(mReq.title());
                module.setDescription(mReq.description());
                module.setSequenceOrder(mReq.sequenceOrder());

                // First module unlocked; subsequent modules locked until prerequisite is passed
                module.setLocked(mReq.sequenceOrder() > 0);

                roadmap.addModule(module);
            });
        }

        Roadmap savedRoadmap = roadmapRepository.save(roadmap);
        return mapToResponse(savedRoadmap);
    }

    @Transactional
    public RoadmapResponse generateRoadmap(User user, com.asjad.studygen.dto.ai.GenerateRoadmapRequest request) {
        if (request.targetTopic() == null || request.targetTopic().isBlank()) {
            throw new IllegalArgumentException("Target topic is required");
        }

        Roadmap roadmap = new Roadmap();
        roadmap.setUser(user);
        roadmap.setTitle(request.targetTopic());
        roadmap.setTargetRole(request.targetRole() != null && !request.targetRole().isBlank() ? request.targetRole() : "Software Engineer");
        roadmap.setActive(true);

        String topic = request.targetTopic();

        // Subject-tailored curriculum starting from basics to advanced for the given topic
        List<Module> curriculumModules = teachingContentService.buildCurriculumForTopic(topic);
        curriculumModules.forEach(roadmap::addModule);
        Roadmap saved = roadmapRepository.save(roadmap);

        // Pre-seed in-depth teaching sub-parts for each module
        List<com.asjad.studygen.entity.Concept> allConcepts = new ArrayList<>();
        for (Module m : saved.getModules()) {
            allConcepts.addAll(generateTeachingSubPartsForModule(m, topic));
        }
        conceptRepository.saveAll(allConcepts);

        return mapToResponse(saved);
    }

    private List<com.asjad.studygen.entity.Concept> generateTeachingSubPartsForModule(Module module, String topic) {
        List<com.asjad.studygen.entity.Concept> list = new ArrayList<>();
        String mTitle = module.getTitle();
        int seq = module.getSequenceOrder() + 1;

        list.add(new com.asjad.studygen.entity.Concept(
                module,
                "Part " + seq + ".1: Core Concepts & Architectural Theory",
                "### In-Depth Conceptual Breakdown: " + mTitle + "\n\n" +
                "Understanding " + mTitle + " in " + topic + " requires building a deterministic mental model of how components interact at runtime.\n\n" +
                "#### Fundamental Mechanics:\n" +
                "- **Deterministic State Transitions**: Ensuring that operations produce predictable, repeatable outcomes without hidden side effects.\n" +
                "- **Separation of Concerns**: Keeping business logic strictly decoupled from transport, serialization, and storage layers.\n" +
                "- **Contract-First Design**: Defining explicit type signatures and validation constraints at system boundaries.\n\n" +
                "#### Architectural Analogy:\n" +
                "Think of this layer as an automated dispatch controller: inputs are strictly verified before being processed by specialized worker threads, guaranteeing zero corrupted state.",
                "An intuitive mental model for " + mTitle
        ));

        list.add(new com.asjad.studygen.entity.Concept(
                module,
                "Part " + seq + ".2: Hands-On Code Patterns & Implementation",
                "### Practical Implementation & Production Idioms\n\n" +
                "Let us translate the architectural theory of " + mTitle + " into clean, maintainable, production-ready code.\n\n" +
                "#### Step-by-Step Implementation Guide:\n" +
                "1. **Initialization**: Configure dependencies and sanitize incoming inputs.\n" +
                "2. **Core Domain Processing**: Execute the business logic with transaction safety and immutable data models.\n" +
                "3. **Result Dissemination**: Return typed responses and emit structured metrics for observability.\n\n" +
                "#### Key Idioms to Follow:\n" +
                "- Use immutability wherever possible to eliminate concurrency race conditions.\n" +
                "- Inject dependencies through constructors for maximum testability.",
                "Hands-on step-by-step code guidance for " + mTitle
        ));

        list.add(new com.asjad.studygen.entity.Concept(
                module,
                "Part " + seq + ".3: Production Pitfalls, Optimization & Edge Cases",
                "### Advanced Optimization & Common Pitfalls to Avoid\n\n" +
                "In high-throughput environments, naive implementations of " + mTitle + " can lead to resource leaks and cascading latency spikes.\n\n" +
                "#### Critical Anti-Patterns & How to Avoid Them:\n" +
                "- **Silent Failure**: Never catch exceptions without logging actionable context or re-throwing.\n" +
                "- **Unbounded Resource Allocation**: Always configure maximum pool sizes, request timeouts, and backpressure.\n" +
                "- **Cache Invalidation Drift**: Pair caches with definitive time-to-live (TTL) limits and write-through/invalidation listeners.\n\n" +
                "#### Production Rule of Thumb:\n" +
                "Profile before optimizing. Measure P99 latency percentiles rather than averages.",
                "Production survival tips and performance optimization"
        ));

        return list;
    }

    private Module createModule(String title, String desc, int order, boolean locked) {
        Module m = new Module();
        m.setTitle(title);
        m.setDescription(desc);
        m.setSequenceOrder(order);
        m.setLocked(locked);
        return m;
    }

    private RoadmapResponse mapToResponse(Roadmap roadmap) {
        var moduleResponses = roadmap.getModules().stream()
                .map(m -> new ModuleResponse(
                        m.getId(), m.getTitle(), m.getDescription(),
                        m.getSequenceOrder(), m.isLocked(),
                        m.isCompleted(), m.getMasteryScore()
                )).collect(Collectors.toList());

        return new RoadmapResponse(
                roadmap.getId(), roadmap.getTitle(), roadmap.getTargetRole(),
                roadmap.isActive(), moduleResponses
        );
    }

    public record RoadmapSummaryDTO(
            Long id,
            String title,
            String targetRole,
            String status,
            int progressPercent,
            int completedModules,
            int totalModules,
            String createdAt
    ) {}

    public record UpdateRoadmapRequest(
            String title,
            String targetRole,
            Boolean isActive,
            String status
    ) {}

    @Transactional(readOnly = true)
    public List<RoadmapSummaryDTO> getUserRoadmaps(User user) {
        List<Roadmap> list = roadmapRepository.findByUserId(user.getId());
        return list.stream().map(r -> {
            int total = r.getModules() != null ? r.getModules().size() : 0;
            int completed = r.getModules() != null ? (int) r.getModules().stream().filter(Module::isCompleted).count() : 0;
            int progress = total > 0 ? (completed * 100) / total : 0;

            String status;
            if (!r.isActive()) {
                status = "ARCHIVED";
            } else if (completed == 0) {
                status = "DRAFT";
            } else {
                status = "ACTIVE";
            }

            String createdStr = r.getCreatedAt() != null ? r.getCreatedAt().toString() : "";

            return new RoadmapSummaryDTO(
                    r.getId(),
                    r.getTitle(),
                    r.getTargetRole(),
                    status,
                    progress,
                    completed,
                    total,
                    createdStr
            );
        }).toList();
    }

    @Transactional
    public RoadmapSummaryDTO updateRoadmap(User user, Long roadmapId, UpdateRoadmapRequest req) {
        Roadmap roadmap = roadmapRepository.findById(roadmapId)
                .orElseThrow(() -> new IllegalArgumentException("Roadmap not found with id: " + roadmapId));

        if (roadmap.getUser() != null && !roadmap.getUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied to roadmap: " + roadmapId);
        }

        if (req.title() != null && !req.title().isBlank()) {
            roadmap.setTitle(req.title());
        }
        if (req.targetRole() != null) {
            roadmap.setTargetRole(req.targetRole());
        }
        if (req.isActive() != null) {
            roadmap.setActive(req.isActive());
        }

        Roadmap saved = roadmapRepository.save(roadmap);

        int total = saved.getModules() != null ? saved.getModules().size() : 0;
        int completed = saved.getModules() != null ? (int) saved.getModules().stream().filter(Module::isCompleted).count() : 0;
        int progress = total > 0 ? (completed * 100) / total : 0;

        String status;
        if (!saved.isActive()) {
            status = "ARCHIVED";
        } else if (completed == 0) {
            status = "DRAFT";
        } else {
            status = "ACTIVE";
        }

        return new RoadmapSummaryDTO(
                saved.getId(),
                saved.getTitle(),
                saved.getTargetRole(),
                status,
                progress,
                completed,
                total,
                saved.getCreatedAt() != null ? saved.getCreatedAt().toString() : ""
        );
    }

    @Transactional
    public void deleteRoadmap(User user, Long roadmapId) {
        Roadmap roadmap = roadmapRepository.findById(roadmapId)
                .orElseThrow(() -> new IllegalArgumentException("Roadmap not found with id: " + roadmapId));

        if (roadmap.getUser() != null && !roadmap.getUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied to roadmap: " + roadmapId);
        }

        roadmapRepository.delete(roadmap);
    }

    public record DetailedModuleDTO(
            Long id,
            String sequenceNumber,
            String title,
            String description,
            java.util.List<String> skills,
            String difficulty,
            Integer estimatedMinutes,
            Boolean isLocked,
            Boolean isCompleted,
            Integer attempts,
            String statusBadge,
            java.util.List<String> prerequisites,
            Double bestScore,
            java.util.List<String> weakConcepts,
            java.util.List<String> sourceChips
    ) {}

    public record RoadmapDetailDTO(
            Long id,
            String title,
            String targetRole,
            String status,
            Boolean isActive,
            Boolean isDraft,
            Integer hoursPerWeek,
            Integer targetWeeks,
            Integer remainingHours,
            Integer totalHours,
            String learningStyle,
            Integer progressPercent,
            Integer completedModulesCount,
            Integer totalModulesCount,
            java.util.List<DetailedModuleDTO> modules,
            WhatChangedDTO whatChanged
    ) {}

    public record WhatChangedDTO(
            java.util.List<String> keptModules,
            java.util.List<String> removedModules,
            Long parentRoadmapId
    ) {}

    public record GraphNodeDTO(
            Long id,
            String label,
            String status,
            String difficulty,
            Integer sequenceOrder
    ) {}

    public record GraphEdgeDTO(
            Long source,
            Long target,
            String relationship
    ) {}

    public record RoadmapGraphDTO(
            java.util.List<GraphNodeDTO> nodes,
            java.util.List<GraphEdgeDTO> edges
    ) {}

    public record RecreateRequestDTO(
            String reason,
            String targetTopic,
            String targetRole,
            Boolean archiveOld
    ) {}

    public record RecreateResponseDTO(
            Long newRoadmapId,
            RoadmapDetailDTO roadmapDetail
    ) {}

    @Transactional(readOnly = true)
    public RoadmapDetailDTO getRoadmapById(User user, Long roadmapId) {
        Roadmap r = roadmapRepository.findById(roadmapId)
                .orElseThrow(() -> new IllegalArgumentException("Roadmap not found: " + roadmapId));

        return buildDetailDTO(r);
    }

    @Transactional(readOnly = true)
    public RoadmapGraphDTO getRoadmapGraph(User user, Long roadmapId) {
        Roadmap r = roadmapRepository.findById(roadmapId)
                .orElseThrow(() -> new IllegalArgumentException("Roadmap not found: " + roadmapId));

        var nodes = r.getModules().stream().map(m -> new GraphNodeDTO(
                m.getId(),
                m.getTitle(),
                m.isCompleted() ? "COMPLETED" : (m.isLocked() ? "LOCKED" : "UNLOCKED"),
                m.getSequenceOrder() == 0 ? "BEGINNER" : (m.getSequenceOrder() < 3 ? "INTERMEDIATE" : "ADVANCED"),
                m.getSequenceOrder()
        )).toList();

        var edges = new java.util.ArrayList<GraphEdgeDTO>();
        for (int i = 0; i < nodes.size() - 1; i++) {
            edges.add(new GraphEdgeDTO(nodes.get(i).id(), nodes.get(i + 1).id(), "PREREQUISITE"));
        }

        return new RoadmapGraphDTO(nodes, edges);
    }

    @Transactional
    public RoadmapDetailDTO finalizeRoadmap(User user, Long roadmapId, boolean skip) {
        Roadmap r = roadmapRepository.findById(roadmapId)
                .orElseThrow(() -> new IllegalArgumentException("Roadmap not found: " + roadmapId));
        r.setActive(true);
        Roadmap saved = roadmapRepository.save(r);
        return buildDetailDTO(saved);
    }

    @Transactional
    public RecreateResponseDTO recreateRoadmap(User user, Long roadmapId, RecreateRequestDTO req) {
        Roadmap oldRoadmap = roadmapRepository.findById(roadmapId)
                .orElseThrow(() -> new IllegalArgumentException("Roadmap not found: " + roadmapId));

        if (Boolean.TRUE.equals(req.archiveOld())) {
            oldRoadmap.setActive(false);
            roadmapRepository.save(oldRoadmap);
        }

        Roadmap newRoadmap = new Roadmap();
        newRoadmap.setUser(user);
        newRoadmap.setTitle(req.targetTopic() != null && !req.targetTopic().isBlank() ? req.targetTopic() : oldRoadmap.getTitle() + " (v2)");
        newRoadmap.setTargetRole(req.targetRole() != null && !req.targetRole().isBlank() ? req.targetRole() : oldRoadmap.getTargetRole());
        newRoadmap.setActive(true);

        String topic = newRoadmap.getTitle();
        var newModules = java.util.List.of(
                createModule("Module 1: Foundations & Prerequisites of " + topic, "Core principles, updated for latest requirements", 0, false),
                createModule("Module 2: Accelerated Deep Dive in " + topic, "Hands-on projects and intensive drills", 1, true),
                createModule("Module 3: Advanced System Design & Optimization", "Scalability, error handling, and architecture", 2, true),
                createModule("Module 4: Capstone Mastery Project", "End-to-end deployment and portfolio project", 3, true)
        );
        newModules.forEach(newRoadmap::addModule);

        Roadmap savedNew = roadmapRepository.save(newRoadmap);

        WhatChangedDTO whatChanged = new WhatChangedDTO(
                java.util.List.of("Module 1: Foundations of " + topic, "Module 3: System Design"),
                java.util.List.of("Legacy introductory drills"),
                oldRoadmap.getId()
        );

        RoadmapDetailDTO detail = buildDetailDTO(savedNew, whatChanged);
        return new RecreateResponseDTO(savedNew.getId(), detail);
    }

    private RoadmapDetailDTO buildDetailDTO(Roadmap r) {
        return buildDetailDTO(r, null);
    }

    private RoadmapDetailDTO buildDetailDTO(Roadmap r, WhatChangedDTO whatChanged) {
        int total = r.getModules() != null ? r.getModules().size() : 0;
        int completed = r.getModules() != null ? (int) r.getModules().stream().filter(Module::isCompleted).count() : 0;
        int progress = total > 0 ? (completed * 100) / total : 0;

        boolean isDraft = r.getCreatedAt() != null && completed == 0 && total > 0 && r.getModules().stream().allMatch(m -> !m.isCompleted());
        String status = !r.isActive() ? "ARCHIVED" : (isDraft ? "DRAFT" : "ACTIVE");

        var moduleDTOs = r.getModules().stream().map(m -> {
            String seqStr = String.format("%02d", m.getSequenceOrder() + 1);
            String diff = m.getSequenceOrder() == 0 ? "BEGINNER" : (m.getSequenceOrder() < 3 ? "INTERMEDIATE" : "ADVANCED");
            String statusBadge = m.isCompleted() ? "COMPLETED" : (m.isLocked() ? "LOCKED" : (m.getSequenceOrder() == 0 ? "IN_PROGRESS" : "UNLOCKED"));
            
            return new DetailedModuleDTO(
                    m.getId(),
                    seqStr,
                    m.getTitle(),
                    m.getDescription(),
                    java.util.List.of("Core Skills", "Syntax", "Patterns"),
                    diff,
                    45 + (m.getSequenceOrder() * 15),
                    m.isLocked(),
                    m.isCompleted(),
                    m.isCompleted() ? 1 : 0,
                    statusBadge,
                    m.getSequenceOrder() > 0 ? java.util.List.of("Module " + m.getSequenceOrder()) : java.util.List.of(),
                    m.isCompleted() ? 92.5 : null,
                    m.isCompleted() ? java.util.List.of() : java.util.List.of("Edge case validation"),
                    java.util.List.of("Official Docs", "Practice Sandbox")
            );
        }).toList();

        int hoursPerWeek = 10;
        int targetWeeks = 8;
        int totalHours = hoursPerWeek * targetWeeks;
        int remainingHours = totalHours - (completed * (totalHours / Math.max(1, total)));

        return new RoadmapDetailDTO(
                r.getId(),
                r.getTitle(),
                r.getTargetRole(),
                status,
                r.isActive(),
                isDraft,
                hoursPerWeek,
                targetWeeks,
                remainingHours,
                totalHours,
                "PRACTICAL",
                progress,
                completed,
                total,
                moduleDTOs,
                whatChanged
        );
    }
}