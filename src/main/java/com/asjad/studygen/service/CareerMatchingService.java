package com.asjad.studygen.service;

import com.asjad.studygen.entity.JobPosting;
import com.asjad.studygen.entity.Module;
import com.asjad.studygen.entity.User;
import com.asjad.studygen.repository.JobPostingRepository;
import com.asjad.studygen.repository.ModuleRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CareerMatchingService {

    private final JobPostingRepository jobPostingRepository;
    private final ModuleRepository moduleRepository;
    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public record JobMatchRequest(
            String jobTitle,
            String company,
            String jobDescription,
            List<String> explicitSkills
    ) {}

    public record JobMatchResponse(
            Long jobPostingId,
            String jobTitle,
            String company,
            int matchPercentage,
            List<String> matchedSkills,
            List<String> missingSkills,
            String actionableAdvice
    ) {}

    public record CareerRecommendationsResponse(
            int totalEvaluated,
            List<JobMatchResponse> matches
    ) {}

    @Transactional
    public JobMatchResponse matchJob(User user, JobMatchRequest request) {
        List<String> requiredSkills = extractRequiredSkills(request);

        // Get user mastered skills from completed modules
        List<Module> completedModules = moduleRepository.findByRoadmapUserIdAndCompletedTrue(user.getId());
        Set<String> masteredSkills = new HashSet<>();
        for (Module m : completedModules) {
            masteredSkills.add(m.getTitle().toLowerCase().trim());
            // Extract keywords from module description
            if (m.getDescription() != null) {
                for (String word : m.getDescription().split("[,;\\s]+")) {
                    if (word.length() > 2) {
                        masteredSkills.add(word.toLowerCase().trim());
                    }
                }
            }
        }

        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        for (String skill : requiredSkills) {
            String skillLower = skill.toLowerCase().trim();
            boolean isMatched = masteredSkills.stream().anyMatch(ms ->
                    ms.contains(skillLower) || skillLower.contains(ms));

            if (isMatched) {
                matched.add(skill);
            } else {
                missing.add(skill);
            }
        }

        int matchPercentage = requiredSkills.isEmpty() ? 100 :
                (int) Math.round(((double) matched.size() / requiredSkills.size()) * 100);

        String advice;
        if (matchPercentage == 100) {
            advice = "Outstanding! You match 100% of the core competencies for this role. You are ready to apply!";
        } else if (matchPercentage >= 70) {
            advice = String.format("Strong profile! You match %d%% of requirements. To reach 100%%, prioritize completing modules on: %s.",
                    matchPercentage, String.join(", ", missing));
        } else {
            advice = String.format("Skill gap identified: You match %d%% of this role. Focus on mastering: %s.",
                    matchPercentage, String.join(", ", missing));
        }

        // Save job posting
        String skillsJson;
        try {
            skillsJson = objectMapper.writeValueAsString(requiredSkills);
        } catch (Exception e) {
            skillsJson = "[]";
        }

        JobPosting jobPosting = JobPosting.builder()
                .title(request.jobTitle() != null ? request.jobTitle() : "Software Engineer")
                .company(request.company() != null ? request.company() : "Tech Partner")
                .description(request.jobDescription())
                .requiredSkillsJson(skillsJson)
                .build();
        JobPosting saved = jobPostingRepository.save(jobPosting);

        return new JobMatchResponse(
                saved.getId(),
                saved.getTitle(),
                saved.getCompany(),
                matchPercentage,
                matched,
                missing,
                advice
        );
    }

    @Transactional
    public CareerRecommendationsResponse getRecommendations(User user) {
        seedDefaultJobsIfEmpty();

        List<JobPosting> allJobs = jobPostingRepository.findAll();
        List<JobMatchResponse> results = new ArrayList<>();

        for (JobPosting job : allJobs) {
            List<String> requiredSkills = parseSkillsFromJson(job.getRequiredSkillsJson());
            JobMatchResponse match = evaluateUserAgainstSkills(user, job.getId(), job.getTitle(), job.getCompany(), requiredSkills);
            results.add(match);
        }

        results.sort((a, b) -> Integer.compare(b.matchPercentage(), a.matchPercentage()));

        return new CareerRecommendationsResponse(results.size(), results);
    }

    private List<String> extractRequiredSkills(JobMatchRequest request) {
        if (request.explicitSkills() != null && !request.explicitSkills().isEmpty()) {
            return request.explicitSkills();
        }

        if (request.jobDescription() == null || request.jobDescription().isBlank()) {
            return List.of("Java", "Spring Boot", "SQL", "Git", "REST APIs");
        }

        try {
            ChatClient chatClient = chatClientBuilder.build();
            String prompt = String.format("""
                    Extract the top 5 to 8 technical skills or competencies required in this job description.
                    Return ONLY a JSON array of strings, e.g. ["Java", "Docker", "Kubernetes", "PostgreSQL"].
                    Do not include Markdown formatting or explanation.

                    Job Title: %s
                    Job Description: %s
                    """, request.jobTitle(), request.jobDescription());

            String response = chatClient.prompt(prompt).call().content();
            if (response != null) {
                String cleanJson = response.trim();
                if (cleanJson.startsWith("```")) {
                    cleanJson = cleanJson.replaceAll("^```[a-zA-Z]*\\s*", "").replaceAll("```$", "").trim();
                }
                List<String> parsed = objectMapper.readValue(cleanJson, new TypeReference<List<String>>() {});
                if (parsed != null && !parsed.isEmpty()) {
                    return parsed;
                }
            }
        } catch (Exception e) {
            log.warn("AI skill extraction fallback triggered: {}", e.getMessage());
        }

        // Fallback keyword extraction
        List<String> commonSkills = List.of(
                "Java", "Python", "JavaScript", "TypeScript", "React", "Node",
                "Spring Boot", "Docker", "Kubernetes", "AWS", "SQL", "MySQL",
                "Git", "REST", "CI/CD", "Linux", "MongoDB"
        );
        String descLower = request.jobDescription().toLowerCase();
        List<String> found = new ArrayList<>();
        for (String skill : commonSkills) {
            if (descLower.contains(skill.toLowerCase())) {
                found.add(skill);
            }
        }
        return found.isEmpty() ? List.of("Software Engineering", "Problem Solving", "Version Control") : found;
    }

    private JobMatchResponse evaluateUserAgainstSkills(User user, Long jobId, String title, String company, List<String> requiredSkills) {
        List<Module> completedModules = moduleRepository.findByRoadmapUserIdAndCompletedTrue(user.getId());
        Set<String> masteredSkills = new HashSet<>();
        for (Module m : completedModules) {
            masteredSkills.add(m.getTitle().toLowerCase().trim());
            if (m.getDescription() != null) {
                for (String word : m.getDescription().split("[,;\\s]+")) {
                    if (word.length() > 2) {
                        masteredSkills.add(word.toLowerCase().trim());
                    }
                }
            }
        }

        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        for (String skill : requiredSkills) {
            String skillLower = skill.toLowerCase().trim();
            boolean isMatched = masteredSkills.stream().anyMatch(ms ->
                    ms.contains(skillLower) || skillLower.contains(ms));

            if (isMatched) {
                matched.add(skill);
            } else {
                missing.add(skill);
            }
        }

        int matchPercentage = requiredSkills.isEmpty() ? 100 :
                (int) Math.round(((double) matched.size() / requiredSkills.size()) * 100);

        String advice = String.format("Match %d%%. %s", matchPercentage,
                missing.isEmpty() ? "Ready to interview!" : "Recommended to learn: " + String.join(", ", missing));

        return new JobMatchResponse(jobId, title, company, matchPercentage, matched, missing, advice);
    }

    private List<String> parseSkillsFromJson(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private void seedDefaultJobsIfEmpty() {
        if (jobPostingRepository.count() == 0) {
            List<JobPosting> seed = List.of(
                    JobPosting.builder()
                            .title("Full Stack Software Engineer")
                            .company("TechCorp Labs")
                            .description("Developing modern microservices and dynamic web applications.")
                            .requiredSkillsJson("[\"React\", \"Spring Boot\", \"MySQL\", \"REST\", \"Git\"]")
                            .build(),
                    JobPosting.builder()
                            .title("Junior Cloud & DevOps Specialist")
                            .company("CloudScale Systems")
                            .description("Automating infrastructure deployment and container orchestration.")
                            .requiredSkillsJson("[\"Docker\", \"Kubernetes\", \"CI/CD\", \"Linux\", \"AWS\"]")
                            .build(),
                    JobPosting.builder()
                            .title("AI & Data Platform Engineer")
                            .company("OmniAI Solutions")
                            .description("Building intelligent LLM workflows, RAG pipelines, and API integrations.")
                            .requiredSkillsJson("[\"Python\", \"LLM\", \"Vector DB\", \"SQL\", \"Docker\"]")
                            .build()
            );
            jobPostingRepository.saveAll(seed);
        }
    }
}
