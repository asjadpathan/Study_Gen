package com.asjad.studygen.controller;

import com.asjad.studygen.dto.assessment.*;
import com.asjad.studygen.entity.User;
import com.asjad.studygen.service.AssessmentService;
import com.asjad.studygen.service.MasteryProgressionService;
import com.asjad.studygen.service.RemediationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assessments")
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentService assessmentService;
    private final MasteryProgressionService progressionService;
    private final RemediationService remediationService;

    @PostMapping("/diagnostic/generate")
    public ResponseEntity<AssessmentResponse> generateDiagnosticQuiz(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid DiagnosticQuizRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(assessmentService.generateDiagnosticQuiz(user, request));
    }

    @PostMapping("/modules/{moduleId}/generate")
    public ResponseEntity<AssessmentResponse> generateModuleAssessment(
            @AuthenticationPrincipal User user,
            @PathVariable Long moduleId) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(assessmentService.generateModuleAssessment(user, moduleId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssessmentResponse> getAssessmentById(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        return ResponseEntity.ok(assessmentService.getAssessmentById(user, id));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<AttemptResultResponse> submitAttempt(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody @Valid SubmitAttemptRequest request) {

        return ResponseEntity.ok(progressionService.submitAttempt(user, id, request));
    }

    @GetMapping("/attempts/{attemptId}/remediation")
    public ResponseEntity<RemediationResponse> getRemediation(
            @AuthenticationPrincipal User user,
            @PathVariable Long attemptId) {

        return ResponseEntity.ok(remediationService.getOrGenerateRemediation(user, attemptId));
    }
}
