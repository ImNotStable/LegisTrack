-- V3 migration: ingestion run ledger for idempotent scheduling
-- Rules: add new migration (do not modify prior), include created_at/updated_at, indexes.

CREATE TABLE ingestion_runs (
    id BIGSERIAL PRIMARY KEY,
    from_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL, -- IN_PROGRESS | SUCCESS | FAILURE
    started_at TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP NULL,
    document_count INTEGER DEFAULT 0,
    error_message TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Enforce only one successful run per from_date for idempotency (partial unique index)
CREATE UNIQUE INDEX ux_ingestion_runs_from_date_success ON ingestion_runs(from_date) WHERE status = 'SUCCESS';

-- Indexes to query recent runs / status transitions
CREATE INDEX idx_ingestion_runs_from_date ON ingestion_runs(from_date);
CREATE INDEX idx_ingestion_runs_status ON ingestion_runs(status);
CREATE INDEX idx_ingestion_runs_started_at ON ingestion_runs(started_at);

-- Reuse existing trigger function for updated_at if present
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'update_ingestion_runs_updated_at'
    ) THEN
        CREATE TRIGGER update_ingestion_runs_updated_at BEFORE UPDATE ON ingestion_runs FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
    END IF;
END $$;
