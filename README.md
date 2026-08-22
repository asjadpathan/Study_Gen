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

## Backend Setup

### 1. Database Configuration

The backend uses PostgreSQL. A `docker-compose.yml` file is provided to quickly spin up a database instance.

From the root directory, run:
```bash
docker-compose up -d
```
This will start a PostgreSQL container with the following credentials (defined in `application.properties`):
- **Database:** `studygen`
- **User:** `user`
- **Password:** `StudyGen123`
- **Port:** `5432`

### 2. Run the Backend

You can run the Spring Boot application using the provided Maven Wrapper:

```bash
./mvnw spring-boot:run
```
*(On Windows, use `mvnw.cmd spring-boot:run`)*

The backend will be available at `http://localhost:8080`.

---

## Frontend Setup

### 1. Install Dependencies

Navigate to the frontend directory:

```bash
cd frontend (For change the Directory)
npm install (Intalling the dependancies)
```

### 2. Run the Frontend (Development Mode)

Start the Vite development server:

```bash
npm run dev 
```

The frontend will typically be available at `http://localhost:5173`. Check the terminal output for the exact URL.

---

## Environment Variables & Configuration

### Backend
Backend configuration is located in `src/main/resources/application.properties`. It includes:
- Database connection details
- JWT settings (Secret key and expiration)

### Frontend
Vite configuration is in `frontend/vite.config.js`.

## Features
- User Authentication with JWT
- Spring Security integration
- PostgreSQL for persistent storage
- React with React Router for the UI
