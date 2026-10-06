package com.asjad.studygen;

import com.asjad.studygen.dto.RegisterRequest;
import com.asjad.studygen.dto.document.ChatMessageRequest;
import com.asjad.studygen.dto.roadmap.CreateRoadmapRequest;
import com.asjad.studygen.dto.roadmap.ModuleRequest;
import com.asjad.studygen.entity.Concept;
import com.asjad.studygen.entity.Module;
import com.asjad.studygen.repository.*;
import com.asjad.studygen.service.StudyAssistantService;
import com.asjad.studygen.service.StudyGuideService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class Phase4IntegrationTests {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private ConceptRepository conceptRepository;

    @Autowired
    private StudyAssistantService chatService;

    @Autowired
    private StudyGuideService studyGuideService;

    @BeforeAll
    static void setupEnv() {
        Dotenv.configure().ignoreIfMissing().systemProperties().load();
    }

    @BeforeEach
    void cleanDb() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        userRepository.findByEmail("rag@example.com").ifPresent(userRepository::delete);
    }

    @Test
    void testDocumentUploadRAGChatAndStudyGuide() throws Exception {
        // 1. Register User
        RegisterRequest registerRequest = new RegisterRequest("RAG Researcher", "rag@example.com", "Password123");
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(registerResult.getResponse().getContentAsString()).get("token").asText();

        // 2. Upload Document
        String samplePdfText = "Distributed Consensus in Raft: Raft is a consensus algorithm designed to be easy to understand. It achieves consensus via leader election, log replication, and safety.";
        MockMultipartFile sampleFile = new MockMultipartFile(
                "file",
                "raft-paper-summary.txt",
                "text/plain",
                samplePdfText.getBytes()
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/documents/upload")
                        .file(sampleFile)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.filename").value("raft-paper-summary.txt"))
                .andExpect(jsonPath("$.totalChunks").isNumber())
                .andReturn();

        long docId = objectMapper.readTree(uploadResult.getResponse().getContentAsString()).get("id").asLong();

        // Verify document is listed
        mockMvc.perform(get("/api/documents")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(docId));

        // 3. Create Roadmap & Module
        CreateRoadmapRequest roadmapRequest = new CreateRoadmapRequest(
                "Distributed Systems", "Principal Engineer",
                List.of(new ModuleRequest("Consensus Protocols", "Raft and Paxos", 0))
        );
        MvcResult rmResult = mockMvc.perform(post("/api/roadmaps")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roadmapRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        long modId = objectMapper.readTree(rmResult.getResponse().getContentAsString()).get("modules").get(0).get("id").asLong();
        Module module = moduleRepository.findById(modId).orElseThrow();

        Concept concept = new Concept(module, "Leader Election", "Raft uses randomized election timeouts to ensure fast leader election without split votes.");
        conceptRepository.save(concept);

        // 4. Mock ChatClient for AI responses
        org.springframework.ai.chat.client.ChatClient mockChatClient = mock(org.springframework.ai.chat.client.ChatClient.class);
        org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec requestSpec = mock(org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec.class);
        org.springframework.ai.chat.client.ChatClient.CallResponseSpec responseSpec = mock(org.springframework.ai.chat.client.ChatClient.CallResponseSpec.class);

        when(mockChatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("Raft ensures consensus by electing a single leader who oversees all log entries.");

        ReflectionTestUtils.setField(chatService, "chatClient", mockChatClient);
        ReflectionTestUtils.setField(studyGuideService, "chatClient", mockChatClient);

        // 5. Test RAG Study Assistant Chat
        ChatMessageRequest chatReq = new ChatMessageRequest(modId, "How does leader election work in Raft?");
        mockMvc.perform(post("/api/chat/message")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sender").value("ASSISTANT"))
                .andExpect(jsonPath("$.messageText").isNotEmpty());

        // Verify history contains both USER and ASSISTANT messages
        mockMvc.perform(get("/api/chat/history?moduleId=" + modId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].sender").value("USER"))
                .andExpect(jsonPath("$[1].sender").value("ASSISTANT"));

        // 6. Test Study Guide synthesis
        when(responseSpec.content()).thenReturn("# Consensus Protocols — In-Depth Study Guide\n\n## Core Concepts\nRaft is an intuitive consensus model.");

        mockMvc.perform(get("/api/study-guide/modules/" + modId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moduleId").value(modId))
                .andExpect(jsonPath("$.moduleTitle").value("Consensus Protocols"))
                .andExpect(jsonPath("$.markdownContent").isNotEmpty())
                .andExpect(jsonPath("$.keyTakeaways").isArray());
    }
}
