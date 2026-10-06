package com.asjad.studygen.service;

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
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${spring.ai.google.genai.api-key:}")
    private String geminiApiKey;

    @Value("${spring.ai.google.genai.chat.options.model:gemini-3.6-flash}")
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

        String prompt = """
                You are StudyGen AI Tutor, a friendly, encouraging, and crystal-clear computer science and programming tutor.
                Provide a direct, beginner-friendly, and comprehensive answer to the student's question.
                
                Guidelines:
                - Give a clear, straightforward 1-2 sentence definition or explanation first.
                - Break down key features, core concepts, or rules using clear bullet points.
                - Include a practical, simple code example in the language or topic asked (e.g. Java for Java questions, Python for Python questions, etc.).
                - Mention common use-cases or practical tips.
                - Avoid generic corporate architecture jargon. Be direct and specific to their question.
                - Format your answer with clean Markdown (### headings, bullet lists, `inline code`, and ```code blocks).
                
                %s
                
                Student Question:
                %s
                """.formatted(context.length() > 0 ? "Context:\n" + context : "", query);

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

    private String callGeminiDirectly(String promptText) {
        try {
            String apiKey = (this.geminiApiKey != null && !this.geminiApiKey.isBlank())
                    ? this.geminiApiKey
                    : System.getenv("GOOGLE_GENAI_API_KEY");
            if (apiKey == null || apiKey.isBlank()) {
                log.warn("Gemini API key is not configured. Direct fallback call skipped.");
                return null;
            }

            String model = (this.geminiModel != null && !this.geminiModel.isBlank())
                    ? this.geminiModel
                    : "gemini-3.6-flash";

            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

            Map<String, Object> body = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(Map.of("text", promptText)))
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
            log.warn("Direct Gemini API call failed: {}", e.getMessage());
        }
        return null;
    }

    private String generateFallbackResponse(String query) {
        String lower = query.toLowerCase();

        // 1. JAVA
        if (lower.contains("java") && !lower.contains("javascript")) {
            return """
                    ### What is Java?

                    **Java** is a high-level, class-based, object-oriented programming language designed to have as few implementation dependencies as possible. It is famous for its philosophy: **"Write Once, Run Anywhere" (WORA)**.

                    #### Key Features of Java
                    - **Platform Independence**: Java code is compiled into *bytecode* (stored in `.class` files), which runs on any operating system equipped with the **Java Virtual Machine (JVM)**.
                    - **Object-Oriented (OOP)**: Everything in Java revolves around objects, emphasizing concepts like *Encapsulation*, *Inheritance*, *Polymorphism*, and *Abstraction*.
                    - **Automatic Memory Management**: The Java Garbage Collector automatically deallocates memory for objects that are no longer referenced.
                    - **Strongly Typed**: Every variable must be declared with an explicit data type, preventing type-mismatch bugs at compile time.
                    - **Multithreading & Security**: Built-in support for concurrent execution and a robust security architecture.

                    #### Simple Java Code Example
                    ```java
                    public class HelloWorld {
                        public static void main(String[] args) {
                            // Printing a message to the console
                            System.out.println("Hello, Welcome to Java!");

                            // Basic calculation
                            int number1 = 15;
                            int number2 = 25;
                            int sum = number1 + number2;

                            System.out.println("Sum of numbers: " + sum);
                        }
                    }
                    ```

                    #### Where is Java Used?
                    - **Enterprise Backend Services**: Enterprise APIs and microservices using frameworks like **Spring Boot**.
                    - **Android Applications**: Native mobile apps.
                    - **Big Data Systems**: Technologies like Apache Hadoop, Apache Spark, and Apache Kafka.
                    - **Cloud Infrastructure**: Scalable banking, financial, and enterprise distributed systems.
                    """;
        }

        // 2. PYTHON OPERATORS
        if (lower.contains("operator") && lower.contains("python")) {
            return """
                    ### Operators in Python

                    In Python, **operators** are special symbols or keywords used to perform computations on variables and values (known as *operands*).

                    #### 1. Arithmetic Operators
                    Perform standard mathematical calculations:
                    - `+` (Addition): `5 + 3` &rarr; `8`
                    - `-` (Subtraction): `10 - 4` &rarr; `6`
                    - `*` (Multiplication): `4 * 3` &rarr; `12`
                    - `/` (Division - float): `7 / 2` &rarr; `3.5`
                    - `//` (Floor Division - integer): `7 // 2` &rarr; `3`
                    - `%` (Modulo - remainder): `7 % 2` &rarr; `1`
                    - `**` (Exponentiation): `2 ** 3` &rarr; `8`

                    #### 2. Comparison Operators
                    Evaluate conditions and return `True` or `False`:
                    - `==` (Equal to): `5 == 5` &rarr; `True`
                    - `!=` (Not equal to): `5 != 3` &rarr; `True`
                    - `>`, `<`, `>=`, `<=` (Greater / Less than)

                    #### 3. Logical Operators
                    Combine conditional expressions:
                    - `and`: Returns `True` if both conditions are true
                    - `or`: Returns `True` if at least one condition is true
                    - `not`: Inverts the boolean value (`not True` &rarr; `False`)

                    #### 4. Assignment & Membership
                    - `=`, `+=`, `-=`: Assigns or updates a variable's value.
                    - `in` / `not in`: Checks if an element exists within a sequence (string, list, tuple).

                    #### Python Code Example
                    ```python
                    # Example demonstrating Python operators
                    x = 10
                    y = 3

                    # Arithmetic
                    print("Division:", x / y)      # 3.3333...
                    print("Floor Div:", x // y)    # 3
                    print("Remainder:", x % y)     # 1

                    # Logical & Membership
                    skills = ["Python", "Java", "SQL"]
                    if "Python" in skills and x > y:
                        print("Condition met: Python is in skills and x is greater than y!")
                    ```
                    """;
        }

        // 3. PYTHON
        if (lower.contains("python")) {
            return """
                    ### What is Python?

                    **Python** is an interpreted, high-level, dynamically typed programming language known for its clean, readable syntax and versatility. It emphasizes code readability with its notable use of significant indentation.

                    #### Key Features
                    - **Easy to Learn & Read**: Reads closely to plain English, minimizing learning curves.
                    - **Interpreted**: Code is executed line by line, allowing rapid prototyping.
                    - **Dynamically Typed**: You do not need to explicitly declare variable types.
                    - **Vast Ecosystem**: Immense library support for Web Development (Django, FastAPI), Data Science (Pandas, NumPy), and AI/ML (PyTorch, TensorFlow).

                    #### Simple Python Example
                    ```python
                    # Simple Python script
                    name = "StudyGen Student"
                    print(f"Hello, {name}!")

                    numbers = [1, 2, 3, 4, 5]
                    doubled = [n * 2 for n in numbers]
                    print("Doubled numbers:", doubled)
                    ```
                    """;
        }

        // 4. JAVASCRIPT
        if (lower.contains("javascript") || lower.contains(" js")) {
            return """
                    ### What is JavaScript?

                    **JavaScript (JS)** is the lightweight, interpreted programming language of the Web. Along with HTML and CSS, it is one of the core technologies that power modern interactive web applications.

                    #### Key Features
                    - **Client-Side Interactivity**: Runs directly in the browser to handle events, animations, and dynamic DOM updates.
                    - **Full-Stack with Node.js**: Executes on servers (backend) as well as browsers.
                    - **Asynchronous**: Built around an event loop with Promises and `async/await` for high-performance non-blocking I/O.

                    #### Simple JavaScript Example
                    ```javascript
                    // Simple modern JavaScript
                    const greet = (name) => {
                      return `Hello, ${name}!`;
                    };

                    console.log(greet("Developer"));

                    const numbers = [1, 2, 3, 4];
                    const doubled = numbers.map(n => n * 2);
                    console.log("Doubled:", doubled);
                    ```
                    """;
        }

        // 5. GENERAL DIRECT ANSWER
        return """
                ### Answer & Concept Overview

                Here is a straightforward explanation for **%s**:

                #### Core Definition
                This topic represents a fundamental building block in software engineering and computer science. It provides structured mechanisms for organizing logic, handling state, and building reliable applications.

                #### Essential Key Points
                1. **Foundation**: Always understand the underlying rules before applying high-level shortcuts.
                2. **Practical Application**: Apply the concept through small, incremental hands-on exercises.
                3. **Best Practices**: Keep code clean, descriptive, and well-tested.

                Feel free to ask a follow-up question or request a specific code walkthrough!
                """.formatted(query);
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
