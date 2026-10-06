package com.asjad.studygen.controller;

import com.asjad.studygen.dto.assessment.AssessmentResponse;
import com.asjad.studygen.dto.assessment.RemediationResponse;
import com.asjad.studygen.dto.practice.DrillEvaluationRequest;
import com.asjad.studygen.dto.practice.DrillEvaluationResponse;
import com.asjad.studygen.dto.practice.DrillItemDTO;
import com.asjad.studygen.dto.practice.ModuleDrillsResponse;
import com.asjad.studygen.entity.Concept;
import com.asjad.studygen.entity.Module;
import com.asjad.studygen.entity.User;
import com.asjad.studygen.repository.ConceptRepository;
import com.asjad.studygen.repository.ModuleRepository;
import com.asjad.studygen.service.AssessmentService;
import com.asjad.studygen.service.DrillService;
import com.asjad.studygen.service.RemediationService;
import com.asjad.studygen.service.TeachingContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/modules")
@RequiredArgsConstructor
public class ModuleController {

    private final AssessmentService assessmentService;
    private final RemediationService remediationService;
    private final DrillService drillService;
    private final ModuleRepository moduleRepository;
    private final ConceptRepository conceptRepository;
    private final TeachingContentService teachingContentService;

    public record ModuleSubPartDTO(
            Long id,
            String title,
            String level, // "BASIC", "INTERMEDIATE", "ADVANCED"
            String contentBody,
            String keyTakeaways,
            String codeSnippet,
            String pitfalls,
            boolean completed
    ) {}

    public record ModuleStudyResponse(
            Long id,
            String title,
            String description,
            int sequenceOrder,
            boolean isLocked,
            boolean isCompleted,
            double masteryScore,
            Long roadmapId,
            String roadmapTitle,
            List<ModuleSubPartDTO> subParts,
            Long previousModuleId,
            String previousModuleTitle,
            Long nextModuleId,
            String nextModuleTitle
    ) {}

    @GetMapping("/{id}")
    @Transactional
    public ResponseEntity<ModuleStudyResponse> getModuleStudyDetails(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        Module module = moduleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Module not found: " + id));

        // Module 0 (first module) is always unlocked.
        // Subsequent modules are unlocked once the prerequisite module is completed with >= 80% marks.
        if (module.getSequenceOrder() > 0 && module.isLocked()) {
            if (module.getRoadmap() != null) {
                var prevOpt = moduleRepository.findByRoadmapIdAndSequenceOrder(
                        module.getRoadmap().getId(), module.getSequenceOrder() - 1
                );
                if (prevOpt.isPresent() && prevOpt.get().isCompleted()) {
                    module.setLocked(false);
                    moduleRepository.save(module);
                }
            }
        }

        if (module.isLocked()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Module " + (module.getSequenceOrder() + 1) + " is locked. You must pass the prerequisite module assessment with at least 80% marks to unlock it.");
        }

        String roadmapTopic = module.getRoadmap() != null ? module.getRoadmap().getTitle() : module.getTitle();
        List<ModuleSubPartDTO> subParts = teachingContentService.generateDetailedTeachingSubParts(module, roadmapTopic);

        // Sync completion state with persisted concepts if any
        List<Concept> concepts = conceptRepository.findByModuleId(id);
        if (concepts != null && !concepts.isEmpty()) {
            for (int i = 0; i < Math.min(subParts.size(), concepts.size()); i++) {
                Concept c = concepts.get(i);
                ModuleSubPartDTO sp = subParts.get(i);
                subParts.set(i, new ModuleSubPartDTO(
                        c.getId(),
                        sp.title(),
                        sp.level(),
                        sp.contentBody(),
                        sp.keyTakeaways(),
                        sp.codeSnippet(),
                        sp.pitfalls(),
                        c.isCompleted()
                ));
            }
        }

        Long previousModuleId = null;
        String previousModuleTitle = null;
        Long nextModuleId = null;
        String nextModuleTitle = null;

        if (module.getRoadmap() != null) {
            Long rId = module.getRoadmap().getId();
            var prevOpt = moduleRepository.findByRoadmapIdAndSequenceOrder(rId, module.getSequenceOrder() - 1);
            if (prevOpt.isPresent()) {
                previousModuleId = prevOpt.get().getId();
                previousModuleTitle = prevOpt.get().getTitle();
            }

            var nextOpt = moduleRepository.findByRoadmapIdAndSequenceOrder(rId, module.getSequenceOrder() + 1);
            if (nextOpt.isPresent()) {
                nextModuleId = nextOpt.get().getId();
                nextModuleTitle = nextOpt.get().getTitle();
            }
        }

        return ResponseEntity.ok(new ModuleStudyResponse(
                module.getId(),
                module.getTitle(),
                module.getDescription(),
                module.getSequenceOrder(),
                module.isLocked(),
                module.isCompleted(),
                module.getMasteryScore(),
                module.getRoadmap() != null ? module.getRoadmap().getId() : null,
                module.getRoadmap() != null ? module.getRoadmap().getTitle() : null,
                subParts,
                previousModuleId,
                previousModuleTitle,
                nextModuleId,
                nextModuleTitle
        ));
    }

