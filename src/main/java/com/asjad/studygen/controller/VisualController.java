package com.asjad.studygen.controller;

import com.asjad.studygen.dto.visual.MindMapNodeDTO;
import com.asjad.studygen.dto.visual.RoadmapGraphDTO;
import com.asjad.studygen.entity.User;
import com.asjad.studygen.service.RoadmapVisualService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/visual")
@RequiredArgsConstructor
public class VisualController {

    private final RoadmapVisualService visualService;

    @GetMapping("/roadmaps/{roadmapId}/graph")
    public ResponseEntity<RoadmapGraphDTO> getRoadmapGraph(
            @AuthenticationPrincipal User user,
            @PathVariable Long roadmapId) {

        return ResponseEntity.ok(visualService.getRoadmapGraph(user, roadmapId));
    }

    @GetMapping("/modules/{moduleId}/mindmap")
    public ResponseEntity<MindMapNodeDTO> getModuleMindMap(
            @AuthenticationPrincipal User user,
            @PathVariable Long moduleId) {

        return ResponseEntity.ok(visualService.getModuleMindMap(user, moduleId));
    }
}
