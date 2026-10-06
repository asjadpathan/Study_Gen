package com.asjad.studygen.service;

import com.asjad.studygen.dto.document.ChatMessageRequest;
import com.asjad.studygen.dto.document.ChatMessageResponse;
import com.asjad.studygen.entity.Concept;
import com.asjad.studygen.entity.StudyChatMessage;
import com.asjad.studygen.entity.User;
import com.asjad.studygen.repository.ConceptRepository;
import com.asjad.studygen.repository.ModuleRepository;
import com.asjad.studygen.repository.StudyChatMessageRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudyAssistantService {

    private final StudyChatMessageRepository chatRepository;
    private final ModuleRepository moduleRepository;
    private final ConceptRepository conceptRepository;
    private final DocumentProcessingService documentService;
    private final ChatClient.Builder chatClientBuilder;

    private ChatClient chatClient;

    @PostConstruct
    void init() {
        this.chatClient = chatClientBuilder.build();
    }

    @Transactional
    public ChatMessageResponse sendMessage(User user, ChatMessageRequest request) {
        // Save User Message
        StudyChatMessage userMsg = new StudyChatMessage(user, request.moduleId(), "USER", request.message());
        chatRepository.save(userMsg);

        // Gather context
        StringBuilder context = new StringBuilder();
        if (request.moduleId() != null) {
            moduleRepository.findById(request.moduleId()).ifPresent(m -> {
                context.append("Current Module: ").append(m.getTitle()).append("\n");
                context.append("Module Description: ").append(m.getDescription()).append("\n");
                List<Concept> concepts = conceptRepository.findByModuleId(m.getId());
                for (Concept c : concepts) {
                    context.append("Concept: ").append(c.getTitle()).append(": ").append(c.getContentBody()).append("\n");
                }
            });
        }

        // Retrieve relevant document chunks (RAG)
        List<String> docChunks = documentService.searchRelevantChunks(user.getId(), request.message(), 2);
        if (!docChunks.isEmpty()) {
            context.append("Uploaded Reference Context:\n");
            for (String chunk : docChunks) {
                context.append("- ").append(chunk).append("\n");
            }
        }

        String prompt = """
                You are StudyGen AI Tutor, an adaptive, encouraging, and brilliant computer science & engineering tutor.
                Answer the student's question clearly with helpful analogies, practical examples, or code snippets when helpful.
                Ground your answer in the provided context if applicable.

                Context:
                %s

                Student Question:
                %s
                """.formatted(context.toString(), request.message());

        String aiReply = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        if (aiReply == null || aiReply.isBlank()) {
            aiReply = "Here is an explanation tailored to your study module and background.";
        }

        // Save Assistant Message
        StudyChatMessage assistantMsg = new StudyChatMessage(user, request.moduleId(), "ASSISTANT", aiReply);
        StudyChatMessage savedAssistant = chatRepository.save(assistantMsg);

        return new ChatMessageResponse(
                savedAssistant.getId(),
                savedAssistant.getModuleId(),
                savedAssistant.getSender(),
                savedAssistant.getMessageText(),
                savedAssistant.getCreatedAt().toString()
        );
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getChatHistory(User user, Long moduleId) {
        List<StudyChatMessage> messages;
        if (moduleId != null) {
            messages = chatRepository.findByUserIdAndModuleIdOrderByCreatedAtAsc(user.getId(), moduleId);
        } else {
            messages = chatRepository.findByUserIdOrderByCreatedAtAsc(user.getId());
        }

        return messages.stream()
                .map(m -> new ChatMessageResponse(
                        m.getId(),
                        m.getModuleId(),
                        m.getSender(),
                        m.getMessageText(),
                        m.getCreatedAt().toString()
                )).collect(Collectors.toList());
    }
}
