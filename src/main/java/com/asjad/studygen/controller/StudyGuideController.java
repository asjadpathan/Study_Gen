package com.asjad.studygen.controller;

import com.asjad.studygen.dto.document.StudyGuideResponse;
import com.asjad.studygen.entity.User;
import com.asjad.studygen.service.StudyGuideService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/study-guide")
@RequiredArgsConstructor
public class StudyGuideController {

    private final StudyGuideService studyGuideService;

    @GetMapping("/modules/{moduleId}")
    public ResponseEntity<StudyGuideResponse> getModuleStudyGuide(
            @AuthenticationPrincipal User user,
            @PathVariable Long moduleId) {

        return ResponseEntity.ok(studyGuideService.generateStudyGuide(user, moduleId));
    }
}
