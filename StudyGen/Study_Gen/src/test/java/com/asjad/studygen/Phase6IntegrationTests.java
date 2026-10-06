package com.asjad.studygen;

import com.asjad.studygen.dto.RegisterRequest;
import com.asjad.studygen.entity.Module;
import com.asjad.studygen.entity.Roadmap;
import com.asjad.studygen.entity.User;
import com.asjad.studygen.repository.ModuleRepository;
import com.asjad.studygen.repository.RoadmapRepository;
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

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class Phase6IntegrationTests {

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
        RegisterRequest registerReq = new RegisterRequest("Career Candidate", email, password);
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
    void testJobSkillGapMatchingAndRecommendations() throws Exception {
        String token = getAuthToken("career_test@example.com", "Password123!");
        User user = userRepository.findByEmail("career_test@example.com").orElseThrow();

        // 1. Setup completed modules for the user: "React" and "Spring Boot"
        Roadmap roadmap = new Roadmap();
        roadmap.setUser(user);
        roadmap.setTitle("Full Stack Mastery");
        roadmap = roadmapRepository.save(roadmap);

        Module m1 = new Module();
        m1.setRoadmap(roadmap);
        m1.setTitle("React");
        m1.setDescription("React Hooks, State Management, and JSX");
        m1.setSequenceOrder(1);
        m1.setLocked(false);
        m1.setCompleted(true);
        m1.setMasteryScore(90.0);
        moduleRepository.save(m1);

        Module m2 = new Module();
        m2.setRoadmap(roadmap);
        m2.setTitle("Spring Boot");
        m2.setDescription("REST APIs, JPA, and Security");
        m2.setSequenceOrder(2);
        m2.setLocked(false);
        m2.setCompleted(true);
        m2.setMasteryScore(88.0);
        moduleRepository.save(m2);

        // 2. Perform Job Match with 4 required skills: React, Spring Boot, Docker, Kubernetes
        // User has React & Spring Boot, so 2 out of 4 = 50% match
        String matchPayload = """
                {
                    "jobTitle": "Full Stack Cloud Engineer",
                    "company": "Apex Technologies",
                    "jobDescription": "Build modern cloud-native systems with React, Spring Boot, Docker, and Kubernetes.",
                    "explicitSkills": ["React", "Spring Boot", "Docker", "Kubernetes"]
                }
                """;

        mockMvc.perform(post("/api/career/job-match")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(matchPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobTitle").value("Full Stack Cloud Engineer"))
                .andExpect(jsonPath("$.company").value("Apex Technologies"))
                .andExpect(jsonPath("$.matchPercentage").value(50))
                .andExpect(jsonPath("$.matchedSkills").isArray())
                .andExpect(jsonPath("$.missingSkills").isArray())
                .andExpect(jsonPath("$.actionableAdvice").isNotEmpty());

        // 3. Test Career Recommendations Endpoint
        mockMvc.perform(get("/api/career/recommendations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEvaluated").isNumber())
                .andExpect(jsonPath("$.matches").isArray());

        // 4. Test Ecosystem Discovery Endpoint
        mockMvc.perform(get("/api/ecosystem/discovery?topic=Full%20Stack")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queryTopic").value("Full Stack"))
                .andExpect(jsonPath("$.hackathons").isArray())
                .andExpect(jsonPath("$.openSourceProjects").isArray())
                .andExpect(jsonPath("$.researchPapers").isArray());
    }
}
