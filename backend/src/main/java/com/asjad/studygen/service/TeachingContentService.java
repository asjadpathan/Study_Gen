package com.asjad.studygen.service;

import com.asjad.studygen.controller.ModuleController.ModuleSubPartDTO;
import com.asjad.studygen.entity.Module;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TeachingContentService {

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
}
