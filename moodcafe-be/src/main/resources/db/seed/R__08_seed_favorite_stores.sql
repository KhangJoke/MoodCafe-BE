-- =============================================================
-- R__08: SEED 15 FAVORITE STORES FOR CUSTOMERS
-- =============================================================

INSERT INTO favorite_stores (user_id, store_id)
SELECT u.user_id, s.store_id
FROM users u, stores s
WHERE (
    (u.email = 'customer1@moodcafe.com' AND s.name IN ('The Workshop Specialty Coffee', 'Mood Cafe District 1', 'Sống Vội Concept Workspace', 'Artisan Lab Specialty Coffee')) OR
    (u.email = 'customer2@moodcafe.com' AND s.name IN ('The Workshop Specialty Coffee', 'Yên Cà Phê Mộc & Sách', 'Nhà Cổ 1985 Vintage Cafe')) OR
    (u.email = 'customer3@moodcafe.com' AND s.name IN ('The Hideout Espresso & Lounge', 'Mood Cafe Thao Dien', 'Sunset View River Cafe')) OR
    (u.email = 'customer4@moodcafe.com' AND s.name IN ('The Green Haven Garden', 'Mây Concept Rooftop Cafe', 'The Oasis Botanical Cafe')) OR
    (u.email = 'customer5@moodcafe.com' AND s.name IN ('Yên Cà Phê Mộc & Sách', 'Cỏ Mềm Garden & Tea'))
)
ON CONFLICT (user_id, store_id) DO NOTHING;
