-- V23: Add is_highlighted column to store_tags table and initialize default highlights for active stores

-- 1. Add is_highlighted column
ALTER TABLE store_tags ADD COLUMN IF NOT EXISTS is_highlighted BOOLEAN NOT NULL DEFAULT FALSE;

-- 2. Create index for fast highlight lookup
CREATE INDEX IF NOT EXISTS idx_store_tags_highlighted ON store_tags(store_id, is_highlighted);

-- 3. Initialize default highlight tags (up to 4 per store, prioritizing approved tags with distinct categories)
WITH ranked_tags AS (
    SELECT 
        st.store_tag_id,
        ROW_NUMBER() OVER (
            PARTITION BY st.store_id 
            ORDER BY 
                CASE 
                    WHEN tc.code = 'VIBE' THEN 1 
                    WHEN tc.code = 'PURPOSE' THEN 2 
                    WHEN tc.code = 'AMENITY' THEN 3 
                    ELSE 4 
                END,
                st.created_at ASC
        ) as rn
    FROM store_tags st
    JOIN tags t ON st.tag_id = t.tag_id
    LEFT JOIN tag_categories tc ON t.tag_category_id = tc.tag_category_id
    WHERE st.status = 'APPROVED'
)
UPDATE store_tags
SET is_highlighted = TRUE
WHERE store_tag_id IN (
    SELECT store_tag_id FROM ranked_tags WHERE rn <= 4
);
