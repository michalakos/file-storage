-- V3__create_file_permissions_table.sql
-- Description: Create file_permissions table for file sharing functionality

CREATE TABLE file_permissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    file_metadata_id UUID NOT NULL REFERENCES file_metadata(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    access_level VARCHAR(20) NOT NULL CHECK (access_level IN ('VIEW', 'OWNER')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Ensure unique permission per file per user
    UNIQUE(file_metadata_id, user_id)
);

-- Create indexes for performance
CREATE INDEX idx_file_permissions_file_metadata_id ON file_permissions(file_metadata_id);
CREATE INDEX idx_file_permissions_user_id ON file_permissions(user_id);
CREATE INDEX idx_file_permissions_access_level ON file_permissions(access_level);

-- Create trigger to update updated_at automatically
CREATE TRIGGER update_file_permissions_updated_at
    BEFORE UPDATE ON file_permissions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Create a view to easily find files shared with a user
CREATE VIEW shared_files AS
SELECT
    fp.user_id,
    fp.access_level,
    fm.id as file_id,
    fm.filename,
    fm.content_type,
    fm.size,
    fm.upload_date,
    u.username as owner_username
FROM file_permissions fp
JOIN file_metadata fm ON fp.file_metadata_id = fm.id
JOIN users u ON fm.owner_id = u.id
WHERE fp.access_level = 'VIEW'; -- Only show files shared with VIEW access

-- Create a view to show file ownership and permissions
CREATE VIEW file_access_summary AS
SELECT
    fm.id as file_id,
    fm.filename,
    fm.content_type,
    fm.size,
    owner.username as owner,
    COALESCE(shared_count.count, 0) as shared_with_count
FROM file_metadata fm
JOIN users owner ON fm.owner_id = owner.id
LEFT JOIN (
    SELECT file_metadata_id, COUNT(*) as count
    FROM file_permissions
    WHERE access_level = 'VIEW'
    GROUP BY file_metadata_id
) shared_count ON fm.id = shared_count.file_metadata_id;