CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       email VARCHAR(255) UNIQUE NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       full_name VARCHAR(255) NOT NULL,
                       current_streak INTEGER,
                       total_study_minutes INTEGER,
                       created_at TIMESTAMP NOT NULL
);