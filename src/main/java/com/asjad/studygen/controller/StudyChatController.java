package com.asjad.studygen.controller;

import com.asjad.studygen.dto.document.ChatMessageRequest;
import com.asjad.studygen.dto.document.ChatMessageResponse;
import com.asjad.studygen.entity.User;
import com.asjad.studygen.service.StudyAssistantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class StudyChatController {

    private final StudyAssistantService chatService;

    @PostMapping("/message")
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid ChatMessageRequest request) {

        return ResponseEntity.ok(chatService.sendMessage(user, request));
    }

    @GetMapping("/history")
    public ResponseEntity<List<ChatMessageResponse>> getChatHistory(
            @AuthenticationPrincipal User user,
            @RequestParam(value = "moduleId", required = false) Long moduleId) {

        return ResponseEntity.ok(chatService.getChatHistory(user, moduleId));
    }
}
