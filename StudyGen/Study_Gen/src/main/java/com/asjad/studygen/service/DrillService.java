package com.asjad.studygen.service;

import com.asjad.studygen.dto.practice.*;
import com.asjad.studygen.entity.Module;
import com.asjad.studygen.entity.User;
import com.asjad.studygen.repository.ModuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DrillService {

    private final ModuleRepository moduleRepository;
    private final UserActivityService userActivityService;

    @Transactional(readOnly = true)
    public ModuleDrillsResponse getDrillsForModule(User user, Long moduleId) {
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new IllegalArgumentException("Module not found: " + moduleId));

        String title = module.getTitle();
        String desc = module.getDescription() != null ? module.getDescription() : "";
        String combined = (title + " " + desc).toLowerCase();

        boolean isTechnical = combined.matches(".*(java|react|spring|python|code|script|sql|api|backend|frontend|database|web|dev|cloud|docker|git|algorithm|data structure|function|class|method).*");

        List<DrillItemDTO> drills = new ArrayList<>();

        // 1. Scenario Drill
        drills.add(new DrillItemDTO(
                "drill-" + moduleId + "-scenario-1",
                "SCENARIO",
                "Real-World Scenario: " + title,
                "INTERMEDIATE",
                "Imagine you are leading a team building a production system utilizing " + title + ". During peak traffic, you notice performance degradation and unexpected state mutations. How would you diagnose the bottleneck, restructure the logic, and ensure thread safety or data consistency?",
                List.of(
                        "Identify resource contention or unindexed queries",
                        "Apply caching or optimistic locking where appropriate",
                        "Decouple heavy synchronous workloads with asynchronous processing",
                        "Add robust observability and latency metrics"
                ),
                "To resolve this scenario, start by examining thread dumps and APM latency traces to pinpoint bottlenecks. Next, eliminate shared mutable state by enforcing immutability or using scoped containers. For high-throughput paths, introduce caching with a well-defined TTL, and offload non-critical side effects to asynchronous background queues.",
                null,
                null,
                null,
                null,
                "Focus on diagnosing the root cause before prescribing the architectural remedy.",
                null,
                null,
                null,
                null
        ));

        // 2. Code Exercise (Only for PRACTICAL / Technical modules)
        if (isTechnical) {
            String starterCode = "// Implement the solution for " + title + "\n" +
                    "public class Solution {\n" +
                    "    public boolean validateProcess(String[] inputs, int threshold) {\n" +
                    "        // TODO: Validate constraints and process items\n" +
                    "        if (inputs == null || inputs.length == 0) return false;\n" +
                    "        \n" +
                    "        return inputs.length <= threshold;\n" +
                    "    }\n" +
                    "}";

            drills.add(new DrillItemDTO(
                    "drill-" + moduleId + "-code-1",
                    "CODE",
                    "Hands-on Code Exercise: " + title,
                    "ADVANCED",
                    "Write a resilient processing method that takes an array of payload strings and a threshold limit. It should validate non-null inputs, ensure boundaries are respected, and return true if and only if all elements meet the validation criteria.",
                    null,
                    null,
                    "java",
                    starterCode,
                    "The method should validate input constraints, prevent null pointer exceptions, and return a boolean status within O(n) time complexity.",
                    List.of(
                            new DrillTestCaseDTO("inputs: [\"alpha\", \"beta\"], threshold: 5", "true"),
                            new DrillTestCaseDTO("inputs: [], threshold: 3", "false"),
                            new DrillTestCaseDTO("inputs: [\"a\", \"b\", \"c\", \"d\"], threshold: 2", "false")
                    ),
                    "Remember to check for edge cases where the input array might be null or exceeds the specified threshold.",
                    "public class Solution {\n" +
                            "    public boolean validateProcess(String[] inputs, int threshold) {\n" +
                            "        if (inputs == null || inputs.length == 0) return false;\n" +
                            "        if (inputs.length > threshold) return false;\n" +
                            "        for (String s : inputs) {\n" +
                            "            if (s == null || s.trim().isEmpty()) return false;\n" +
                            "        }\n" +
                            "        return true;\n" +
                            "    }\n" +
                            "}",
                    null,
                    null,
                    null
            ));
        }

        // 3. Conceptual / Edge Case Drill
        drills.add(new DrillItemDTO(
                "drill-" + moduleId + "-concept-1",
                "CONCEPTUAL",
                "Conceptual Drill: " + title,
                "BEGINNER",
                "Which of the following statements most accurately reflects the core trade-off when adopting " + title + " in modern software architecture?",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(
                        "It eliminates all latency but increases memory overhead exponentially.",
                        "It provides modular separation of concerns and maintainability at the expense of initial configuration complexity.",
                        "It should only be used in legacy monolithic systems and is deprecated in microservices.",
                        "It guarantees zero compile-time errors regardless of developer implementation."
                ),
                1,
                "Option B is correct. The primary trade-off of this architectural pattern is enhanced modularity, separation of concerns, and testability, traded against initial setup overhead and learning curve."
        ));

        return new ModuleDrillsResponse(module.getId(), module.getTitle(), isTechnical, drills);
    }

    @Transactional(readOnly = true)
    public List<DrillItemDTO> generateMoreDrills(User user, Long moduleId) {
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new IllegalArgumentException("Module not found: " + moduleId));

        String title = module.getTitle();
        String uniqueId = UUID.randomUUID().toString().substring(0, 6);

        List<DrillItemDTO> moreDrills = new ArrayList<>();

        moreDrills.add(new DrillItemDTO(
                "drill-" + moduleId + "-scenario-" + uniqueId,
                "SCENARIO",
                "Edge Case Analysis: " + title,
                "ADVANCED",
                "Your application experienced an unhandled outage when a downstream dependency failed during a " + title + " cycle. Detail how you would implement a circuit-breaker, fallback logic, and graceful degradation.",
                List.of(
                        "Configure timeout thresholds and retry backoffs with jitter",
                        "Implement circuit-breaker state transitions (Closed, Open, Half-Open)",
                        "Provide deterministic cached or synthetic fallback responses",
                        "Emit telemetry alerts to on-call engineers"
                ),
                "To harden this interaction, wrap the downstream invocation in a circuit breaker. Configure a fast-failing timeout (e.g. 500ms) with exponential backoff and jitter. When the failure rate surpasses 50%, transition to Open state immediately and serve a cached or graceful fallback response to the user while notifying telemetry.",
                null,
                null,
                null,
                null,
                "Think about fault tolerance patterns like Resilience4j, timeouts, and fallback caches.",
                null,
                null,
                null,
                null
        ));

        moreDrills.add(new DrillItemDTO(
                "drill-" + moduleId + "-concept-" + uniqueId,
                "CONCEPTUAL",
                "Diagnostics & Debugging: " + title,
                "INTERMEDIATE",
                "When debugging intermittent failures in " + title + ", what is the most effective initial investigation step?",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(
                        "Restart all production servers without saving logs.",
                        "Inspect distributed trace IDs and correlation logs across service boundaries.",
                        "Disable all unit tests to expedite hotfix deployment.",
                        "Rewrite the entire subsystem from scratch in a different language."
                ),
                1,
                "Option B is correct. Distributed tracing and structured logs with correlation IDs allow developers to trace request flows across boundaries and pinpoint exact failure points."
        ));

        userActivityService.logActivity(user, "DRILL_PRACTICE", 10);

        return moreDrills;
    }

    @Transactional
    public DrillEvaluationResponse evaluateScenarioAnswer(User user, DrillEvaluationRequest request) {
        String answer = request.userAnswer() != null ? request.userAnswer().trim() : "";

        userActivityService.logActivity(user, "SCENARIO_CHECK", 5);

        if (answer.length() < 25) {
            return new DrillEvaluationResponse(
                    "NEEDS_IMPROVEMENT",
                    "Your response is too brief to adequately assess. Consider elaborating on the diagnostic approach, architectural trade-offs, and mitigation strategies.",
                    request.criteria() != null ? request.criteria() : List.of("Elaborate on core mechanisms", "Explain trade-offs")
            );
        }

        List<String> criteria = request.criteria() != null ? request.criteria() : List.of();
        List<String> missing = new ArrayList<>();

        for (String c : criteria) {
            // Check if any major keyword from the criterion appears in the answer
            String[] words = c.toLowerCase().split("\\s+");
            boolean matched = false;
            for (String w : words) {
                if (w.length() > 4 && answer.toLowerCase().contains(w)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                missing.add(c);
            }
        }

        if (missing.isEmpty() || missing.size() < criteria.size() / 2) {
            return new DrillEvaluationResponse(
                    "CORRECT",
                    "Outstanding analysis! You covered the key architectural principles and articulated a clear, production-grade strategy.",
                    missing.isEmpty() ? List.of("None! You hit all primary rubric criteria.") : missing
            );
        } else {
            return new DrillEvaluationResponse(
                    "PARTIALLY_CORRECT",
                    "Good start! You identified several important considerations, but some critical architectural aspects could be explored in greater depth.",
                    missing
            );
        }
    }
}
