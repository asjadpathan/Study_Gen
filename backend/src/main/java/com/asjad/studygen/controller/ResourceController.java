package com.asjad.studygen.controller;

import com.asjad.studygen.entity.User;
import com.asjad.studygen.service.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    @GetMapping
    public ResponseEntity<List<ResourceService.ResourceResponse>> getResources(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String category) {

        return ResponseEntity.ok(resourceService.getResources(user, category));
    }

    @GetMapping("/saved")
    public ResponseEntity<List<ResourceService.ResourceResponse>> getSavedResources(
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(resourceService.getSavedResources(user));
    }

    @PostMapping("/{id}/save")
    public ResponseEntity<ResourceService.SaveResult> saveResource(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        return ResponseEntity.ok(resourceService.saveResource(user, id));
    }

    @DeleteMapping("/{id}/save")
    public ResponseEntity<ResourceService.SaveResult> unsaveResource(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        return ResponseEntity.ok(resourceService.unsaveResource(user, id));
    }
}
