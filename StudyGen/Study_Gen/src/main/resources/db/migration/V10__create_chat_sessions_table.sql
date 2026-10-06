CREATE TABLE chat_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    context_tag VARCHAR(100) NOT NULL DEFAULT 'General',
    module_id BIGINT NULL REFERENCES modules(id) ON DELETE SET NULL,
    roadmap_id BIGINT NULL REFERENCES roadmaps(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_activity_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_session_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_chat_sessions_user ON chat_sessions(user_id, last_activity_at DESC);

ALTER TABLE study_chat_messages
ADD COLUMN session_id BIGINT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
ADD COLUMN citations_json TEXT NULL;

CREATE INDEX idx_chat_messages_session ON study_chat_messages(session_id);
