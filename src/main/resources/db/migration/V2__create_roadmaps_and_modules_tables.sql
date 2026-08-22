CREATE TABLE roadmaps (
                          id BIGSERIAL PRIMARY KEY,
                          user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                          title VARCHAR(255) NOT NULL,
                          target_role VARCHAR(100),
                          is_active BOOLEAN NOT NULL DEFAULT TRUE,
                          created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE modules (
                         id BIGSERIAL PRIMARY KEY,
                         roadmap_id BIGINT NOT NULL REFERENCES roadmaps(id) ON DELETE CASCADE,
                         title VARCHAR(255) NOT NULL,
                         description TEXT,
                         sequence_order INT NOT NULL,
                         is_locked BOOLEAN NOT NULL DEFAULT TRUE,
                         is_completed BOOLEAN NOT NULL DEFAULT FALSE,
                         mastery_score DOUBLE PRECISION NOT NULL DEFAULT 0.0
);

CREATE INDEX idx_roadmaps_user_id ON roadmaps(user_id);
CREATE INDEX idx_modules_roadmap_id ON modules(roadmap_id);