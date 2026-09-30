-- =========================================================================
-- V34: STORE MENU CATEGORIES AND MENU ITEMS SCHEMA
-- Supports menu management for cafe merchants and menu display for customers
-- =========================================================================

CREATE TABLE IF NOT EXISTS store_menu_categories (
    category_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL REFERENCES stores(store_id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    display_order INT DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS store_menu_items (
    item_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL REFERENCES stores(store_id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES store_menu_categories(category_id) ON DELETE RESTRICT,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    price NUMERIC(12,2) NOT NULL DEFAULT 0,
    image_url VARCHAR(500),
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_menu_categories_store ON store_menu_categories(store_id, is_deleted);
CREATE INDEX IF NOT EXISTS idx_menu_items_store ON store_menu_items(store_id, is_deleted);
CREATE INDEX IF NOT EXISTS idx_menu_items_category ON store_menu_items(category_id, is_deleted);
