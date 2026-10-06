package com.asjad.studygen.service;

import com.asjad.studygen.entity.JobPosting;
import com.asjad.studygen.entity.Module;
import com.asjad.studygen.entity.User;
import com.asjad.studygen.entity.UserJobMatch;
import com.asjad.studygen.repository.JobPostingRepository;
import com.asjad.studygen.repository.ModuleRepository;
import com.asjad.studygen.repository.UserJobMatchRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class CareerMatchingService {

    private final JobPostingRepository jobPostingRepository;
    private final UserJobMatchRepository userJobMatchRepository;
    private final ModuleRepository moduleRepository;
    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public record JobMatchRequest(
            String jobTitle,
            String company,
            String jobDescription,
            List<String> explicitSkills,
            String url
    ) {
        // Overloaded constructor for backwards-compatibility
        public JobMatchRequest(String jobTitle, String company, String jobDescription, List<String> explicitSkills) {
            this(jobTitle, company, jobDescription, explicitSkills, null);
        }
    }

    public record SkillAnalysisItem(
            String skill,
            String category,
            String importance, // "Required" | "Nice-to-have"
            String status,     // "Covered" | "Partly covered" | "Missing"
            Long matchedModuleId,
            String matchedModuleTitle,
            String gapAdvice
    ) {}

    public record SuggestedModuleItem(
            Long moduleId,
            String title,
            String topic,
            String description,
            Integer estimatedHours,
            String associatedSkill
    ) {}

    public record JobMatchResponse(
            Long jobPostingId,
            String jobTitle,
            String company,
            int matchPercentage,
            List<String> matchedSkills,
            List<String> missingSkills,
            String actionableAdvice,
            String seniority,
            List<String> responsibilities,
            List<SkillAnalysisItem> skillsAnalysis,
            List<SuggestedModuleItem> suggestedModules,
            int userCompletedModulesCount,
            String jobDescription,
            String sourceUrl,
            String createdAt
    ) {
        // Overloaded constructor for 7-arg backwards-compatibility
        public JobMatchResponse(Long jobPostingId, String jobTitle, String company, int matchPercentage,
                                List<String> matchedSkills, List<String> missingSkills, String actionableAdvice) {
            this(jobPostingId, jobTitle, company, matchPercentage, matchedSkills, missingSkills, actionableAdvice,
                    "Mid-Level", Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                    0, null, null, null);
        }
    }

    public record JobMatchHistoryItem(
            Long id,
            String jobTitle,
            String company,
            String seniority,
            int matchPercentage,
            int totalSkills,
            int matchedSkillsCount,
            String createdAt
    ) {}

    public record CareerRecommendationsResponse(
            int totalEvaluated,
            List<JobMatchResponse> matches
    ) {}

    @Transactional
    public JobMatchResponse matchJob(User user, JobMatchRequest request) {
        String description = request.jobDescription();

        // 1. If URL is provided, fetch job description from URL
        if ((description == null || description.isBlank()) && request.url() != null && !request.url().isBlank()) {
            description = fetchJobDescriptionFromUrl(request.url().trim());
        }

        if (description == null || description.isBlank()) {
            description = "Software Engineer position with modern web, cloud, and distributed architectures.";
        }

        String resolvedTitle = request.jobTitle() != null && !request.jobTitle().isBlank()
                ? request.jobTitle()
                : extractJobTitleFromText(description);

        String resolvedCompany = request.company() != null && !request.company().isBlank()
                ? request.company()
                : "Tech Partner";

        // 2. Extract seniority & key responsibilities
        String seniority = detectSeniority(resolvedTitle, description);
        List<String> responsibilities = extractResponsibilities(description);

        // 3. Extract required skills
        List<String> requiredSkills = extractRequiredSkills(request, description, resolvedTitle);

        // 4. Retrieve user completed and in-progress modules
        List<Module> completedModules = moduleRepository.findByRoadmapUserIdAndCompletedTrue(user.getId());
        List<Module> allUserModules = moduleRepository.findAll(); // check roadmaps for user
        List<Module> userAllModules = allUserModules.stream()
                .filter(m -> m.getRoadmap() != null && m.getRoadmap().getUser() != null
                        && m.getRoadmap().getUser().getId().equals(user.getId()))
                .toList();

        // Build skill analysis
        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        List<SkillAnalysisItem> skillsAnalysis = new ArrayList<>();

        for (int i = 0; i < requiredSkills.size(); i++) {
            String skill = requiredSkills.get(i);
            String skillLower = skill.toLowerCase().trim();

            // Match against completed modules
            Optional<Module> matchedCompleted = completedModules.stream()
                    .filter(m -> isModuleMatch(m, skillLower))
                    .findFirst();

            // Match against any user module (in progress)
            Optional<Module> matchedAny = userAllModules.stream()
                    .filter(m -> isModuleMatch(m, skillLower))
                    .findFirst();

            String category = categorizeSkill(skill);
            String importance = i < 4 || isSkillRequiredInText(skillLower, description) ? "Required" : "Nice-to-have";

            String status;
            Long matchedModId = null;
            String matchedModTitle = null;
            String gapAdvice;

            if (matchedCompleted.isPresent()) {
                status = "Covered";
                matchedModId = matchedCompleted.get().getId();
                matchedModTitle = matchedCompleted.get().getTitle();
                gapAdvice = "Covered by completed module: " + matchedModTitle;
                matched.add(skill);
            } else if (matchedAny.isPresent()) {
                status = "Partly covered";
                matchedModId = matchedAny.get().getId();
                matchedModTitle = matchedAny.get().getTitle();
                gapAdvice = "In progress in roadmap. Pass test in '" + matchedModTitle + "' to verify coverage.";
                matched.add(skill); // counts partially towards match
            } else {
                status = "Missing";
                gapAdvice = "Recommended: Add a " + skill + " module to your roadmap or review key concepts.";
                missing.add(skill);
            }

            skillsAnalysis.add(new SkillAnalysisItem(
                    skill,
                    category,
                    importance,
                    status,
                    matchedModId,
                    matchedModTitle,
                    gapAdvice
            ));
        }

        int totalSkills = requiredSkills.size();
        int matchPercentage = totalSkills == 0 ? 100 : (int) Math.round(((double) matched.size() / totalSkills) * 100);

        String advice;
        if (matchPercentage == 100) {
            advice = "Outstanding! You match 100% of the competencies for this role. You are ready to apply!";
        } else if (matchPercentage >= 70) {
            advice = String.format("Strong profile! You match %d%% of requirements. To reach 100%%, close the gap on: %s.",
                    matchPercentage, String.join(", ", missing));
        } else {
            advice = String.format("Skill gap identified: You match %d%% of this role. Prioritize mastering: %s.",
                    matchPercentage, String.join(", ", missing));
        }

        // 5. Generate suggested modules for missing skills
        List<SuggestedModuleItem> suggestedModules = buildSuggestedModules(missing, userAllModules);

        // 6. Save JobPosting (legacy table)
        String skillsJson = toJson(requiredSkills);
        JobPosting jobPosting = JobPosting.builder()
                .title(resolvedTitle)
                .company(resolvedCompany)
                .description(description)
                .requiredSkillsJson(skillsJson)
                .build();
        JobPosting savedPosting = jobPostingRepository.save(jobPosting);

        // 7. Save UserJobMatch for history tracking
        UserJobMatch userMatch = UserJobMatch.builder()
                .user(user)
                .jobTitle(resolvedTitle)
                .company(resolvedCompany)
                .seniority(seniority)
                .matchPercentage(matchPercentage)
                .responsibilitiesJson(toJson(responsibilities))
                .matchedSkillsJson(toJson(matched))
                .missingSkillsJson(toJson(missing))
                .skillsAnalysisJson(toJson(skillsAnalysis))
                .suggestedModulesJson(toJson(suggestedModules))
                .actionableAdvice(advice)
                .jobDescription(description)
                .sourceUrl(request.url())
                .build();
        UserJobMatch savedUserMatch = userJobMatchRepository.save(userMatch);

        String nowFormatted = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));

        return new JobMatchResponse(
                savedUserMatch.getId(),
                resolvedTitle,
                resolvedCompany,
                matchPercentage,
                matched,
                missing,
                advice,
                seniority,
                responsibilities,
                skillsAnalysis,
                suggestedModules,
                completedModules.size(),
                description,
                request.url(),
                nowFormatted
        );
    }

    @Transactional(readOnly = true)
    public List<JobMatchHistoryItem> getUserJobMatches(User user) {
        List<UserJobMatch> list = userJobMatchRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        List<JobMatchHistoryItem> items = new ArrayList<>();

        for (UserJobMatch m : list) {
            List<String> matched = parseStringList(m.getMatchedSkillsJson());
            List<String> missing = parseStringList(m.getMissingSkillsJson());
            int total = matched.size() + missing.size();
            String date = m.getCreatedAt() != null
                    ? m.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
                    : "Recently";

            items.add(new JobMatchHistoryItem(
                    m.getId(),
                    m.getJobTitle(),
                    m.getCompany() != null ? m.getCompany() : "Tech Partner",
                    m.getSeniority() != null ? m.getSeniority() : "Mid-Level",
                    m.getMatchPercentage(),
                    total,
                    matched.size(),
                    date
            ));
        }
        return items;
    }

    @Transactional(readOnly = true)
    public JobMatchResponse getJobMatchDetail(User user, Long matchId) {
        UserJobMatch m = userJobMatchRepository.findByIdAndUserId(matchId, user.getId())
                .orElseThrow(() -> new NoSuchElementException("Job match record not found."));

        List<String> matched = parseStringList(m.getMatchedSkillsJson());
        List<String> missing = parseStringList(m.getMissingSkillsJson());
        List<String> responsibilities = parseStringList(m.getResponsibilitiesJson());
        List<SkillAnalysisItem> skillsAnalysis = parseSkillsAnalysis(m.getSkillsAnalysisJson());
        List<SuggestedModuleItem> suggestedModules = parseSuggestedModules(m.getSuggestedModulesJson());

        List<Module> completed = moduleRepository.findByRoadmapUserIdAndCompletedTrue(user.getId());
        String date = m.getCreatedAt() != null
                ? m.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
                : "Recently";

        return new JobMatchResponse(
                m.getId(),
                m.getJobTitle(),
                m.getCompany(),
                m.getMatchPercentage(),
                matched,
                missing,
                m.getActionableAdvice(),
                m.getSeniority() != null ? m.getSeniority() : "Mid-Level",
                responsibilities,
                skillsAnalysis,
                suggestedModules,
                completed.size(),
                m.getJobDescription(),
                m.getSourceUrl(),
                date
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

    // --- URL Fetcher ---
    private String fetchJobDescriptionFromUrl(String urlString) {
        if (!urlString.startsWith("http://") && !urlString.startsWith("https://")) {
            throw new IllegalArgumentException("Invalid URL: must begin with http:// or https://");
        }

        try {
            URI uri = URI.create(urlString);
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(6))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(8))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,text/plain")
                    .GET()
                    .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 403 || resp.statusCode() == 401) {
                throw new IllegalArgumentException("The target job posting URL blocked automated access (HTTP " + resp.statusCode() + "). Please copy and paste the job description text directly.");
            }
            if (resp.statusCode() >= 400) {
                throw new IllegalArgumentException("Failed to fetch job URL: HTTP " + resp.statusCode() + " (" + resp.uri() + ").");
            }

            // Clean html
            String raw = resp.body();
            String cleaned = raw.replaceAll("<script[^>]*>[\\s\\S]*?</script>", " ")
                    .replaceAll("<style[^>]*>[\\s\\S]*?</style>", " ")
                    .replaceAll("<[^>]*>", " ")
                    .replaceAll("&nbsp;", " ")
                    .replaceAll("&amp;", "&")
                    .replaceAll("\\s+", " ")
                    .trim();

            if (cleaned.length() < 50) {
                throw new IllegalArgumentException("The URL was fetched successfully but contained insufficient job posting content.");
            }
            return cleaned.length() > 12000 ? cleaned.substring(0, 12000) : cleaned;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Job URL fetch failed: {}", e.getMessage());
            throw new IllegalArgumentException("Could not connect to URL: " + e.getMessage() + ". Please paste the job description directly.");
        }
    }

    private boolean isModuleMatch(Module m, String skillLower) {
        if (m.getTitle() != null && m.getTitle().toLowerCase().contains(skillLower)) return true;
        if (m.getDescription() != null && m.getDescription().toLowerCase().contains(skillLower)) return true;
        return false;
    }

    private String detectSeniority(String title, String desc) {
        String combined = (title + " " + desc).toLowerCase();
        if (combined.contains("lead") || combined.contains("principal") || combined.contains("architect") || combined.contains("staff")) {
            return "Lead / Staff";
        }
        if (combined.contains("senior") || combined.contains("sr.") || combined.contains("5+ years") || combined.contains("6+ years")) {
            return "Senior";
        }
        if (combined.contains("junior") || combined.contains("jr.") || combined.contains("associate") || combined.contains("intern") || combined.contains("entry level")) {
            return "Junior";
        }
        return "Mid-Level";
    }

    private List<String> extractResponsibilities(String desc) {
        List<String> bullets = new ArrayList<>();
        // Look for lines starting with bullet symbols or numbers
        Pattern pattern = Pattern.compile("(?m)^[\\s]*[\\*\\-\\•\\–\\d+\\.]\\s*([^\\r\\n]+)");
        Matcher matcher = pattern.matcher(desc);
        while (matcher.find() && bullets.size() < 6) {
            String b = matcher.group(1).trim();
            if (b.length() > 15 && b.length() < 250) {
                bullets.add(b);
            }
        }

        if (bullets.isEmpty()) {
            // Fallback split sentences
            String[] sentences = desc.split("[\\.\\n]+");
            for (String s : sentences) {
                String clean = s.trim();
                String lower = clean.toLowerCase();
                if ((lower.contains("responsible for") || lower.contains("design") || lower.contains("develop")
                        || lower.contains("build") || lower.contains("collaborate") || lower.contains("maintain"))
                        && clean.length() > 20 && clean.length() < 200) {
                    bullets.add(clean);
                    if (bullets.size() >= 6) break;
                }
            }
        }

        if (bullets.isEmpty()) {
            bullets = List.of(
                    "Design and implement reliable, scalable software services and applications.",
                    "Collaborate with cross-functional product and engineering teams.",
                    "Ensure high-quality code through testing, documentation, and code reviews.",
                    "Participate in architectural discussions and performance tuning."
            );
        }
        return bullets;
    }

    private String categorizeSkill(String skill) {
        String s = skill.toLowerCase();
        if (s.matches(".*(react|vue|angular|javascript|typescript|css|html|frontend|nextjs|tailwind|ui|redux).*")) {
            return "Frontend";
        }
        if (s.matches(".*(java|spring|python|node|backend|c#|golang|api|rest|graphql|microservice|django).*")) {
            return "Backend";
        }
        if (s.matches(".*(sql|mysql|postgres|mongo|database|redis|cassandra|dynamodb|db).*")) {
            return "Database";
        }
        if (s.matches(".*(docker|kubernetes|aws|cloud|azure|gcp|devops|ci/cd|linux|git|terraform).*")) {
            return "Cloud & DevOps";
        }
        if (s.matches(".*(design|architecture|system|data structure|algorithm|security|concurrency).*")) {
            return "Architecture & CS";
        }
        return "Core Competency";
    }

    private boolean isSkillRequiredInText(String skillLower, String desc) {
        String lower = desc.toLowerCase();
        int idx = lower.indexOf(skillLower);
        if (idx == -1) return false;
        int start = Math.max(0, idx - 80);
        String surrounding = lower.substring(start, idx);
        return surrounding.contains("require") || surrounding.contains("must") || surrounding.contains("qualification")
                || surrounding.contains("essential") || surrounding.contains("mandatory");
    }

    private List<SuggestedModuleItem> buildSuggestedModules(List<String> missingSkills, List<Module> userAllModules) {
        List<SuggestedModuleItem> suggestions = new ArrayList<>();

        for (String skill : missingSkills) {
            // Check if user already has an incomplete module matching this skill
            Optional<Module> existing = userAllModules.stream()
                    .filter(m -> !m.isCompleted() && isModuleMatch(m, skill.toLowerCase()))
                    .findFirst();

            if (existing.isPresent()) {
                Module m = existing.get();
                suggestions.add(new SuggestedModuleItem(
                        m.getId(),
                        m.getTitle(),
                        m.getRoadmap() != null ? m.getRoadmap().getTitle() : "Active Roadmap",
                        m.getDescription() != null ? m.getDescription() : "Continue module to close gap on " + skill,
                        4,
                        skill
                ));
            } else {
                // Synthesize a suggested module card
                suggestions.add(new SuggestedModuleItem(
                        null,
                        "Mastering " + skill + " Fundamentals",
                        skill,
                        "Core concepts, hands-on architectural patterns, and practical test drills for " + skill + ".",
                        5,
                        skill
                ));
            }
            if (suggestions.size() >= 4) break;
        }

        return suggestions;
    }

    private String extractJobTitleFromText(String desc) {
        String firstLine = desc.split("\n")[0].trim();
        if (firstLine.length() > 5 && firstLine.length() < 60) {
            return firstLine;
        }
        return "Software Engineer";
    }

    private List<String> extractRequiredSkills(JobMatchRequest request, String description, String title) {
        if (request.explicitSkills() != null && !request.explicitSkills().isEmpty()) {
            return request.explicitSkills();
        }

        try {
            ChatClient chatClient = chatClientBuilder.build();
            String prompt = String.format("""
                    Extract 5 to 8 key technical skills, tools, or frameworks required in this job description.
                    Return ONLY a JSON array of strings, e.g. ["Java", "Docker", "Kubernetes", "PostgreSQL"].
                    Do not include Markdown formatting or explanation.

                    Job Title: %s
                    Job Description: %s
                    """, title, description);

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

        // Common skill heuristics
        List<String> commonSkills = List.of(
                "Java", "Python", "JavaScript", "TypeScript", "React", "Node",
                "Spring Boot", "Docker", "Kubernetes", "AWS", "SQL", "MySQL",
                "PostgreSQL", "Git", "REST APIs", "CI/CD", "Linux", "MongoDB",
                "System Design", "Microservices"
        );
        String descLower = description.toLowerCase();
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

    private List<String> parseStringList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<SkillAnalysisItem> parseSkillsAnalysis(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<SkillAnalysisItem>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<SuggestedModuleItem> parseSuggestedModules(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<SuggestedModuleItem>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
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
