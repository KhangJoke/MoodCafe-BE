package com.moodcafe.loyalty.service;

import com.moodcafe.booking.abstraction.repository.BookingRepository;
import com.moodcafe.booking.entity.Booking;
import com.moodcafe.loyalty.abstraction.repository.ReservationLoyaltyRewardRepository;
import com.moodcafe.loyalty.entity.ReservationLoyaltyReward;
import com.moodcafe.store.abstraction.repository.StoreReviewRepository;
import com.moodcafe.store.entity.StoreReview;
import com.moodcafe.loyalty.dto.RewardPointCommand;
import com.moodcafe.loyalty.service.CustomerLoyaltyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Slf4j
@Service
public class LoyaltyRewardService {

    private static final BigDecimal SPEND_REWARD_RATE = new BigDecimal("0.01"); // 1%
    private static final long ON_TIME_CHECK_IN_POINTS = 20L;
    private static final long SELF_CHECK_OUT_POINTS = 30L;
    private static final long VERIFIED_REVIEW_POINTS = 50L;

    private final ReservationLoyaltyRewardRepository rewardRepository;
    private final CustomerLoyaltyService customerLoyaltyService;
    private final StoreReviewRepository storeReviewRepository;
    private final BookingRepository bookingRepository;

    @Autowired
    public LoyaltyRewardService(
            ReservationLoyaltyRewardRepository rewardRepository,
            CustomerLoyaltyService customerLoyaltyService,
            StoreReviewRepository storeReviewRepository,
            BookingRepository bookingRepository
    ) {
        this.rewardRepository = rewardRepository;
        this.customerLoyaltyService = customerLoyaltyService;
        this.storeReviewRepository = storeReviewRepository;
        this.bookingRepository = bookingRepository;
    }

    /**
     * Awards SPEND points (1% gross_amount) upon COMPLETED or CLAIMED_LATE for canonical Booking.
     */
    @Transactional
    public void awardBookingSpendReward(Booking booking) {
        if (booking == null || booking.getGrossAmount() == null ||
                !(booking.getStatus() == com.moodcafe.booking.entity.enums.BookingStatus.COMPLETED ||
                  booking.getStatus() == com.moodcafe.booking.entity.enums.BookingStatus.CLAIMED_LATE)) return;
        bookingRepository.findByIdForUpdate(booking.getBookingId()).orElseThrow();
        if (rewardRepository.findByBookingIdAndRewardType(booking.getBookingId(), "SPEND").isPresent()) {
            return;
        }

        long calculatedPoints = booking.getGrossAmount()
                .multiply(SPEND_REWARD_RATE)
                .setScale(0, RoundingMode.FLOOR)
                .longValue();

        long pointsToAward = calculatedPoints;
        if (pointsToAward <= 0) return;

        ReservationLoyaltyReward reward = ReservationLoyaltyReward.builder()
                .bookingId(booking.getBookingId())
                .userId(booking.getUser().getUserId())
                .rewardType("SPEND")
                .pointsAwarded(pointsToAward)
                .build();
        rewardRepository.save(reward);

        customerLoyaltyService.creditRewardPoints(RewardPointCommand.builder()
                .userId(booking.getUser().getUserId())
                .points(pointsToAward)
                .rewardType("SPEND")
                .sourceId(booking.getBookingId().toString())
                .businessKey("BOOKING_SPEND_" + booking.getBookingId())
                .description("Thưởng chi tiêu đặt bàn " + booking.getBookingCode())
                .build());
        log.info("Awarded {} SPEND points to user {} for booking {}",
                pointsToAward, booking.getUser().getUserId(), booking.getBookingId());
    }

    /**
     * Awards configured ON_TIME_CHECK_IN points if customer checks in before grace deadline for canonical Booking.
     */
    @Transactional
    public void awardBookingOnTimeCheckIn(Booking booking) {
        if (booking == null || booking.getStatus() != com.moodcafe.booking.entity.enums.BookingStatus.COMPLETED
                || booking.getCheckedInAt() == null || booking.getGraceDeadline() == null
                || booking.getCheckedInAt().isAfter(booking.getGraceDeadline())) return;
        if (rewardRepository.findByBookingIdAndRewardType(booking.getBookingId(), "ON_TIME_CHECK_IN").isPresent()) {
            return;
        }

        ReservationLoyaltyReward reward = ReservationLoyaltyReward.builder()
                .bookingId(booking.getBookingId())
                .userId(booking.getUser().getUserId())
                .rewardType("ON_TIME_CHECK_IN")
                .pointsAwarded(ON_TIME_CHECK_IN_POINTS)
                .build();
        rewardRepository.save(reward);

        customerLoyaltyService.creditRewardPoints(RewardPointCommand.builder()
                .userId(booking.getUser().getUserId())
                .points(ON_TIME_CHECK_IN_POINTS)
                .rewardType("ON_TIME_CHECK_IN")
                .sourceId(booking.getBookingId().toString())
                .businessKey("BOOKING_ON_TIME_" + booking.getBookingId())
                .description("Thưởng nhận bàn đúng giờ cho đơn " + booking.getBookingCode())
                .build());
        log.info("Awarded {} ON_TIME_CHECK_IN points to user {} for booking {}",
                ON_TIME_CHECK_IN_POINTS, booking.getUser().getUserId(), booking.getBookingId());
    }

