# StudyGen

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg?style=flat-square)](https://github.com/asjadpathan/Study_Gen/actions)
[![Version](https://img.shields.io/badge/version-1.0.0-blue.svg?style=flat-square)](https://github.com/asjadpathan/Study_Gen/releases)
[![License: MIT](https://img.shields.io/badge/license-MIT-green.svg?style=flat-square)](LICENSE)
[![Java 21](https://img.shields.io/badge/Java-21-orange.svg?style=flat-square&logo=openjdk)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-brightgreen.svg?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-2.0.0-green.svg?style=flat-square)](https://spring.io/projects/spring-ai)
[![React](https://img.shields.io/badge/React-19.2-61DAFB.svg?style=flat-square&logo=react)](https://react.dev/)
[![Vite](https://img.shields.io/badge/Vite-8.1-646CFF.svg?style=flat-square&logo=vite)](https://vitejs.dev/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1.svg?style=flat-square&logo=postgresql)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED.svg?style=flat-square&logo=docker)](https://www.docker.com/)

---

## Project Description

**StudyGen** is an enterprise-grade, AI-powered adaptive learning and skill-mastery platform designed to bridge the divide between surface-level video tutorials and verifiable engineering proficiency. The system replaces static curricula with dynamic intelligence: it evaluates student baselines via an adaptive diagnostic assessment, generates granular multi-module learning roadmaps, delivers structured three-tier curriculum instruction (Foundations, Architecture, Production Edge Cases), and strictly enforces progression through an automated 80% passing threshold gate.

Built with a modern reactive React frontend and a robust Spring Boot microservice backend, StudyGen integrates Spring AI with Google Gemini models, multi-stage Docker containerization, Flyway database version control, and RAG-driven document tutoring.

---

## Key Features

- **Baseline Diagnostic Assessment**: Assesses learner competencies across foundational, architectural, and practical dimensions to synthesize personalized syllabi targeting verified knowledge gaps.
- **Three-Tier Progressive Curriculum**: Delivers pedagogical content in structured stages: Part 1 (Fundamentals & Syntax), Part 2 (System Architecture & Runnable Code), and Part 3 (Optimization, Concurrency, and Production Edge Cases).
- **Strict Mastery Gating (80% Rule)**: Enforces sequential module mastery by locking downstream modules until the student achieves an 80% or higher score on the module evaluation.
- **Targeted AI Remediation & Drills**: Automatically triggers focused remedial lessons, targeted concept drills, and spaced repetition queues when evaluation scores fall below the passing threshold.
- **Deep Focus Study Room & Analytics**: Incorporates customizable Pomodoro sprints (25/5 and 50/10 intervals), ambient audio generators, weekly study time distributions, and GitHub-style 60-day activity heatmaps.
- **Document Intelligence (RAG Tutor)**: Ingests technical documentation and PDFs, extracts key concepts and flashcards, and provides an interactive context-aware AI tutor for document exploration.
- **Career Pathways & Readiness Navigator**: Benchmarks user competencies against market demands, calculates role-readiness scores, and outlines concrete skill bridge roadmaps.
- **Practice Hub & Spaced Repetition**: Provides categorized challenge sets, timed drill exams, and automated concept retention reviews.
- **Curated Resource Catalog**: Delivers a searchable, categorized repository of official documentation, cheat sheets, guides, and developer hubs.

---

## Tech Stack

### Backend Architecture
| Layer / Component | Technology | Version | Purpose |
|---|---|---|---|
| **Runtime Environment** | Java (OpenJDK) | 21 (LTS) | Core platform runtime utilizing virtual threads and modern language features |
| **Framework** | Spring Boot | 4.1.0 | Core microservice framework providing dependency injection and web controllers |
| **Generative AI** | Spring AI / Google GenAI | 2.0.0 / `gemini-3.6-flash` | Orchestrates LLM prompt pipelines, syllabus generation, and RAG tutor workflows |
| **Security & Auth** | Spring Security & JJWT | 0.12.5 | Stateless JWT authentication with HMAC SHA-256 signing and endpoint authorization |
| **Persistence (ORM)** | Spring Data JPA / Hibernate | 6.x | Entity mapping, relational queries, and transactional data access |
| **Database Migrations** | Flyway | Built-in | Automated schema versioning and sequential DDL migrations (V1 through V12) |
| **Configuration** | Dotenv Java | 3.0.0 | Environment configuration loader for local execution |
| **Build Automation** | Apache Maven | 3.9+ | Dependency resolution and lifecycle management with Maven Wrapper (`mvnw`) |

### Frontend Architecture
| Layer / Component | Technology | Version | Purpose |
|---|---|---|---|
| **UI Library** | React | 19.2.7 | Declarative user interfaces utilizing concurrent features and modern hooks |
| **Build Tooling** | Vite | 8.1.1 | High-speed ESM bundler and Hot Module Replacement (HMR) development server |
| **Routing** | React Router | 7.18.2 | Client-side routing, protected route guards, and nested layouts |
| **HTTP Client** | Axios | 1.20.0 | Promise-based HTTP client with global JWT interceptors and error handling |
| **Form Management** | React Hook Form | 7.88.0 | Performant, uncontrolled form state handling and validation |
| **Sanitization** | DOMPurify | 3.4.16 | Client-side HTML sanitization for rendered Markdown and code explanations |
| **Styling** | Vanilla CSS Modules | CSS3 | Scoped styles, design system tokens, responsive grids, and glassmorphism |
| **Linter** | Oxlint | 1.71.0 | Fast JavaScript and React AST code quality analysis |

### Infrastructure & Persistence
| Component | Technology | Version | Purpose |
|---|---|---|---|
| **Relational Database** | PostgreSQL | 16 (Alpine) | Primary ACID-compliant persistent relational storage |
| **Containerization** | Docker & Docker Compose | 24+ | Multi-service local orchestration and reproducible deployment images |
| **Reverse Proxy** | Nginx | Alpine | Reverse proxy, static asset delivery, and SSL termination |
| **CI/CD Automation** | GitHub Actions | v4 | Automated testing and SSH deployment pipeline to AWS EC2 |

---

## Directory Structure

```text
Study_Gen/
├── .github/
│   └── workflows/                # CI/CD deployment pipelines (EC2 orchestration)
├── backend/
│   ├── src/                      # Spring Boot Java application source and resources
│   ├── Dockerfile                # Multi-stage container build for Java runtime
│   ├── pom.xml                   # Maven dependencies, plugins, and build lifecycle
│   └── .env.example              # Backend environment template
├── frontend/
│   ├── src/                      # React pages, components, services, and hooks
│   ├── public/                   # Static browser assets and icons
│   ├── Dockerfile                # Nginx production build container configuration
│   ├── nginx.conf                # Web server reverse proxy configuration
│   ├── package.json              # Frontend scripts and dependencies
│   └── vite.config.js            # Bundler configuration and dev proxy settings
├── docs/
│   ├── CORE_LOGIC_DOCUMENTATION.md # In-depth platform logic and architectural specs
│   ├── ai_features_overview.md   # Detailed AI flow architecture and GenAI prompts
│   └── analysis_results.md       # Quality audits, benchmarks, and verification logs
├── docker-compose.yml             # Orchestration for PostgreSQL, backend, and frontend
├── .env.example                  # Root environment configuration template
├── CONTRIBUTING.md               # Developer setup and contribution guidelines
├── LICENSE                       # MIT open source license
└── README.md                     # Platform overview and technical documentation
```

---

## Getting Started

### Prerequisites

Ensure the following environments are installed on your workstation:
- **Java Development Kit (JDK)**: Version 21 or higher (`java -version`)
- **Node.js & npm**: Node.js 20+ and npm 10+ (`node -v`, `npm -v`)
- **Docker & Docker Compose**: Docker 24+ (`docker --version`, `docker compose version`)
- **Google Gemini API Key**: Valid API key from [Google AI Studio](https://aistudio.google.com/)

---

### Step-by-Step Installation

#### 1. Clone the Repository
```bash
git clone https://github.com/asjadpathan/Study_Gen.git
cd Study_Gen
```

#### 2. Configure Environment Variables
Copy `.env.example` to create your local `.env` file:

```bash
# On Linux / macOS:
cp .env.example .env

# On Windows (PowerShell):
Copy-Item .env.example .env
```

Open `.env` and set your credentials:
```ini
# PostgreSQL
DB_HOST=localhost
DB_PORT=5432
DB_NAME=studygen
DB_USERNAME=user
DB_PASSWORD=StudyGen123
DB_URL=jdbc:postgresql://localhost:5432/studygen

# Spring Boot & Security
SERVER_PORT=8080
BACKEND_PORT=8080
JWT_SECRET=studygen_super_secret_jwt_key_2026_at_least_32_bytes_long!
JWT_EXPIRATION=86400000

# Google Gemini AI
GOOGLE_GENAI_API_KEY=your_gemini_api_key_here
GEMINI_MODEL=gemini-3.6-flash

# Frontend Development
VITE_PORT=5173
VITE_API_URL=/api
VITE_BACKEND_TARGET=http://localhost:8080
```

---

### Local Development Setup

#### Option A: Run Full Stack with Docker Compose (Recommended)
Spins up PostgreSQL, the Spring Boot backend, and the frontend reverse proxy container:

```bash
docker compose up -d
```
- Frontend application: `http://localhost`
- Backend API: `http://localhost:8080`
- PostgreSQL: `localhost:5432`

To shut down containers:
```bash
docker compose down
```

#### Option B: Run Standalone Services Locally

**1. Start Database Container:**
```bash
docker compose up -d postgres-db
```

**2. Start Spring Boot Backend:**
```bash
# Linux / macOS
cd backend
chmod +x ./mvnw
./mvnw spring-boot:run

# Windows (PowerShell)
cd backend
.\mvnw.cmd spring-boot:run
```
*Backend initializes Flyway migrations `V1` through `V12` automatically and serves on `http://localhost:8080`.*

**3. Start Frontend Development Server:**
```bash
cd frontend
npm install
npm run dev
```
*Frontend hot-module reloading server runs at `http://localhost:5173`.*

---

## Usage Examples

### 1. User Authentication
Register a new account and receive a stateless JWT access token:

```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "alex_dev",
    "email": "alex@example.com",
    "password": "SecurePassword123!"
  }'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "alex@example.com",
    "password": "SecurePassword123!"
  }'
```

*Response:*
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "username": "alex_dev",
  "email": "alex@example.com"
}
```

### 2. Generate an Adaptive Roadmap
Submit diagnostic test responses to trigger AI syllabus generation:

```bash
curl -X POST http://localhost:8080/api/roadmaps/generate \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "Distributed Systems & Kafka",
    "targetRole": "Backend Systems Engineer",
    "diagnosticScore": 3,
    "totalQuestions": 5,
    "answers": [
      { "questionId": "q1", "selectedOption": 2, "correct": true },
      { "questionId": "q2", "selectedOption": 0, "correct": false }
    ]
  }'
```

### 3. Submit Module Assessment (80% Passing Rule)
Submit answers for a completed module test. Achieving $\ge 80\%$ unlocks the next sequential module:

```bash
curl -X POST http://localhost:8080/api/modules/42/submit-test \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "submissions": [
      { "questionId": "m42_1", "selectedOption": 1 },
      { "questionId": "m42_2", "selectedOption": 3 },
      { "questionId": "m42_3", "selectedOption": 0 },
      { "questionId": "m42_4", "selectedOption": 2 },
      { "questionId": "m42_5", "selectedOption": 1 }
    ]
  }'
```

*Success Response (Score $\ge 80\%$):*
```json
{
  "score": 100.0,
  "passed": true,
  "nextModuleUnlocked": 43,
  "message": "Module mastered successfully. Module 43 is now unlocked."
}
```

### 4. Query RAG Document AI Tutor
Ask contextual technical questions grounded in uploaded documentation:

```bash
curl -X POST http://localhost:8080/api/documents/12/chat \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Explain how partitioning affects consumer group rebalancing in this document."
  }'
```

---

## Contributing

Contributions, bug reports, and pull requests are welcome. For guidelines on code conventions, branch strategies, and pull request workflows, please read [CONTRIBUTING.md](CONTRIBUTING.md).

---

## License

This project is licensed under the **MIT License**. For complete terms and permissions, see the [LICENSE](LICENSE) file.
