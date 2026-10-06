package com.asjad.studygen;

import com.asjad.studygen.dto.RegisterRequest;
import com.asjad.studygen.dto.practice.ConceptReviewRequest;
import com.asjad.studygen.dto.roadmap.CreateRoadmapRequest;
import com.asjad.studygen.dto.roadmap.ModuleRequest;
import com.asjad.studygen.entity.Concept;
import com.asjad.studygen.entity.Module;
import com.asjad.studygen.repository.*;
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

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class Phase3IntegrationTests {

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
    private ConceptRepository conceptRepository;

    @Autowired
    private UserConceptReviewRepository reviewRepository;

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
        reviewRepository.deleteAll();
        conceptRepository.deleteAll();
        roadmapRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testRoadmapGraphAndMindMapEndpoints() throws Exception {
        // Register user
        RegisterRequest registerRequest = new RegisterRequest("Graph Explorer", "graph@example.com", "Password123");
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(registerResult.getResponse().getContentAsString()).get("token").asText();

        // Create Roadmap with 3 modules
        CreateRoadmapRequest roadmapRequest = new CreateRoadmapRequest(
                "FullStack Architecture", "Lead Architect",
                List.of(
                        new ModuleRequest("Frontend Layer", "UI and React", 0),
                        new ModuleRequest("Backend Layer", "APIs and Spring Boot", 1),
                        new ModuleRequest("Database Layer", "MySQL and Persistence", 2)
                )
        );

        MvcResult roadmapResult = mockMvc.perform(post("/api/roadmaps")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roadmapRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode roadmapJson = objectMapper.readTree(roadmapResult.getResponse().getContentAsString());
        long roadmapId = roadmapJson.get("id").asLong();
        long mod0Id = roadmapJson.get("modules").get(0).get("id").asLong();

        // 1. Verify D3.js DAG graph endpoint
        mockMvc.perform(get("/api/visual/roadmaps/" + roadmapId + "/graph")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roadmapId").value(roadmapId))
                .andExpect(jsonPath("$.title").value("FullStack Architecture"))
                .andExpect(jsonPath("$.nodes.length()").value(3))
                .andExpect(jsonPath("$.nodes[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.nodes[1].status").value("LOCKED"))
                .andExpect(jsonPath("$.nodes[2].status").value("LOCKED"))
                .andExpect(jsonPath("$.links.length()").value(2))
                .andExpect(jsonPath("$.links[0].type").value("PREREQUISITE"));

        // 2. Add concept to module 0
        Module mod0 = moduleRepository.findById(mod0Id).orElseThrow();
        Concept concept = new Concept(mod0, "Virtual DOM Reconciliation", "Virtual DOM diffing algorithm explains high UI render speed.");
        concept.setSimplifiedRemediationBody("Like reviewing only the edited lines in a document rather than retyping the entire book.");
        conceptRepository.save(concept);

        // 3. Verify Mind Map endpoint
        mockMvc.perform(get("/api/visual/modules/" + mod0Id + "/mindmap")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("Frontend Layer"))
                .andExpect(jsonPath("$.children.length()").value(1))
                .andExpect(jsonPath("$.children[0].label").value("Virtual DOM Reconciliation"));
    }

    @Test
    void testSuperMemo2SpacedRepetitionAndPracticeDrills() throws Exception {
        // Register user
        RegisterRequest registerRequest = new RegisterRequest("SRS Student", "srs@example.com", "Password123");
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(registerResult.getResponse().getContentAsString()).get("token").asText();

        // Create roadmap with module and concept
        CreateRoadmapRequest roadmapRequest = new CreateRoadmapRequest(
                "Algorithms", "Computer Scientist",
                List.of(new ModuleRequest("Dynamic Programming", "Memoization & Tabulation", 0))
        );
        MvcResult roadmapResult = mockMvc.perform(post("/api/roadmaps")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roadmapRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        long modId = objectMapper.readTree(roadmapResult.getResponse().getContentAsString()).get("modules").get(0).get("id").asLong();
        Module module = moduleRepository.findById(modId).orElseThrow();

        Concept concept = new Concept(module, "Optimal Substructure", "A problem has optimal substructure if an optimal solution can be constructed from optimal solutions of its subproblems.");
        concept.setSimplifiedRemediationBody("Finding the shortest path to Paris via Lyon means taking the shortest path to Lyon first.");
        Concept savedConcept = conceptRepository.save(concept);

        // 1. Get Practice Drills (flashcards)
        mockMvc.perform(get("/api/practice/modules/" + modId + "/drills")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moduleId").value(modId))
                .andExpect(jsonPath("$.flashcards.length()").value(1))
                .andExpect(jsonPath("$.flashcards[0].front").value("Optimal Substructure"));

        // 2. First Review (Quality 5 - perfect recall)
        ConceptReviewRequest reviewReq1 = new ConceptReviewRequest(savedConcept.getId(), 5);
        mockMvc.perform(post("/api/practice/review")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewReq1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repetitionNumber").value(1))
                .andExpect(jsonPath("$.intervalDays").value(1))
                .andExpect(jsonPath("$.easinessFactor").value(2.6));

        // 3. Second Review (Quality 4 - good recall) -> interval advances to 6 days
        ConceptReviewRequest reviewReq2 = new ConceptReviewRequest(savedConcept.getId(), 4);
        mockMvc.perform(post("/api/practice/review")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewReq2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repetitionNumber").value(2))
                .andExpect(jsonPath("$.intervalDays").value(6));

        // 4. Third Review (Quality 1 - blackout/failure) -> resets repetition to 0 and interval to 1
        ConceptReviewRequest reviewReq3 = new ConceptReviewRequest(savedConcept.getId(), 1);
        mockMvc.perform(post("/api/practice/review")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewReq3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repetitionNumber").value(0))
                .andExpect(jsonPath("$.intervalDays").value(1));
    }
}
