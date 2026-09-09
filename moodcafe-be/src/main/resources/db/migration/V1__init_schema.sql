-- =========================================================
-- MOODCAFE - INITIAL DATABASE SCHEMA
-- UUID VERSION
-- =========================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;


-- =========================================================
-- 1. ROLES
-- System-level roles only
-- CUSTOMER / ADMIN
-- =========================================================

CREATE TABLE roles
(
    role_id    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(50) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO roles (name)
VALUES ('CUSTOMER'),
       ('ADMIN');


-- =========================================================
-- 2. USERS
-- =========================================================

CREATE TABLE users
(
    user_id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    email             VARCHAR(255) NOT NULL UNIQUE,
    password          VARCHAR(255),

    user_name         VARCHAR(150) NOT NULL,
    avatar_url        VARCHAR(500),

    role_id           UUID NOT NULL,

    is_active         BOOLEAN NOT NULL DEFAULT TRUE,
    is_email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    require_password_change BOOLEAN NOT NULL DEFAULT FALSE,

    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_users_role
        FOREIGN KEY (role_id)
            REFERENCES roles (role_id)
);


-- =========================================================
-- 3. REFRESH TOKENS
-- =========================================================

CREATE TABLE refresh_tokens
(
    refresh_token_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_id          UUID NOT NULL,

    token            VARCHAR(500) NOT NULL UNIQUE,

    expires_at       TIMESTAMP NOT NULL,
    revoked          BOOLEAN NOT NULL DEFAULT FALSE,

    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id)
            REFERENCES users (user_id)
            ON DELETE CASCADE
);


-- =========================================================
-- 4. STORES
-- =========================================================

