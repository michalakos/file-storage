-- V2__create_file_metadata_table.sql
-- Description: Create file_metadata table for storing file information

CREATE TABLE file_metadata (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    size BIGINT NOT NULL,
    original_file_size BIGINT NOT NULL,
    upload_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    storage_path VARCHAR(500) UNIQUE NOT NULL,
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Create indexes for performance
CREATE INDEX idx_file_metadata_owner_id ON file_metadata(owner_id);
CREATE INDEX idx_file_metadata_filename ON file_metadata(filename);
CREATE INDEX idx_file_metadata_content_type ON file_metadata(content_type);
CREATE INDEX idx_file_metadata_upload_date ON file_metadata(upload_date);
CREATE INDEX idx_file_metadata_size ON file_metadata(size);

-- Create trigger to update updated_at automatically
CREATE TRIGGER update_file_metadata_updated_at
    BEFORE UPDATE ON file_metadata
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Create a view for file statistics per user
CREATE VIEW user_file_stats AS
SELECT
    owner_id,
    COUNT(*) as total_files,
    SUM(size) as total_size,
    AVG(size) as avg_file_size,
    MAX(upload_date) as last_upload_date
FROM file_metadata
GROUP BY owner_id;