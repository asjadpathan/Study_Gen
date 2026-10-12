package com.asjad.studygen.service;

import com.asjad.studygen.controller.ModuleController.ModuleSubPartDTO;
import com.asjad.studygen.dto.roadmap.InterviewQnADTO;
import com.asjad.studygen.dto.roadmap.LearnMoreRequest;
import com.asjad.studygen.dto.roadmap.ModuleLearnMoreResponse;
import com.asjad.studygen.entity.Module;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class TeachingContentService {

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-3.6-flash}")
    private String geminiModel;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private RestClient restClient;

    public List<Module> buildCurriculumForTopic(String topic) {
        String lower = topic.toLowerCase();
        List<Module> list = new ArrayList<>();

        if (lower.contains("dbms") || lower.contains("database") || lower.contains("sql")) {
            list.add(createModule("Module 1: Introduction to DBMS & Relational Model", "Core database definitions, 3-tier architecture, data independence, and DBMS vs flat files", 0));
            list.add(createModule("Module 2: Entity-Relationship (ER) Modeling & Schema Design", "Entities, attributes, relationships, cardinality, and ER-to-relational table mapping", 1));
            list.add(createModule("Module 3: SQL Fundamentals & Data Definition (DDL)", "CREATE, ALTER, DROP, TRUNCATE, primary keys, foreign keys, and integrity constraints", 2));
            list.add(createModule("Module 4: SQL Querying & Data Manipulation (DML)", "SELECT, WHERE, ORDER BY, GROUP BY, aggregate functions, and conditional filtering", 3));
            list.add(createModule("Module 5: Relational Algebra & Advanced Multi-Table Joins", "INNER, LEFT, RIGHT, FULL OUTER, CROSS joins, subqueries, and set operations", 4));
            list.add(createModule("Module 6: Functional Dependencies & Database Normalization", "Anomalies, 1NF, 2NF, 3NF, BCNF, lossless join decomposition, and dependency preservation", 5));
            list.add(createModule("Module 7: Physical Storage, File Organization & Indexing", "Disk blocks, buffer pools, clustered vs non-clustered indexes, B-Trees, and B+ Trees", 6));
            list.add(createModule("Module 8: Transactions & ACID Properties", "Atomicity, Consistency, Isolation, Durability, transaction states, and serializability schedules", 7));
            list.add(createModule("Module 9: Concurrency Control & Deadlock Management", "Lock-based protocols (2PL), strict 2PL, timestamp ordering, and deadlock detection/recovery", 8));
            list.add(createModule("Module 10: Query Optimization, Recovery & Modern NoSQL", "Query execution plans, Write-Ahead Logging (WAL), checkpointing, and CAP theorem trade-offs", 9));
        } else if (lower.contains("java") || lower.contains("spring")) {
            list.add(createModule("Module 1: Java Basics & JVM Architecture", "JDK vs JRE vs JVM, bytecode, memory areas (Heap vs Stack), primitive types, and control flow", 0));
            list.add(createModule("Module 2: Object-Oriented Programming (OOP) in Java", "Classes, objects, constructors, encapsulation, inheritance, polymorphism, and abstraction", 1));
            list.add(createModule("Module 3: Interfaces, Packages & Exception Handling", "Checked vs unchecked exceptions, custom exceptions, try-with-resources, and interface defaults", 2));
            list.add(createModule("Module 4: Java Collections Framework & Generics", "ArrayList, LinkedList, HashMap, HashSet, TreeMap, Big-O lookup velocities, and type safety", 3));
            list.add(createModule("Module 5: Java Streams API & Functional Programming", "Lambdas, Stream pipelines, map-filter-reduce, Optional, Collectors, and method references", 4));
            list.add(createModule("Module 6: Multithreading, Concurrency & Synchronization", "Thread lifecycle, Runnable, synchronized blocks, Locks, volatile, and ThreadPoolExecutor", 5));
            list.add(createModule("Module 7: Modern File I/O, Serialization & Networking", "Java NIO, Paths, Files, Buffers, JSON serialization with Jackson, and HTTP client requests", 6));
            list.add(createModule("Module 8: Spring Boot Core & Dependency Injection", "Inversion of Control (IoC), ApplicationContext, @Component, @Service, and @Autowired mechanics", 7));
            list.add(createModule("Module 9: Spring Data JPA & Transaction Management", "Entities, repositories, derived queries, @Transactional boundaries, and connection pooling", 8));
            list.add(createModule("Module 10: Enterprise Testing & JVM Performance Tuning", "JUnit 5, Mockito, test slices, Garbage Collection tuning (G1, ZGC), and profiling", 9));
        } else if (lower.contains("python")) {
            list.add(createModule("Module 1: Python Basics & Execution Mechanics", "Python interpreter, dynamic typing, variables, operators, conditionals, and loops", 0));
            list.add(createModule("Module 2: Core Data Structures & Comprehensions", "Lists, tuples, dictionaries, sets, slicing techniques, and dictionary/list comprehensions", 1));
            list.add(createModule("Module 3: Functions, Scopes & Functional Constructs", "Parameters, *args, **kwargs, LEGB variable scope, lambda functions, closures, and decorators", 2));
            list.add(createModule("Module 4: Object-Oriented Python & Magic Methods", "Classes, __init__, dunder methods, inheritance, composition, and method resolution order (MRO)", 3));
            list.add(createModule("Module 5: Error Handling & Context Managers", "Try, except, else, finally blocks, custom exceptions, and resource cleanup using 'with' statements", 4));
            list.add(createModule("Module 6: Modules, Virtual Environments & Packaging", "Imports, sys.path, standard library, virtualenv/pip, and package distribution", 5));
            list.add(createModule("Module 7: File I/O, Serialization & JSON Processing", "Working with text/binary files, os/pathlib modules, json parsing, and CSV manipulation", 6));
            list.add(createModule("Module 8: Concurrency in Python & The GIL", "Threading vs Multiprocessing, Asyncio event loops, coroutines, and GIL bottlenecks", 7));
            list.add(createModule("Module 9: Web APIs & Framework Fundamentals", "Building modern REST APIs using FastAPI or Flask, Pydantic schemas, and request handling", 8));
            list.add(createModule("Module 10: Testing, Profiling & Production Deployment", "Pytest, mocking, type hinting with mypy, memory profiling, and Docker deployment", 9));
        } else if (lower.contains("react") || lower.contains("web") || lower.contains("javascript")) {
            list.add(createModule("Module 1: Modern JavaScript (ES6+) Essentials", "Let/const, arrow functions, destructuring, spread/rest, template literals, and ES modules", 0));
            list.add(createModule("Module 2: Asynchronous JS & Promises", "Event loop, Call stack, Microtask queue, Promises, async/await, and Fetch API error handling", 1));
            list.add(createModule("Module 3: React Fundamentals & JSX Architecture", "Virtual DOM, JSX compilation, functional components, props vs state, and unidirectional data flow", 2));
            list.add(createModule("Module 4: Essential React Hooks (useState & useEffect)", "State management, component lifecycles, dependency arrays, side effects, and cleanup functions", 3));
            list.add(createModule("Module 5: Advanced Hooks & Custom Hook Design", "useRef, useMemo, useCallback, building reusable custom hooks for API calls and events", 4));
            list.add(createModule("Module 6: Component Composition & Styling Architecture", "CSS Modules, component composition patterns, conditional styling, and UI component design", 5));
            list.add(createModule("Module 7: Client-Side Routing & Navigation", "React Router v6, nested routes, URL parameters, navigation guards, and breadcrumb trails", 6));
            list.add(createModule("Module 8: Global State Management & Context API", "React Context, useReducer pattern, state hoisting, and scalable application store patterns", 7));
            list.add(createModule("Module 9: Performance Optimization & Code Splitting", "React.memo, dynamic import(), lazy loading with Suspense, and eliminating unnecessary re-renders", 8));
            list.add(createModule("Module 10: Production Build, Testing & Deployment", "Vite build pipelines, Vitest/React Testing Library, accessibility (a11y), and deployment", 9));
        } else {
            // High-quality progressive curriculum for any general topic
            list.add(createModule("Module 1: Foundations & Core Principles of " + topic, "Definitions, mental models, key terminology, and foundational primitives of " + topic, 0));
            list.add(createModule("Module 2: Basic Concepts & Primary Building Blocks", "Essential structures, syntax rules, environment setup, and fundamental components", 1));
            list.add(createModule("Module 3: Working Mechanics & Practical Hands-on", "Step-by-step implementation, standard patterns, and practical execution workflows", 2));
            list.add(createModule("Module 4: Data Flow, Input Handling & Validation", "Handling inputs defensively, sanitizing data, and managing state transformations safely", 3));
            list.add(createModule("Module 5: Modular Architecture & Component Design", "Separation of concerns, modular design, interfaces, and clean code principles", 4));
            list.add(createModule("Module 6: Essential Libraries, Tools & Ecosystem", "Ecosystem tooling, dependencies, package management, and developer utilities", 5));
            list.add(createModule("Module 7: Error Handling, Debugging & Reliability", "Exception handling, edge cases, logging strategies, and defensive fault tolerance", 6));
            list.add(createModule("Module 8: Advanced Techniques & In-Depth Mechanics", "Deep dive into performance, scalability, concurrency, and advanced paradigms", 7));
            list.add(createModule("Module 9: Optimization, Security & Best Practices", "Performance tuning, resource management, security vulnerabilities, and industry standards", 8));
            list.add(createModule("Module 10: Production Deployment & Capstone Mastery", "End-to-end implementation, automated testing, deployment configurations, and monitoring", 9));
        }

        return list;
    }

    private Module createModule(String title, String desc, int order) {
        Module m = new Module();
        m.setTitle(title);
        m.setDescription(desc);
        m.setSequenceOrder(order);
        m.setLocked(order > 0); // First module is unlocked; subsequent modules locked until prerequisite is passed with 80%
        return m;
    }

    public List<ModuleSubPartDTO> generateDetailedTeachingSubParts(Module module, String topicName) {
        String mTitle = module.getTitle();
        String context = (topicName != null && !topicName.isBlank()) ? topicName : mTitle;
        String lowerContext = context.toLowerCase() + " " + mTitle.toLowerCase();

        List<ModuleSubPartDTO> list = new ArrayList<>();

        if (lowerContext.contains("dbms") || lowerContext.contains("database") || lowerContext.contains("sql")) {
            list.add(buildDbmsBasicPart(module));
            list.add(buildDbmsIntermediatePart(module));
            list.add(buildDbmsAdvancedPart(module));
        } else if (lowerContext.contains("java") || lowerContext.contains("spring")) {
            list.add(buildJavaBasicPart(module));
            list.add(buildJavaIntermediatePart(module));
            list.add(buildJavaAdvancedPart(module));
        } else if (lowerContext.contains("python")) {
            list.add(buildPythonBasicPart(module));
            list.add(buildPythonIntermediatePart(module));
            list.add(buildPythonAdvancedPart(module));
        } else {
            list.add(buildGeneralBasicPart(module, context));
            list.add(buildGeneralIntermediatePart(module, context));
            list.add(buildGeneralAdvancedPart(module, context));
        }

        return list;
    }

    // ==========================================
    // DBMS / SQL TAILORED TEACHING CONTENT
    // ==========================================
    private ModuleSubPartDTO buildDbmsBasicPart(Module module) {
        String title = "Part 1: Core Fundamentals & Concept Basics";
        String content = """
                ### 1. What is this Topic & Why Do We Need It?
                A **Database Management System (DBMS)** is specialized software that stores, retrieves, and organizes data systematically.
                
                #### Why Flat Files (Like TXT or CSV) Fail:
                - **Data Redundancy & Inconsistency**: The same user address saved in 5 text files leads to out-of-sync records when updated.
                - **Difficulty in Accessing Data**: In a text file, searching for one customer requires scanning all lines sequentially.
                - **No Concurrent Access**: Two users opening a file simultaneously overwrite each other's changes.
                - **No Security or Integrity Controls**: Anyone who can read the file can corrupt or delete critical numbers.
                
                ### 2. Real-World Analogy
                Imagine a modern city library:
                - Instead of throwing books into random unlabeled boxes, books are organized into cataloged shelves (tables).
                - Each book has a unique barcode (Primary Key).
                - A librarian (the Database Engine) enforces check-out rules, verifies student IDs, and ensures books aren't checked out by two people at the same time.
                
                ### 3. Core Terminology & Mental Model
                - **Relation / Table**: A two-dimensional grid of rows and columns.
                - **Tuple / Row**: A single, complete record (e.g., one student's profile).
                - **Attribute / Column**: A specific property of the record (e.g., email, age, GPA).
                - **Primary Key**: A column (or set of columns) whose value uniquely identifies every row. No duplicates or NULLs allowed.
                - **Foreign Key**: A column that references the primary key of another table to link records safely.
                """;

        String code = """
                -- 1. Create a clean, normalized relational table with primary key & constraints
                CREATE TABLE students (
                    student_id      SERIAL PRIMARY KEY,
                    first_name      VARCHAR(50) NOT NULL,
                    last_name       VARCHAR(50) NOT NULL,
                    email           VARCHAR(100) UNIQUE NOT NULL,
                    enrollment_date DATE DEFAULT CURRENT_DATE
                );

                -- 2. Insert valid records into the table
                INSERT INTO students (first_name, last_name, email)
                VALUES ('John', 'Doe', 'john.doe@university.edu');

                -- 3. Query the data cleanly with explicit column selection
                SELECT student_id, first_name, last_name, email
                FROM students
                WHERE email = 'john.doe@university.edu';
                """;

        String pitfalls = "Common Beginner Mistake: Forgetting to define a Primary Key or using 'SELECT *' in production. Always define explicit constraints (PRIMARY KEY, NOT NULL, UNIQUE) to protect data integrity.";
        String takeaways = "A DBMS guarantees data consistency and fast querying. Tables organize data into rows (tuples) and columns (attributes) connected by primary and foreign keys.";

        return new ModuleSubPartDTO(1L, title, "BASIC", content, takeaways, code, pitfalls, false);
    }

    private ModuleSubPartDTO buildDbmsIntermediatePart(Module module) {
        String title = "Part 2: In-Depth Technical Mechanics & Practical Implementation";
        String content = """
                ### 1. Architectural Mechanics & Relational Schema Design
                Once the basic tables are defined, the power of a DBMS lies in **structuring relationships** and **querying across tables**.
                
                #### Key Relationship Cardinalities:
                1. **One-to-One (1:1)**: e.g., A Student has one StudentPassport.
                2. **One-to-Many (1:N)**: e.g., One Department has many Professors. Placed as a Foreign Key in the 'Many' table.
                3. **Many-to-Many (M:N)**: e.g., Students enroll in Courses. Implemented using an intermediate **Junction Table** with foreign keys pointing to both entities.
                
                ### 2. Multi-Table Operations & Join Mechanics
                - **INNER JOIN**: Returns only rows where matching keys exist in both tables.
                - **LEFT OUTER JOIN**: Returns all rows from the left table, plus matched values from the right table (or NULL if no match exists).
                - **Aggregation with GROUP BY**: Summarizes records by categories (e.g., counting total enrollments per course).
                """;

        String code = """
                -- Normalized Schema with One-to-Many and Many-to-Many Junction Table
                CREATE TABLE courses (
                    course_id   SERIAL PRIMARY KEY,
                    title       VARCHAR(100) NOT NULL,
                    credits     INT CHECK (credits > 0)
                );

                CREATE TABLE enrollments (
                    enrollment_id   SERIAL PRIMARY KEY,
                    student_id      INT NOT NULL REFERENCES students(student_id) ON DELETE CASCADE,
                    course_id       INT NOT NULL REFERENCES courses(course_id) ON DELETE RESTRICT,
                    grade           CHAR(2)
                );

                -- Querying with INNER JOIN and Aggregate Grouping
                SELECT 
                    c.title AS course_title,
                    COUNT(e.student_id) AS total_enrolled_students
                FROM courses c
                INNER JOIN enrollments e ON c.course_id = e.course_id
                GROUP BY c.course_id, c.title
                HAVING COUNT(e.student_id) >= 1
                ORDER BY total_enrolled_students DESC;
                """;

        String pitfalls = "Common Architecture Mistake: Storing comma-separated lists of IDs in a single column instead of using a junction table. This violates First Normal Form (1NF) and breaks query indexing.";
        String takeaways = "Use foreign keys with explicit ON DELETE rules to enforce referential integrity. Model Many-to-Many relationships using a junction table.";

        return new ModuleSubPartDTO(2L, title, "INTERMEDIATE", content, takeaways, code, pitfalls, false);
    }

    private ModuleSubPartDTO buildDbmsAdvancedPart(Module module) {
        String title = "Part 3: Advanced Optimization, Edge Cases & Production Pitfalls";
        String content = """
                ### 1. High-Performance Indexing & Storage Engine Internals
                In enterprise databases with millions of rows, sequential table scans cause catastrophic latency spikes.
                
                #### B+ Tree Indexing:
                - **Clustered Index**: Determines the physical order of rows on disk (usually the Primary Key). Each table can have only ONE clustered index.
                - **Non-Clustered / Secondary Index**: A separate B+ Tree structure holding sorted column values pointing to row pointers (Heap RID or Primary Key).
                
                ### 2. Transactions & ACID Guarantees
                - **Atomicity**: All operations succeed or all roll back (All-or-Nothing).
                - **Consistency**: The database transitions from one valid state to another without violating constraints.
                - **Isolation**: Concurrent transactions execute without dirty reads or non-repeatable reads based on the configured isolation level.
                - **Durability**: Once committed, changes survive server crashes and power loss via Write-Ahead Logging (WAL).
                """;

        String code = """
                -- 1. Create a high-performance composite index for frequently queried filters
                CREATE INDEX idx_enrollments_student_course 
                ON enrollments (student_id, course_id);

                -- 2. Execute an atomic financial/enrollment transaction with lock safety
                BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ;

                -- Select with row-level pessimistic locking to prevent race conditions
                SELECT student_id, first_name 
                FROM students 
                WHERE student_id = 1 
                FOR UPDATE;

                -- Perform updates defensively
                INSERT INTO enrollments (student_id, course_id, grade)
                VALUES (1, 101, 'A');

                COMMIT;
                """;

        String pitfalls = "Critical Production Pitfall: Neglecting composite index column order or holding long-running transactions that cause lock contention and deadlocks. Always keep transactions concise.";
        String takeaways = "B+ Tree indexes accelerate range queries and lookups from O(N) to O(log N). Use transactions with appropriate isolation levels to safeguard ACID invariants.";

        return new ModuleSubPartDTO(3L, title, "ADVANCED", content, takeaways, code, pitfalls, false);
    }

    // ==========================================
    // JAVA / BACKEND TAILORED TEACHING CONTENT
    // ==========================================
    private ModuleSubPartDTO buildJavaBasicPart(Module module) {
        String title = "Part 1: Core Fundamentals & Concept Basics";
        String content = """
                ### 1. What is this Topic & Core Foundations?
                Java is a strongly typed, class-based, object-oriented programming language designed on the principle of **WORA** (Write Once, Run Anywhere).
                
                #### How the JVM Works:
                - Source Code (`.java`) is compiled by `javac` into platform-independent **Bytecode** (`.class`).
                - The **Java Virtual Machine (JVM)** interprets and JIT-compiles bytecode into native CPU instructions at runtime.
                - Memory is organized into:
                  - **Stack Memory**: Stores primitive local variables and execution frames (fast, thread-isolated).
                  - **Heap Memory**: Stores objects and dynamic data managed by the Garbage Collector.
                """;

        String code = """
                // Basic Java class demonstrating clean variable declarations and control flow
                public class FundamentalsDemo {
                    public static void main(String[] args) {
                        int studentCount = 35;
                        double passingThreshold = 75.5;
                        String courseName = "Core Software Engineering";

                        System.out.println("Topic: " + courseName);
                        if (studentCount > 0) {
                            System.out.printf("Capacity active: %d students enrolled.%n", studentCount);
                        }
                    }
                }
                """;

        String pitfalls = "Beginner Mistake: Confusing '==' (reference identity comparison) with '.equals()' (value equality comparison) for Strings and Objects.";
        String takeaways = "Java code compiles to bytecode executed on the JVM. Primitives live on the Stack; Objects live on the Heap.";

        return new ModuleSubPartDTO(1L, title, "BASIC", content, takeaways, code, pitfalls, false);
    }

    private ModuleSubPartDTO buildJavaIntermediatePart(Module module) {
        String title = "Part 2: In-Depth Technical Mechanics & Practical Implementation";
        String content = """
                ### 1. Object-Oriented Principles & Modern Idioms
                - **Encapsulation**: Hide internal state behind private fields and validated getters/setters.
                - **Composition over Inheritance**: Favor embedding focused dependencies rather than deep class hierarchies.
                - **Polymorphism via Interfaces**: Program to interfaces rather than concrete implementations for loose coupling and testability.
                """;

        String code = """
                import java.util.List;
                import java.util.Optional;

                // Practical Domain Record with Immutability and Defensive Methods
                public record Course(Long id, String title, int credits) {
                    public Course {
                        if (credits <= 0) {
                            throw new IllegalArgumentException("Credits must be greater than 0");
                        }
                    }
                }

                public class CourseService {
                    private final List<Course> courseCatalog;

                    public CourseService(List<Course> catalog) {
                        this.courseCatalog = List.copyOf(catalog); // Defensive immutable copy
                    }

                    public Optional<Course> findCourseById(Long id) {
                        return courseCatalog.stream()
                                .filter(c -> c.id().equals(id))
                                .findFirst();
                    }
                }
                """;

        String pitfalls = "Architecture Mistake: Returning mutable collections directly from getters, allowing external code to modify internal state unexpectedly.";
        String takeaways = "Use records and immutable collections to ensure thread safety and avoid unpredictable state mutations.";

        return new ModuleSubPartDTO(2L, title, "INTERMEDIATE", content, takeaways, code, pitfalls, false);
    }

    private ModuleSubPartDTO buildJavaAdvancedPart(Module module) {
        String title = "Part 3: Advanced Optimization, Edge Cases & Production Pitfalls";
        String content = """
                ### 1. Concurrency, Memory Leaks & JVM Optimization
                - **Thread Safety**: Use `ConcurrentHashMap`, `AtomicInteger`, or `ReentrantLock` when multiple threads access shared resources.
                - **Garbage Collection Optimization**: Minimize short-lived object allocations in tight loops to reduce GC pause times.
                - **CompletableFuture**: Execute asynchronous I/O operations non-blockingly without thread starvation.
                """;

        String code = """
                import java.util.concurrent.CompletableFuture;
                import java.util.concurrent.ExecutorService;
                import java.util.concurrent.Executors;

                public class AsyncProcessingDemo {
                    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(4);

                    public CompletableFuture<String> fetchUserDataAsync(Long userId) {
                        return CompletableFuture.supplyAsync(() -> {
                            // Simulating non-blocking downstream I/O call
                            return "Processed user payload for ID: " + userId;
                        }, EXECUTOR).exceptionally(ex -> "Fallback: Failed to fetch user: " + ex.getMessage());
                    }
                }
                """;

        String pitfalls = "Production Pitfall: Unhandled exceptions inside CompletableFuture pipelines causing silent task disappearance, or unclosed resource streams causing file descriptor leaks.";
        String takeaways = "Always provide custom bounded ThreadPools and attach .exceptionally() handlers to asynchronous CompletableFuture pipelines.";

        return new ModuleSubPartDTO(3L, title, "ADVANCED", content, takeaways, code, pitfalls, false);
    }

    // ==========================================
    // PYTHON TAILORED TEACHING CONTENT
    // ==========================================
    private ModuleSubPartDTO buildPythonBasicPart(Module module) {
        String title = "Part 1: Core Fundamentals & Concept Basics";
        String content = """
                ### 1. What is this Topic & Core Foundations?
                Python is an interpreted, dynamically typed, high-level programming language emphasizing readability and developer velocity.
                
                #### Core Mental Model:
                - **Everything is an Object**: In Python, functions, modules, numbers, and strings are first-class objects.
                - **Dynamic Typing**: Variables are labels bound to objects in memory, not fixed memory slots.
                - **Indentation is Syntax**: Clean code structure is enforced by the interpreter.
                """;

        String code = """
                # Python basic structures and idiomatic formatting
                def calculate_metrics(values: list[float]) -> dict:
                    if not values:
                        return {"count": 0, "average": 0.0}
                    
                    total = sum(values)
                    avg = total / len(values)
                    return {"count": len(values), "average": round(avg, 2)}

                sample_scores = [85.5, 92.0, 78.5, 90.0]
                results = calculate_metrics(sample_scores)
                print(f"Calculated Metrics: {results}")
                """;

        String pitfalls = "Beginner Mistake: Using mutable default arguments like 'def func(items=[])'. The list is created once at definition time and shared across all calls!";
        String takeaways = "Python variables are object references. Use type hints for readability and never use mutable default arguments.";

        return new ModuleSubPartDTO(1L, title, "BASIC", content, takeaways, code, pitfalls, false);
    }

    private ModuleSubPartDTO buildPythonIntermediatePart(Module module) {
        String title = "Part 2: In-Depth Technical Mechanics & Practical Implementation";
        String content = """
                ### 1. Comprehensions, Generators & Context Managers
                - **List & Dict Comprehensions**: Declarative transformations over iterables with minimal boilerplate.
                - **Generators (`yield`)**: Lazy memory evaluation that processes items one at a time instead of loading gigabyte datasets into RAM.
                - **Context Managers (`with`)**: Deterministic resource allocation and guaranteed cleanup.
                """;

        String code = """
                from contextlib import contextmanager

                # Safe Context Manager demonstrating deterministic resource acquisition
                @contextmanager
                def safe_database_connection(endpoint: str):
                    print(f"Connecting to {endpoint}...")
                    connection = {"status": "CONNECTED", "endpoint": endpoint}
                    try:
                        yield connection
                    finally:
                        print("Closing connection cleanly and committing state...")
                        connection["status"] = "CLOSED"

                with safe_database_connection("db.prod.internal") as conn:
                    print(f"Executing query with: {conn['status']}")
                """;

        String pitfalls = "Common Mistake: Loading massive files entirely into memory with .read() instead of iterating line-by-line or using generator streams.";
        String takeaways = "Use generator expressions and context managers to guarantee memory efficiency and deterministic resource cleanup.";

        return new ModuleSubPartDTO(2L, title, "INTERMEDIATE", content, takeaways, code, pitfalls, false);
    }

    private ModuleSubPartDTO buildPythonAdvancedPart(Module module) {
        String title = "Part 3: Advanced Optimization, Edge Cases & Production Pitfalls";
        String content = """
                ### 1. Concurrency, The GIL & Performance Optimization
                - **Global Interpreter Lock (GIL)**: Allows only one thread to execute Python bytecode at a time.
                - **I/O-Bound Workloads**: Use `asyncio` or `threading` to await network/disk responses concurrently.
                - **CPU-Bound Workloads**: Use `multiprocessing` to bypass the GIL across multiple CPU cores.
                """;

        String code = """
                import asyncio

                async def fetch_api_payload(service_id: int) -> dict:
                    print(f"Initiating non-blocking async request to service {service_id}")
                    await asyncio.sleep(0.1) # Simulating network latency
                    return {"service": service_id, "status": 200}

                async def main():
                    tasks = [fetch_api_payload(i) for i in range(1, 4)]
                    responses = await asyncio.gather(*tasks)
                    print(f"All payloads received concurrently: {responses}")

                asyncio.run(main())
                """;

        String pitfalls = "Production Pitfall: Using threading for heavy mathematical computations expecting multicore speedups. The GIL will serialize execution; use multiprocessing or NumPy instead.";
        String takeaways = "Use asyncio for high-concurrency I/O and multiprocessing for CPU-bound computations to achieve true multicore parallelism.";

        return new ModuleSubPartDTO(3L, title, "ADVANCED", content, takeaways, code, pitfalls, false);
    }

    // ==========================================
    // GENERAL TOPIC TAILORED TEACHING CONTENT
    // ==========================================
    private ModuleSubPartDTO buildGeneralBasicPart(Module module, String topic) {
        String title = "Part 1: Core Fundamentals & Concept Basics";
        String content = """
                ### 1. What is this Topic & Why Do We Need It?
                Mastering **%s** requires establishing a clear foundational mental model of why it exists and what problems it solves in modern technology.
                
                #### Core Problem Solved:
                - Provides structured, predictable abstractions instead of ad-hoc, error-prone manual workflows.
                - Standardizes protocols and architectural boundaries so complex systems scale reliably.
                
                ### 2. Real-World Analogy
                Think of this topic as the foundational blueprint of a building:
                Before erecting high-rise walls or plumbing, you must understand soil mechanics, foundation depth, and load-bearing columns. Once the foundations are solid, all advanced features become natural to implement.
                
                ### 3. Core Terminology & Building Blocks:
                - **Invariants**: Rules that must always hold true for data to remain valid.
                - **Separation of Concerns**: Keeping logic isolated from presentation and persistence.
                - **Deterministic Output**: Ensuring the same valid inputs always yield predictable outcomes.
                """.formatted(module.getTitle());

        String code = """
                // 1. Foundational Example: Demonstrating core mechanics of %s
                public class %sBasics {
                    public static void main(String[] args) {
                        System.out.println("Initializing foundation for: %s");
                        // 1. Establish deterministic baseline
                        // 2. Validate essential inputs
                        // 3. Confirm expected state
                    }
                }
                """.formatted(module.getTitle(), module.getTitle().replaceAll("[^a-zA-Z0-9]", ""), module.getTitle());

        String pitfalls = "Common Beginner Mistake: Skipping fundamental concepts and attempting complex implementations before understanding core invariants.";
        String takeaways = "Build a solid foundation first. Understand why the topic exists and its fundamental rules before diving into advanced patterns.";

        return new ModuleSubPartDTO(1L, title, "BASIC", content, takeaways, code, pitfalls, false);
    }

    private ModuleSubPartDTO buildGeneralIntermediatePart(Module module, String topic) {
        String title = "Part 2: In-Depth Technical Mechanics & Practical Implementation";
        String content = """
                ### 1. Practical Architecture & Implementation Guide
                Now that the foundational rules are clear, let us examine how **%s** operates under the hood in practical applications.
                
                #### Step-by-Step Architectural Flow:
                1. **Input Ingestion & Boundary Defense**: Validate incoming payloads against strict schema constraints.
                2. **Core Domain Processing**: Execute the core business invariants with transactional integrity.
                3. **Result Dissemination**: Emit typed responses and capture telemetry metrics for observability.
                
                ### 2. Standard Industry Best Practices:
                - Favor immutability to prevent unexpected side effects across concurrent contexts.
                - Isolate side effects to distinct boundary adapters.
                """.formatted(module.getTitle());

        String code = """
                // Practical Implementation demonstrating clean architecture for %s
                public class %sImplementation {
                    public void executeWorkflow(String inputPayload) {
                        if (inputPayload == null || inputPayload.isBlank()) {
                            throw new IllegalArgumentException("Payload cannot be empty");
                        }
                        System.out.println("Executing verified workflow for: " + inputPayload);
                    }
                }
                """.formatted(module.getTitle(), module.getTitle().replaceAll("[^a-zA-Z0-9]", ""));

        String pitfalls = "Implementation Mistake: Failing to validate boundaries early, causing errors to propagate deep into downstream components.";
        String takeaways = "Apply defensive input validation and separate business logic from transport and persistence layers.";

        return new ModuleSubPartDTO(2L, title, "INTERMEDIATE", content, takeaways, code, pitfalls, false);
    }

    private ModuleSubPartDTO buildGeneralAdvancedPart(Module module, String topic) {
        String title = "Part 3: Advanced Optimization, Edge Cases & Production Pitfalls";
        String content = """
                ### 1. Production Scalability, Edge Cases & Resiliency
                In enterprise and high-throughput environments, standard implementations of **%s** encounter bottlenecks around latency, concurrency, and resource constraints.
                
                #### Critical Anti-Patterns to Avoid:
                - **Silent Failures**: Never swallow exceptions without structured logging or actionable recovery.
                - **Unbounded Resources**: Always configure bounded thread pools, connection limits, and request timeouts.
                - **Cascading Failures**: Implement circuit breakers and graceful degradation when dependencies slow down.
                
                ### 2. Senior Engineer Production Checklist:
                - [x] Verified P99 latency thresholds under concurrent load.
                - [x] Configured health checks and structured metric telemetry.
                - [x] Automated test coverage across failure edge cases.
                """.formatted(module.getTitle());

        String code = """
                // Production-grade resilient implementation with circuit breaking and logging
                public class %sProductionResilience {
                    public String processWithProtection(String request) {
                        try {
                            // Execute with timeout and backpressure bounds
                            return "Processed: " + request;
                        } catch (Exception e) {
                            // Fail gracefully without crashing the service
                            return "Graceful fallback response";
                        }
                    }
                }
                """.formatted(module.getTitle().replaceAll("[^a-zA-Z0-9]", ""));

        String pitfalls = "Critical Production Pitfall: Unbounded resource allocation causing cascading memory leaks and thread exhaustion under peak traffic.";
        String takeaways = "Design for failure. Use bounded resources, circuit breakers, and structured observability to ensure high reliability.";

        return new ModuleSubPartDTO(3L, title, "ADVANCED", content, takeaways, code, pitfalls, false);
    }

    // ==========================================
    // LEARN MORE & IN-DEPTH STUDY GENERATOR
    // ==========================================
    public ModuleLearnMoreResponse generateSubPartLearnMore(Module module, LearnMoreRequest request) {
        String partTitle = request != null && request.partTitle() != null ? request.partTitle() : "Core Topic Deep-Dive";
        String partLevel = request != null && request.partLevel() != null ? request.partLevel() : "INTERMEDIATE";
        String contentBody = request != null && request.contentBody() != null ? request.contentBody() : "";
        String roadmapTopic = (request != null && request.roadmapTopic() != null && !request.roadmapTopic().isBlank())
                ? request.roadmapTopic()
                : (module.getRoadmap() != null ? module.getRoadmap().getTitle() : module.getTitle());

        // 1. Attempt AI Generation via Gemini if key is configured
        ModuleLearnMoreResponse aiResponse = callGeminiForLearnMore(roadmapTopic, module.getTitle(), partTitle, partLevel, contentBody);
        if (aiResponse != null) {
            return aiResponse;
        }

        // 2. Intelligent rich fallback tailored to topic
        return generateFallbackLearnMore(roadmapTopic, module.getTitle(), partTitle, partLevel, contentBody);
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
                            String k = line.substring("GOOGLE_GENAI_API_KEY=".length()).trim();
                            if (!k.isBlank()) return k;
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private ModuleLearnMoreResponse callGeminiForLearnMore(String topic, String moduleTitle, String partTitle, String partLevel, String contentBody) {
        try {
            String apiKey = resolveApiKey();
            if (apiKey == null || apiKey.isBlank()) {
                return null;
            }

            String model = "gemini-3.8-flash";
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

            String prompt = """
                    You are a world-class principal software engineer and CS professor. A student needs an extensive, high-impact "Learn More" deep dive for this topic:
                    Overall Track/Topic: %s
                    Module: %s
                    Sub-Part Lesson: %s (Level: %s)
                    Existing Summary: %s
                    
                    INSTRUCTIONS:
                    Generate a rich, deeply educational JSON response with:
                    1. "title": An engaging deep-dive title.
                    2. "summary": A crisp 2-sentence summary explaining why this concept is critical in modern software systems.
                    3. "deepDiveMarkdown": In-depth, crystal-clear Markdown covering:
                       - ### 1. Under-The-Hood Architecture & Runtime Lifecycle
                       - ### 2. Memory, Complexity & Performance Trade-offs
                       - ### 3. Real-World Failure Modes & Edge Cases
                       - ### 4. When to Use vs Alternatives (with a clean comparison table or bullets)
                    4. "codeSnippet": A comprehensive, production-grade, readable code snippet with inline explanatory comments.
                    5. "codeLanguage": Language identifier (e.g. "java", "python", "sql", "javascript").
                    6. "realWorldUseCases": Array of exactly 3 concrete industry use case descriptions (e.g. in high-scale web apps, payment systems, or distributed pipelines).
                    7. "bestPractices": Array of exactly 4 actionable production engineering rules of thumb.
                    8. "interviewQuestions": Array of exactly 3 objects with "question" and "answer" fields representing top technical interview questions on this topic.
                    
                    CRITICAL: Return ONLY valid raw JSON without markdown code fences.
                    """.formatted(topic, moduleTitle, partTitle, partLevel, contentBody);

            Map<String, Object> body = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(Map.of("text", prompt)))
                    ),
                    "generationConfig", Map.of(
                            "temperature", 0.4,
                            "maxOutputTokens", 1600
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
                        String rawText = parts.get(0).path("text").asText();
                        if (rawText != null && !rawText.isBlank()) {
                            String cleanJson = rawText.replaceAll("^```json\\s*", "").replaceAll("```\\s*$", "").trim();
                            JsonNode parsed = objectMapper.readTree(cleanJson);

                            List<String> useCases = new ArrayList<>();
                            if (parsed.path("realWorldUseCases").isArray()) {
                                parsed.path("realWorldUseCases").forEach(node -> useCases.add(node.asText()));
                            }
                            List<String> bestPractices = new ArrayList<>();
                            if (parsed.path("bestPractices").isArray()) {
                                parsed.path("bestPractices").forEach(node -> bestPractices.add(node.asText()));
                            }
                            List<InterviewQnADTO> questions = new ArrayList<>();
                            if (parsed.path("interviewQuestions").isArray()) {
                                parsed.path("interviewQuestions").forEach(qNode -> {
                                    questions.add(new InterviewQnADTO(
                                            qNode.path("question").asText("Technical Question"),
                                            qNode.path("answer").asText("Model technical answer.")
                                    ));
                                });
                            }

                            return new ModuleLearnMoreResponse(
                                    parsed.path("title").asText(partTitle + " — Comprehensive Deep Dive"),
                                    parsed.path("summary").asText("In-depth technical breakdown and production guide for " + partTitle),
                                    parsed.path("deepDiveMarkdown").asText(),
                                    parsed.path("codeSnippet").asText(),
                                    parsed.path("codeLanguage").asText("code"),
                                    useCases,
                                    bestPractices,
                                    questions
                            );
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // Fall through to rich topic-tailored fallback
        }
        return null;
    }

    private ModuleLearnMoreResponse generateFallbackLearnMore(String topic, String moduleTitle, String partTitle, String partLevel, String contentBody) {
        String combined = (topic + " " + moduleTitle + " " + partTitle).toLowerCase();

        // 1. DBMS / SQL / DATABASE
        if (combined.contains("dbms") || combined.contains("database") || combined.contains("sql")) {
            String title = "Deep-Dive Mastery: " + partTitle;
            String summary = "Comprehensive architectural exploration of database engine internals, indexing mechanics, transactional serializability, and enterprise query optimization.";
            String markdown = """
                    ### 1. Under-The-Hood Architecture & Storage Mechanics
                    Relational database management systems (RDBMS) like PostgreSQL and MySQL InnoDB manage persistence via fixed-size memory blocks called **Disk Pages** (typically 8KB or 16KB).
                    
                    - **Buffer Pool**: Dedicated server memory acting as an in-memory cache for disk pages. When querying a row, the storage engine reads the entire 8KB page into the buffer pool.
                    - **Write-Ahead Logging (WAL)**: To guarantee ACID Durability without synchronously writing scattered pages to disk, changes are first appended to an immutable sequential log file before dirty pages are flushed asynchronously.
                    - **Doublewrite Buffer**: Prevents partial page writes during power failures before modifying tablespace files.
                    
                    ### 2. Indexing Internals & B+ Tree Search Complexity
                    - **Clustered Index**: Defines the physical sequential order of data rows on disk (usually the Primary Key). Lookups run in $O(\\log N)$ tree traversals.
                    - **Non-Clustered (Secondary) Index**: Stores sorted index keys with pointers (RowIDs or PKs) to the underlying clustered record.
                    - **Covering Index**: An index containing all columns requested in a `SELECT` statement, avoiding costly secondary table lookups entirely.
                    
                    ### 3. Concurrency, Lock Escalation & Isolation Anomalies
                    | Isolation Level | Dirty Read | Non-Repeatable Read | Phantom Read |
                    |:---|:---:|:---:|:---:|
                    | **Read Uncommitted** | Possible | Possible | Possible |
                    | **Read Committed** | Prevented | Possible | Possible |
                    | **Repeatable Read** | Prevented | Prevented | Possible |
                    | **Serializable** | Prevented | Prevented | Prevented |
                    
                    ### 4. Critical Production Failure Modes & Edge Cases
                    - **Sequential Table Scans on High Volume**: Forgetting an index on foreign keys or filtering columns forces $O(N)$ full table scans, causing CPU saturation and high disk latency.
                    - **N+1 Query Cascades**: Executing individual queries per child record instead of batch `JOIN` queries.
                    - **Long-Running Transactions & Table Lock Bloat**: Uncommitted transactions hold row locks indefinitely, blocking subsequent writes.
                    """;

            String code = """
                    -- Production-grade normalized table design with indexing & query optimization
                    BEGIN;
                    
                    -- 1. Create table with appropriate constraints
                    CREATE TABLE IF NOT EXISTS orders (
                        order_id        BIGSERIAL PRIMARY KEY,
                        customer_id     BIGINT NOT NULL,
                        order_status    VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                        total_amount    NUMERIC(12, 2) NOT NULL CHECK (total_amount >= 0),
                        created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
                    );
                    
                    -- 2. Create composite covering index for fast filtered dashboard lookups
                    CREATE INDEX IF NOT EXISTS idx_orders_customer_status_created
                        ON orders (customer_id, order_status, created_at DESC);
                    
                    -- 3. Verify query execution plan utilizes index scan rather than table scan
                    EXPLAIN ANALYZE
                    SELECT order_id, total_amount, created_at
                    FROM orders
                    WHERE customer_id = 1042 AND order_status = 'COMPLETED'
                    ORDER BY created_at DESC
                    LIMIT 20;
                    
                    COMMIT;
                    """;

            List<String> useCases = List.of(
                    "High-volume e-commerce checkout utilizing pessimistic row locking (`SELECT FOR UPDATE`) to prevent inventory overselling during sales.",
                    "Financial banking ledger enforcing strict Serializable isolation to guarantee mathematical consistency across simultaneous transfers.",
                    "Multi-tenant analytics SaaS leveraging composite indexes `(tenant_id, created_at)` to query millions of records in sub-10ms."
            );

            List<String> bestPractices = List.of(
                    "Always create explicit indexes on foreign key columns used in joins and filters.",
                    "Use `EXPLAIN (ANALYZE, BUFFERS)` in staging to inspect buffer hit ratios and eradicate sequential scans.",
                    "Size database connection pools (HikariCP) conservatively based on hardware cores: `(cores * 2) + effective_spindles`.",
                    "Keep write transactions as short and scoped as possible to minimize lock hold duration and prevent deadlocks."
            );

            List<InterviewQnADTO> questions = List.of(
                    new InterviewQnADTO(
                            "Why are B+ Trees preferred over Binary Search Trees or Hash Tables for database indexing?",
                            "B+ Trees have high fan-out (hundreds of child pointers per node), which reduces tree depth to 3-4 levels, translating to very few disk I/O operations. Unlike Hash Tables, B+ Trees store all records in linked leaf nodes, making range queries ($O(K)$) extremely fast."
                    ),
                    new InterviewQnADTO(
                            "How does Write-Ahead Logging (WAL) ensure durability during a server crash?",
                            "The database writes the intent of every modification to sequential log files on disk BEFORE writing dirty memory pages to tables. On recovery, the database scans the WAL to replay committed transactions (Redo) and roll back uncommitted ones (Undo)."
                    ),
                    new InterviewQnADTO(
                            "What is the Leftmost Prefix Rule in composite indexes?",
                            "When an index is defined on columns (A, B, C), the database can only use the index for queries filtering on (A), (A, B), or (A, B, C). A query filtering only on (B, C) cannot use the composite index because the search tree is partitioned starting with column A."
                    )
            );

            return new ModuleLearnMoreResponse(title, summary, markdown, code, "sql", useCases, bestPractices, questions);
        }

        // 2. JAVA / SPRING
        if (combined.contains("java") || combined.contains("spring")) {
            String title = "Deep-Dive Mastery: " + partTitle;
            String summary = "Comprehensive engineering analysis of Java runtime internals, JVM memory zones, garbage collection cycles, and Spring Framework enterprise patterns.";
            String markdown = """
                    ### 1. JVM Architecture & Memory Model Internals
                    The Java Virtual Machine organizes memory into specialized regions:
                    - **Heap Memory**: Shared across all threads. Divided into **Young Generation** (Eden space, Survivor S0/S1 spaces) and **Old (Tenured) Generation**.
                    - **Metaspace**: Resides in native host memory (replacing PermGen), holding class metadata, bytecode, and method definitions.
                    - **Thread Stacks**: Dedicated memory per thread storing stack frames with local primitive variables and object references.
                    
                    ### 2. Spring IoC Container & Bean Lifecycle Mechanics
                    1. **Component Scanning & Definition Loading**: Spring discovers classes annotated with `@Component`, `@Service`, or `@Repository`.
                    2. **Instantiation & Injection**: The container instantiates beans and injects dependencies via reflection or constructors.
                    3. **BeanPostProcessor Interception**: Spring creates dynamic CGLIB/JDK proxies for beans requiring `@Transactional`, `@Async`, or `@Cacheable`.
                    4. **Initialization & Destruction**: Executes `@PostConstruct` and `DisposableBean` hooks.
                    
                    ### 3. Concurrency, Memory Visibility & The Java Memory Model (JMM)
                    - **Happens-Before Relationship**: Guarantees that memory writes performed by one thread are visibly observed by another thread.
                    - **`volatile`**: Enforces memory visibility and prevents CPU instruction reordering without acquiring heavy monitor locks.
                    - **Atomic Constructs**: Utilize hardware-level Compare-And-Swap (CAS) instructions to eliminate thread blocking.
                    
                    ### 4. Senior Production Anti-Patterns to Avoid
                    - **Thread Pool Queue Unboundedness**: Leaving `LinkedBlockingQueue` unbounded in executor services leads to gradual `OutOfMemoryError` under spikes.
                    - **Self-Invocation Transaction Bypassing**: Calling a `@Transactional` method from another method within the same bean bypasses the Spring CGLIB proxy.
                    """;

            String code = """
                    package com.example.service;
                    
                    import org.springframework.stereotype.Service;
                    import org.springframework.transaction.annotation.Transactional;
                    import java.util.concurrent.ConcurrentHashMap;
                    import java.util.Optional;
                    
                    // Production-grade resilient service demonstrating constructor injection & thread safety
                    @Service
                    public class OrderProcessingService {
                    
                        private final ConcurrentHashMap<String, Long> rateLimiterCache = new ConcurrentHashMap<>();
                    
                        // Constructor injection ensures immutability & simplified unit testing
                        public OrderProcessingService() {}
                    
                        @Transactional(readOnly = true)
                        public Optional<String> fetchOrderDetails(String orderId) {
                            if (orderId == null || orderId.isBlank()) {
                                throw new IllegalArgumentException("Order ID cannot be empty");
                            }
                            // Optimized read path avoids dirty-checking overhead in Hibernate
                            return Optional.of("Order Payload: " + orderId);
                        }
                    
                        @Transactional(rollbackFor = Exception.class)
                        public void executeOrderFulfillment(String orderId) {
                            // Enforce atomic state transitions
                            rateLimiterCache.compute(orderId, (key, count) -> count == null ? 1L : count + 1L);
                        }
                    }
                    """;

            List<String> useCases = List.of(
                    "High-concurrency fintech microservice handling thousands of payment transactions per second with atomic state updates.",
                    "Enterprise distributed order pipeline utilizing Spring Boot and asynchronous message listeners with automatic dead-letter queue routing.",
                    "Sub-10ms latency caching service utilizing Caffeine in-memory cache backed by Redis for multi-node synchronization."
            );

            List<String> bestPractices = List.of(
                    "Always use constructor injection over field `@Autowired` to ensure class immutability and seamless mockability.",
                    "Mark read-only queries with `@Transactional(readOnly = true)` to disable Hibernate entity dirty-checking and optimize connection utilization.",
                    "Always configure explicit thread pool boundaries (core, max, bounded queue, rejection policy) on `ThreadPoolExecutor`.",
                    "Never catch generic `Exception` without structured logging or rethrowing to avoid silent failure anti-patterns."
            );

            List<InterviewQnADTO> questions = List.of(
                    new InterviewQnADTO(
                            "What is the Generational Hypothesis in JVM Garbage Collection?",
                            "The Generational Hypothesis states that the vast majority of objects die very shortly after allocation. JVM garbage collectors leverage this by separating memory into Young and Old generations: collecting Young Gen (Eden) frequently with fast scavenges, and only promoting surviving long-lived objects to Tenured Gen."
                    ),
                    new InterviewQnADTO(
                            "Why does calling a `@Transactional` method from within the same class fail to start a transaction?",
                            "Spring applies transactional behavior by wrapping beans in dynamic proxies (CGLIB). When an external client calls the method, the proxy intercepts the call to open the transaction. Internal calls (`this.method()`) bypass the proxy, calling the raw target object directly."
                    ),
                    new InterviewQnADTO(
                            "What is the difference between `volatile` and `synchronized`?",
                            "`volatile` guarantees variable visibility across CPU caches and prevents instruction reordering, but does NOT guarantee atomicity for compound operations like `count++`. `synchronized` provides both visibility and mutual exclusion (locking) for blocks of code."
                    )
            );

            return new ModuleLearnMoreResponse(title, summary, markdown, code, "java", useCases, bestPractices, questions);
        }

        // 3. PYTHON
        if (combined.contains("python")) {
            String title = "Deep-Dive Mastery: " + partTitle;
            String summary = "Comprehensive exploration of Python runtime execution, CPython bytecode evaluation, memory management, and asynchronous event loop concurrency.";
            String markdown = """
                    ### 1. CPython Interpreter & Execution Mechanics
                    When executing Python code:
                    - **Source to Bytecode**: The parser converts source text into an Abstract Syntax Tree (AST), which the compiler compiles into `.pyc` bytecode instructions.
                    - **The Evaluation Loop**: The CPython runtime enters `ceval.c`, popping bytecode instructions off the execution frame stack.
                    - **Global Interpreter Lock (GIL)**: A mutex protecting CPython memory structures, allowing only one native thread to execute Python bytecodes at any time.
                    
                    ### 2. Memory Management & Object Model Internals
                    - **`PyObject`**: Everything in Python is an object containing a reference count (`ob_refcnt`) and a pointer to its type object (`ob_type`).
                    - **PyMalloc**: Specialized memory allocator optimizing allocations smaller than 512 bytes into memory arenas, pools, and blocks.
                    - **Cyclic Garbage Collector**: Complements reference counting by detecting and collecting isolated circular reference loops (`gc` module).
                    
                    ### 3. Asynchronous Execution & The Event Loop
                    - **Coroutines**: Functions defined with `async def` that return coroutine objects without executing immediately.
                    - **Event Loop**: A cooperative single-threaded task scheduler using OS socket notifications (`epoll`/`kqueue`) to wake waiting coroutines without thread context switching.
                    
                    ### 4. Critical Production Anti-Patterns to Avoid
                    - **Mutable Default Arguments**: Defining `def process(items=[])` retains the same list instance across separate function calls.
                    - **CPU-Bound Multithreading**: Spawning Python threads for heavy mathematical computations causes thread thrashing due to the GIL. Use `multiprocessing` instead.
                    """;

            String code = """
                    from typing import List, Dict, Optional, Generator
                    import asyncio
                    
                    # Production-grade Python pattern demonstrating type hints, generators, and async concurrency
                    class StreamProcessor:
                        def __init__(self, batch_size: int = 100) -> None:
                            self.batch_size: int = batch_size
                    
                        # Generator expression maintains O(1) memory footprint when processing massive streams
                        def chunk_stream(self, data_stream: List[dict]) -> Generator[List[dict], None, None]:
                            for i in range(0, len(data_stream), self.batch_size):
                                yield data_stream[i : i + self.batch_size]
                    
                        async def process_batch_async(self, batch: List[dict]) -> Dict[str, int]:
                            # Simulate cooperative non-blocking I/O dispatch
                            await asyncio.sleep(0.01)
                            return {"processed_count": len(batch)}
                    
                    # Example invocation
                    async def main():
                        processor = StreamProcessor(batch_size=50)
                        sample_data = [{"id": i} for i in range(120)]
                        for chunk in processor.chunk_stream(sample_data):
                            result = await processor.process_batch_async(chunk)
                            print(f"Batch result: {result}")
                    """;

            List<String> useCases = List.of(
                    "High-throughput asynchronous API gateway handling thousands of concurrent WebSocket connections with `asyncio`.",
                    "Data engineering ETL pipeline utilizing generator pipelines to transform gigabytes of stream records with bounded memory.",
                    "Machine learning feature extraction pipeline utilizing vectorized NumPy operations and multiprocessing worker pools."
            );

            List<String> bestPractices = List.of(
                    "Never use mutable default arguments (`def fn(x=[])`); use `def fn(x: Optional[List] = None)` and initialize inside the function.",
                    "Use generator functions (`yield`) instead of list comprehensions when processing large data streams to guarantee $O(1)$ memory usage.",
                    "Leverage `slots=True` in dataclasses to eliminate `__dict__` overhead and reduce memory consumption by up to 40%.",
                    "Use multiprocessing or native C/Rust extensions for CPU-bound tasks to bypass Global Interpreter Lock bottlenecks."
            );

            List<InterviewQnADTO> questions = List.of(
                    new InterviewQnADTO(
                            "What is the Global Interpreter Lock (GIL) and how does it impact I/O-bound vs CPU-bound tasks?",
                            "The GIL is a mutex in CPython that ensures only one thread executes Python bytecode at a time. For I/O-bound tasks, threads release the GIL while waiting for network or disk operations, enabling high concurrency. For CPU-bound tasks, threads fight for the lock, resulting in zero parallelism; multiprocessing must be used instead."
                    ),
                    new InterviewQnADTO(
                            "How does Python manage memory and resolve circular references?",
                            "Python primarily uses reference counting for instant memory deallocation when an object's reference counter drops to zero. To resolve circular references (where objects reference each other), a generational cyclic garbage collector runs periodically, identifying unreachable groups using pointer traversal."
                    ),
                    new InterviewQnADTO(
                            "What is the difference between a generator and an iterator in Python?",
                            "An iterator is any object that implements the iterator protocol (`__iter__()` and `__next__()`). A generator is a function containing `yield` statements that automatically produces an iterator, pausing and saving its execution state between invocations."
                    )
            );

            return new ModuleLearnMoreResponse(title, summary, markdown, code, "python", useCases, bestPractices, questions);
        }

        // 4. GENERAL / COMPUTER SCIENCE FALLBACK
        String title = "Deep-Dive Mastery: " + partTitle;
        String summary = "Comprehensive theoretical and practical deep dive into " + partTitle + " covering architectural invariants, performance trade-offs, and production engineering best practices.";
        String markdown = """
                ### 1. Fundamental Principles & Runtime Architecture
                Mastering **%s** requires building a deterministic mental model of how components interact under load.
                
                - **Deterministic State Transformations**: Ensuring that operations produce predictable, repeatable outcomes without hidden side effects.
                - **Separation of Concerns**: Keeping domain business logic decoupled from transport, serialization, and storage layers.
                - **Defensive Boundary Validation**: Validating all inputs at system entry points before dispatching to core business services.
                
                ### 2. Computational Complexity & Performance Trade-offs
                - **Time Complexity**: Aiming for $O(1)$ or $O(\\log N)$ operations on hot execution paths.
                - **Space Complexity**: Designing bounded data structures to avoid unbounded memory expansion.
                - **Concurrency Control**: Isolate shared state to prevent race conditions and cascading deadlocks.
                
                ### 3. Production Failure Modes & Resiliency Tactics
                - **Cascading Failures**: Implement circuit breakers and graceful degradation when downstream dependencies degrade.
                - **Silent Failures**: Always log actionable context with structured metadata when catching exceptions.
                - **Resource Leaks**: Enforce strict timeouts, bounded connection pools, and proper cleanup handlers.
                """.formatted(partTitle);

        String code = """
                // Production-grade modular implementation demonstrating clean architecture & defensive validation
                public class %sMasteryService {
                
                    public void executeWorkflow(String input) {
                        if (input == null || input.isBlank()) {
                            throw new IllegalArgumentException("Input must be valid and non-empty");
                        }
                        try {
                            // Execute business logic with deterministic error boundaries
                            processCoreDomain(input);
                        } catch (Exception e) {
                            // Log actionable telemetry and fail gracefully
                            throw new RuntimeException("Workflow failed gracefully: " + e.getMessage(), e);
                        }
                    }
                
                    private void processCoreDomain(String payload) {
                        // High-performance domain execution
                    }
                }
                """.formatted(partTitle.replaceAll("[^a-zA-Z0-9]", ""));

        List<String> useCases = List.of(
                "High-scale cloud architecture handling distributed event streams with guaranteed delivery semantics.",
                "Mission-critical transaction processing service with strict consistency and latency SLA thresholds.",
                "Extensible enterprise framework utilizing modular plugin patterns and decoupled dependency injection."
        );

        List<String> bestPractices = List.of(
                "Design for failure: always configure circuit breakers, retry limits, and backpressure mechanisms.",
                "Favor composition over inheritance to keep architectures decoupled and easily maintainable.",
                "Maintain complete test coverage for edge cases, nullability, and concurrent access scenarios.",
                "Emit structured observability metrics (P95/P99 latency, error rates) to detect bottlenecks early."
        );

        List<InterviewQnADTO> questions = List.of(
                new InterviewQnADTO(
                        "Why is separation of concerns critical in modern software architecture?",
                        "Separation of concerns isolates independent business responsibilities into distinct modules. This minimizes blast radius during bugs, simplifies unit testing, and allows individual layers to evolve or scale without cascading rewrites."
                    ),
                new InterviewQnADTO(
                        "How do you design systems defensively against cascading failures?",
                        "By implementing circuit breakers (e.g. Resilience4j), request timeouts, bulkheads to isolate resource pools, and fallback strategies that return degraded or cached data rather than hanging connections."
                    ),
                new InterviewQnADTO(
                        "What is the difference between latency and throughput, and how do they trade off?",
                        "Latency is the time taken to complete a single operation (e.g., in milliseconds). Throughput is the number of operations processed per unit time (e.g., requests per second). Techniques like batching increase throughput but often slightly increase single-request latency."
                    )
        );

        return new ModuleLearnMoreResponse(title, summary, markdown, code, "code", useCases, bestPractices, questions);
    }
}

