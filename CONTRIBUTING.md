# Contributing to StudyGen

Thank you for your interest in contributing to **StudyGen**! We welcome bug fixes, documentation improvements, feature additions, and architectural optimizations.

---

## Code of Conduct

Please uphold a welcoming, respectful, and harassment-free environment for all contributors and community members.

---

## Development Workflow

1. **Fork the Repository**:
   Create your own fork of the StudyGen repository on GitHub.

2. **Clone & Branch**:
   ```bash
   git clone https://github.com/asjadpathan/Study_Gen.git
   cd Study_Gen
   git checkout -b feature/your-feature-name
   ```

3. **Set Up the Environment**:
   - Copy `.env.example` to `.env` in the root directory and configure local keys.
   - Run the database container: `docker compose up -d postgres-db`.
   - Run backend: `cd backend && ./mvnw spring-boot:run` (or `.\mvnw.cmd spring-boot:run` on Windows).
   - Run frontend: `cd frontend && npm install && npm run dev`.

4. **Code Standards & Linting**:
   - **Backend**: Follow standard Java and Spring Boot conventions. Keep controller endpoints RESTful, business logic in service layers, and schema changes tracked in Flyway migrations under `backend/src/main/resources/db/migration/`.
   - **Frontend**: Run `npm run lint` (`oxlint`) inside `frontend/` before committing. Follow clean component separation and avoid unnecessary dependencies.

5. **Commit Message Guidelines**:
   Use clear, conventional commit messages:
   - `feat: add AI remediation drills endpoint`
   - `fix: resolve JWT expiration token parsing issue`
   - `docs: update API contract and architecture diagrams`
   - `refactor: optimize Pomodoro session aggregation query`

6. **Submitting a Pull Request**:
   - Push your branch to your fork:
     ```bash
     git push origin feature/your-feature-name
     ```
   - Open a Pull Request targeting the `main` branch.
   - Provide a clear summary of your changes, related issue numbers, and manual/automated testing steps performed.
