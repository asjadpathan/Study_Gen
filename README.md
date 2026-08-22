# StudyGen

StudyGen is a full-stack application with a Spring Boot backend and a React frontend.

## Prerequisites

Before you begin, ensure you have the following installed on your machine:

*   **Java 21** or higher
*   **Node.js** (v18 or higher recommended) and **npm**
*   **Docker** and **Docker Compose**
*   **Maven** (optional, as `mvnw` is provided)

## Project Structure

*   `/` - Backend (Spring Boot)
*   `/frontend` - Frontend (React + Vite)

---

## Getting Started

### 1. Environment Configuration

The application requires several environment variables to run. A `.env.example` file is provided in the root directory.

1.  Copy `.env.example` to a new file named `.env`:
    ```bash
    cp .env.example .env
    ```
2.  Open the `.env` file and update the values, especially `JWT_SECRET` and `GOOGLE_GENAI_API_KEY`.

### 2. Database Setup

The backend uses PostgreSQL. A `docker-compose.yml` file is provided to quickly spin up a database instance.

From the root directory, run:
```bash
docker-compose up -d
```
This will start a PostgreSQL container with the default credentials defined in the `.env` file (via `application.properties`):
- **Database:** `studygen`
- **User:** `user`
- **Password:** `StudyGen123`
- **Port:** `5432`

---

## Backend Setup

### Run the Backend

You can run the Spring Boot application using the provided Maven Wrapper:

```bash
./mvnw spring-boot:run
```
*(On Windows, use `mvnw.cmd spring-boot:run`)*

The backend will be available at `http://localhost:8080`.

---

## Frontend Setup

### 1. Install Dependencies

Navigate to the frontend directory and install the required packages:

```bash
cd frontend
npm install
```

### 2. Run the Frontend (Development Mode)

Start the Vite development server:

```bash
npm run dev 
```

The frontend will typically be available at `http://localhost:5173`. Check the terminal output for the exact URL.

---

## Features
- User Authentication with JWT
- Spring Security integration
- PostgreSQL for persistent storage
- React with React Router for the UI
- Google Gemini AI Integration for Roadmap Generation
