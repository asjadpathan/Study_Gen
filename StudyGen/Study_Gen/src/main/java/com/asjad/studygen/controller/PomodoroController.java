package com.asjad.studygen.controller;

import com.asjad.studygen.entity.User;
import com.asjad.studygen.service.PomodoroService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/productivity/pomodoro")
@RequiredArgsConstructor
public class PomodoroController {

    private final PomodoroService pomodoroService;

    @PostMapping("/log")
    public ResponseEntity<PomodoroService.PomodoroSessionResponse> logSession(
            @AuthenticationPrincipal User user,
            @RequestBody PomodoroService.PomodoroLogRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pomodoroService.logSession(user.getId(), request));
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<PomodoroService.PomodoroSessionResponse>> getSessions(
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(pomodoroService.getUserSessions(user.getId()));
    }
}
