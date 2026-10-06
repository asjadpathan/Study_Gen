package com.asjad.studygen.controller;

import com.asjad.studygen.dto.chat.*;
import com.asjad.studygen.dto.document.ChatMessageRequest;
import com.asjad.studygen.dto.document.ChatMessageResponse;
import com.asjad.studygen.entity.User;
import com.asjad.studygen.service.ChatSessionService;
import com.asjad.studygen.service.StudyAssistantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class StudyChatController {

    private final StudyAssistantService chatService;
    private final ChatSessionService sessionService;

    @GetMapping("/sessions")
    public ResponseEntity<List<ChatSessionResponse>> getSessions(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(sessionService.getUserSessions(user));
    }

    @PostMapping("/sessions")
    public ResponseEntity<ChatSessionResponse> createSession(
            @AuthenticationPrincipal User user,
            @RequestBody(required = false) CreateChatSessionRequest request) {

        CreateChatSessionRequest req = request != null
                ? request
                : new CreateChatSessionRequest("New Chat", "General", null, null);

        return ResponseEntity.ok(sessionService.createSession(user, req));
    }

    @GetMapping("/sessions/{id}")
    public ResponseEntity<ChatSessionDetailResponse> getSessionDetail(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        return ResponseEntity.ok(sessionService.getSessionDetail(user, id));
    }

    @PostMapping(value = "/sessions/{id}/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamSessionMessage(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody @Valid ChatSessionMessageRequest request) {

        return sessionService.streamSessionMessage(user, id, request);
    }

    @GetMapping("/context-meta")
    public ResponseEntity<ChatContextMetaResponse> getContextMeta(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(sessionService.getContextMeta(user));
    }

    // Legacy support endpoints
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
