package com.asjad.studygen.service;

import com.asjad.studygen.config.PromptRegistry;
import com.asjad.studygen.dto.chat.*;
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatSessionService {

    private final ChatSessionRepository sessionRepository;
    private final StudyChatMessageRepository messageRepository;
    private final RoadmapRepository roadmapRepository;
    private final ModuleRepository moduleRepository;
    private final UploadedDocumentRepository documentRepository;
    private final DocumentProcessingService documentService;
    private final UserActivityService userActivityService;
    private final ChatClient.Builder chatClientBuilder;
    private final PromptRegistry promptRegistry;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${spring.ai.google.genai.api-key:}")
    private String geminiApiKey;

    @Value("${spring.ai.google.genai.chat.options.model:gemini-1.5-flash}")
    private String geminiModel;

    private ChatClient chatClient;
    private RestClient restClient;

    @PostConstruct
    void init() {
        this.restClient = RestClient.builder().build();
        try {
            this.chatClient = chatClientBuilder.build();
        } catch (Exception e) {
            log.warn("Could not initialize ChatClient in ChatSessionService: {}", e.getMessage());
        }
    }

    @Transactional
    public List<ChatSessionResponse> getUserSessions(User user) {
        List<ChatSession> sessions = sessionRepository.findByUserIdOrderByLastActivityAtDesc(user.getId());
        if (sessions.isEmpty()) {
            ChatSession defaultSession = new ChatSession(user, "General Study Consultation", "GENERAL");
            ChatSession saved = sessionRepository.save(defaultSession);

            StudyChatMessage welcome = new StudyChatMessage(
                    user,
                    saved,
                    null,
                    "ASSISTANT",
                    "Hello! I am your StudyGen AI Assistant. I can help synthesize concepts across your roadmaps, explain programming and system design topics, and cite uploaded documentation. What would you like to explore today?",
                    "[]"
            );
            messageRepository.save(welcome);

            return List.of(mapToResponse(saved));
        }

        return sessions.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional
    public ChatSessionResponse createSession(User user, CreateChatSessionRequest request) {
        String title = request.title() != null && !request.title().isBlank()
                ? request.title().trim()
                : "New Study Session";

        String contextTag = request.contextTag() != null && !request.contextTag().isBlank()
                ? request.contextTag().toUpperCase()
                : "GENERAL";

        ChatSession session = new ChatSession(user, title, contextTag);

        if (request.moduleId() != null) {
            moduleRepository.findById(request.moduleId()).ifPresent(m -> {
                session.setModule(m);
                session.setRoadmap(m.getRoadmap());
                if (title.equals("New Study Session")) {
                    session.setTitle("Module: " + m.getTitle());
                }
            });
        } else if (request.roadmapId() != null) {
            roadmapRepository.findById(request.roadmapId()).ifPresent(r -> {
                session.setRoadmap(r);
                if (title.equals("New Study Session")) {
                    session.setTitle("Roadmap: " + r.getTitle());
                }
            });
        }

        ChatSession saved = sessionRepository.save(session);

        String greeting = "Hello! I am ready to assist you"
                + (saved.getModule() != null ? " with **" + saved.getModule().getTitle() + "**." : " with your study goals.")
                + " Ask me any question, request code examples, or ask for analogies!";

        StudyChatMessage welcome = new StudyChatMessage(
                user,
                saved,
                saved.getModule() != null ? saved.getModule().getId() : null,
                "ASSISTANT",
                greeting,
                "[]"
        );
        messageRepository.save(welcome);

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public ChatSessionDetailResponse getSessionDetail(User user, Long sessionId) {
        ChatSession session = sessionRepository.findById(sessionId)
                .filter(s -> s.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Session not found or unauthorized"));

        List<StudyChatMessage> messages = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);

        List<ChatMessageItemDTO> messageDTOs = messages.stream().map(m -> {
            List<ChatCitationDTO> citations = Collections.emptyList();
            if (m.getCitationsJson() != null && !m.getCitationsJson().isBlank()) {
                try {
                    citations = objectMapper.readValue(m.getCitationsJson(), new TypeReference<List<ChatCitationDTO>>() {});
                } catch (Exception ignored) {}
            }
            return new ChatMessageItemDTO(
                    m.getId(),
                    m.getSender(),
                    m.getMessageText(),
                    citations,
                    m.getCreatedAt().toString()
            );
        }).collect(Collectors.toList());

        return new ChatSessionDetailResponse(
                session.getId(),
                session.getTitle(),
                session.getContextTag(),
                session.getLastActivityAt().toString(),
                session.getModule() != null ? session.getModule().getId() : null,
                session.getModule() != null ? session.getModule().getTitle() : null,
                messageDTOs
        );
    }

    @Transactional
    public void deleteSession(User user, Long sessionId) {
        ChatSession session = sessionRepository.findById(sessionId)
                .filter(s -> s.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Session not found or unauthorized"));

        messageRepository.deleteBySessionId(sessionId);
        sessionRepository.delete(session);
    }

    public SseEmitter streamSessionMessage(User user, Long sessionId, ChatSessionMessageRequest request) {
        return streamChatReply(user, sessionId, request);
    }

    public SseEmitter streamChatReply(User user, Long sessionId, ChatSessionMessageRequest request) {
        SseEmitter emitter = new SseEmitter(180_000L);

        ChatSession session = sessionRepository.findById(sessionId)
                .filter(s -> s.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Session not found or unauthorized"));

        CompletableFuture.runAsync(() -> {
            try {
                // 1. Save User Message
                StudyChatMessage userMsg = new StudyChatMessage(
                        user,
                        session,
                        request.moduleId(),
                        "USER",
                        request.message().trim(),
                        "[]"
                );
                messageRepository.save(userMsg);

                session.setLastActivityAt(LocalDateTime.now());
                if (session.getTitle() != null && session.getTitle().startsWith("New Study")) {
                    String cleanSnippet = request.message().trim();
                    if (cleanSnippet.length() > 30) {
                        cleanSnippet = cleanSnippet.substring(0, 27) + "...";
                    }
                    session.setTitle(cleanSnippet);
                }
                sessionRepository.save(session);

                // 2. Generate Citations
                String query = request.message().trim();
                List<ChatCitationDTO> citations = generateCitations(user, session, request);

                // 3. Generate AI Answer
                String fullAnswer = callAiForResponse(user, session, request, query);

                // 4. Stream tokens smoothly
                String[] tokens = fullAnswer.split("(?<=\\s)|(?<=\\n)");
                for (String token : tokens) {
                    Map<String, String> data = Map.of("token", token);
                    emitter.send(SseEmitter.event().data(data));
                    try {
                        Thread.sleep(14); // smooth streaming cadence
                    } catch (InterruptedException ignored) {}
                }

                // 5. Send Citations event
                emitter.send(SseEmitter.event().name("citations").data(citations));

                // 6. Send [DONE] event
                emitter.send(SseEmitter.event().data("[DONE]"));
                emitter.complete();

                // 7. Persist Assistant Message
                String citJson = "[]";
                try {
                    citJson = objectMapper.writeValueAsString(citations);
                } catch (Exception ignored) {}

                StudyChatMessage assistantMsg = new StudyChatMessage(
                        user,
                        session,
                        request.moduleId(),
                        "ASSISTANT",
                        fullAnswer,
                        citJson
                );
                messageRepository.save(assistantMsg);

                userActivityService.logActivity(user, "AI_CHAT_MESSAGE", 2);

            } catch (Exception e) {
                log.warn("Error during SSE streaming: {}", e.getMessage());
                try {
                    emitter.send(SseEmitter.event().data(Map.of("token", "I encountered an interruption. Please try asking again.")));
                    emitter.send(SseEmitter.event().data("[DONE]"));
                    emitter.complete();
                } catch (Exception ignored) {}
            }
        });

        return emitter;
    }

    private String callAiForResponse(User user, ChatSession session, ChatSessionMessageRequest request, String query) {
        StringBuilder context = new StringBuilder();
        if (request.moduleId() != null) {
            moduleRepository.findById(request.moduleId()).ifPresent(m -> {
                context.append("Current Module: ").append(m.getTitle()).append("\n");
                if (m.getDescription() != null) {
                    context.append("Module Description: ").append(m.getDescription()).append("\n");
                }
            });
        } else if (session.getModule() != null) {
            context.append("Current Module: ").append(session.getModule().getTitle()).append("\n");
        }

        try {
            List<String> docChunks = documentService.searchRelevantChunks(user.getId(), query, 2);
            if (!docChunks.isEmpty()) {
                context.append("Uploaded Knowledge Context:\n");
                for (String chunk : docChunks) {
                    context.append("- ").append(chunk).append("\n");
                }
            }
        } catch (Exception ignored) {}

        String prompt = promptRegistry.chatSessionTutorReply(context.toString(), query);

        // 1. First attempt: Direct call to Gemini 3.6 Flash
        String geminiReply = callGeminiDirectly(prompt);
        if (geminiReply != null && !geminiReply.isBlank()) {
            return geminiReply;
        }

        // 2. Second attempt: Spring AI ChatClient
        if (this.chatClient != null) {
            try {
                String aiReply = this.chatClient.prompt()
                        .user(prompt)
                        .call()
                        .content();
                if (aiReply != null && !aiReply.isBlank()) {
                    return aiReply;
                }
            } catch (Exception e) {
                log.warn("ChatClient prompt failed: {}", e.getMessage());
            }
        }

        // 3. Third attempt: Intelligent tailored fallback
        return generateFallbackResponse(query);
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
        // Direct parse from local .env files
        for (String path : List.of(".env", "backend/.env", "../.env")) {
            try {
                java.io.File f = new java.io.File(path);
                if (f.exists() && f.isFile()) {
                    for (String line : java.nio.file.Files.readAllLines(f.toPath())) {
                        line = line.trim();
                        if (line.startsWith("GOOGLE_GENAI_API_KEY=")) {
                            String k = line.substring("GOOGLE_GENAI_API_KEY=".length()).trim();
                            if (!k.isBlank()) return k;
                        }
                        if (line.startsWith("GEMINI_API_KEY=")) {
                            String k = line.substring("GEMINI_API_KEY=".length()).trim();
                            if (!k.isBlank()) return k;
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private String callGeminiDirectly(String promptText) {
        try {
            String apiKey = resolveApiKey();
            if (apiKey == null || apiKey.isBlank()) {
                log.info("Gemini API key is not configured. Utilizing native StudyGen study engine.");
                return null;
            }

            // Models supported by Gemini v1beta endpoint
            List<String> modelsToTry = List.of("gemini-3.8-flash", "gemini-flash-latest", "gemini-3.7-flash");

            for (String model : modelsToTry) {
                String result = executeGeminiPost(model, apiKey, promptText);
                if (result != null && !result.isBlank()) {
                    return result;
                }
            }
        } catch (Exception e) {
            log.warn("Direct Gemini API call failed: {}", e.getMessage());
        }
        return null;
    }

    private String executeGeminiPost(String model, String apiKey, String promptText) {
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

            Map<String, Object> body = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(Map.of("text", promptText)))
                    ),
                    "generationConfig", Map.of(
                            "maxOutputTokens", 1000,
                            "temperature", 0.35
                    )
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
                            return text;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Gemini model {} request failed: {}", model, e.getMessage());
        }
        return null;
    }

    private String generateFallbackResponse(String query) {
        if (query == null || query.isBlank()) {
            return "Hello! How can I assist you with your studies today? Feel free to ask any programming or computer science question.";
        }

        String raw = query.trim();
        String lower = raw.toLowerCase();
        String clean = lower.replaceAll("[!.,?]+$", "").trim();

        // =========================================================================
        // 1. CONVERSATIONAL GREETINGS & POLITE SMALL TALK
        // =========================================================================
        if (clean.matches("^(hi|hello|hey|hola|yo|heya|howdy|sup|good\\s*(morning|afternoon|evening|day)|namaste)$")) {
            return """
                    Hello! 👋 I'm **StudyGen AI**, your personal study assistant.
                    
                    How can I help you today? You can ask me to:
                    - 💡 **Explain concepts**: From data structures to web frameworks and databases.
                    - 💻 **Provide code**: Clean snippets in Java, Python, JavaScript, C++, SQL, and more.
                    - 🗺️ **Guide your learning**: Walk through your active roadmap modules and flashcards.
                    - 🎯 **Prepare for interviews**: Break down technical questions and time complexity.
                    """;
        }

        if (clean.matches("^(how are you|how are you doing|how's it going|how do you do|what's up|wassup|hows it going)$")) {
            return "I'm doing great, thank you for asking! 🚀 Ready to help you learn and level up your skills. What topic or problem would you like to explore today?";
        }

        if (clean.matches("^(who are you|what are you|what can you do|what is studygen|help|help me)$")
                || clean.contains("who are you") || clean.contains("what can you do")) {
            return """
                    I am **StudyGen AI**, an intelligent programming and computer science study companion!
                    
                    Here is what I can do for you:
                    - **Conceptual Breakdowns**: Explain algorithms, data structures, system design, and OOP principles in simple terms.
                    - **Code & Syntax**: Provide clean, idiomatic snippets in languages like Java, Python, JavaScript, C++, and SQL.
                    - **Active Study**: Support your roadmap progress, diagnose weak areas, and reinforce learning intervals.
                    
                    Feel free to ask any question or name a topic to begin!
                    """;
        }

        if (clean.matches("^(thank you|thanks|thx|thank you so much|thanks a lot|appreciate it)$")
                || clean.startsWith("thank you") || clean.startsWith("thanks ")) {
            return "You're very welcome! Keep up the great study momentum. Feel free to ask whenever you need another explanation or code walkthrough! 🌟";
        }

        if (clean.matches("^(ok|okay|cool|got it|understood|alright|great|fine|yes|sure|done|noted)$")) {
            return "Awesome! What concept or problem would you like to tackle next?";
        }

        if (clean.matches("^(bye|goodbye|see you|see ya|cya|good night|gn)$") || clean.startsWith("bye")) {
            return "Goodbye! Great job studying today. Keep your learning streak strong, and I'll be here whenever you're ready to learn again. Have a great day! 👋";
        }

        // =========================================================================
        // 2. DATA STRUCTURES & ALGORITHMS (DSA)
        // =========================================================================
        if (clean.contains("recursion") || clean.contains("recursive")) {
            return """
                    **Recursion** is a programming technique where a function calls itself to solve smaller instances of the same problem.
                    
                    - **Base Case**: The termination condition that stops recursion and prevents a `StackOverflowError`.
                    - **Recursive Step**: The logic that reduces the problem size and moves toward the base case.
                    - **Common Uses**: Tree/Graph traversals (DFS), Divide-and-Conquer (MergeSort), and Backtracking.
                    """;
        }

        if (clean.contains("binary search")) {
            return """
                    **Binary Search** is an efficient search algorithm for **sorted collections** that runs in **O(log n)** time.
                    
                    - It repeatedly compares the target with the middle element.
                    - If the target is smaller, it narrows the search to the left half; if larger, to the right half.
                    - Far faster than Linear Search ($O(n)$) on large datasets.
                    """;
        }

        if (clean.contains("stack") && clean.contains("queue")) {
            return """
                    - **Stack (LIFO - Last In, First Out)**: Elements are added and removed from the top. Operations: `push()` and `pop()` in $O(1)$. Used in function call stacks, undo mechanisms, and expression parsing.
                    - **Queue (FIFO - First In, First Out)**: Elements enter from the rear and exit from the front. Operations: `enqueue()` and `dequeue()` in $O(1)$. Used in BFS traversals, task schedulers, and print queues.
                    """;
        }

        if (clean.contains("stack")) {
            return """
                    A **Stack** is a linear data structure that follows the **LIFO (Last In, First Out)** principle.
                    
                    - **Key Operations**: `push()` (insert), `pop()` (remove top), `peek()` (view top) — all in **O(1)** time.
                    - **Common Uses**: Function call management (Call Stack), undo/redo operations, and matching parentheses.
                    """;
        }

        if (clean.contains("queue")) {
            return """
                    A **Queue** is a linear data structure that follows the **FIFO (First In, First Out)** principle.
                    
                    - **Key Operations**: `enqueue()` (insert at rear) and `dequeue()` (remove from front) — both in **O(1)** time.
                    - **Common Uses**: Breadth-First Search (BFS), asynchronous job processing, and CPU process scheduling.
                    """;
        }

        if (clean.contains("hashmap") || clean.contains("hash table") || clean.contains("hashing")) {
            return """
                    A **HashMap** stores key-value pairs using a hash function to compute bucket indices, providing **O(1) average time** for lookup, insertion, and deletion.
                    
                    - **Collision Handling**: Resolved via **Chaining** (linked lists / trees per bucket) or **Open Addressing** (linear probing).
                    - **Key Takeaway**: Does not guarantee element ordering unless using specialized variants like `LinkedHashMap`.
                    """;
        }

        if (clean.contains("linked list") || clean.contains("linkedlist")) {
            return """
                    A **Linked List** is a linear collection of data elements called **Nodes**, where each node points to the next via a reference/pointer.
                    
                    - **Singly Linked**: Each node holds data and a pointer to the `next` node.
                    - **Doubly Linked**: Nodes hold pointers to both `next` and `prev`.
                    - **vs Array**: $O(1)$ insertion/deletion once the position is located, but requires $O(n)$ sequential access to search.
                    """;
        }

        if (clean.contains("binary search tree") || clean.contains("bst")) {
            return """
                    A **Binary Search Tree (BST)** is a hierarchical structure where each node has at most two children:
                    
                    - Left subtree keys are **strictly less** than the node's key.
                    - Right subtree keys are **strictly greater** than the node's key.
                    - **Performance**: Average $O(\\log n)$ for search, insert, and delete ($O(n)$ if unbalanced).
                    """;
        }

        if (clean.contains("dynamic programming") || clean.contains(" dp")) {
            return """
                    **Dynamic Programming (DP)** is an algorithmic technique for solving optimization problems by breaking them into overlapping subproblems and caching results.
                    
                    - **Top-Down (Memoization)**: Recursive approach with a lookup table to cache solved states.
                    - **Bottom-Up (Tabulation)**: Iterative approach filling a table starting from the base subproblems.
                    - **Prerequisites**: Must have **Overlapping Subproblems** and **Optimal Substructure**.
                    """;
        }

        if (clean.contains("big o") || clean.contains("time complexity") || clean.contains("space complexity")) {
            return """
                    **Big-O Notation** characterizes the upper bound of an algorithm's runtime or memory consumption as the input size ($n$) grows.
                    
                    - $O(1)$: Constant time (array index access, hash map lookup)
                    - $O(\\log n)$: Logarithmic time (binary search)
                    - $O(n)$: Linear time (single pass loop)
                    - $O(n \\log n)$: Linearithmic time (MergeSort, QuickSort average)
                    - $O(n^2)$: Quadratic time (nested loops)
                    """;
        }

        if (clean.contains("quicksort") || clean.contains("merge sort") || clean.contains("mergesort") || clean.contains("sorting")) {
            return """
                    Key sorting algorithms comparison:
                    
                    - **QuickSort**: Divide-and-conquer algorithm partitioning around a pivot. Average **O(n log n)**, in-place memory ($O(\\log n)$ stack).
                    - **MergeSort**: Recursively divides array in half, sorts each, and merges them. Guaranteed **O(n log n)**, requires $O(n)$ extra memory.
                    """;
        }

        // =========================================================================
        // 3. PROGRAMMING LANGUAGES (Java, Python, JS, C++, SQL)
        // =========================================================================
        if (clean.contains("oop") || clean.contains("object oriented")) {
            return """
                    The 4 fundamental pillars of **Object-Oriented Programming (OOP)**:
                    
                    1. **Encapsulation**: Bundling state and methods together while hiding internal details via `private` fields.
                    2. **Inheritance**: Deriving new classes from parent classes to promote code reusability.
                    3. **Polymorphism**: The ability of an object or method to take multiple forms (overloading & overriding).
                    4. **Abstraction**: Exposing essential interfaces while concealing implementation complexities.
                    """;
        }

        if (clean.contains("pointer") && (clean.contains("java") || clean.contains("jvm"))) {
            return """
                    **Java does not have explicit pointers** (unlike C or C++).
                    
                    - **References**: Java uses safe object *references* instead of raw pointers. References point to objects in heap memory, but their actual memory addresses are managed internally by the JVM.
                    - **No Pointer Arithmetic**: You cannot manipulate memory addresses directly (like `ptr++`), which protects against memory leaks and buffer overflows.
                    - **Garbage Collection**: Memory is automatically allocated and cleaned up by the Garbage Collector.
                    """;
        }

        if (clean.contains("pointer")) {
            return """
                    A **Pointer** is a variable that stores the direct memory address of another variable.
                    
                    - **In C / C++**: Explicit pointers (`int *p = &x;`) allow direct memory manipulation, pointer arithmetic, and manual allocation (`malloc`/`free`).
                    - **In Java & Python**: Explicit pointers are removed for security; instead, safe **object references** and automatic Garbage Collection are used.
                    """;
        }

        if ((clean.equals("java") || clean.equals("what is java") || clean.equals("explain java") || clean.startsWith("what is java ") || clean.contains("java language") || clean.contains("java programming")) && !clean.contains("javascript")) {
            return """
                    **Java** is a statically-typed, class-based, object-oriented language known for its platform independence ("Write Once, Run Anywhere") via bytecode executed on the **JVM**.
                    
                    - **Memory**: Managed by an automatic Garbage Collector (GC) dividing memory into Heap and Stack.
                    - **Ecosystem**: Enterprise backends (Spring Boot), Android development, and distributed data systems.
                    """;
        }

        if (clean.equals("python") || clean.equals("what is python") || clean.equals("explain python") || clean.contains("python language") || clean.contains("python programming")) {
            return """
                    **Python** is a high-level, interpreted language emphasizing readability and developer productivity:
                    
                    - **Core Collections**: Lists (`[]`), Tuples (`()`, immutable), Dictionaries (`{}`), Sets (`set()`).
                    - **Key Features**: Dynamic typing, list comprehensions, decorators, and generators (`yield`).
                    - **Dominant Fields**: Machine Learning & AI (PyTorch), Data Engineering, and Web APIs (FastAPI, Django).
                    """;
        }

        if (clean.contains("react")) {
            return """
                    **React** is a declarative, component-driven JavaScript library for building modern user interfaces:
                    
                    - **Virtual DOM**: Minimizes costly browser DOM repaints by calculating efficient reconciliation diffs.
                    - **Core Hooks**: `useState` (reactive state), `useEffect` (side effects & data fetching), and `useMemo` / `useCallback` (performance memoization).
                    """;
        }

        if (clean.contains("javascript") || clean.contains(" js")) {
            return """
                    **JavaScript** is the foundational scripting language of the modern web:
                    
                    - **Asynchronous Flow**: Uses Promises and `async/await` driven by a single-threaded **Event Loop**.
                    - **Closures**: Functions that remember the variables from their outer lexical scope even after the outer function finishes.
                    - **Full-Stack Reach**: Runs natively in all browsers and on scalable backend runtimes via **Node.js**.
                    """;
        }

        if (clean.contains("join") && (clean.contains("sql") || clean.contains("table") || clean.contains("database"))) {
            return """
                    **SQL Joins** combine rows from two or more tables based on a related column:
                    
                    - **INNER JOIN**: Returns only rows where matching keys exist in both tables.
                    - **LEFT JOIN**: Returns all rows from the left table and matched rows from the right table (NULL if unmatched).
                    - **RIGHT JOIN**: Returns all rows from the right table and matched rows from the left.
                    - **FULL OUTER JOIN**: Returns rows whenever there is a match in either table.
                    """;
        }

        if (clean.contains("acid") && clean.contains("database")) {
            return """
                    **ACID** guarantees reliable database transactions in relational database systems:
                    
                    - **Atomicity**: All operations in the transaction succeed, or all roll back (All-or-Nothing).
                    - **Consistency**: The database transitions only between valid states satisfying all constraints.
                    - **Isolation**: Concurrent transactions execute without mutual interference.
                    - **Durability**: Once committed, changes survive system crashes or power failures.
                    """;
        }

        if (clean.contains("sql") || clean.contains("database") || clean.contains("dbms")) {
            return """
                    A **Database Management System (DBMS)** stores, manages, and queries structured data:
                    
                    - **Relational (SQL)**: Tables with schemas and foreign keys (PostgreSQL, MySQL) prioritizing ACID compliance.
                    - **NoSQL**: Flexible document, key-value, or graph models (MongoDB, Redis) prioritizing horizontal scaling.
                    - **Indexing**: Uses B+ Trees to accelerate query lookups dramatically.
                    """;
        }

        // =========================================================================
        // 4. WEB, SYSTEM DESIGN & DEVOPS
        // =========================================================================
        if (clean.contains("rest api") || clean.contains("restful") || clean.contains("http")) {
            return """
                    A **REST API** is a stateless client-server architectural style communicating over HTTP:
                    
                    - **HTTP Methods**: `GET` (read), `POST` (create), `PUT` / `PATCH` (update), `DELETE` (remove).
                    - **Key Status Codes**: `200 OK`, `201 Created`, `400 Bad Request`, `401 Unauthorized`, `404 Not Found`, `500 Server Error`.
                    - **Statelessness**: Every request contains all context needed to process it without server-stored session states.
                    """;
        }

        if (clean.contains("jwt") || clean.contains("token") || clean.contains("auth")) {
            return """
                    A **JWT (JSON Web Token)** is a compact, URL-safe standard (`RFC 7519`) for stateless authentication:
                    
                    - **Structure**: 3 Base64Url segments separated by dots: `Header.Payload.Signature`.
                    - **Verification**: The backend verifies the cryptographic signature with a secret key without database lookups on every request.
                    """;
        }

        if (clean.contains("docker") || clean.contains("container")) {
            return """
                    **Docker** packages applications alongside their runtime, libraries, and configurations into lightweight, portable **Containers**.
                    
                    - Unlike virtual machines that emulate whole hardware systems, containers share the host operating system kernel.
                    - Solves the classic "it works on my machine" problem by guaranteeing identical runtime environments.
                    """;
        }

        if (clean.contains("git")) {
            return """
                    **Git** is a distributed version control system tracking source code history:
                    
                    - **Core Workflow**: `git add` (stage changes) -> `git commit` (snapshot) -> `git push` (sync to remote).
                    - **Branching**: `git branch` & `git switch` isolate feature development cleanly.
                    - **Merge vs Rebase**: Merge combines branch commits with a merge commit; Rebase reapplies commits linearly on top of another branch.
                    """;
        }

        // =========================================================================
        // 5. INTELLIGENT GENERAL FALLBACK
        // =========================================================================
        String topicExtract = raw
                .replaceAll("(?i)^(what\\s+is|what\\s+are|explain|tell\\s+me\\s+about|how\\s+does|how\\s+to\\s+use|can\\s+you\\s+explain|describe)\\s+", "")
                .replaceAll("[!.,?]+$", "")
                .trim();

        if (topicExtract.isBlank()) {
            topicExtract = raw;
        }

        return """
                ### %s
                
                **Overview**: %s is an important concept in modern computer science and software development.
                
                - **Core Role**: It provides the foundational logic for structuring reliable code, organizing data efficiently, and managing system state.
                - **Practical Impact**: Used across production architectures to improve performance, maintainability, and scalability.
                
                💡 *Would you like a short 3-line code example or a specific use case for this? Just ask!*
                """.formatted(topicExtract, topicExtract);
    }

    @Transactional(readOnly = true)
    public ChatContextMetaResponse getContextMeta(User user) {
        List<Roadmap> roadmaps = roadmapRepository.findByUserId(user.getId());
        int roadmapCount = roadmaps.size();
        int moduleCount = 0;
        for (Roadmap r : roadmaps) {
            moduleCount += moduleRepository.findByRoadmapIdOrderBySequenceOrderAsc(r.getId()).size();
        }

        int docCount = documentRepository.findByUserIdOrderByUploadedAtDesc(user.getId()).size();

        return new ChatContextMetaResponse(
                roadmapCount,
                moduleCount,
                docCount,
                null
        );
    }

    private List<ChatCitationDTO> generateCitations(User user, ChatSession session, ChatSessionMessageRequest request) {
        List<ChatCitationDTO> citations = new ArrayList<>();

        if (session.getModule() != null) {
            citations.add(new ChatCitationDTO(
                    session.getModule().getId(),
                    session.getModule().getTitle(),
                    "Roadmap Module",
                    "Core definitions, sequence mechanics, and checkpoints for " + session.getModule().getTitle(),
                    "/modules/" + session.getModule().getId()
            ));
        }

        List<UploadedDocument> docs = documentRepository.findByUserIdOrderByUploadedAtDesc(user.getId());
        if (!docs.isEmpty()) {
            UploadedDocument topDoc = docs.get(0);
            citations.add(new ChatCitationDTO(
                    topDoc.getId(),
                    topDoc.getFilename(),
                    "Knowledge Base Document",
                    "Foundational excerpts and synthesized takeaways from " + topDoc.getFilename(),
                    "/documents/" + topDoc.getId()
            ));
        }

        return citations;
    }

    private ChatSessionResponse mapToResponse(ChatSession s) {
        return new ChatSessionResponse(
                s.getId(),
                s.getTitle(),
                s.getContextTag(),
                s.getLastActivityAt().toString(),
                s.getModule() != null ? s.getModule().getId() : null,
                s.getModule() != null ? s.getModule().getTitle() : null,
                s.getRoadmap() != null ? s.getRoadmap().getId() : null,
                s.getRoadmap() != null ? s.getRoadmap().getTitle() : null
        );
    }
}
