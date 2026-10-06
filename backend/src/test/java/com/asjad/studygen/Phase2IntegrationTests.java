package com.asjad.studygen;

import com.asjad.studygen.dto.RegisterRequest;
import com.asjad.studygen.dto.ai.AiQuizQuestion;
import com.asjad.studygen.dto.ai.AiQuizSuggestion;
import com.asjad.studygen.dto.ai.AiRemediationSuggestion;
import com.asjad.studygen.dto.ai.AiRoadmapSuggestion;
import com.asjad.studygen.dto.ai.AiModuleSuggestion;
import com.asjad.studygen.dto.assessment.DiagnosticQuizRequest;
import com.asjad.studygen.dto.assessment.SubmitAttemptRequest;
import com.asjad.studygen.dto.roadmap.CreateRoadmapRequest;
import com.asjad.studygen.dto.roadmap.ModuleRequest;
import com.asjad.studygen.entity.Module;
import com.asjad.studygen.repository.*;
import com.asjad.studygen.service.AssessmentService;
import com.asjad.studygen.service.RemediationService;
import com.asjad.studygen.service.RoadmapService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
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
class Phase2IntegrationTests {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private AssessmentService assessmentService;

    @Autowired
    private RemediationService remediationService;

    @Autowired
    private RoadmapService roadmapService;

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
        userRepository.findByEmail("adaptive@example.com").ifPresent(userRepository::delete);
        userRepository.findByEmail("brancher@example.com").ifPresent(userRepository::delete);
        userRepository.findByEmail("pipeline@example.com").ifPresent(userRepository::delete);
    }

    @Test
    void testDiagnosticQuizModuleAssessmentGradingAndProgression() throws Exception {
        // 1. Register user
        RegisterRequest registerRequest = new RegisterRequest("Adaptive Learner", "adaptive@example.com", "Password123");
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(registerResult.getResponse().getContentAsString()).get("token").asText();

        // 2. Mock ChatClient for AssessmentService
        org.springframework.ai.chat.client.ChatClient mockChatClient = mock(org.springframework.ai.chat.client.ChatClient.class);
        org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec requestSpec = mock(org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec.class);
        org.springframework.ai.chat.client.ChatClient.CallResponseSpec responseSpec = mock(org.springframework.ai.chat.client.ChatClient.CallResponseSpec.class);

        when(mockChatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);

        AiQuizSuggestion mockQuiz = new AiQuizSuggestion(
                "Kubernetes Basics Quiz",
                List.of(
                        new AiQuizQuestion("What is a Pod?", List.of("Smallest deployable unit", "A cluster", "A disk", "A network"), 0, "Pods encapsulate containers."),
                        new AiQuizQuestion("What manages Pod replicas?", List.of("Volume", "Deployment", "Ingress", "ConfigMap"), 1, "Deployments manage replica sets.")
                )
        );

        when(responseSpec.entity(AiQuizSuggestion.class)).thenReturn(mockQuiz);
        ReflectionTestUtils.setField(assessmentService, "chatClient", mockChatClient);

        // 3. Test Diagnostic Quiz Generation
        DiagnosticQuizRequest diagnosticReq = new DiagnosticQuizRequest("Kubernetes", "Docker basics");
        mockMvc.perform(post("/api/assessments/diagnostic/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(diagnosticReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("DIAGNOSTIC"))
                .andExpect(jsonPath("$.questions.length()").value(2))
                .andReturn();

        // 4. Create Roadmap with 2 modules
        CreateRoadmapRequest roadmapRequest = new CreateRoadmapRequest(
                "K8s Mastery", "Cloud Architect",
                List.of(
                        new ModuleRequest("Pod Architecture", "Module 0 description", 0),
                        new ModuleRequest("Replica Sets & Services", "Module 1 description", 1)
                )
        );
        MvcResult roadmapResult = mockMvc.perform(post("/api/roadmaps")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roadmapRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode roadmapNode = objectMapper.readTree(roadmapResult.getResponse().getContentAsString());
        long mod0Id = roadmapNode.get("modules").get(0).get("id").asLong();
        long mod1Id = roadmapNode.get("modules").get(1).get("id").asLong();

        // Confirm mod0 is unlocked, mod1 is locked
        assertThat(roadmapNode.get("modules").get(0).get("isLocked").asBoolean()).isFalse();
        assertThat(roadmapNode.get("modules").get(1).get("isLocked").asBoolean()).isTrue();

        // 5. Generate Module Assessment for mod0
        MvcResult moduleQuizResult = mockMvc.perform(post("/api/assessments/modules/" + mod0Id + "/generate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("MODULE_GATE"))
                .andExpect(jsonPath("$.moduleId").value(mod0Id))
                .andExpect(jsonPath("$.questions.length()").value(2))
                .andReturn();

        long assessmentId = objectMapper.readTree(moduleQuizResult.getResponse().getContentAsString()).get("id").asLong();

        // 6. Test Failed Attempt (score 50% < 80% passing threshold)
        // User submits [0, 0] -> Question 1 is correct (0), Question 2 is wrong (correct is 1)
        SubmitAttemptRequest failAttemptReq = new SubmitAttemptRequest(List.of(0, 0));
        MvcResult failResult = mockMvc.perform(post("/api/assessments/" + assessmentId + "/submit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(failAttemptReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(50))
                .andExpect(jsonPath("$.passed").value(false))
                .andExpect(jsonPath("$.nextModuleUnlocked").value(false))
                .andReturn();

        long failAttemptId = objectMapper.readTree(failResult.getResponse().getContentAsString()).get("attemptId").asLong();

        // Verify mod1 is STILL locked in DB
        var mod1Check = moduleRepository.findById(mod1Id).orElseThrow();
        assertThat(mod1Check.isLocked()).isTrue();

        // 7. Test AI Remediation Loop for Failed Attempt
        AiRemediationSuggestion mockRemediation = new AiRemediationSuggestion(
                "Confusion between Pods and ReplicaSets",
                "Think of a Pod as a single worker, while a Deployment is the manager ensuring the right number of workers are present.",
                List.of(new AiQuizQuestion("Who manages replica count?", List.of("Deployment", "Pod"), 0, "Deployment is the manager."))
        );
        when(responseSpec.entity(AiRemediationSuggestion.class)).thenReturn(mockRemediation);
        ReflectionTestUtils.setField(remediationService, "chatClient", mockChatClient);

        mockMvc.perform(get("/api/assessments/attempts/" + failAttemptId + "/remediation")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalScore").value(50))
                .andExpect(jsonPath("$.diagnosis").isNotEmpty())
                .andExpect(jsonPath("$.simplifiedExplanation").isNotEmpty())
                .andExpect(jsonPath("$.retestQuestions.length()").value(1));

        // 8. Test Passing Attempt (score 100% >= 80% passing threshold)
        // User submits [0, 1] -> Both questions correct
        SubmitAttemptRequest passAttemptReq = new SubmitAttemptRequest(List.of(0, 1));
        mockMvc.perform(post("/api/assessments/" + assessmentId + "/submit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(passAttemptReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(100))
                .andExpect(jsonPath("$.passed").value(true))
                .andExpect(jsonPath("$.nextModuleUnlocked").value(true));

        // Verify mod0 is completed and mod1 is now UNLOCKED in DB
        var mod0Updated = moduleRepository.findById(mod0Id).orElseThrow();
        var mod1Updated = moduleRepository.findById(mod1Id).orElseThrow();
        assertThat(mod0Updated.isCompleted()).isTrue();
        assertThat(mod0Updated.getMasteryScore()).isEqualTo(100.0);
        assertThat(mod1Updated.isLocked()).isFalse();
    }

    @Test
    void testDynamicRoadmapRecalculation() throws Exception {
        // Register user
        RegisterRequest registerRequest = new RegisterRequest("Roadmap Brancher", "brancher@example.com", "Password123");
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(registerResult.getResponse().getContentAsString()).get("token").asText();

        // Create an initial roadmap with 2 modules
        CreateRoadmapRequest roadmapRequest = new CreateRoadmapRequest(
                "Initial Path", "Junior Developer",
                List.of(
                        new ModuleRequest("Basics 101", "Intro", 0),
                        new ModuleRequest("Advanced 201", "Old second module", 1)
                )
        );

        MvcResult createResult = mockMvc.perform(post("/api/roadmaps")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roadmapRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        long roadmapId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        // Mark module 0 as completed in DB
        var mod0 = moduleRepository.findByRoadmapIdAndSequenceOrder(roadmapId, 0).orElseThrow();
        mod0.setCompleted(true);
        mod0.setMasteryScore(90.0);
        moduleRepository.save(mod0);

        // Mock ChatClient for RoadmapService recalculation
        org.springframework.ai.chat.client.ChatClient mockChatClient = mock(org.springframework.ai.chat.client.ChatClient.class);
        org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec requestSpec = mock(org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec.class);
        org.springframework.ai.chat.client.ChatClient.CallResponseSpec responseSpec = mock(org.springframework.ai.chat.client.ChatClient.CallResponseSpec.class);

        when(mockChatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);

        AiRoadmapSuggestion updatedSuggestion = new AiRoadmapSuggestion(
                "Accelerated Path",
                List.of(
                        new AiModuleSuggestion("Microservices Architecture", "New second module", 1),
                        new AiModuleSuggestion("Cloud Native Systems", "New third module", 2)
                )
        );
        when(responseSpec.entity(AiRoadmapSuggestion.class)).thenReturn(updatedSuggestion);
        ReflectionTestUtils.setField(roadmapService, "chatClient", mockChatClient);

        // Call POST /api/roadmaps/{id}/recalculate
        com.asjad.studygen.dto.ai.GenerateRoadmapRequest recalcReq = new com.asjad.studygen.dto.ai.GenerateRoadmapRequest(
                "Microservices", "Senior Architect", "Completed Basics 101", 10, "Fast-paced"
        );

        mockMvc.perform(post("/api/roadmaps/" + roadmapId + "/recalculate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(recalcReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Accelerated Path"))
                .andExpect(jsonPath("$.modules.length()").value(3))
                .andExpect(jsonPath("$.modules[0].title").value("Basics 101"))
                .andExpect(jsonPath("$.modules[0].isCompleted").value(true))
                .andExpect(jsonPath("$.modules[1].title").value("Microservices Architecture"))
                .andExpect(jsonPath("$.modules[1].isLocked").value(false))
                .andExpect(jsonPath("$.modules[2].title").value("Cloud Native Systems"))
                .andExpect(jsonPath("$.modules[2].isLocked").value(true))
                .andReturn();

        // Verify in DB that old "Advanced 201" module was removed and replaced
        List<Module> dbModules = moduleRepository.findByRoadmapIdOrderBySequenceOrderAsc(roadmapId);
        assertThat(dbModules).hasSize(3);
        assertThat(dbModules.get(0).getTitle()).isEqualTo("Basics 101");
        assertThat(dbModules.get(1).getTitle()).isEqualTo("Microservices Architecture");
        assertThat(dbModules.get(2).getTitle()).isEqualTo("Cloud Native Systems");
    }

    @Test
    void testDiagnosticPipelineAndConceptHierarchy() throws Exception {
        // Register user
        RegisterRequest registerRequest = new RegisterRequest("Pipeline Student", "pipeline@example.com", "Password123");
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(registerResult.getResponse().getContentAsString()).get("token").asText();

        // Mock chat client returning modules with concepts
        var mockClient = mock(org.springframework.ai.chat.client.ChatClient.class);
        var reqSpec = mock(org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec.class);
        var respSpec = mock(org.springframework.ai.chat.client.ChatClient.CallResponseSpec.class);
        when(mockClient.prompt()).thenReturn(reqSpec);
        when(reqSpec.user(anyString())).thenReturn(reqSpec);
        when(reqSpec.call()).thenReturn(respSpec);

        AiRoadmapSuggestion suggestion = new AiRoadmapSuggestion(
                "Java Backend",
                List.of(
                        new AiModuleSuggestion(
                                "Week 1: Java Fundamentals",
                                "Foundations",
                                0,
                                List.of(
                                        new com.asjad.studygen.dto.ai.AiConceptSuggestion("Java Primitive Data Types"),
                                        new com.asjad.studygen.dto.ai.AiConceptSuggestion("Variables & Scope")
                                )
                        )
                )
        );
        when(respSpec.entity(AiRoadmapSuggestion.class)).thenReturn(suggestion);
        ReflectionTestUtils.setField(roadmapService, "chatClient", mockClient);

        // 1. Generate roadmap with diagnostic calibrated parameters
        com.asjad.studygen.dto.ai.GenerateRoadmapRequest request = new com.asjad.studygen.dto.ai.GenerateRoadmapRequest(
                "Java Backend", "Software Developer", "INTERMEDIATE", 10, "PROJECT_BASED",
                null, null, null, 80, "Solid fundamentals in memory and variables"
        );

        MvcResult result = mockMvc.perform(post("/api/roadmaps/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.modules").isArray())
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(result.getResponse().getContentAsString());
        long roadmapId = responseNode.get("id").asLong();
        int totalConcepts = responseNode.get("totalConcepts").asInt();
        assertThat(totalConcepts).isGreaterThan(0);

        JsonNode firstModule = responseNode.get("modules").get(0);
        assertThat(firstModule.get("concepts").size()).isGreaterThan(0);

        long firstConceptId = firstModule.get("concepts").get(0).get("id").asLong();
        boolean initialCompleted = firstModule.get("concepts").get(0).get("isCompleted").asBoolean();
        assertThat(initialCompleted).isFalse();

        // 2. Toggle concept completion
        mockMvc.perform(post("/api/concepts/" + firstConceptId + "/toggle-complete")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isCompleted").value(true));

        // 3. Verify roadmap metrics updated
        mockMvc.perform(get("/api/roadmaps/" + roadmapId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedConcepts").value(1));
    }
}
