package com.asjad.studygen.controller;

import com.asjad.studygen.entity.User;
import com.asjad.studygen.service.EcosystemDiscoveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ecosystem")
@RequiredArgsConstructor
public class EcosystemController {

    private final EcosystemDiscoveryService ecosystemDiscoveryService;

    @GetMapping("/discovery")
    public ResponseEntity<EcosystemDiscoveryService.EcosystemDiscoveryResponse> discoverEcosystem(
            @AuthenticationPrincipal User user,
            @RequestParam(name = "topic", required = false) String topic) {

        return ResponseEntity.ok(ecosystemDiscoveryService.discoverEcosystem(user, topic));
    }
}
