CREATE TABLE user_job_matches (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    job_title VARCHAR(255) NOT NULL,
    company VARCHAR(255),
    seniority VARCHAR(100),
    match_percentage INT NOT NULL DEFAULT 0,
    responsibilities_json TEXT,
    matched_skills_json TEXT,
    missing_skills_json TEXT,
    skills_analysis_json TEXT,
    suggested_modules_json TEXT,
    actionable_advice TEXT,
    job_description TEXT,
    source_url VARCHAR(1024),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_job_matches_user_id ON user_job_matches(user_id);
