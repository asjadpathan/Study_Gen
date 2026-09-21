package com.asjad.studygen;

import com.asjad.studygen.dto.RegisterRequest;
import com.asjad.studygen.entity.Module;
import com.asjad.studygen.entity.Roadmap;
import com.asjad.studygen.entity.User;
import com.asjad.studygen.entity.UserActivityLog;
import com.asjad.studygen.repository.*;
import com.asjad.studygen.service.UserActivityService;
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

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class Phase5IntegrationTests {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoadmapRepository roadmapRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private UserActivityLogRepository activityLogRepository;

    @Autowired
    private UserActivityService userActivityService;

    @BeforeAll
    static void initEnv() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        dotenv.entries().forEach(entry -> {
            if (System.getProperty(entry.getKey()) == null) {
                System.setProperty(entry.getKey(), entry.getValue());
            }
        });
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    private String getAuthToken(String email, String password) throws Exception {
        RegisterRequest registerReq = new RegisterRequest("Test Analyst", email, password);
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andReturn();

        if (result.getResponse().getStatus() == 201) {
            JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
            return root.get("token").asText();
        }

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return root.get("token").asText();
    }

    @Test
    void testPomodoroLogging_HeatmapAndProfileAnalytics() throws Exception {
        String token = getAuthToken("analytics_user@test.com", "Password123!");
        User user = userRepository.findByEmail("analytics_user@test.com").orElseThrow();

        // 1. Create a roadmap & module for context
        Roadmap roadmap = new Roadmap();
        roadmap.setUser(user);
        roadmap.setTitle("Full Stack Web Dev");
        roadmap.setTargetRole("Full Stack Engineer");
        roadmap = roadmapRepository.save(roadmap);

        Module module = new Module();
        module.setRoadmap(roadmap);
        module.setTitle("React Fundamentals");
        module.setDescription("Components, Props, and State");
        module.setSequenceOrder(1);
        module.setLocked(false);
        module.setCompleted(true);
        module.setMasteryScore(95.0);
        module = moduleRepository.save(module);

        // 2. Log Pomodoro Session via API
        String pomodoroPayload = """
                {
                    "durationMinutes": 25,
                    "moduleId": %d
                }
                """.formatted(module.getId());

        mockMvc.perform(post("/api/productivity/pomodoro/log")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pomodoroPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.durationMinutes").value(25))
                .andExpect(jsonPath("$.moduleTitle").value("React Fundamentals"))
                .andExpect(jsonPath("$.userTotalStudyMinutes").value(25))
                .andExpect(jsonPath("$.currentStreak").value(1));

        // 3. Log a second pomodoro session
        String secondSessionPayload = """
                {
                    "durationMinutes": 50,
                    "moduleId": %d
                }
                """.formatted(module.getId());

        mockMvc.perform(post("/api/productivity/pomodoro/log")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondSessionPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.durationMinutes").value(50))
                .andExpect(jsonPath("$.userTotalStudyMinutes").value(75));

        // 4. Verify Pomodoro Sessions List
        mockMvc.perform(get("/api/productivity/pomodoro/sessions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // 5. Test Streak Engine across multiple simulated days
        user = userRepository.findById(user.getId()).orElseThrow();
        // Insert activity for yesterday to verify multi-day streak calculation
        UserActivityLog yesterdayLog = UserActivityLog.builder()
                .user(user)
                .activityType("MODULE_COMPLETED")
                .minutesLogged(30)
                .loggedDate(LocalDate.now().minusDays(1))
                .build();
        activityLogRepository.save(yesterdayLog);

        int updatedStreak = userActivityService.updateUserStreak(user);
        assertThat(updatedStreak).isGreaterThanOrEqualTo(2);

        // 6. Test Streak Heatmap Endpoint
        mockMvc.perform(get("/api/analytics/streak-heatmap?days=30")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStreak").value(updatedStreak))
                .andExpect(jsonPath("$.totalActiveDays").value(2))
                .andExpect(jsonPath("$.heatmap").isArray());

        // 7. Test Profile Summary Endpoint
        mockMvc.perform(get("/api/analytics/profile-summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("analytics_user@test.com"))
                .andExpect(jsonPath("$.totalStudyMinutes").value(75))
                .andExpect(jsonPath("$.totalRoadmaps").value(1))
                .andExpect(jsonPath("$.completedModulesCount").value(1))
                .andExpect(jsonPath("$.badges").isArray());
    }
}
