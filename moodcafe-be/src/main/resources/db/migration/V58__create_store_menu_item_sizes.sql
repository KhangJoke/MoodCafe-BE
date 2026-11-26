-- =========================================================================
-- V58: STORE MENU ITEM SIZES (VARIANTS WITH PER-SIZE PRICING)
-- Allows menu items to optionally define multiple sizes (e.g. S, M, L)
-- with distinct prices while keeping single-price items 100% compatible.
-- =========================================================================

CREATE TABLE IF NOT EXISTS store_menu_item_sizes (
    size_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    item_id UUID NOT NULL REFERENCES store_menu_items(item_id) ON DELETE CASCADE,
    size_name VARCHAR(50) NOT NULL,
    price NUMERIC(12, 2) NOT NULL CHECK (price >= 0),
    display_order INT NOT NULL DEFAULT 0,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_menu_item_sizes_item ON store_menu_item_sizes(item_id, display_order);
