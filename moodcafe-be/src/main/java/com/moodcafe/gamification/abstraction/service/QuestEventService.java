package com.moodcafe.gamification.abstraction.service;

import com.moodcafe.booking.entity.Booking;
import com.moodcafe.store.entity.StoreReview;

public interface QuestEventService {
    void onBookingCheckedIn(Booking booking);
    void onBookingTableExpired(Booking booking);
    void onBookingClaimedLate(Booking booking);
    void onReviewSubmitted(StoreReview review);
}
