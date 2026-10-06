CREATE TABLE user_concept_reviews (
    id BIGSERIAL PRIMARY KEY,
    concept_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    easiness_factor DOUBLE PRECISION NOT NULL DEFAULT 2.5,
    interval_days INT NOT NULL DEFAULT 1,
    repetition_number INT NOT NULL DEFAULT 0,
    next_review_date DATE NOT NULL,
    last_reviewed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reviews_concept FOREIGN KEY (concept_id) REFERENCES concepts(id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_reviews_user_next_date ON user_concept_reviews(user_id, next_review_date);
