-- =========================================================
-- V3: SEED DEVELOPMENT DATA
-- Users, Stores, and Store Staff Assignments
-- =========================================================

-- ---------------------------------------------------------
-- 1. SEED USERS
-- Password hash: $2a$12$L9b/UfjlHCb5Z8HA5xzc4evpNaGQyqzcTfE6bRF7UmkeUG2ct1Uty - 12345
-- ---------------------------------------------------------

INSERT INTO users (
    email,
    password,
    user_name,
    role_id,
    is_active,
    is_email_verified,
    require_password_change
)
VALUES
    (
        'admin@moodcafe.com',
        '$2a$12$L9b/UfjlHCb5Z8HA5xzc4evpNaGQyqzcTfE6bRF7UmkeUG2ct1Uty',
        'System Admin',
        (SELECT role_id FROM roles WHERE name = 'ADMIN'),
        TRUE,
        TRUE,
        FALSE
    ),
    (
        'owner1@moodcafe.com',
        '$2a$12$L9b/UfjlHCb5Z8HA5xzc4evpNaGQyqzcTfE6bRF7UmkeUG2ct1Uty',
        'Nguyen Van An',
        (SELECT role_id FROM roles WHERE name = 'CUSTOMER'),
        TRUE,
        TRUE,
        FALSE
    ),
    (
        'owner2@moodcafe.com',
        '$2a$12$L9b/UfjlHCb5Z8HA5xzc4evpNaGQyqzcTfE6bRF7UmkeUG2ct1Uty',
        'Tran Minh Khang',
        (SELECT role_id FROM roles WHERE name = 'CUSTOMER'),
        TRUE,
        TRUE,
        FALSE
    ),
    (
        'staff1@moodcafe.com',
        '$2a$12$L9b/UfjlHCb5Z8HA5xzc4evpNaGQyqzcTfE6bRF7UmkeUG2ct1Uty',
        'Le Minh Duc',
        (SELECT role_id FROM roles WHERE name = 'CUSTOMER'),
        TRUE,
        TRUE,
        FALSE
    ),
    (
        'staff2@moodcafe.com',
        '$2a$12$L9b/UfjlHCb5Z8HA5xzc4evpNaGQyqzcTfE6bRF7UmkeUG2ct1Uty',
        'Pham Gia Bao',
        (SELECT role_id FROM roles WHERE name = 'CUSTOMER'),
        TRUE,
        TRUE,
        FALSE
    )
ON CONFLICT (email) DO NOTHING;


-- ---------------------------------------------------------
-- 2. SEED STORES
-- Status: ACTIVE
-- ---------------------------------------------------------

INSERT INTO stores (
    name,
    description,
    address,
    latitude,
    longitude,
    opening_time,
    closing_time,
    price_range,
    phone,
    email,
    status
)
SELECT
    'Mood Cafe District 1',
    'A cozy cafe in the heart of District 1.',
    '123 Nguyen Hue, District 1, Ho Chi Minh City',
    10.7735000,
    106.7042000,
    '07:00:00'::TIME,
    '22:30:00'::TIME,
    '35.000 - 75.000 VND',
    '02838221234',
    'district1@moodcafe.com',
    'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM stores WHERE name = 'Mood Cafe District 1');

INSERT INTO stores (
    name,
    description,
    address,
    latitude,
    longitude,
    opening_time,
    closing_time,
    price_range,
    phone,
    email,
    status
)
SELECT
    'Mood Cafe Thao Dien',
    'A relaxing cafe designed for work, study, and conversations.',
    '45 Xuan Thuy, Thao Dien, Thu Duc City, Ho Chi Minh City',
    10.8035000,
    106.7328000,
    '07:00:00'::TIME,
    '23:00:00'::TIME,
    '40.000 - 85.000 VND',
    '02838225678',
    'thaodien@moodcafe.com',
    'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM stores WHERE name = 'Mood Cafe Thao Dien');

