package com.asjad.studygen.controller;

import com.asjad.studygen.dto.roadmap.CreateRoadmapRequest;
import com.asjad.studygen.dto.roadmap.RoadmapResponse;
import com.asjad.studygen.entity.User;
import com.asjad.studygen.service.RoadmapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/roadmaps")
@RequiredArgsConstructor
public class RoadmapController {

    private final RoadmapService roadmapService;

    @PostMapping
    public ResponseEntity<RoadmapResponse> createRoadmap(
            @AuthenticationPrincipal User user,
            @RequestBody CreateRoadmapRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(roadmapService.createRoadmap(user, request));
    }

    @PostMapping("/generate")
    public ResponseEntity<RoadmapResponse> generateRoadmap(
            @AuthenticationPrincipal User user,
            @RequestBody com.asjad.studygen.dto.ai.GenerateRoadmapRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(roadmapService.generateRoadmap(user, request));
    }

    @GetMapping
    public ResponseEntity<java.util.List<RoadmapService.RoadmapSummaryDTO>> getUserRoadmaps(
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(roadmapService.getUserRoadmaps(user));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<RoadmapService.RoadmapSummaryDTO> updateRoadmap(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody RoadmapService.UpdateRoadmapRequest request) {

        return ResponseEntity.ok(roadmapService.updateRoadmap(user, id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoadmapService.RoadmapDetailDTO> getRoadmapById(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(roadmapService.getRoadmapById(user, id));
    }

    @GetMapping("/{id}/graph")
    public ResponseEntity<RoadmapService.RoadmapGraphDTO> getRoadmapGraph(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(roadmapService.getRoadmapGraph(user, id));
    }

    @PostMapping("/{id}/finalize")
    public ResponseEntity<RoadmapService.RoadmapDetailDTO> finalizeRoadmap(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestParam(defaultValue = "true") boolean skip) {
        return ResponseEntity.ok(roadmapService.finalizeRoadmap(user, id, skip));
    }

    @PostMapping("/{id}/recreate")
    public ResponseEntity<RoadmapService.RecreateResponseDTO> recreateRoadmap(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody RoadmapService.RecreateRequestDTO request) {
        return ResponseEntity.ok(roadmapService.recreateRoadmap(user, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoadmap(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        roadmapService.deleteRoadmap(user, id);
        return ResponseEntity.noContent().build();
    }
}