    /**
     * Awards configured SELF_CHECK_OUT points if customer performs self-service check-out (sitting >= 30m) for canonical Booking.
     */
    @Transactional
    public void awardBookingSelfCheckOut(Booking booking) {
        awardBookingSelfCheckOut(booking, 30L);
    }

    @Transactional
    public void awardBookingSelfCheckOut(Booking booking, long durationMinutes) {
        if (booking == null || durationMinutes < 30 || booking.getCheckedInAt() == null
                || booking.getCompletedAt() == null
                || java.time.Duration.between(booking.getCheckedInAt(), booking.getCompletedAt()).toMinutes() < 30) return;
        if (rewardRepository.findByBookingIdAndRewardType(booking.getBookingId(), "SELF_CHECK_OUT").isPresent()) {
            return;
        }

        ReservationLoyaltyReward reward = ReservationLoyaltyReward.builder()
                .bookingId(booking.getBookingId())
                .userId(booking.getUser().getUserId())
                .rewardType("SELF_CHECK_OUT")
                .pointsAwarded(SELF_CHECK_OUT_POINTS)
                .build();
        rewardRepository.save(reward);

        customerLoyaltyService.creditRewardPoints(RewardPointCommand.builder()
                .userId(booking.getUser().getUserId())
                .points(SELF_CHECK_OUT_POINTS)
                .rewardType("SELF_CHECK_OUT")
                .sourceId(booking.getBookingId().toString())
                .businessKey("BOOKING_SELF_CHECKOUT_" + booking.getBookingId())
                .description("Thưởng tự trả bàn (Self Check-out) cho đơn " + booking.getBookingCode())
                .build());
        log.info("Awarded {} SELF_CHECK_OUT points to user {} for booking {}",
                SELF_CHECK_OUT_POINTS, booking.getUser().getUserId(), booking.getBookingId());
    }

    /**
     * Awards configured VERIFIED_REVIEW points when customer reviews a store after COMPLETED or CLAIMED_LATE.
     */
    @Transactional
    public void awardVerifiedReview(UUID reviewId) {
        if (reviewId == null) return;
        storeReviewRepository.findById(reviewId).ifPresent(this::awardVerifiedReview);
    }

    @Transactional
    public void awardVerifiedReview(StoreReview review) {
        if (review == null || review.getUser() == null) return;
        if (review.getImages() == null || review.getImages().isEmpty()) {
            return;
        }
        UUID reviewUserId = review.getUser().getUserId();

        if (review.getBookingId() != null && bookingRepository != null) {
            Booking booking = bookingRepository.findByIdForUpdate(review.getBookingId()).orElse(null);
            if (booking == null) {
                log.warn("Cannot award review points: booking {} not found", review.getBookingId());
                return;
            }

            String bStatus = booking.getStatus() != null ? booking.getStatus().name() : "";
            boolean validStay = "COMPLETED".equals(bStatus) || "CLAIMED_LATE".equals(bStatus);
            if (!validStay) {
                log.warn("Cannot award review points: booking {} status is {}",
                        booking.getBookingId(), bStatus);
                return;
            }

            if (review.getStore() == null || booking.getStore() == null || !booking.getStore().getStoreId().equals(review.getStore().getStoreId())) {
                log.warn("Cannot award review points: store mismatch");
                return;
            }

            if (booking.getUser() == null || !booking.getUser().getUserId().equals(reviewUserId)) {
                log.warn("Cannot award review points: user mismatch");
                return;
            }

            if (rewardRepository.findByBookingIdAndRewardType(booking.getBookingId(), "VERIFIED_REVIEW").isPresent()) {
                return;
            }

            ReservationLoyaltyReward reward = ReservationLoyaltyReward.builder()
                    .bookingId(booking.getBookingId())
                    .userId(reviewUserId)
                    .rewardType("VERIFIED_REVIEW")
                    .pointsAwarded(VERIFIED_REVIEW_POINTS)
                    .build();
            rewardRepository.save(reward);

            customerLoyaltyService.creditRewardPoints(RewardPointCommand.builder()
                    .userId(reviewUserId)
                    .points(VERIFIED_REVIEW_POINTS)
                    .rewardType("VERIFIED_REVIEW")
                    .sourceId(review.getReviewId().toString())
                    .businessKey("BOOKING_REVIEW_" + review.getReviewId())
                    .description("Thưởng đánh giá có ảnh sau khi trải nghiệm tại " + booking.getStore().getName())
                    .build());
            log.info("Awarded {} VERIFIED_REVIEW points to user {} for review {}",
                    VERIFIED_REVIEW_POINTS, reviewUserId, review.getReviewId());
        }
    }
}
