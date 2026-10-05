package com.moodcafe.notification.entity.enums;

public enum NotificationType {

    // Store
    STORE_APPROVED,
    STORE_REJECTED,
    STORE_UPDATED,

    // Store staff
    STAFF_INVITED,
    STAFF_ADDED,
    STAFF_REMOVED,

    // Booking / reservation
    RESERVATION_CREATED,
    RESERVATION_CONFIRMED,
    RESERVATION_CANCELLED,
    RESERVATION_UPDATED,

    // Favorite store
    FAVORITE_STORE_UPDATED,

    // Vibe Snap
    VIBE_SNAP_SUBMITTED,
    VIBE_SNAP_VERIFIED,
    VIBE_SNAP_REJECTED,

    // Realtime Domain Events - Admin
    STORE_REGISTRATION_SUBMITTED,
    TAG_REQUEST_SUBMITTED,
    REVIEW_REPORTED,
    TAG_LOW_RATING_ALERT,

    // Realtime Domain Events - Merchant
    STORE_STATUS_UPDATED,
    TAG_REQUEST_RESOLVED,
    REVIEW_CREATED,
    TAG_QUALITY_WARNING,

    // Realtime Domain Events - Customer
    REVIEW_REPLIED,
    REVIEW_REPORT_RESOLVED,

    // System
    SYSTEM_ANNOUNCEMENT
}