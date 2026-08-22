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

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoadmapService {

    private final RoadmapRepository roadmapRepository;
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

                // Unlock only the first module
                module.setLocked(mReq.sequenceOrder() != 0);

                roadmap.addModule(module);
            });
        }

        Roadmap savedRoadmap = roadmapRepository.save(roadmap);
        return mapToResponse(savedRoadmap);
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
}