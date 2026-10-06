package com.asjad.studygen.service;

import com.asjad.studygen.entity.Roadmap;
import com.asjad.studygen.entity.User;
import com.asjad.studygen.repository.RoadmapRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EcosystemDiscoveryService {

    private final RoadmapRepository roadmapRepository;
    private final ChatClient.Builder chatClientBuilder;

    private ChatClient chatClient;

    @PostConstruct
    void init() {
        this.chatClient = chatClientBuilder.build();
    }

    public record ResourceItem(
            String title,
            String description,
            String url
    ) {}

    public record EcosystemDiscoveryResponse(
            String queryTopic,
            List<ResourceItem> learningResources,
            List<ResourceItem> competitions,
            List<ResourceItem> news,
            List<ResourceItem> jobs
    ) {}

    // AI response DTO for structured parsing
    private record AiResourceDiscovery(
            List<ResourceItem> learningResources,
            List<ResourceItem> competitions,
            List<ResourceItem> news,
            List<ResourceItem> jobs
    ) {}

    public EcosystemDiscoveryResponse discoverEcosystem(User user, String requestedTopic) {
        String topic = requestedTopic;
        if (topic != null) {
            try {
                topic = java.net.URLDecoder.decode(topic, java.nio.charset.StandardCharsets.UTF_8);
            } catch (Exception ignored) {}
        }

        if (topic == null || topic.isBlank()) {
            List<Roadmap> roadmaps = roadmapRepository.findByUserId(user.getId());
            if (!roadmaps.isEmpty()) {
                topic = roadmaps.get(0).getTitle();
            } else {
                topic = "Full Stack Web & AI";
            }
        }

        String prompt = """
                You are a helpful assistant that curates educational and career resources.
                For the topic "%s", find and return resources in 4 categories.
                
                For each category, provide 2-4 items. Each item must have:
                - title: A descriptive title
                - description: A short 1-2 sentence description
                - url: A valid, real URL where this resource can be found
                
                Categories:
                1. learningResources — Articles, courses, tutorials, videos
                2. competitions — Hackathons, coding challenges, contests
                3. news — Recent articles, journals, industry news
                4. jobs — Career opportunities, job listings
                
                IMPORTANT: Return ONLY raw valid JSON. Do NOT wrap the response in ```json``` markdown tags.
                """.formatted(topic);

        try {
            AiResourceDiscovery aiResult = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .entity(AiResourceDiscovery.class);

            if (aiResult != null) {
                return new EcosystemDiscoveryResponse(
                        topic,
                        aiResult.learningResources() != null ? aiResult.learningResources() : List.of(),
                        aiResult.competitions() != null ? aiResult.competitions() : List.of(),
                        aiResult.news() != null ? aiResult.news() : List.of(),
                        aiResult.jobs() != null ? aiResult.jobs() : List.of()
                );
            }
        } catch (Exception e) {
            log.warn("AI resource discovery failed for topic '{}': {}. Using fallback.", topic, e.getMessage(), e);
        }

        return fallbackResources(topic);
    }

    private EcosystemDiscoveryResponse fallbackResources(String topic) {
        List<ResourceItem> learning = new ArrayList<>();
        learning.add(new ResourceItem(
                topic + " — Official Documentation",
                "Start with the official documentation and guides for " + topic + ".",
                "https://developer.mozilla.org"));
        learning.add(new ResourceItem(
                "freeCodeCamp: " + topic,
                "Interactive tutorials and certifications on " + topic + ".",
                "https://www.freecodecamp.org"));

        List<ResourceItem> competitions = new ArrayList<>();
        competitions.add(new ResourceItem(
                "Devpost Hackathons",
                "Find hackathons related to " + topic + " and build real projects.",
                "https://devpost.com/hackathons"));
        competitions.add(new ResourceItem(
                "MLH Events",
                "Major League Hacking events for students and developers.",
                "https://mlh.io/seasons/2026/events"));

        List<ResourceItem> news = new ArrayList<>();
        news.add(new ResourceItem(
                "Hacker News",
                "Community-curated technology news and discussions.",
                "https://news.ycombinator.com"));
        news.add(new ResourceItem(
                "Dev.to",
                "Community of developers sharing articles and tutorials.",
                "https://dev.to"));

        List<ResourceItem> jobs = new ArrayList<>();
        jobs.add(new ResourceItem(
                "LinkedIn Jobs: " + topic,
                "Search for " + topic + " related job opportunities.",
                "https://www.linkedin.com/jobs"));
        jobs.add(new ResourceItem(
                "Indeed: " + topic + " Engineer",
                "Browse engineering roles requiring " + topic + " skills.",
                "https://www.indeed.com"));

        return new EcosystemDiscoveryResponse(topic, learning, competitions, news, jobs);
    }
}
