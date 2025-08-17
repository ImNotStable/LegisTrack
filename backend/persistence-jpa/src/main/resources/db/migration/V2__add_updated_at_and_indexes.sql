-- V2 migration: add updated_at columns to document_actions and document_sponsors, plus indexes for query fields
-- Follows rule: never modify old migrations; create new versioned file.

ALTER TABLE document_actions
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT NOW();

ALTER TABLE document_sponsors
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT NOW();

-- Add indexes to improve common lookups / sorting
CREATE INDEX IF NOT EXISTS idx_documents_introduction_date ON documents(introduction_date);
CREATE INDEX IF NOT EXISTS idx_documents_congress_session ON documents(congress_session);
CREATE INDEX IF NOT EXISTS idx_documents_bill_type ON documents(bill_type);

-- Industry tags array may benefit from GIN index for containment queries (future tag search)
-- Using pg_trgm or btree_gin not enabled here; simple GIN for text[]
CREATE INDEX IF NOT EXISTS idx_ai_analyses_industry_tags ON ai_analyses USING GIN (industry_tags);