INSERT INTO stores (
    name,
    description,
    address,
    latitude,
    longitude,
    opening_time,
    closing_time,
    price_range,
    phone,
    email,
    status
)
SELECT
    'Mood Cafe Phu Nhuan',
    'A modern neighborhood cafe with a calm atmosphere.',
    '88 Phan Dang Luu, Phu Nhuan District, Ho Chi Minh City',
    10.7981000,
    106.6830000,
    '06:30:00'::TIME,
    '22:00:00'::TIME,
    '30.000 - 65.000 VND',
    '02838229999',
    'phunhuan@moodcafe.com',
    'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM stores WHERE name = 'Mood Cafe Phu Nhuan');


-- ---------------------------------------------------------
-- 3. SEED STORE STAFF & OWNERSHIP ASSIGNMENTS
-- ---------------------------------------------------------
-- Relationships:
--   owner1 -> Mood Cafe District 1 (OWNER)
--   owner1 -> Mood Cafe Thao Dien (OWNER)
--   owner2 -> Mood Cafe Phu Nhuan (OWNER)
--   staff1 -> Mood Cafe District 1 (STAFF)
--   staff1 -> Mood Cafe Thao Dien (STAFF)
--   staff2 -> Mood Cafe District 1 (STAFF)
--   staff2 -> Mood Cafe Phu Nhuan (STAFF)
-- ---------------------------------------------------------

INSERT INTO store_staffs (
    store_id,
    user_id,
    store_role_id,
    status,
    joined_at
)
VALUES
    -- Store 1 (District 1): Owner 1
    (
        (SELECT store_id FROM stores WHERE name = 'Mood Cafe District 1'),
        (SELECT user_id FROM users WHERE email = 'owner1@moodcafe.com'),
        (SELECT store_role_id FROM store_roles WHERE name = 'OWNER'),
        'ACTIVE',
        CURRENT_TIMESTAMP
    ),
    -- Store 1 (District 1): Staff 1
    (
        (SELECT store_id FROM stores WHERE name = 'Mood Cafe District 1'),
        (SELECT user_id FROM users WHERE email = 'staff1@moodcafe.com'),
        (SELECT store_role_id FROM store_roles WHERE name = 'STAFF'),
        'ACTIVE',
        CURRENT_TIMESTAMP
    ),
    -- Store 1 (District 1): Staff 2
    (
        (SELECT store_id FROM stores WHERE name = 'Mood Cafe District 1'),
        (SELECT user_id FROM users WHERE email = 'staff2@moodcafe.com'),
        (SELECT store_role_id FROM store_roles WHERE name = 'STAFF'),
        'ACTIVE',
        CURRENT_TIMESTAMP
    ),

    -- Store 2 (Thao Dien): Owner 1
    (
        (SELECT store_id FROM stores WHERE name = 'Mood Cafe Thao Dien'),
        (SELECT user_id FROM users WHERE email = 'owner1@moodcafe.com'),
        (SELECT store_role_id FROM store_roles WHERE name = 'OWNER'),
        'ACTIVE',
        CURRENT_TIMESTAMP
    ),
    -- Store 2 (Thao Dien): Staff 1
    (
        (SELECT store_id FROM stores WHERE name = 'Mood Cafe Thao Dien'),
        (SELECT user_id FROM users WHERE email = 'staff1@moodcafe.com'),
        (SELECT store_role_id FROM store_roles WHERE name = 'STAFF'),
        'ACTIVE',
        CURRENT_TIMESTAMP
    ),

    -- Store 3 (Phu Nhuan): Owner 2
    (
        (SELECT store_id FROM stores WHERE name = 'Mood Cafe Phu Nhuan'),
        (SELECT user_id FROM users WHERE email = 'owner2@moodcafe.com'),
        (SELECT store_role_id FROM store_roles WHERE name = 'OWNER'),
        'ACTIVE',
        CURRENT_TIMESTAMP
    ),
    -- Store 3 (Phu Nhuan): Staff 2
    (
        (SELECT store_id FROM stores WHERE name = 'Mood Cafe Phu Nhuan'),
        (SELECT user_id FROM users WHERE email = 'staff2@moodcafe.com'),
        (SELECT store_role_id FROM store_roles WHERE name = 'STAFF'),
        'ACTIVE',
        CURRENT_TIMESTAMP
    )
ON CONFLICT (store_id, user_id) DO NOTHING;
