package com.asjad.studygen.service;

import com.asjad.studygen.entity.Resource;
import com.asjad.studygen.entity.Roadmap;
import com.asjad.studygen.entity.User;
import com.asjad.studygen.entity.UserSavedResource;
import com.asjad.studygen.repository.ResourceRepository;
import com.asjad.studygen.repository.RoadmapRepository;
import com.asjad.studygen.repository.UserSavedResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final UserSavedResourceRepository userSavedResourceRepository;
    private final RoadmapRepository roadmapRepository;

    public record ResourceResponse(
            Long id,
            String title,
            String category,
            String description,
            String whyFitsYou,
            String difficulty,
            String sourceLabel,
            String confidence,
            String url,
            String searchQuery,
            boolean saved
    ) {}

    public record SaveResult(
            boolean success,
            boolean saved,
            Long resourceId,
            String message
    ) {}

    @Transactional(readOnly = true)
    public List<ResourceResponse> getResources(User user, String category) {
        List<Resource> resources;
        if (category == null || category.isBlank() || category.equalsIgnoreCase("All")) {
            resources = resourceRepository.findAll();
        } else {
            resources = resourceRepository.findByCategoryIgnoreCase(category.trim());
        }

        Set<Long> savedIds = (user != null)
                ? userSavedResourceRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                    .stream()
                    .map(usr -> usr.getResource().getId())
                    .collect(Collectors.toSet())
                : Collections.emptySet();

        // Optional personalization hint if user has active roadmaps
        String roadmapHint = "";
        if (user != null) {
            List<Roadmap> userRoadmaps = roadmapRepository.findByUserId(user.getId());
            if (!userRoadmaps.isEmpty()) {
                roadmapHint = userRoadmaps.get(0).getTitle();
            }
        }

        final String activeTopic = roadmapHint;

        return resources.stream().map(r -> {
            String personalizedWhy = r.getWhyFitsYou();
            if (!activeTopic.isBlank() && personalizedWhy != null && !personalizedWhy.toLowerCase().contains(activeTopic.toLowerCase())) {
                personalizedWhy = personalizedWhy + " (Matches your active roadmap: " + activeTopic + ")";
            }

            return new ResourceResponse(
                    r.getId(),
                    r.getTitle(),
                    r.getCategory(),
                    r.getDescription(),
                    personalizedWhy,
                    r.getDifficulty() != null ? r.getDifficulty() : "INTERMEDIATE",
                    r.getSourceLabel() != null ? r.getSourceLabel() : "Curated",
                    r.getConfidence() != null ? r.getConfidence() : "Verified",
                    r.getUrl(),
                    r.getSearchQuery() != null ? r.getSearchQuery() : r.getTitle(),
                    savedIds.contains(r.getId())
            );
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<ResourceResponse> getSavedResources(User user) {
        if (user == null) return Collections.emptyList();

        List<UserSavedResource> savedList = userSavedResourceRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        return savedList.stream().map(usr -> {
            Resource r = usr.getResource();
            return new ResourceResponse(
                    r.getId(),
                    r.getTitle(),
                    r.getCategory(),
                    r.getDescription(),
                    r.getWhyFitsYou(),
                    r.getDifficulty() != null ? r.getDifficulty() : "INTERMEDIATE",
                    r.getSourceLabel() != null ? r.getSourceLabel() : "Curated",
                    r.getConfidence() != null ? r.getConfidence() : "Verified",
                    r.getUrl(),
                    r.getSearchQuery() != null ? r.getSearchQuery() : r.getTitle(),
                    true
            );
        }).toList();
    }

    @Transactional
    public SaveResult saveResource(User user, Long resourceId) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new NoSuchElementException("Resource not found with ID: " + resourceId));

        if (userSavedResourceRepository.existsByUserIdAndResourceId(user.getId(), resourceId)) {
            return new SaveResult(true, true, resourceId, "Resource is already saved in your library.");
        }

        UserSavedResource saved = UserSavedResource.builder()
                .user(user)
                .resource(resource)
                .build();
        userSavedResourceRepository.save(saved);

        return new SaveResult(true, true, resourceId, "Saved to your library.");
    }

    @Transactional
    public SaveResult unsaveResource(User user, Long resourceId) {
        userSavedResourceRepository.deleteByUserIdAndResourceId(user.getId(), resourceId);
        return new SaveResult(true, false, resourceId, "Removed from your library.");
    }
}
