-- Initial database schema for LegisTrack

-- Documents table
CREATE TABLE documents (
    id BIGSERIAL PRIMARY KEY,
    bill_id VARCHAR(50) UNIQUE NOT NULL,
    title TEXT NOT NULL,
    official_summary TEXT,
    introduction_date DATE,
    congress_session INTEGER,
    bill_type VARCHAR(20),
    full_text_url VARCHAR(500),
    status VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Sponsors table
CREATE TABLE sponsors (
    id BIGSERIAL PRIMARY KEY,
    bioguide_id VARCHAR(20) UNIQUE NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    party VARCHAR(10),
    state VARCHAR(2),
    district VARCHAR(10),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Document sponsors join table
CREATE TABLE document_sponsors (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    sponsor_id BIGINT NOT NULL REFERENCES sponsors(id) ON DELETE CASCADE,
    is_primary_sponsor BOOLEAN DEFAULT FALSE,
    sponsor_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(document_id, sponsor_id)
);

-- Document actions table
CREATE TABLE document_actions (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    action_date DATE NOT NULL,
    action_type VARCHAR(100),
    action_text TEXT NOT NULL,
    chamber VARCHAR(20),
    action_code VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- AI analyses table
CREATE TABLE ai_analyses (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    general_effect_text TEXT,
    economic_effect_text TEXT,
    industry_tags TEXT[], -- PostgreSQL array for tags
    is_valid BOOLEAN DEFAULT TRUE,
    analysis_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    model_used VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX idx_documents_bill_id ON documents(bill_id);
CREATE INDEX idx_documents_introduction_date ON documents(introduction_date);
CREATE INDEX idx_sponsors_bioguide_id ON sponsors(bioguide_id);
CREATE INDEX idx_document_sponsors_document_id ON document_sponsors(document_id);
CREATE INDEX idx_document_sponsors_sponsor_id ON document_sponsors(sponsor_id);
CREATE INDEX idx_document_actions_document_id ON document_actions(document_id);
CREATE INDEX idx_document_actions_date ON document_actions(action_date);
CREATE INDEX idx_ai_analyses_document_id ON ai_analyses(document_id);
CREATE INDEX idx_ai_analyses_is_valid ON ai_analyses(is_valid);

-- Create updated_at trigger function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for updated_at
CREATE TRIGGER update_documents_updated_at BEFORE UPDATE ON documents FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_sponsors_updated_at BEFORE UPDATE ON sponsors FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_ai_analyses_updated_at BEFORE UPDATE ON ai_analyses FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
