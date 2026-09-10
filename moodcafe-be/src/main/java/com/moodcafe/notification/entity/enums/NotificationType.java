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

    // System
    SYSTEM_ANNOUNCEMENT
}