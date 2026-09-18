-- =============================================================
-- R__01: SEED USERS (2 ADMIN, 5 STORE OWNER, 2 CASHIER STAFF, 5 CUSTOMER)
-- Password for all accounts: 123456
-- =============================================================

INSERT INTO users (email, password, full_name, avatar_url, role_id, is_active, is_email_verified, is_first_login, is_deleted)
SELECT 
    'admin1@moodcafe.com',
    '$2a$10$DvpOjEE7srLlQG4wzcgAAOHAApdp3NcdgMAKG2cUUs6fHPkntOeBW',
    'Nguyễn Quản Trị',
    'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=200',
    r.role_id, TRUE, TRUE, FALSE, FALSE
FROM roles r WHERE r.name = 'ADMIN'
ON CONFLICT (email) DO UPDATE 
SET password = EXCLUDED.password, full_name = EXCLUDED.full_name, is_active = TRUE;

INSERT INTO users (email, password, full_name, avatar_url, role_id, is_active, is_email_verified, is_first_login, is_deleted)
SELECT 
    'admin2@moodcafe.com',
    '$2a$10$DvpOjEE7srLlQG4wzcgAAOHAApdp3NcdgMAKG2cUUs6fHPkntOeBW',
    'Trần Hệ Thống',
    'https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=200',
    r.role_id, TRUE, TRUE, FALSE, FALSE
FROM roles r WHERE r.name = 'ADMIN'
ON CONFLICT (email) DO UPDATE 
SET password = EXCLUDED.password, full_name = EXCLUDED.full_name, is_active = TRUE;

-- 5 STORE OWNERS (system role MERCHANT_STAFF, store_role OWNER)
INSERT INTO users (email, password, full_name, avatar_url, role_id, is_active, is_email_verified, is_first_login, is_deleted)
SELECT 
    'owner1@moodcafe.com',
    '$2a$10$DvpOjEE7srLlQG4wzcgAAOHAApdp3NcdgMAKG2cUUs6fHPkntOeBW',
    'Lê Hoàng Phúc',
    'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200',
    r.role_id, TRUE, TRUE, FALSE, FALSE
FROM roles r WHERE r.name = 'MERCHANT_STAFF'
ON CONFLICT (email) DO UPDATE 
SET password = EXCLUDED.password, full_name = EXCLUDED.full_name, is_active = TRUE;

INSERT INTO users (email, password, full_name, avatar_url, role_id, is_active, is_email_verified, is_first_login, is_deleted)
SELECT 
    'owner2@moodcafe.com',
    '$2a$10$DvpOjEE7srLlQG4wzcgAAOHAApdp3NcdgMAKG2cUUs6fHPkntOeBW',
    'Phạm Minh Tuấn',
    'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200',
    r.role_id, TRUE, TRUE, FALSE, FALSE
FROM roles r WHERE r.name = 'MERCHANT_STAFF'
ON CONFLICT (email) DO UPDATE 
SET password = EXCLUDED.password, full_name = EXCLUDED.full_name, is_active = TRUE;

INSERT INTO users (email, password, full_name, avatar_url, role_id, is_active, is_email_verified, is_first_login, is_deleted)
SELECT 
    'owner3@moodcafe.com',
    '$2a$10$DvpOjEE7srLlQG4wzcgAAOHAApdp3NcdgMAKG2cUUs6fHPkntOeBW',
    'Võ Thị Mai Phương',
    'https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=200',
    r.role_id, TRUE, TRUE, FALSE, FALSE
FROM roles r WHERE r.name = 'MERCHANT_STAFF'
ON CONFLICT (email) DO UPDATE 
SET password = EXCLUDED.password, full_name = EXCLUDED.full_name, is_active = TRUE;

INSERT INTO users (email, password, full_name, avatar_url, role_id, is_active, is_email_verified, is_first_login, is_deleted)
SELECT 
    'owner4@moodcafe.com',
    '$2a$10$DvpOjEE7srLlQG4wzcgAAOHAApdp3NcdgMAKG2cUUs6fHPkntOeBW',
    'Đặng Hữu Phước',
    'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=200',
    r.role_id, TRUE, TRUE, FALSE, FALSE
FROM roles r WHERE r.name = 'MERCHANT_STAFF'
ON CONFLICT (email) DO UPDATE 
SET password = EXCLUDED.password, full_name = EXCLUDED.full_name, is_active = TRUE;

INSERT INTO users (email, password, full_name, avatar_url, role_id, is_active, is_email_verified, is_first_login, is_deleted)
SELECT 
    'owner5@moodcafe.com',
    '$2a$10$DvpOjEE7srLlQG4wzcgAAOHAApdp3NcdgMAKG2cUUs6fHPkntOeBW',
    'Bùi Khánh Duy',
    'https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=200',
    r.role_id, TRUE, TRUE, FALSE, FALSE
FROM roles r WHERE r.name = 'MERCHANT_STAFF'
ON CONFLICT (email) DO UPDATE 
SET password = EXCLUDED.password, full_name = EXCLUDED.full_name, is_active = TRUE;

