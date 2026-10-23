package com.moodcafe.loyalty.abstraction.repository;

import com.moodcafe.loyalty.entity.ReservationLoyaltyReward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReservationLoyaltyRewardRepository extends JpaRepository<ReservationLoyaltyReward, UUID> {

    Optional<ReservationLoyaltyReward> findByReservationIdAndRewardType(UUID reservationId, String rewardType);

    Optional<ReservationLoyaltyReward> findByBookingIdAndRewardType(UUID bookingId, String rewardType);
}
