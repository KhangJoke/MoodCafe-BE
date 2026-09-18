-- V25: Clean legacy seed data to transition to Repeatable Seed Migrations (R__*.sql)
-- Truncate existing data tables while preserving roles, store_roles, system_configurations, subscription_plans
TRUNCATE TABLE 
    favorite_stores,
    tag_ratings,
    review_images,
    reviews,
    visit_verifications,
    vibe_survey_logs,
    user_preferences,
    onboarding_questions,
    store_tags,
    store_images,
    store_staffs,
    store_subscriptions,
    stores,
    tags,
    tag_categories,
    refresh_tokens,
    notifications,
    users
CASCADE;
