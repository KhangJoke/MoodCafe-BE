-- =============================================================
-- R__12: SEED MERCHANT SUBSCRIPTIONS & SPONSORED LISTINGS
-- Provides active packages for store owners and sample sponsored listings
-- =============================================================

-- 1. Seed Active Subscriptions for Store Owners
-- Owner 1 (Stores 1-4) -> PREMIUM package
INSERT INTO user_subscriptions (user_id, subscription_plan_id, start_date, end_date, status, monthly_free_sponsored_used)
SELECT u.user_id, p.subscription_plan_id, CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP + INTERVAL '20 days', 'ACTIVE', 0
FROM users u, subscription_plans p
WHERE u.email = 'owner1@moodcafe.com' AND p.name = 'PREMIUM'
  AND NOT EXISTS (SELECT 1 FROM user_subscriptions us WHERE us.user_id = u.user_id AND us.status = 'ACTIVE');

-- Owner 2 (Stores 5-8) -> PRO package
INSERT INTO user_subscriptions (user_id, subscription_plan_id, start_date, end_date, status, monthly_free_sponsored_used)
SELECT u.user_id, p.subscription_plan_id, CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP + INTERVAL '25 days', 'ACTIVE', 0
FROM users u, subscription_plans p
WHERE u.email = 'owner2@moodcafe.com' AND p.name = 'PRO'
  AND NOT EXISTS (SELECT 1 FROM user_subscriptions us WHERE us.user_id = u.user_id AND us.status = 'ACTIVE');

-- Owner 3 (Stores 9-12) -> BASIC package
INSERT INTO user_subscriptions (user_id, subscription_plan_id, start_date, end_date, status, monthly_free_sponsored_used)
SELECT u.user_id, p.subscription_plan_id, CURRENT_TIMESTAMP - INTERVAL '30 days', NULL, 'ACTIVE', 0
FROM users u, subscription_plans p
WHERE u.email = 'owner3@moodcafe.com' AND p.name = 'BASIC'
  AND NOT EXISTS (SELECT 1 FROM user_subscriptions us WHERE us.user_id = u.user_id AND us.status = 'ACTIVE');

-- Owner 4 (Stores 13-16) -> BASIC package
INSERT INTO user_subscriptions (user_id, subscription_plan_id, start_date, end_date, status, monthly_free_sponsored_used)
SELECT u.user_id, p.subscription_plan_id, CURRENT_TIMESTAMP - INTERVAL '30 days', NULL, 'ACTIVE', 0
FROM users u, subscription_plans p
WHERE u.email = 'owner4@moodcafe.com' AND p.name = 'BASIC'
  AND NOT EXISTS (SELECT 1 FROM user_subscriptions us WHERE us.user_id = u.user_id AND us.status = 'ACTIVE');

-- Owner 5 (Stores 17-20) -> BASIC package
INSERT INTO user_subscriptions (user_id, subscription_plan_id, start_date, end_date, status, monthly_free_sponsored_used)
SELECT u.user_id, p.subscription_plan_id, CURRENT_TIMESTAMP - INTERVAL '30 days', NULL, 'ACTIVE', 0
FROM users u, subscription_plans p
WHERE u.email = 'owner5@moodcafe.com' AND p.name = 'BASIC'
  AND NOT EXISTS (SELECT 1 FROM user_subscriptions us WHERE us.user_id = u.user_id AND us.status = 'ACTIVE');

-- 2. Seed Sample Sponsored Listings
INSERT INTO sponsored_listings (
    store_id, user_id, placement, duration_type, start_date, end_date,
    amount, is_free_quota_used, status, view_count, click_count, title,
    custom_banner_url, payment_method, payment_status
)
SELECT s.store_id, u.user_id, 'TOP_BANNER_VIP', 'ONE_MONTH',
       CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP + INTERVAL '25 days',
       480000, FALSE, 'ACTIVE', 1240, 185, 'Specialty Coffee Đích Thực Tại Trung Tâm',
       'https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?auto=format&fit=crop&w=1200&q=80',
       'VNPAY', 'SUCCESS'
FROM stores s, users u
WHERE s.name = 'The Workshop Specialty Coffee' AND u.email = 'owner1@moodcafe.com'
  AND NOT EXISTS (SELECT 1 FROM sponsored_listings sl WHERE sl.store_id = s.store_id AND sl.is_deleted = FALSE);

INSERT INTO sponsored_listings (
    store_id, user_id, placement, duration_type, start_date, end_date,
    amount, is_free_quota_used, status, view_count, click_count, title,
    payment_method, payment_status
)
SELECT s.store_id, u.user_id, 'WEEKEND_PICKS', 'ONE_WEEK',
       CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP + INTERVAL '5 days',
       120000, FALSE, 'ACTIVE', 680, 92, 'Rooftop ngắm hoàng hôn cực chill cuối tuần',
       'MOMO', 'SUCCESS'
FROM stores s, users u
WHERE s.name = 'Mây Concept Rooftop Cafe' AND u.email = 'owner2@moodcafe.com'
  AND NOT EXISTS (SELECT 1 FROM sponsored_listings sl WHERE sl.store_id = s.store_id AND sl.is_deleted = FALSE);

INSERT INTO sponsored_listings (
    store_id, user_id, placement, duration_type, start_date, end_date,
    amount, is_free_quota_used, status, view_count, click_count, title,
    payment_method, payment_status
)
SELECT s.store_id, u.user_id, 'NEW_OPENING', 'ONE_WEEK',
       CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP + INTERVAL '6 days',
       100000, FALSE, 'ACTIVE', 430, 58, 'Không gian xanh ngập tràn ánh nắng',
       'VNPAY', 'SUCCESS'
FROM stores s, users u
WHERE s.name = 'The Green Haven Garden' AND u.email = 'owner1@moodcafe.com'
  AND NOT EXISTS (SELECT 1 FROM sponsored_listings sl WHERE sl.store_id = s.store_id AND sl.is_deleted = FALSE);