CREATE TABLE stores
(
    store_id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    name          VARCHAR(255) NOT NULL,

    description   TEXT,

    address       VARCHAR(500) NOT NULL,

    latitude      DECIMAL(10, 7),
    longitude     DECIMAL(10, 7),

    opening_time  TIME,
    closing_time  TIME,

    price_range   VARCHAR(50),

    phone         VARCHAR(20),
    email         VARCHAR(255),

    status        VARCHAR(30) NOT NULL DEFAULT 'PENDING',

    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- =========================================================
-- 5. STORE ROLES
-- Store-level roles:
-- OWNER / MANAGER / STAFF
-- =========================================================

CREATE TABLE store_roles
(
    store_role_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    name          VARCHAR(30) NOT NULL UNIQUE,
    description   VARCHAR(255),

    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO store_roles (name, description)
VALUES ('OWNER', 'Store owner'),
       ('MANAGER', 'Store manager'),
       ('STAFF', 'Store staff');


-- =========================================================
-- 6. STORE STAFFS
-- =========================================================

CREATE TABLE store_staffs
(
    store_staff_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    store_id       UUID NOT NULL,
    user_id        UUID NOT NULL,
    store_role_id  UUID NOT NULL,

    status         VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

    joined_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_store_staffs_store
        FOREIGN KEY (store_id)
            REFERENCES stores (store_id)
            ON DELETE CASCADE,

    CONSTRAINT fk_store_staffs_user
        FOREIGN KEY (user_id)
            REFERENCES users (user_id)
            ON DELETE CASCADE,

    CONSTRAINT fk_store_staffs_role
        FOREIGN KEY (store_role_id)
            REFERENCES store_roles (store_role_id),

    CONSTRAINT uq_store_staff
        UNIQUE (store_id, user_id)
);


-- =========================================================
-- 7. STORE IMAGES
-- =========================================================

CREATE TABLE store_images
(
    store_image_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    store_id       UUID NOT NULL,

    image_url      VARCHAR(500) NOT NULL,

    is_primary     BOOLEAN NOT NULL DEFAULT FALSE,

    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_store_images_store
        FOREIGN KEY (store_id)
            REFERENCES stores (store_id)
            ON DELETE CASCADE
);


-- =========================================================
-- 8. AMENITIES
-- =========================================================

CREATE TABLE amenities
(
    amenity_id  UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    name        VARCHAR(100) NOT NULL UNIQUE,

    description VARCHAR(255),

    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- =========================================================
-- 9. STORE AMENITIES
-- =========================================================

CREATE TABLE store_amenities
(
    store_amenity_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    store_id         UUID NOT NULL,
    amenity_id       UUID NOT NULL,

    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_store_amenities_store
        FOREIGN KEY (store_id)
            REFERENCES stores (store_id)
            ON DELETE CASCADE,

    CONSTRAINT fk_store_amenities_amenity
        FOREIGN KEY (amenity_id)
            REFERENCES amenities (amenity_id)
            ON DELETE CASCADE,

    CONSTRAINT uq_store_amenity
        UNIQUE (store_id, amenity_id)
);


-- =========================================================
-- 10. FAVORITE STORES
-- =========================================================

CREATE TABLE favorite_stores
(
    favorite_store_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_id           UUID NOT NULL,
    store_id          UUID NOT NULL,

    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_favorite_stores_user
        FOREIGN KEY (user_id)
            REFERENCES users (user_id)
            ON DELETE CASCADE,

    CONSTRAINT fk_favorite_stores_store
        FOREIGN KEY (store_id)
            REFERENCES stores (store_id)
            ON DELETE CASCADE,

    CONSTRAINT uq_user_favorite_store
        UNIQUE (user_id, store_id)
);


-- =========================================================
-- 11. VIBE TAGS
-- =========================================================

CREATE TABLE vibe_tags
(
    vibe_tag_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    name        VARCHAR(100) NOT NULL UNIQUE,

    description TEXT,

    tag_type    VARCHAR(30) NOT NULL,

    is_active   BOOLEAN NOT NULL DEFAULT TRUE,

    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_vibe_tag_type
        CHECK (tag_type IN ('PRIMARY', 'SECONDARY'))
);


-- =========================================================
-- 12. STORE VIBE TAGS
-- Admin-approved tags assigned to stores
-- =========================================================

CREATE TABLE store_vibe_tags
(
    store_vibe_tag_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    store_id          UUID NOT NULL,
    vibe_tag_id       UUID NOT NULL,

    status            VARCHAR(30) NOT NULL DEFAULT 'PENDING',

    approved_at       TIMESTAMP,
    revoked_at        TIMESTAMP,

    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_store_vibe_tags_store
        FOREIGN KEY (store_id)
            REFERENCES stores (store_id)
            ON DELETE CASCADE,

    CONSTRAINT fk_store_vibe_tags_vibe_tag
        FOREIGN KEY (vibe_tag_id)
            REFERENCES vibe_tags (vibe_tag_id)
            ON DELETE CASCADE,

    CONSTRAINT uq_store_vibe_tag
        UNIQUE (store_id, vibe_tag_id),

    CONSTRAINT chk_store_vibe_tag_status
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'REVOKED'))
);


-- =========================================================
-- 13. VISIT VERIFICATIONS
-- Vibe Snap Realtime
-- =========================================================

CREATE TABLE visit_verifications
(
    visit_verification_id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_id                   UUID NOT NULL,
    store_id                 UUID NOT NULL,

    image_url                 VARCHAR(500) NOT NULL,

    latitude                  DECIMAL(10, 7) NOT NULL,
    longitude                 DECIMAL(10, 7) NOT NULL,

    captured_at               TIMESTAMP NOT NULL,

    distance_from_store_meters DECIMAL(8, 2),

    status                    VARCHAR(30) NOT NULL DEFAULT 'PENDING',

    verified_at               TIMESTAMP,

    created_at                TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_visit_verifications_user
        FOREIGN KEY (user_id)
            REFERENCES users (user_id)
            ON DELETE CASCADE,

    CONSTRAINT fk_visit_verifications_store
        FOREIGN KEY (store_id)
            REFERENCES stores (store_id)
            ON DELETE CASCADE,

    CONSTRAINT chk_visit_verification_status
        CHECK (status IN ('PENDING', 'VERIFIED', 'REJECTED'))
);


-- =========================================================
-- 14. VIBE SURVEY LOGS
-- =========================================================

CREATE TABLE vibe_survey_logs
(
    vibe_survey_log_id    UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    visit_verification_id UUID NOT NULL,
    vibe_tag_id           UUID NOT NULL,

    response              VARCHAR(30) NOT NULL,

    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_vibe_survey_visit
        FOREIGN KEY (visit_verification_id)
            REFERENCES visit_verifications (visit_verification_id)
            ON DELETE CASCADE,

    CONSTRAINT fk_vibe_survey_tag
        FOREIGN KEY (vibe_tag_id)
            REFERENCES vibe_tags (vibe_tag_id)
            ON DELETE CASCADE,

    CONSTRAINT chk_vibe_survey_response
        CHECK (response IN ('CORRECT', 'NEUTRAL', 'WRONG'))
);


-- =========================================================
-- 15. REVIEWS
-- =========================================================

CREATE TABLE reviews
(
    review_id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    visit_verification_id UUID NOT NULL UNIQUE,

    overall_rating        DECIMAL(2, 1) NOT NULL,

    quietness_rating      INTEGER,
    lighting_rating       INTEGER,
    seating_rating        INTEGER,
    outlet_rating         INTEGER,

    content               TEXT,

    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_reviews_visit
        FOREIGN KEY (visit_verification_id)
            REFERENCES visit_verifications (visit_verification_id)
            ON DELETE CASCADE,

    CONSTRAINT chk_reviews_overall_rating
        CHECK (overall_rating >= 1 AND overall_rating <= 5),

    CONSTRAINT chk_reviews_quietness
        CHECK (quietness_rating IS NULL OR quietness_rating BETWEEN 1 AND 5),

    CONSTRAINT chk_reviews_lighting
        CHECK (lighting_rating IS NULL OR lighting_rating BETWEEN 1 AND 5),

    CONSTRAINT chk_reviews_seating
        CHECK (seating_rating IS NULL OR seating_rating BETWEEN 1 AND 5),

    CONSTRAINT chk_reviews_outlet
        CHECK (outlet_rating IS NULL OR outlet_rating BETWEEN 1 AND 5)
);


-- =========================================================
-- 16. REVIEW IMAGES
-- =========================================================

CREATE TABLE review_images
(
    review_image_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    review_id       UUID NOT NULL,

    image_url       VARCHAR(500) NOT NULL,

    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_review_images_review
        FOREIGN KEY (review_id)
            REFERENCES reviews (review_id)
            ON DELETE CASCADE
);


-- =========================================================
-- 17. SUBSCRIPTION PLANS
-- =========================================================

CREATE TABLE subscription_plans
(
    subscription_plan_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    name                 VARCHAR(100) NOT NULL UNIQUE,

    description          TEXT,

    price                DECIMAL(12, 2) NOT NULL DEFAULT 0,

    duration_days        INTEGER,

    is_active            BOOLEAN NOT NULL DEFAULT TRUE,

    created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- =========================================================
-- 18. STORE SUBSCRIPTIONS
-- =========================================================

CREATE TABLE store_subscriptions
(
    store_subscription_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    store_id              UUID NOT NULL,
    subscription_plan_id  UUID NOT NULL,

    start_date            TIMESTAMP NOT NULL,
    end_date              TIMESTAMP,

    status                VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_store_subscriptions_store
        FOREIGN KEY (store_id)
            REFERENCES stores (store_id)
            ON DELETE CASCADE,

    CONSTRAINT fk_store_subscriptions_plan
        FOREIGN KEY (subscription_plan_id)
            REFERENCES subscription_plans (subscription_plan_id),

    CONSTRAINT chk_store_subscription_status
        CHECK (status IN ('PENDING', 'ACTIVE', 'EXPIRED', 'CANCELLED'))
);


-- =========================================================
-- 19. NOTIFICATIONS
-- =========================================================

CREATE TABLE notifications
(
    notification_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_id         UUID NOT NULL,

    title           VARCHAR(255) NOT NULL,
    content         TEXT,

    is_read         BOOLEAN NOT NULL DEFAULT FALSE,
    type            VARCHAR(50) NOT NULL,
    reference_id    VARCHAR(255),

    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at         TIMESTAMP,

    CONSTRAINT fk_notifications_user
        FOREIGN KEY (user_id)
            REFERENCES users (user_id)
            ON DELETE CASCADE
);


-- =========================================================
-- INDEXES
-- =========================================================

-- Users
CREATE INDEX idx_users_role_id ON users(role_id);
CREATE INDEX idx_users_active ON users(is_active);

-- Refresh Tokens
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
CREATE INDEX idx_refresh_tokens_revoked ON refresh_tokens(revoked);

-- Stores
CREATE INDEX idx_stores_status ON stores(status);
CREATE INDEX idx_stores_location ON stores(latitude, longitude);

-- Store Staffs
CREATE INDEX idx_store_staffs_store_id ON store_staffs(store_id);
CREATE INDEX idx_store_staffs_user_id ON store_staffs(user_id);
CREATE INDEX idx_store_staffs_role_id ON store_staffs(store_role_id);

-- Store Images
CREATE INDEX idx_store_images_store_id ON store_images(store_id);

-- Store Amenities
CREATE INDEX idx_store_amenities_store_id ON store_amenities(store_id);
CREATE INDEX idx_store_amenities_amenity_id ON store_amenities(amenity_id);

-- Favorite Stores
CREATE INDEX idx_favorite_stores_user_id ON favorite_stores(user_id);
CREATE INDEX idx_favorite_stores_store_id ON favorite_stores(store_id);

-- Vibe Tags
CREATE INDEX idx_vibe_tags_type ON vibe_tags(tag_type);
CREATE INDEX idx_vibe_tags_active ON vibe_tags(is_active);

-- Store Vibe Tags
CREATE INDEX idx_store_vibe_tags_store_id ON store_vibe_tags(store_id);
CREATE INDEX idx_store_vibe_tags_vibe_tag_id ON store_vibe_tags(vibe_tag_id);
CREATE INDEX idx_store_vibe_tags_status ON store_vibe_tags(status);

-- Visit Verifications
CREATE INDEX idx_visit_verifications_user_id ON visit_verifications(user_id);
CREATE INDEX idx_visit_verifications_store_id ON visit_verifications(store_id);
CREATE INDEX idx_visit_verifications_status ON visit_verifications(status);
CREATE INDEX idx_visit_verifications_captured_at
    ON visit_verifications(captured_at DESC);

-- Vibe Survey Logs
CREATE INDEX idx_vibe_survey_logs_visit_id
    ON vibe_survey_logs(visit_verification_id);

CREATE INDEX idx_vibe_survey_logs_vibe_tag_id
    ON vibe_survey_logs(vibe_tag_id);

CREATE INDEX idx_vibe_survey_logs_response
    ON vibe_survey_logs(response);

-- Reviews
CREATE INDEX idx_reviews_created_at
    ON reviews(created_at DESC);

-- Review Images
CREATE INDEX idx_review_images_review_id
    ON review_images(review_id);

-- Store Subscriptions
CREATE INDEX idx_store_subscriptions_store_id
    ON store_subscriptions(store_id);

CREATE INDEX idx_store_subscriptions_plan_id
    ON store_subscriptions(subscription_plan_id);

CREATE INDEX idx_store_subscriptions_status
    ON store_subscriptions(status);

CREATE INDEX idx_store_subscriptions_end_date
    ON store_subscriptions(end_date);

-- Notifications
CREATE INDEX idx_notifications_user_id
    ON notifications(user_id);

CREATE INDEX idx_notifications_is_read
    ON notifications(is_read);

CREATE INDEX idx_notifications_created_at
    ON notifications(created_at DESC);