    private List<Concept> seedSubPartsForModule(Module module) {
        String base = module.getTitle();
        List<Concept> list = new ArrayList<>();

        list.add(new Concept(
                module,
                "Part 1: Core Fundamentals & Principles of " + base,
                "### Understanding the Foundations\n\n" +
                "To master " + base + ", you must first establish a solid mental model of how the underlying components interact.\n\n" +
                "#### Key Principles:\n" +
                "- **Deterministic State**: Ensure state transformations are predictable and side-effects are isolated.\n" +
                "- **Separation of Concerns**: Decouple business logic from infrastructure and presentation.\n" +
                "- **Defensive Design**: Validate boundaries, handle edge cases early, and fail gracefully with actionable error semantics."
        ));

        list.add(new Concept(
                module,
                "Part 2: Architecture & Hands-on Implementation",
                "### Practical Architectural Mechanics\n\n" +
                "Now that the foundational principles are clear, let us examine how to implement this pattern in production-grade code.\n\n" +
                "#### Architectural Flow:\n" +
                "1. **Input Ingestion & Validation**: Sanitize payloads against defined schema constraints.\n" +
                "2. **Core Domain Execution**: Apply domain invariants and transactional integrity.\n" +
                "3. **Result Dispatch & Telemetry**: Emit domain events and capture structured observability metrics."
        ));

        list.add(new Concept(
                module,
                "Part 3: Advanced Optimization & Production Patterns",
                "### Scaling, Optimization, and Edge Cases\n\n" +
                "In enterprise and high-throughput environments, standard implementations face bottlenecks around latency, concurrency, and resource contention.\n\n" +
                "#### Advanced Considerations:\n" +
                "- **Caching Strategies**: Layered local and distributed caching with bounded TTLs to mitigate cache stampedes.\n" +
                "- **Resilience & Fault Tolerance**: Circuit breakers, bulkheads, and exponential back-off retries for downstream dependencies.\n" +
                "- **Observability**: Distributed tracing, structured JSON logs, and SLA alert thresholds."
        ));

        return conceptRepository.saveAll(list);
    }

    @PostMapping("/{id}/test")
    public ResponseEntity<AssessmentResponse> getOrCreateModuleTest(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        return ResponseEntity.ok(assessmentService.getOrCreateModuleTest(user, id));
    }

    @GetMapping("/{id}/remediation")
    public ResponseEntity<RemediationResponse> getModuleRemediation(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        return ResponseEntity.ok(remediationService.getModuleRemediation(user, id));
    }

    @GetMapping("/{id}/drills")
    public ResponseEntity<ModuleDrillsResponse> getModuleDrills(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        return ResponseEntity.ok(drillService.getDrillsForModule(user, id));
    }

    @PostMapping("/{id}/drills/generate")
    public ResponseEntity<List<DrillItemDTO>> generateMoreDrills(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        return ResponseEntity.ok(drillService.generateMoreDrills(user, id));
    }

    @PostMapping("/{id}/drills/evaluate")
    public ResponseEntity<DrillEvaluationResponse> evaluateScenarioAnswer(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody DrillEvaluationRequest request) {

        return ResponseEntity.ok(drillService.evaluateScenarioAnswer(user, request));
    }
}
