package com.asjad.studygen.controller;

import com.asjad.studygen.entity.User;
import com.asjad.studygen.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/streak-heatmap")
    public ResponseEntity<AnalyticsService.StreakHeatmapResponse> getStreakHeatmap(
            @AuthenticationPrincipal User user,
            @RequestParam(name = "days", required = false, defaultValue = "180") Integer days) {

        return ResponseEntity.ok(analyticsService.getStreakHeatmap(user.getId(), days));
    }

    @GetMapping("/profile-summary")
    public ResponseEntity<AnalyticsService.ProfileSummaryResponse> getProfileSummary(
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(analyticsService.getProfileSummary(user.getId()));
    }
}
