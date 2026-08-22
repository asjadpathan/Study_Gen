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
}