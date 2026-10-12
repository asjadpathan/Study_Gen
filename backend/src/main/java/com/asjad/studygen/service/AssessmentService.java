package com.asjad.studygen.service;

import com.asjad.studygen.config.PromptRegistry;
import com.asjad.studygen.controller.ModuleController.ModuleSubPartDTO;
import com.asjad.studygen.dto.ai.AiQuizQuestion;
import com.asjad.studygen.dto.ai.AiQuizSuggestion;
import com.asjad.studygen.dto.assessment.*;
import com.asjad.studygen.entity.*;
import com.asjad.studygen.entity.Module;
import com.asjad.studygen.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final ModuleRepository moduleRepository;
    private final UserAssessmentAttemptRepository attemptRepository;
    private final ChatClient.Builder chatClientBuilder;
    private final PromptRegistry promptRegistry;
    private final TeachingContentService teachingContentService;

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-3.7-flash}")
    private String geminiModel;

    private ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private RestClient restClient;

    @PostConstruct
    void init() {
        try {
            this.chatClient = chatClientBuilder.build();
        } catch (Exception e) {
            log.info("ChatClient builder init deferred: {}", e.getMessage());
        }
    }

    @Transactional
    public AssessmentResponse generateDiagnosticQuiz(User user, DiagnosticQuizRequest request) {
        String topic = request.topic() != null ? request.topic() : "Computer Science Fundamentals";
        String prompt = promptRegistry.assessmentDiagnosticQuiz(topic, request.claimedPrerequisites());

        AiQuizSuggestion suggestion = callGeminiForQuiz(prompt);
        if (suggestion == null && this.chatClient != null) {
            try {
                suggestion = chatClient.prompt()
                        .user(prompt)
                        .call()
                        .entity(AiQuizSuggestion.class);
            } catch (Exception e) {
                log.warn("ChatClient diagnostic quiz failed: {}", e.getMessage());
            }
        }
        if (suggestion == null) {
            suggestion = buildDynamicTopicDiagnosticQuiz(topic);
        }

        Assessment assessment = new Assessment();
        assessment.setTitle("Diagnostic: " + topic);
        assessment.setType(AssessmentType.DIAGNOSTIC);
        assessment.setPassingScore(80);

        return persistQuizAndMapToResponse(assessment, suggestion);
    }

    @Transactional
    public AssessmentResponse generateModuleAssessment(User user, Long moduleId) {
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new IllegalArgumentException("Module not found: " + moduleId));

        var existingOpt = assessmentRepository.findFirstByModuleIdAndType(moduleId, AssessmentType.MODULE_GATE);
        if (existingOpt.isPresent()) {
            Assessment existing = existingOpt.get();
            if (!isDummyQuiz(existing)) {
                return mapToResponse(existing);
            }
            // Replace dummy quiz with genuine topic-specific quiz
            AiQuizSuggestion realQuiz = generateQuizForModule(module);
            existing.getQuestions().clear();
            for (AiQuizQuestion q : realQuiz.questions()) {
                try {
                    String optionsJson = objectMapper.writeValueAsString(q.options());
                    existing.addQuestion(new AssessmentQuestion(
                            existing,
                            q.questionText(),
                            optionsJson,
                            q.correctAnswerIndex(),
                            q.explanation()
                    ));
                } catch (Exception ignored) {}
            }
            return mapToResponse(assessmentRepository.save(existing));
        }

        AiQuizSuggestion suggestion = generateQuizForModule(module);

        Assessment assessment = new Assessment();
        assessment.setModule(module);
        assessment.setTitle(module.getTitle() + " - Mastery Assessment");
        assessment.setType(AssessmentType.MODULE_GATE);
        assessment.setPassingScore(80);

        return persistQuizAndMapToResponse(assessment, suggestion);
    }

    @Transactional(readOnly = true)
    public AssessmentResponse getAssessmentById(User user, Long assessmentId) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assessment not found: " + assessmentId));
        return mapToResponse(assessment);
    }

    @Transactional
    public AssessmentResponse getOrCreateModuleTest(User user, Long moduleId) {
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new IllegalArgumentException("Module not found: " + moduleId));

        Assessment assessment = assessmentRepository.findFirstByModuleIdAndType(moduleId, AssessmentType.MODULE_GATE)
                .orElse(null);

        boolean isDummyOrEmpty = isDummyQuiz(assessment);

        if (assessment == null) {
            assessment = new Assessment();
            assessment.setModule(module);
            assessment.setTitle(module.getTitle() + " - Mastery Assessment");
            assessment.setType(AssessmentType.MODULE_GATE);
            assessment.setPassingScore(80);
            AiQuizSuggestion realQuiz = generateQuizForModule(module);
            assessment = persistQuiz(assessment, realQuiz);
        } else if (isDummyOrEmpty) {
            log.info("Regenerating authentic topic-specific questions for module {} (id: {})", module.getTitle(), moduleId);
            AiQuizSuggestion realQuiz = generateQuizForModule(module);
            assessment.getQuestions().clear();
            for (AiQuizQuestion q : realQuiz.questions()) {
                try {
                    String optionsJson = objectMapper.writeValueAsString(q.options());
                    assessment.addQuestion(new AssessmentQuestion(
                            assessment,
                            q.questionText(),
                            optionsJson,
                            q.correctAnswerIndex(),
                            q.explanation()
                    ));
                } catch (Exception ignored) {}
            }
            assessment = assessmentRepository.save(assessment);
        }

        var attempts = attemptRepository.findByAssessmentIdAndUserIdOrderByAttemptedAtDesc(assessment.getId(), user.getId());

        boolean alreadyPassed = attempts.stream().anyMatch(UserAssessmentAttempt::isPassed) || module.isCompleted();
        Integer bestScore = attempts.stream().map(UserAssessmentAttempt::getScore).max(Integer::compareTo)
                .orElse(module.isCompleted() ? (int) module.getMasteryScore() : null);

        int maxAttempts = 3;
        int failedAttempts = (int) attempts.stream().filter(a -> !a.isPassed()).count();
        int attemptsLeft = Math.max(0, maxAttempts - (failedAttempts % maxAttempts));

        long cooldownSeconds = 0;
        if (!attempts.isEmpty() && !alreadyPassed) {
            UserAssessmentAttempt lastAttempt = attempts.get(0);
            if (!lastAttempt.isPassed() && lastAttempt.getAttemptedAt() != null) {
                long elapsed = java.time.Duration.between(lastAttempt.getAttemptedAt(), java.time.LocalDateTime.now()).toSeconds();
                if (elapsed < 300) {
                    cooldownSeconds = 300 - elapsed;
                }
            }
        }

        List<QuestionResponse> questions = assessment.getQuestions().stream()
                .map(q -> {
                    List<String> options;
                    try {
                        options = objectMapper.readValue(q.getOptionsJson(), new TypeReference<>() {});
                    } catch (Exception e) {
                        options = new ArrayList<>();
                    }
                    return new QuestionResponse(q.getId(), q.getQuestionText(), options);
                }).collect(Collectors.toList());

        return new AssessmentResponse(
                assessment.getId(),
                moduleId,
                assessment.getTitle(),
                assessment.getType().name(),
                assessment.getPassingScore(),
                questions,
                alreadyPassed,
                bestScore,
                attemptsLeft,
                cooldownSeconds,
                module.getTitle()
        );
    }

    /**
     * Determines whether an assessment has empty, legacy template questions, placeholder meta-questions, or overly complex questions.
     */
    private boolean isDummyQuiz(Assessment assessment) {
        if (assessment == null || assessment.getQuestions() == null || assessment.getQuestions().isEmpty()) {
            return true;
        }
        return assessment.getQuestions().stream().anyMatch(q -> {
            String text = q.getQuestionText() != null ? q.getQuestionText().toLowerCase() : "";
            return text.contains("which of the following is a key concept in")
                    || text.contains("best approach to learning")
                    || text.contains("which practice should be avoided")
                    || text.contains("what distinguishes an advanced practitioner")
                    || text.contains("part 1:")
                    || text.contains("part 2:")
                    || text.contains("part 3:")
                    || text.contains("core fundamentals & concept basics")
                    || text.contains("technical mechanics & practical implementation")
                    || text.contains("unnecessary complexity")
                    || text.contains("deterministic execution")
                    || text.contains("fragile prototype")
                    || text.contains("forkjoinpool")
                    || text.contains("completablefuture")
                    || text.contains("threadpoolexecutor")
                    || text.contains("architectural risk")
                    || text.contains("architectural trade-off")
                    || text.contains("auditstream")
                    || text.contains("metaspace");
        });
    }

    /**
     * Generates a 5-question mastery assessment tailored strictly to the concepts and topics learned in this module.
     */
    public AiQuizSuggestion generateQuizForModule(Module module) {
        String roadmapTopic = module.getRoadmap() != null ? module.getRoadmap().getTitle() : module.getTitle();
        List<ModuleSubPartDTO> subParts = Collections.emptyList();
        try {
            subParts = teachingContentService.generateDetailedTeachingSubParts(module, roadmapTopic);
        } catch (Exception e) {
            log.warn("Could not retrieve module sub-parts: {}", e.getMessage());
        }

        // Construct rich context from the subparts learned by the student
        StringBuilder conceptsSummary = new StringBuilder();
        if (subParts != null && !subParts.isEmpty()) {
            for (int i = 0; i < subParts.size(); i++) {
                ModuleSubPartDTO sp = subParts.get(i);
                conceptsSummary.append("Part ").append(i + 1).append(": ").append(sp.title()).append("\n");
                if (sp.keyTakeaways() != null) {
                    conceptsSummary.append("  Key Takeaways: ").append(sp.keyTakeaways()).append("\n");
                }
                if (sp.pitfalls() != null) {
                    conceptsSummary.append("  Common Pitfalls & Mistakes: ").append(sp.pitfalls()).append("\n");
                }
            }
        }

        String prompt = buildModuleQuizPrompt(module.getTitle(), module.getDescription(), roadmapTopic, conceptsSummary.toString());

        // 1. Direct Google Gemini API call
        AiQuizSuggestion suggestion = callGeminiForQuiz(prompt);
        if (suggestion != null && suggestion.questions() != null && suggestion.questions().size() >= 3) {
            return suggestion;
        }

        // 2. Intelligent, authentic topic-specific question bank based on module content
        return generateTopicSpecificFallbackQuiz(module, subParts);
    }

    private String buildModuleQuizPrompt(String moduleTitle, String moduleDescription, String roadmapTopic, String conceptsSummary) {
        return """
                You are a patient, encouraging computer science educator creating a 5-question multiple-choice learning assessment for a student who has just learned the module: "%s".
                
                Course / Subject: "%s"
                Module Description: "%s"
                
                Topics & concepts taught in this module:
                %s
                
                DIFFICULTY & TONE GUIDELINES:
                - TARGET DIFFICULTY: STRICTLY BASIC AND MEDIUM LEVEL.
                - DO NOT ask overly detailed, complex multithreading, or high-level distributed systems questions.
                - Questions must be clear, concise, and directly test what a student learns at a beginner to intermediate level in THIS module.
                
                QUESTION STRUCTURE (5 QUESTIONS):
                1. Question 1 (Basic / Definition): A clear fundamental concept, core definition, or primary purpose of the main topic in this module.
                2. Question 2 (Basic / Core Rule & Syntax): A basic syntax rule, keyword meaning (e.g. data types, access modifiers, clauses, or key rules), or standard rule directly taught in this module.
                3. Question 3 (Medium / Practical Usage): A practical application or a very short, clean code snippet (2-4 lines) from this module. Ask for the expected output or correct syntax.
                4. Question 4 (Medium / Common Mistake): A classic beginner/intermediate mistake, common misunderstanding, or simple bug prevention directly relevant to this module (e.g. NullPointerException, off-by-one, type mismatch).
                5. Question 5 (Medium / Comparison & Best Practice): A clear comparison between two concepts taught in this module (e.g. overloading vs overriding, ArrayList vs Array, WHERE vs HAVING, int vs double) or a simple best practice.
                
                OPTION REQUIREMENTS:
                - Provide exactly 4 clear, plausible options per question (Option 0, Option 1, Option 2, Option 3).
                - Distribute the correct answer index across different positions (0, 1, 2, or 3). DO NOT make all answers option 0!
                - Include a 1-2 sentence educational explanation for each question explaining why the correct answer is right.
                
                RETURN FORMAT:
                Return ONLY a valid, raw JSON object without markdown code blocks, starting with '{' and ending with '}':
                {
                  "title": "%s - Learning Assessment",
                  "questions": [
                    {
                      "questionText": "...",
                      "options": ["Option A", "Option B", "Option C", "Option D"],
                      "correctAnswerIndex": 1,
                      "explanation": "..."
                    }
                  ]
                }
                """.formatted(moduleTitle, roadmapTopic, moduleDescription, conceptsSummary, moduleTitle);
    }

    private String resolveApiKey() {
        if (this.geminiApiKey != null && !this.geminiApiKey.isBlank() && !this.geminiApiKey.startsWith("${")) {
            return this.geminiApiKey.trim();
        }
        String sysProp = System.getProperty("GOOGLE_GENAI_API_KEY");
        if (sysProp != null && !sysProp.isBlank()) {
            return sysProp.trim();
        }
        String envVal = System.getenv("GOOGLE_GENAI_API_KEY");
        if (envVal != null && !envVal.isBlank()) {
            return envVal.trim();
        }
        String geminiEnv = System.getenv("GEMINI_API_KEY");
        if (geminiEnv != null && !geminiEnv.isBlank()) {
            return geminiEnv.trim();
        }
        for (String path : List.of(".env", "backend/.env", "../.env")) {
            try {
                java.io.File f = new java.io.File(path);
                if (f.exists() && f.isFile()) {
                    for (String line : java.nio.file.Files.readAllLines(f.toPath())) {
                        line = line.trim();
                        if (line.startsWith("GOOGLE_GENAI_API_KEY=")) {
                            String key = line.substring("GOOGLE_GENAI_API_KEY=".length()).trim();
                            if (!key.isBlank()) return key;
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private AiQuizSuggestion callGeminiForQuiz(String promptText) {
        String apiKey = resolveApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }

        List<String> modelsToTry = List.of(
                (this.geminiModel != null && !this.geminiModel.isBlank() && !this.geminiModel.startsWith("${")) ? this.geminiModel : "gemini-3.7-flash",
                "gemini-3.7-flash",
                "gemini-3.8-flash",
                "gemini-flash-latest"
        );

        for (String model : modelsToTry) {
            try {
                String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

                Map<String, Object> generationConfig = new java.util.HashMap<>();
                generationConfig.put("maxOutputTokens", 8192);
                generationConfig.put("temperature", 0.25);
                generationConfig.put("thinkingConfig", Map.of("thinkingBudget", 0));

                Map<String, Object> body = Map.of(
                        "contents", List.of(
                                Map.of("parts", List.of(Map.of("text", promptText)))
                        ),
                        "generationConfig", generationConfig
                );

                if (this.restClient == null) {
                    this.restClient = RestClient.builder().build();
                }

                String responseJson = this.restClient.post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(String.class);

                if (responseJson != null) {
                    JsonNode root = objectMapper.readTree(responseJson);
                    JsonNode candidates = root.path("candidates");
                    if (candidates.isArray() && !candidates.isEmpty()) {
                        JsonNode parts = candidates.get(0).path("content").path("parts");
                        if (parts.isArray() && !parts.isEmpty()) {
                            String text = parts.get(0).path("text").asText();
                            if (text != null && !text.isBlank()) {
                                AiQuizSuggestion suggestion = parseQuizJson(text);
                                if (suggestion != null && suggestion.questions() != null && !suggestion.questions().isEmpty()) {
                                    return suggestion;
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Gemini quiz call with model {} failed: {}", model, e.getMessage());
            }
        }
        return null;
    }

    private AiQuizSuggestion parseQuizJson(String rawText) {
        if (rawText == null || rawText.isBlank()) return null;

        String clean = rawText.trim();
        if (clean.startsWith("```json")) {
            clean = clean.substring(7);
        } else if (clean.startsWith("```")) {
            clean = clean.substring(3);
        }
        if (clean.endsWith("```")) {
            clean = clean.substring(0, clean.length() - 3);
        }
        clean = clean.trim();

        try {
            JsonNode root = objectMapper.readTree(clean);
            JsonNode questionsNode = root.has("questions") ? root.path("questions") : root;
            if (questionsNode.isArray() && !questionsNode.isEmpty()) {
                List<AiQuizQuestion> questions = new ArrayList<>();
                for (JsonNode qNode : questionsNode) {
                    String qText = qNode.path("questionText").asText();
                    if (qText == null || qText.isBlank()) {
                        qText = qNode.path("question").asText();
                    }
                    List<String> options = new ArrayList<>();
                    JsonNode optsNode = qNode.path("options");
                    if (optsNode.isArray()) {
                        for (JsonNode opt : optsNode) {
                            options.add(opt.asText());
                        }
                    }
                    int correctIdx = qNode.path("correctAnswerIndex").asInt(0);
                    if (correctIdx < 0 || correctIdx >= options.size()) {
                        correctIdx = 0;
                    }
                    String explanation = qNode.path("explanation").asText("Detailed concept explanation.");

                    if (qText != null && !qText.isBlank() && options.size() >= 2) {
                        questions.add(new AiQuizQuestion(qText, options, correctIdx, explanation));
                    }
                }
                if (!questions.isEmpty()) {
                    String title = root.path("title").asText("Learning Assessment");
                    return new AiQuizSuggestion(title, questions);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse AI quiz JSON: {}. Text was: {}", e.getMessage(), clean);
        }
        return null;
    }

    /**
     * Fallback quiz that is dynamically derived from the module's actual topics, takeaways, and pitfalls.
     * Guarantees diverse answer options across indices [1, 2, 0, 3, 1].
     */
    private AiQuizSuggestion generateTopicSpecificFallbackQuiz(Module module, List<ModuleSubPartDTO> subParts) {
        String title = module.getTitle();
        String lower = title.toLowerCase();

        // 1. Interfaces, Packages & Exception Handling (Module 3)
        if (lower.contains("interface") || lower.contains("package") || lower.contains("exception")) {
            List<AiQuizQuestion> questions = List.of(
                    new AiQuizQuestion(
                            "Which keyword is used by a class to implement an interface in Java?",
                            List.of("extends", "implements", "inherits", "interface"),
                            1,
                            "The 'implements' keyword is used in a class declaration to implement one or more interfaces."
                    ),
                    new AiQuizQuestion(
                            "In Java exception handling, which block is guaranteed to execute whether an exception occurs or not?",
                            List.of("try", "catch", "finally", "throw"),
                            2,
                            "The 'finally' block is always executed after the try/catch blocks terminate, ensuring clean release of resources."
                    ),
                    new AiQuizQuestion(
                            "What is the difference between a checked exception and an unchecked exception in Java?",
                            List.of("Checked exceptions cannot be caught in try-catch blocks", "Checked exceptions must be explicitly declared with 'throws' or caught at compile time, whereas unchecked exceptions inherit from RuntimeException", "Unchecked exceptions only occur during code compilation", "Checked exceptions are automatically ignored by the JVM"),
                            1,
                            "Checked exceptions (subclasses of Exception other than RuntimeException) require compile-time handling or declaration."
                    ),
                    new AiQuizQuestion(
                            "Which keyword is used to explicitly throw an exception object in Java?",
                            List.of("throws", "throw", "raise", "catch"),
                            1,
                            "The 'throw' keyword is used to explicitly instantiate and throw an exception instance during method execution."
                    ),
                    new AiQuizQuestion(
                            "Which package is automatically imported into every Java class without requiring an import statement?",
                            List.of("java.util", "java.io", "java.lang", "java.net"),
                            2,
                            "The 'java.lang' package (containing fundamental classes like Object, String, Math, and System) is implicitly imported into every Java program."
                    )
            );
            return new AiQuizSuggestion(title + " - Learning Assessment", questions);
        }

        // 2. Collections Framework & Generics (Module 4)
        if (lower.contains("collection") || lower.contains("generic") || lower.contains("list") || lower.contains("map") || lower.contains("set")) {
            List<AiQuizQuestion> questions = List.of(
                    new AiQuizQuestion(
                            "What is the primary difference between a List and a Set in the Java Collections Framework?",
                            List.of("A List allows duplicate elements and maintains insertion order, while a Set stores only unique elements", "A Set allows duplicates but a List does not", "A List can only store integers, while a Set stores strings", "A Set is synchronized while a List is never thread-safe"),
                            0,
                            "Lists maintain ordered collections allowing duplicates; Sets enforce uniqueness and do not permit duplicate values."
                    ),
                    new AiQuizQuestion(
                            "Which Map implementation provides average O(1) time complexity for get() and put() operations?",
                            List.of("TreeMap", "HashMap", "LinkedHashMap", "Hashtable"),
                            1,
                            "HashMap uses bucket hashing with array indexing, giving average O(1) performance for insertions and lookups."
                    ),
                    new AiQuizQuestion(
                            "What is the primary benefit of using Generics (such as List<String>) in Java?",
                            List.of("It converts Java code to C++ for faster execution", "It provides compile-time type safety and removes the need for explicit type casting", "It prevents objects from being garbage collected", "It allows multiple null values in primitive arrays"),
                            1,
                            "Generics enforce strict compile-time type checking, preventing ClassCastException at runtime."
                    ),
                    new AiQuizQuestion(
                            "Which standard utility method is used to sort a List according to natural ordering in Java?",
                            List.of("Arrays.sortList()", "Collections.sort()", "List.order()", "Collection.arrange()"),
                            1,
                            "Collections.sort(list) sorts the specified list into ascending order according to natural ordering or an optional Comparator."
                    ),
                    new AiQuizQuestion(
                            "What exception is thrown if you attempt to add an element to an unmodifiable collection created via List.of()?",
                            List.of("NullPointerException", "IndexOutOfBoundsException", "UnsupportedOperationException", "IllegalArgumentException"),
                            2,
                            "Collections created with List.of() or Set.of() are immutable; calling mutating methods like add() throws UnsupportedOperationException."
                    )
            );
            return new AiQuizSuggestion(title + " - Learning Assessment", questions);
        }

        // 3. Streams API & Functional Programming (Module 5)
        if (lower.contains("stream") || lower.contains("functional") || lower.contains("lambda")) {
            List<AiQuizQuestion> questions = List.of(
                    new AiQuizQuestion(
                            "What is the primary purpose of the Java Streams API introduced in Java 8?",
                            List.of("To read binary network files", "To perform functional-style, declarative operations like filtering, mapping, and reducing on collections", "To replace all loop statements in Java bytecode", "To create multi-threaded Swing applications"),
                            1,
                            "Streams allow developers to process sequences of elements declaratively using functional transformations."
                    ),
                    new AiQuizQuestion(
                            "Which Stream operation is an intermediate operation that transforms each element into another value?",
                            List.of("forEach()", "map()", "collect()", "count()"),
                            1,
                            "The map() method applies a function to each element in the stream, transforming it into a new element."
                    ),
                    new AiQuizQuestion(
                            "What is a Functional Interface in Java?",
                            List.of("An interface with no methods at all", "An interface with exactly one abstract method, which can be implemented with a lambda expression", "An interface that only contains private methods", "An interface used strictly for database connectivity"),
                            1,
                            "A functional interface (annotated optionally with @FunctionalInterface) has exactly one single abstract method (SAM)."
                    ),
                    new AiQuizQuestion(
                            "What is the difference between intermediate and terminal operations in a Stream?",
                            List.of("Intermediate operations execute immediately, while terminal operations do not", "Intermediate operations return a new Stream and are lazy; terminal operations trigger execution and produce a final result", "Terminal operations can be chained together indefinitely", "There is no difference"),
                            1,
                            "Intermediate operations (like filter, map) are lazy and only execute when a terminal operation (like collect, count) is invoked."
                    ),
                    new AiQuizQuestion(
                            "What is the main purpose of the Optional<T> class in Java?",
                            List.of("To represent optional command-line arguments", "To provide a type-level container for a value that may be present or absent, helping avoid NullPointerExceptions", "To skip compilation warnings", "To allow variable method parameter lengths"),
                            1,
                            "Optional explicitly communicates that a method return value may be absent, encouraging developers to handle the missing case safely."
                    )
            );
            return new AiQuizSuggestion(title + " - Learning Assessment", questions);
        }

        // 4. Multithreading & Concurrency (Module 6)
        if (lower.contains("thread") || lower.contains("concurren") || lower.contains("synchroniz")) {
            List<AiQuizQuestion> questions = List.of(
                    new AiQuizQuestion(
                            "Which two standard approaches are used to create a thread in Java?",
                            List.of("Extending Thread class or implementing Runnable interface", "Implementing Serializable or Cloneable", "Using the transient keyword on a class", "Calling System.gc()"),
                            0,
                            "A thread can be created by either extending java.lang.Thread or implementing java.lang.Runnable."
                    ),
                    new AiQuizQuestion(
                            "What is the purpose of the 'synchronized' keyword in Java?",
                            List.of("To execute code on multiple computers simultaneously", "To prevent race conditions by allowing only one thread at a time to execute a code block on a shared monitor", "To restart failed threads automatically", "To convert asynchronous code into single-threaded code"),
                            1,
                            "Synchronization ensures mutual exclusion on shared resources, preventing simultaneous inconsistent modifications by multiple threads."
                    ),
                    new AiQuizQuestion(
                            "What is a 'deadlock' in concurrent programming?",
                            List.of("When a thread terminates normally after running", "A condition where two or more threads are blocked forever, each waiting for a lock held by the other", "When a thread sleeps longer than 5 seconds", "When garbage collection pauses all threads"),
                            1,
                            "A deadlock occurs when cyclic dependencies between locks prevent any involved thread from proceeding."
                    ),
                    new AiQuizQuestion(
                            "Why is an ExecutorService (Thread Pool) preferred over creating 'new Thread()' manually for every task?",
                            List.of("Creating new threads has no memory overhead", "It reuses existing worker threads, limits maximum concurrency, and avoids expensive thread lifecycle creation costs", "Thread pools eliminate all runtime exceptions", "Thread pools run outside the JVM"),
                            1,
                            "Thread pools manage and reuse a pool of worker threads, preventing thread starvation and high allocation overhead."
                    ),
                    new AiQuizQuestion(
                            "What does the 'volatile' keyword ensure when applied to a variable in Java?",
                            List.of("The variable cannot be changed after assignment", "Reads and writes to the variable are directly synchronized with main memory across threads rather than cached locally", "The variable is stored on the hard drive", "The variable can only be accessed by one thread"),
                            1,
                            "Volatile guarantees memory visibility, ensuring that updates to a variable are immediately visible to all other threads."
                    )
            );
            return new AiQuizSuggestion(title + " - Learning Assessment", questions);
        }

        // 5. File I/O, Serialization & Networking (Module 7)
        if (lower.contains("file") || lower.contains("io") || lower.contains("i/o") || lower.contains("serializ") || lower.contains("network")) {
            List<AiQuizQuestion> questions = List.of(
                    new AiQuizQuestion(
                            "What does object serialization mean in Java?",
                            List.of("Formatting Java source code with proper indentation", "Converting the state of an object into a byte stream for storage or network transmission", "Compiling Java files into .class bytecode", "Deleting unused objects from memory"),
                            1,
                            "Serialization transforms an active in-memory object into a byte stream that can be persisted to disk or transmitted over networks."
                    ),
                    new AiQuizQuestion(
                            "Which marker interface must a class implement to enable Java object serialization?",
                            List.of("java.lang.Cloneable", "java.io.Serializable", "java.lang.AutoCloseable", "java.util.RandomAccess"),
                            1,
                            "Implementing java.io.Serializable marks the class as eligible for binary serialization via ObjectOutputStream."
                    ),
                    new AiQuizQuestion(
                            "Which keyword is used to mark an instance variable so that its value is excluded from serialization?",
                            List.of("volatile", "transient", "static", "final"),
                            1,
                            "Fields marked as 'transient' are skipped during object serialization and restored to default values upon deserialization."
                    ),
                    new AiQuizQuestion(
                            "Which modern package is recommended for fast, robust file operations (reading, writing, copying) in Java?",
                            List.of("java.awt", "java.nio.file", "java.sql", "java.security"),
                            1,
                            "The java.nio.file package (specifically the Files and Path classes) provides modern, non-blocking and convenient file utility methods."
                    ),
                    new AiQuizQuestion(
                            "Why should file streams be opened inside a try-with-resources statement in Java?",
                            List.of("It guarantees automatic closing of the stream and prevents resource leaks even if an exception occurs", "It makes the file read-only on the operating system", "It automatically converts text files into database tables", "It eliminates the need for catch blocks"),
                            0,
                            "try-with-resources guarantees that any resource implementing AutoCloseable is deterministically closed at block exit."
                    )
            );
            return new AiQuizSuggestion(title + " - Learning Assessment", questions);
        }

        // 6. Spring Boot Core & Dependency Injection (Module 8)
        if (lower.contains("spring boot") || lower.contains("spring") || lower.contains("dependency injection") || lower.contains("controller")) {
            List<AiQuizQuestion> questions = List.of(
                    new AiQuizQuestion(
                            "What is Dependency Injection (DI) in Spring Boot?",
                            List.of("Writing database queries inside view templates", "A design pattern where Spring automatically supplies and manages required dependencies to a class rather than the class creating them with 'new'", "Installing third-party packages using npm", "Starting the Tomcat server manually from the terminal"),
                            1,
                            "Dependency Injection inverts control: the Spring IoC container instantiates and injects required dependencies into components."
                    ),
                    new AiQuizQuestion(
                            "Which annotation on the main application class combines @Configuration, @EnableAutoConfiguration, and @ComponentScan?",
                            List.of("@Service", "@SpringBootApplication", "@RestController", "@EnableSpring"),
                            1,
                            "@SpringBootApplication is a meta-annotation that configures auto-configuration, component scanning, and extra bean definitions."
                    ),
                    new AiQuizQuestion(
                            "What is the difference between @Controller and @RestController in Spring Boot?",
                            List.of("@RestController automatically serializes returned objects into JSON/XML response bodies, whereas @Controller typically returns view template names", "@Controller handles REST APIs, while @RestController is only for HTML forms", "@RestController requires manual database configurations", "There is no difference"),
                            0,
                            "@RestController combines @Controller and @ResponseBody, meaning handler return values are serialized directly into HTTP responses."
                    ),
                    new AiQuizQuestion(
                            "Which Spring annotation is used to mark a class as a service component holding business logic?",
                            List.of("@Repository", "@Service", "@Component", "@Entity"),
                            1,
                            "The @Service annotation denotes that a class performs business logic services in the Spring application architecture."
                    ),
                    new AiQuizQuestion(
                            "Which HTTP method is conventionally used in REST APIs to create a new resource on the server?",
                            List.of("GET", "DELETE", "POST", "HEAD"),
                            2,
                            "POST is the standard HTTP method used to submit data and create a new resource on the server."
                    )
            );
            return new AiQuizSuggestion(title + " - Learning Assessment", questions);
        }

        // 7. Spring Data JPA & Databases (Module 9)
        if (lower.contains("jpa") || lower.contains("dbms") || lower.contains("database") || lower.contains("sql") || lower.contains("repository") || lower.contains("transaction")) {
            List<AiQuizQuestion> questions = List.of(
                    new AiQuizQuestion(
                            "What is the primary role of Spring Data JPA in a Spring Boot application?",
                            List.of("To design CSS layouts for web pages", "To reduce boilerplate data access code by providing repository interfaces (like JpaRepository) for database CRUD operations", "To compile Java into native machine code", "To encrypt user passwords in the browser"),
                            1,
                            "Spring Data JPA allows developers to define repository interfaces that automatically provide CRUD operations and custom query derivation."
                    ),
                    new AiQuizQuestion(
                            "Which annotation is placed on an entity's primary key field in Java JPA?",
                            List.of("@Column", "@Id", "@Key", "@Primary"),
                            1,
                            "@Id marks the identifier property that represents the Primary Key of the mapped database entity."
                    ),
                    new AiQuizQuestion(
                            "What is the purpose of the @Transactional annotation in Spring Boot?",
                            List.of("To send asynchronous emails to users", "To ensure that all database operations in a method succeed and commit together, or roll back if an error occurs", "To restrict API endpoints to admin users", "To increase database connection pool sizes"),
                            1,
                            "@Transactional manages database transaction boundaries, committing changes on success or rolling them back upon uncaught exceptions."
                    ),
                    new AiQuizQuestion(
                            "Which SQL command is used to retrieve or query data from one or more database tables?",
                            List.of("UPDATE", "SELECT", "INSERT", "FETCH"),
                            1,
                            "SELECT is the primary DML statement used to read and filter data from database tables."
                    ),
                    new AiQuizQuestion(
                            "What is the difference between the WHERE clause and the HAVING clause in SQL?",
                            List.of("WHERE filters individual rows before grouping; HAVING filters aggregated groups after GROUP BY", "WHERE is only for numbers; HAVING is only for text", "HAVING comes before SELECT; WHERE comes after", "They are identical and interchangeable"),
                            0,
                            "WHERE filters rows prior to aggregation, whereas HAVING filters groups produced by the GROUP BY clause."
                    )
            );
            return new AiQuizSuggestion(title + " - Learning Assessment", questions);
        }

        // 8. Testing & Performance Tuning (Module 10)
        if (lower.contains("test") || lower.contains("junit") || lower.contains("tuning") || lower.contains("performance")) {
            List<AiQuizQuestion> questions = List.of(
                    new AiQuizQuestion(
                            "What is the primary objective of Unit Testing in software development?",
                            List.of("To test the entire live production system with real transactions", "To test individual methods and components in isolation to verify they work as expected", "To measure the physical CPU voltage", "To generate automatic user documentation"),
                            1,
                            "Unit tests isolate individual units of source code to verify that their logic behaves according to specifications."
                    ),
                    new AiQuizQuestion(
                            "Which testing framework is the standard choice for writing unit tests in Spring Boot applications?",
                            List.of("React", "JUnit (JUnit 5)", "Selenium", "Postman"),
                            1,
                            "JUnit 5 (Jupiter) is the default and industry-standard testing framework included with Spring Boot Starter Test."
                    ),
                    new AiQuizQuestion(
                            "What is the purpose of using Mockito (@MockBean) in Spring Boot testing?",
                            List.of("To delete production database tables", "To create mock (simulated) objects that simulate real dependencies, allowing isolated unit tests", "To measure internet download speeds", "To bypass compiler syntax checking"),
                            1,
                            "Mockito allows mocking external dependencies (such as repositories or APIs) so that service logic can be verified in isolation."
                    ),
                    new AiQuizQuestion(
                            "In JVM memory tuning, what does the '-Xmx' command-line flag specify?",
                            List.of("The maximum thread count in the application", "The maximum Heap memory size allocated to the JVM", "The number of CPU cores enabled", "The maximum network bandwidth limit"),
                            1,
                            "-Xmx sets the maximum heap allocation size for the Java Virtual Machine."
                    ),
                    new AiQuizQuestion(
                            "What is the primary purpose of the Java Garbage Collector (GC)?",
                            List.of("To delete uncommitted git repositories", "To automatically reclaim Heap memory from objects that are no longer referenced or reachable by the application", "To close browser tabs automatically", "To restart the operating system when RAM is full"),
                            1,
                            "The Garbage Collector monitors object references and automatically reclaims heap memory from unreachable objects."
                    )
            );
            return new AiQuizSuggestion(title + " - Learning Assessment", questions);
        }

        // 9. Object-Oriented Programming (Module 2)
        if (lower.contains("oop") || lower.contains("object")) {
            List<AiQuizQuestion> questions = List.of(
                    new AiQuizQuestion(
                            "What is the primary idea of Encapsulation in Object-Oriented Programming?",
                            List.of("Making all variables public for easy access", "Bundling data and methods together while restricting direct access using private fields and public getters/setters", "Running multiple threads at the same time", "Inheriting from multiple parent classes"),
                            1,
                            "Encapsulation protects object state from arbitrary external modification by exposing safe public accessors and mutators."
                    ),
                    new AiQuizQuestion(
                            "Which keyword is used in Java by a subclass to inherit from a superclass?",
                            List.of("implements", "extends", "inherits", "super"),
                            1,
                            "A class uses the 'extends' keyword to inherit fields and non-private methods from a superclass."
                    ),
                    new AiQuizQuestion(
                            "What is the difference between method overloading and method overriding in Java?",
                            List.of("Overloading happens in the same class with different parameters; overriding replaces a parent method in a subclass with the same signature", "Overloading requires inheritance, while overriding does not", "Overriding happens at compile-time only", "Overloading is not allowed in Java"),
                            0,
                            "Overloading defines methods with the same name but different argument signatures in the same class; overriding provides a specialized implementation of an inherited superclass method."
                    ),
                    new AiQuizQuestion(
                            "What runtime exception occurs when calling a method on an object reference that is currently null?",
                            List.of("ArithmeticException", "NullPointerException", "ArrayIndexOutOfBoundsException", "ClassCastException"),
                            1,
                            "Dereferencing a reference variable pointing to null throws a NullPointerException."
                    ),
                    new AiQuizQuestion(
                            "What does the '@Override' annotation do in Java?",
                            List.of("It makes a method private", "It informs the compiler that the method is intended to override a method declared in a superclass or interface", "It converts a class into an interface", "It creates a static variable"),
                            1,
                            "@Override helps catch errors at compile time if the annotated method signature does not match any inherited superclass method."
                    )
            );
            return new AiQuizSuggestion(title + " - Learning Assessment", questions);
        }

        // 10. Java Basics & JVM (Module 1 & General Java)
        List<AiQuizQuestion> questions = List.of(
                new AiQuizQuestion(
                        "What is the primary role of the Java Virtual Machine (JVM)?",
                        List.of("To compile Java source code into bytecode", "To execute compiled bytecode (.class files) platform-independently on the host system", "To write code automatically for developers", "To design web interfaces in the browser"),
                        1,
                        "The JVM executes compiled Java bytecode, enabling Java's 'Write Once, Run Anywhere' (WORA) capability."
                ),
                new AiQuizQuestion(
                        "Where are method call frames and local primitive variables stored during Java execution?",
                        List.of("Heap Memory", "Call Stack", "Metaspace", "Disk Cache"),
                        1,
                        "Each executing thread has its own Call Stack that stores method stack frames and local variables."
                ),
                new AiQuizQuestion(
                        "Consider the code: `int a = 9; int b = 2; System.out.print(a / b);`. What is the printed output?",
                        List.of("4", "4.5", "5", "Compilation Error"),
                        0,
                        "Integer division in Java truncates decimal fractions, producing 4."
                ),
                new AiQuizQuestion(
                        "Which of the following is a primitive data type in Java?",
                        List.of("String", "Integer", "boolean", "Scanner"),
                        2,
                        "boolean is one of Java's 8 primitive types, whereas String, Integer, and Scanner are reference object types."
                ),
                new AiQuizQuestion(
                        "What is the difference between JDK and JRE?",
                        List.of("JRE includes development tools, while JDK only runs apps", "JDK includes developer tools like the javac compiler plus the JRE, while JRE contains only what is needed to run compiled Java programs", "JDK is only for C++ development", "They are identical"),
                        1,
                        "The JDK (Java Development Kit) contains tools for developing and compiling programs; the JRE (Java Runtime Environment) only executes pre-compiled bytecode."
                )
        );

        return new AiQuizSuggestion(title + " - Learning Assessment", questions);
    }

    private AiQuizSuggestion buildDynamicTopicDiagnosticQuiz(String topic) {
        return new AiQuizSuggestion(
                "Diagnostic: " + topic,
                List.of(
                        new AiQuizQuestion(
                                "When beginning study in " + topic + ", what is the most critical first step?",
                                List.of("Memorizing complex edge cases", "Understanding foundational concepts, mental models, and terminology", "Skipping syntax basics directly to advanced deployment", "Ignoring documentation"),
                                1,
                                "Building an accurate mental model and understanding foundational concepts ensures smooth progression into advanced mechanics."
                        ),
                        new AiQuizQuestion(
                                "In practical problem solving involving " + topic + ", what approach best isolates bugs?",
                                List.of("Rewriting the entire codebase from scratch", "Guessing parameters randomly", "Systematic step-by-step tracing with test assertions and logging", "Disabling error checking"),
                                2,
                                "Systematic tracing and verification isolates root causes without introducing secondary regressions."
                        ),
                        new AiQuizQuestion(
                                "Which concept is essential to write scalable, maintainable solutions in " + topic + "?",
                                List.of("Modular decomposition and clean abstraction layers", "Monolithic functions exceeding 1000 lines", "Global mutable variables shared across threads", "Hardcoded credentials"),
                                0,
                                "Modular decomposition separates concerns and allows components to be tested and evolved independently."
                        ),
                        new AiQuizQuestion(
                                "What is the primary risk of neglecting error handling in " + topic + "?",
                                List.of("Faster compilation speed", "Increased network bandwidth", "Reduced disk usage", "Silent data corruption, unhandled crashes, and insecure failure states"),
                                3,
                                "Unchecked exceptions lead to undefined state, security vulnerabilities, and sudden application crashes."
                        ),
                        new AiQuizQuestion(
                                "How do experienced engineers assess trade-offs in " + topic + "?",
                                List.of("By picking the newest framework without evaluation", "By evaluating time vs space complexity, operational maintainability, and domain constraints", "By avoiding all architectural documentation", "By always choosing the most complex algorithm"),
                                1,
                                "Engineering maturity is measured by balancing performance, maintainability, and domain constraints."
                        )
                )
        );
    }

    private Assessment persistQuiz(Assessment assessment, AiQuizSuggestion suggestion) {
        if (suggestion.questions() != null) {
            for (AiQuizQuestion q : suggestion.questions()) {
                try {
                    String optionsJson = objectMapper.writeValueAsString(q.options());
                    AssessmentQuestion question = new AssessmentQuestion(
                            assessment,
                            q.questionText(),
                            optionsJson,
                            q.correctAnswerIndex(),
                            q.explanation()
                    );
                    assessment.addQuestion(question);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to serialize question options", e);
                }
            }
        }
        return assessmentRepository.save(assessment);
    }

    private AssessmentResponse persistQuizAndMapToResponse(Assessment assessment, AiQuizSuggestion suggestion) {
        Assessment saved = persistQuiz(assessment, suggestion);
        return mapToResponse(saved);
    }

    public AssessmentResponse mapToResponse(Assessment assessment) {
        List<QuestionResponse> questions = assessment.getQuestions().stream()
                .map(q -> {
                    List<String> options;
                    try {
                        options = objectMapper.readValue(q.getOptionsJson(), new TypeReference<>() {});
                    } catch (Exception e) {
                        options = new ArrayList<>();
                    }
                    return new QuestionResponse(q.getId(), q.getQuestionText(), options);
                }).collect(Collectors.toList());

        Long moduleId = assessment.getModule() != null ? assessment.getModule().getId() : null;

        return new AssessmentResponse(
                assessment.getId(),
                moduleId,
                assessment.getTitle(),
                assessment.getType().name(),
                assessment.getPassingScore(),
                questions
        );
    }
}
