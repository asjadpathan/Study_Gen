package com.asjad.studygen.service;

import com.asjad.studygen.dto.visual.GraphLinkDTO;
import com.asjad.studygen.dto.visual.GraphNodeDTO;
import com.asjad.studygen.dto.visual.MindMapNodeDTO;
import com.asjad.studygen.dto.visual.RoadmapGraphDTO;
import com.asjad.studygen.entity.Concept;
import com.asjad.studygen.entity.Module;
import com.asjad.studygen.entity.Roadmap;
import com.asjad.studygen.entity.User;
import com.asjad.studygen.repository.ConceptRepository;
import com.asjad.studygen.repository.ModuleRepository;
import com.asjad.studygen.repository.RoadmapRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoadmapVisualService {

    private final RoadmapRepository roadmapRepository;
    private final ModuleRepository moduleRepository;
    private final ConceptRepository conceptRepository;

    @Transactional(readOnly = true)
    public RoadmapGraphDTO getRoadmapGraph(User user, Long roadmapId) {
        Roadmap roadmap = roadmapRepository.findById(roadmapId)
                .orElseThrow(() -> new IllegalArgumentException("Roadmap not found: " + roadmapId));

        if (!roadmap.getUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied to roadmap");
        }

        List<Module> modules = moduleRepository.findByRoadmapIdOrderBySequenceOrderAsc(roadmapId);
        List<GraphNodeDTO> nodes = new ArrayList<>();
        List<GraphLinkDTO> links = new ArrayList<>();

        for (int i = 0; i < modules.size(); i++) {
            Module m = modules.get(i);
            String status;
            if (m.isCompleted()) {
                status = "COMPLETED";
            } else if (!m.isLocked()) {
                status = "IN_PROGRESS";
            } else {
                status = "LOCKED";
            }

            nodes.add(new GraphNodeDTO(
                    m.getId(),
                    m.getTitle(),
                    status,
                    m.getSequenceOrder(),
                    m.getMasteryScore(),
                    60 + (m.getSequenceOrder() * 15) // Estimated minutes
            ));

            if (i > 0) {
                links.add(new GraphLinkDTO(
                        modules.get(i - 1).getId(),
                        m.getId(),
                        "PREREQUISITE"
                ));
            }
        }

        return new RoadmapGraphDTO(roadmap.getId(), roadmap.getTitle(), nodes, links);
    }

    @Transactional(readOnly = true)
    public MindMapNodeDTO getModuleMindMap(User user, Long moduleId) {
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new IllegalArgumentException("Module not found: " + moduleId));

        List<Concept> concepts = conceptRepository.findByModuleId(moduleId);
        List<MindMapNodeDTO> conceptNodes = new ArrayList<>();

        if (concepts.isEmpty()) {
            // Generate standard concept hierarchy from module title & description
            conceptNodes.add(new MindMapNodeDTO(
                    "mod-" + moduleId + "-c1",
                    "Core Principles",
                    "FOUNDATION",
                    List.of(new MindMapNodeDTO("sub-1", "Theoretical Model", "THEORY", List.of()))
            ));
            conceptNodes.add(new MindMapNodeDTO(
                    "mod-" + moduleId + "-c2",
                    "Practical Applications",
                    "PRACTICE",
                    List.of(new MindMapNodeDTO("sub-2", "Hands-on Implementation", "APPLICATION", List.of()))
            ));
        } else {
            for (Concept c : concepts) {
                conceptNodes.add(new MindMapNodeDTO(
                        "concept-" + c.getId(),
                        c.getTitle(),
                        "CONCEPT",
                        List.of(
                                new MindMapNodeDTO("c-body-" + c.getId(), "Core Concepts", "DETAIL", List.of()),
                                new MindMapNodeDTO("c-rem-" + c.getId(), "Intuitive Analogy", "ANALOGY", List.of())
                        )
                ));
            }
        }

        return new MindMapNodeDTO(
                "module-" + module.getId(),
                module.getTitle(),
                "ROOT",
                conceptNodes
        );
    }
}