-- 2 CASHIER STAFF (system role MERCHANT_STAFF, store_role CASHIER)
INSERT INTO users (email, password, full_name, avatar_url, role_id, is_active, is_email_verified, is_first_login, is_deleted)
SELECT 
    'cashier1@moodcafe.com',
    '$2a$10$DvpOjEE7srLlQG4wzcgAAOHAApdp3NcdgMAKG2cUUs6fHPkntOeBW',
    'Trần Thu Thảo',
    'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200',
    r.role_id, TRUE, TRUE, FALSE, FALSE
FROM roles r WHERE r.name = 'MERCHANT_STAFF'
ON CONFLICT (email) DO UPDATE 
SET password = EXCLUDED.password, full_name = EXCLUDED.full_name, is_active = TRUE;

INSERT INTO users (email, password, full_name, avatar_url, role_id, is_active, is_email_verified, is_first_login, is_deleted)
SELECT 
    'cashier2@moodcafe.com',
    '$2a$10$DvpOjEE7srLlQG4wzcgAAOHAApdp3NcdgMAKG2cUUs6fHPkntOeBW',
    'Ngô Văn Kiên',
    'https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=200',
    r.role_id, TRUE, TRUE, FALSE, FALSE
FROM roles r WHERE r.name = 'MERCHANT_STAFF'
ON CONFLICT (email) DO UPDATE 
SET password = EXCLUDED.password, full_name = EXCLUDED.full_name, is_active = TRUE;

-- 5 CUSTOMER USERS
INSERT INTO users (email, password, full_name, avatar_url, role_id, is_active, is_email_verified, is_first_login, is_deleted)
SELECT 
    'customer1@moodcafe.com',
    '$2a$10$DvpOjEE7srLlQG4wzcgAAOHAApdp3NcdgMAKG2cUUs6fHPkntOeBW',
    'Nguyễn Minh Anh',
    'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200',
    r.role_id, TRUE, TRUE, FALSE, FALSE
FROM roles r WHERE r.name = 'CUSTOMER'
ON CONFLICT (email) DO UPDATE 
SET password = EXCLUDED.password, full_name = EXCLUDED.full_name, is_active = TRUE;

INSERT INTO users (email, password, full_name, avatar_url, role_id, is_active, is_email_verified, is_first_login, is_deleted)
SELECT 
    'customer2@moodcafe.com',
    '$2a$10$DvpOjEE7srLlQG4wzcgAAOHAApdp3NcdgMAKG2cUUs6fHPkntOeBW',
    'Trần Bảo Ngọc',
    'https://images.unsplash.com/photo-1517841905240-472988babdf9?w=200',
    r.role_id, TRUE, TRUE, FALSE, FALSE
FROM roles r WHERE r.name = 'CUSTOMER'
ON CONFLICT (email) DO UPDATE 
SET password = EXCLUDED.password, full_name = EXCLUDED.full_name, is_active = TRUE;

INSERT INTO users (email, password, full_name, avatar_url, role_id, is_active, is_email_verified, is_first_login, is_deleted)
SELECT 
    'customer3@moodcafe.com',
    '$2a$10$DvpOjEE7srLlQG4wzcgAAOHAApdp3NcdgMAKG2cUUs6fHPkntOeBW',
    'Lê Quang Huy',
    'https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=200',
    r.role_id, TRUE, TRUE, FALSE, FALSE
FROM roles r WHERE r.name = 'CUSTOMER'
ON CONFLICT (email) DO UPDATE 
SET password = EXCLUDED.password, full_name = EXCLUDED.full_name, is_active = TRUE;

INSERT INTO users (email, password, full_name, avatar_url, role_id, is_active, is_email_verified, is_first_login, is_deleted)
SELECT 
    'customer4@moodcafe.com',
    '$2a$10$DvpOjEE7srLlQG4wzcgAAOHAApdp3NcdgMAKG2cUUs6fHPkntOeBW',
    'Phạm Quỳnh Như',
    'https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=200',
    r.role_id, TRUE, TRUE, FALSE, FALSE
FROM roles r WHERE r.name = 'CUSTOMER'
ON CONFLICT (email) DO UPDATE 
SET password = EXCLUDED.password, full_name = EXCLUDED.full_name, is_active = TRUE;

INSERT INTO users (email, password, full_name, avatar_url, role_id, is_active, is_email_verified, is_first_login, is_deleted)
SELECT 
    'customer5@moodcafe.com',
    '$2a$10$DvpOjEE7srLlQG4wzcgAAOHAApdp3NcdgMAKG2cUUs6fHPkntOeBW',
    'Hoàng Đức Long',
    'https://images.unsplash.com/photo-1501196354995-cbb51c65aaea?w=200',
    r.role_id, TRUE, TRUE, FALSE, FALSE
FROM roles r WHERE r.name = 'CUSTOMER'
ON CONFLICT (email) DO UPDATE 
SET password = EXCLUDED.password, full_name = EXCLUDED.full_name, is_active = TRUE;
