-- Migration V3: Add storage fields, remove BYTEA columns
-- This migration adds storage_key, content_type, image_size to drawings
-- and avatar_storage_key, avatar_content_type, avatar_size to users
-- Then removes the old BYTEA columns

-- Step 1: Add new columns to drawings
ALTER TABLE drawings ADD COLUMN IF NOT EXISTS storage_key VARCHAR(200);
ALTER TABLE drawings ADD COLUMN IF NOT EXISTS content_type VARCHAR(100);
ALTER TABLE drawings ADD COLUMN IF NOT EXISTS image_size BIGINT;

-- Step 2: Add new columns to users
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_storage_key VARCHAR(200);
-- avatar_content_type already exists from V2
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_size BIGINT;

-- Step 3: Migrate existing data from BYTEA to storage key format
-- For local storage, the storage key is "drawing:{id}" and "avatar:{id}"
UPDATE drawings SET 
    storage_key = 'drawing:' || id,
    content_type = content_type,
    image_size = octet_length(image_data)
WHERE storage_key IS NULL;

UPDATE users SET 
    avatar_storage_key = 'avatar:' || id,
    avatar_size = octet_length(avatar_data)
WHERE has_avatar AND avatar_storage_key IS NULL;

-- Step 4: Make new columns NOT NULL where appropriate
ALTER TABLE drawings ALTER COLUMN storage_key SET NOT NULL;
ALTER TABLE drawings ALTER COLUMN content_type SET NOT NULL;
ALTER TABLE drawings ALTER COLUMN image_size SET NOT NULL;

-- Step 5: Drop old NOT NULL constraints on old columns, then drop them
ALTER TABLE drawings ALTER COLUMN image_data DROP NOT NULL;
-- Don't drop content_type NOT NULL - it's used by the new column
ALTER TABLE users ALTER COLUMN avatar_data DROP NOT NULL;
-- Keep avatar_content_type - it's used by the new column

-- Step 6: Drop old BYTEA columns
ALTER TABLE drawings DROP COLUMN IF EXISTS image_data;
ALTER TABLE users DROP COLUMN IF EXISTS avatar_data;
ALTER TABLE users DROP COLUMN IF EXISTS has_avatar;

-- Step 7: Add indexes for new columns
CREATE INDEX IF NOT EXISTS idx_drawings_storage_key ON drawings (storage_key);
CREATE INDEX IF NOT EXISTS idx_users_avatar_storage_key ON users (avatar_storage_key);