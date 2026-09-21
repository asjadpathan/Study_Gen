package com.asjad.studygen.controller;

import com.asjad.studygen.entity.User;
import com.asjad.studygen.service.CareerMatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/career")
@RequiredArgsConstructor
public class CareerController {

    private final CareerMatchingService careerMatchingService;

    @PostMapping("/job-match")
    public ResponseEntity<CareerMatchingService.JobMatchResponse> matchJob(
            @AuthenticationPrincipal User user,
            @RequestBody CareerMatchingService.JobMatchRequest request) {

        return ResponseEntity.ok(careerMatchingService.matchJob(user, request));
    }

    @GetMapping("/recommendations")
    public ResponseEntity<CareerMatchingService.CareerRecommendationsResponse> getRecommendations(
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(careerMatchingService.getRecommendations(user));
    }
}
