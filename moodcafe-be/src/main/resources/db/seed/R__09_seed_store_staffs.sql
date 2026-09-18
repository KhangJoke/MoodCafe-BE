-- =============================================================
-- R__09: SEED STORE STAFFS (5 OWNERS + 2 CASHIERS)
-- =============================================================

-- Owner 1 (Stores 1-4)
INSERT INTO store_staffs (store_id, user_id, store_role_id, status)
SELECT s.store_id, u.user_id, sr.store_role_id, 'ACTIVE'
FROM stores s, users u, store_roles sr
WHERE u.email = 'owner1@moodcafe.com'
  AND sr.name = 'OWNER'
  AND s.name IN ('The Workshop Specialty Coffee', 'Yên Cà Phê Mộc & Sách', 'The Hideout Espresso & Lounge', 'The Green Haven Garden')
ON CONFLICT (store_id, user_id) DO NOTHING;

-- Owner 2 (Stores 5-8)
INSERT INTO store_staffs (store_id, user_id, store_role_id, status)
SELECT s.store_id, u.user_id, sr.store_role_id, 'ACTIVE'
FROM stores s, users u, store_roles sr
WHERE u.email = 'owner2@moodcafe.com'
  AND sr.name = 'OWNER'
  AND s.name IN ('Mây Concept Rooftop Cafe', 'Cỏ Mềm Garden & Tea', 'Sống Vội Concept Workspace', 'Nhà Cổ 1985 Vintage Cafe')
ON CONFLICT (store_id, user_id) DO NOTHING;

-- Owner 3 (Stores 9-12)
INSERT INTO store_staffs (store_id, user_id, store_role_id, status)
SELECT s.store_id, u.user_id, sr.store_role_id, 'ACTIVE'
FROM stores s, users u, store_roles sr
WHERE u.email = 'owner3@moodcafe.com'
  AND sr.name = 'OWNER'
  AND s.name IN ('Mood Cafe Thao Dien', 'The Oasis Botanical Cafe', 'Artisan Lab Specialty Coffee', 'Sunset View River Cafe')
ON CONFLICT (store_id, user_id) DO NOTHING;

-- Owner 4 (Stores 13-16)
INSERT INTO store_staffs (store_id, user_id, store_role_id, status)
SELECT s.store_id, u.user_id, sr.store_role_id, 'ACTIVE'
FROM stores s, users u, store_roles sr
WHERE u.email = 'owner4@moodcafe.com'
  AND sr.name = 'OWNER'
  AND s.name IN ('Chợ Lớn Heritage Coffee', 'Deadline Zone 24/7 Workspace', 'Vườn Nhiệt Đới Sài Gòn', 'Hẻm Nhỏ Acoustic Coffee')
ON CONFLICT (store_id, user_id) DO NOTHING;

-- Owner 5 (Stores 17-20)
INSERT INTO store_staffs (store_id, user_id, store_role_id, status)
SELECT s.store_id, u.user_id, sr.store_role_id, 'ACTIVE'
FROM stores s, users u, store_roles sr
WHERE u.email = 'owner5@moodcafe.com'
  AND sr.name = 'OWNER'
  AND s.name IN ('Mood Cafe District 1', 'Mood Cafe Phu Nhuan', 'The Minimalist Studio & Cafe', 'Vòm Xanh Glasshouse Cafe')
ON CONFLICT (store_id, user_id) DO NOTHING;

-- Cashier 1 assigned to Store 1 (The Workshop Specialty Coffee)
INSERT INTO store_staffs (store_id, user_id, store_role_id, status)
SELECT s.store_id, u.user_id, sr.store_role_id, 'ACTIVE'
FROM stores s, users u, store_roles sr
WHERE u.email = 'cashier1@moodcafe.com'
  AND sr.name = 'CASHIER'
  AND s.name = 'The Workshop Specialty Coffee'
ON CONFLICT (store_id, user_id) DO NOTHING;

-- Cashier 2 assigned to Store 5 (Mây Concept Rooftop Cafe)
INSERT INTO store_staffs (store_id, user_id, store_role_id, status)
SELECT s.store_id, u.user_id, sr.store_role_id, 'ACTIVE'
FROM stores s, users u, store_roles sr
WHERE u.email = 'cashier2@moodcafe.com'
  AND sr.name = 'CASHIER'
  AND s.name = 'Mây Concept Rooftop Cafe'
ON CONFLICT (store_id, user_id) DO NOTHING;
