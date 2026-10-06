ALTER TABLE uploaded_documents
ADD COLUMN status VARCHAR(50) NOT NULL DEFAULT 'Ready',
ADD COLUMN roadmap_id BIGINT NULL REFERENCES roadmaps(id) ON DELETE SET NULL,
ADD COLUMN summary_overview TEXT NULL,
ADD COLUMN summary_key_points_json TEXT NULL,
ADD COLUMN summary_audience VARCHAR(255) NULL,
ADD COLUMN notes_markdown TEXT NULL;

CREATE INDEX idx_docs_roadmap_id ON uploaded_documents(roadmap_id);
