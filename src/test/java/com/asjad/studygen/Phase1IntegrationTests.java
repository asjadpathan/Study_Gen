package com.asjad.studygen;

import com.asjad.studygen.dto.RegisterRequest;
import com.asjad.studygen.dto.roadmap.CreateRoadmapRequest;
import com.asjad.studygen.dto.roadmap.ModuleRequest;
import com.asjad.studygen.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class Phase1IntegrationTests {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.asjad.studygen.service.RoadmapService roadmapService;

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
        userRepository.findByEmail("test@example.com").ifPresent(userRepository::delete);
        userRepository.findByEmail("test2@example.com").ifPresent(userRepository::delete);
    }

    @Test
    void testAuthAndProtectedRoadmapSlice1Flow() throws Exception {
        // 1. Register a new user
        RegisterRequest registerRequest = new RegisterRequest(
                "Test User",
                "test@example.com",
                "Password123"
        );

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andReturn();

        JsonNode registerJson = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        String token = registerJson.get("token").asText();
        assertThat(token).isNotBlank();

        // 2. Test invalid/expired token returns 401 Unauthorized (JwtAuthenticationEntryPoint verification)
        mockMvc.perform(get("/api/roadmaps")
                        .header("Authorization", "Bearer invalid-malformed-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));

        // 3. Test unauthenticated request returns 401 Unauthorized
        mockMvc.perform(get("/api/roadmaps"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));

        // 4. Test Slice 1: Manual Roadmap Creation with JWT
        CreateRoadmapRequest roadmapRequest = new CreateRoadmapRequest(
                "DevOps Mastery",
                "Cloud Engineer",
                List.of(
                        new ModuleRequest("Linux Fundamentals", "Basic OS commands", 0),
                        new ModuleRequest("Docker & Containers", "Containerization basics", 1),
                        new ModuleRequest("Kubernetes Orchestration", "Managing clusters", 2)
                )
        );

        MvcResult createRoadmapResult = mockMvc.perform(post("/api/roadmaps")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roadmapRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("DevOps Mastery"))
                .andExpect(jsonPath("$.modules.length()").value(3))
                .andExpect(jsonPath("$.modules[0].isLocked").value(false))
                .andExpect(jsonPath("$.modules[1].isLocked").value(true))
                .andExpect(jsonPath("$.modules[2].isLocked").value(true))
                .andReturn();

        JsonNode roadmapJson = objectMapper.readTree(createRoadmapResult.getResponse().getContentAsString());
        long roadmapId = roadmapJson.get("id").asLong();

        // 5. Query user roadmaps via GET /api/roadmaps
        mockMvc.perform(get("/api/roadmaps")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("DevOps Mastery"));

        // 6. Query specific roadmap via GET /api/roadmaps/{id}
        mockMvc.perform(get("/api/roadmaps/" + roadmapId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(roadmapId))
                .andExpect(jsonPath("$.modules[0].title").value("Linux Fundamentals"))
                .andExpect(jsonPath("$.modules[0].isLocked").value(false))
                .andExpect(jsonPath("$.modules[1].isLocked").value(true));
    }

    @Test
    void testAiRoadmapGenerationSlice2Flow() throws Exception {
        // Mock ChatClient on RoadmapService
        org.springframework.ai.chat.client.ChatClient mockChatClient = mock(org.springframework.ai.chat.client.ChatClient.class);
        org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec requestSpec = mock(org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec.class);
        org.springframework.ai.chat.client.ChatClient.CallResponseSpec responseSpec = mock(org.springframework.ai.chat.client.ChatClient.CallResponseSpec.class);

        when(mockChatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.entity(com.asjad.studygen.dto.ai.AiRoadmapSuggestion.class)).thenReturn(
                new com.asjad.studygen.dto.ai.AiRoadmapSuggestion(
                        "Docker Containers Mastery",
                        List.of(
                                new com.asjad.studygen.dto.ai.AiModuleSuggestion("Docker Basics", "Learn images and containers", 0),
                                new com.asjad.studygen.dto.ai.AiModuleSuggestion("Docker Compose", "Multi-container setups", 1)
                        )
                )
        );

        org.springframework.test.util.ReflectionTestUtils.setField(roadmapService, "chatClient", mockChatClient);

        // Register user for AI test
        RegisterRequest registerRequest = new RegisterRequest(
                "AI Learner",
                "ai.learner@example.com",
                "Secret123"
        );

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(registerResult.getResponse().getContentAsString()).get("token").asText();

        // Call Slice 2 endpoint POST /api/roadmaps/generate
        com.asjad.studygen.dto.ai.GenerateRoadmapRequest aiRequest = new com.asjad.studygen.dto.ai.GenerateRoadmapRequest(
                "Docker Containers",
                "DevOps Engineer",
                "Basic Linux command line",
                5,
                "Hands-on practical"
        );

        MvcResult aiResult = mockMvc.perform(post("/api/roadmaps/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(aiRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Docker Containers Mastery"))
                .andExpect(jsonPath("$.targetRole").value("DevOps Engineer"))
                .andExpect(jsonPath("$.modules.length()").value(2))
                .andExpect(jsonPath("$.modules[0].title").value("Docker Basics"))
                .andExpect(jsonPath("$.modules[0].isLocked").value(false))
                .andExpect(jsonPath("$.modules[1].title").value("Docker Compose"))
                .andExpect(jsonPath("$.modules[1].isLocked").value(true))
                .andReturn();

        JsonNode responseJson = objectMapper.readTree(aiResult.getResponse().getContentAsString());
        long generatedRoadmapId = responseJson.get("id").asLong();

        // Verify persisted to MySQL database and readable via GET /api/roadmaps/{id}
        mockMvc.perform(get("/api/roadmaps/" + generatedRoadmapId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(generatedRoadmapId))
                .andExpect(jsonPath("$.title").value("Docker Containers Mastery"))
                .andExpect(jsonPath("$.modules.length()").value(2))
                .andExpect(jsonPath("$.modules[0].title").value("Docker Basics"))
                .andExpect(jsonPath("$.modules[0].isLocked").value(false))
                .andExpect(jsonPath("$.modules[1].title").value("Docker Compose"))
                .andExpect(jsonPath("$.modules[1].isLocked").value(true));
    }
}
