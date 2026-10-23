package com.moodcafe.loyalty;

import com.moodcafe.auth.entity.Role;
import com.moodcafe.auth.entity.User;
import com.moodcafe.loyalty.abstraction.repository.ReservationLoyaltyRewardRepository;
import com.moodcafe.loyalty.entity.ReservationLoyaltyReward;
import com.moodcafe.loyalty.service.LoyaltyRewardService;
import com.moodcafe.reservation.abstraction.repository.ReservationRepository;
import com.moodcafe.reservation.entity.Reservation;
import com.moodcafe.store.abstraction.repository.StoreReviewRepository;
import com.moodcafe.store.entity.ReviewImage;
import com.moodcafe.store.entity.Store;
import com.moodcafe.store.entity.StoreReview;
import com.moodcafe.wallet.dto.RewardPointCommand;
import com.moodcafe.wallet.entity.PointTransaction;
import com.moodcafe.wallet.service.CustomerWalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerifiedReviewRewardIT {

    @Mock
    private ReservationLoyaltyRewardRepository rewardRepository;
    @Mock
    private CustomerWalletService customerWalletService;
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private StoreReviewRepository storeReviewRepository;

    private LoyaltyRewardService loyaltyRewardService;

    private final UUID userId = UUID.randomUUID();
    private final UUID storeId = UUID.randomUUID();
    private final UUID reservationId = UUID.randomUUID();
    private final UUID reviewId = UUID.randomUUID();

    private User user;
    private Store store;
    private Reservation reservation;
    private StoreReview review;

    @BeforeEach
    void setUp() {
        loyaltyRewardService = new LoyaltyRewardService(
                rewardRepository,
                customerWalletService,
                reservationRepository,
                storeReviewRepository
        );

        user = User.builder()
                .userId(userId)
                .fullName("Customer Tester")
                .role(Role.builder().name("USER").build())
                .build();

        store = Store.builder()
                .storeId(storeId)
                .name("Aroma House")
                .build();

        reservation = Reservation.builder()
                .reservationId(reservationId)
                .userId(userId)
                .storeId(storeId)
                .status("CHECKED_IN")
                .grossAmount(BigDecimal.valueOf(100000))
                .build();

        review = StoreReview.builder()
                .reviewId(reviewId)
                .user(user)
                .store(store)
                .reservationId(reservationId)
                .overallRating(BigDecimal.valueOf(5.0))
                .images(List.of(ReviewImage.builder().imageUrl("https://cdn.example.com/snap1.jpg").build()))
                .build();
    }

    @Test
    @DisplayName("R12: Successfully awards 2,000 MoodPoints for verified review with image and checked-in booking")
    void shouldAwardVerifiedReviewSuccessfully() {
        when(storeReviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(rewardRepository.findByReservationIdAndRewardType(reservationId, "VERIFIED_REVIEW")).thenReturn(Optional.empty());

        when(customerWalletService.creditRewardPoints(any(RewardPointCommand.class)))
                .thenReturn(PointTransaction.builder().build());

        loyaltyRewardService.awardVerifiedReview(reviewId);

        ArgumentCaptor<RewardPointCommand> cmdCaptor = ArgumentCaptor.forClass(RewardPointCommand.class);
        verify(customerWalletService).creditRewardPoints(cmdCaptor.capture());
        RewardPointCommand cmd = cmdCaptor.getValue();

        assertEquals(userId, cmd.getUserId());
        assertEquals(2000L, cmd.getPoints());
        assertEquals("VERIFIED_REVIEW", cmd.getRewardType());
        assertEquals("RESERVATION_REVIEW_" + reviewId, cmd.getBusinessKey());

        verify(rewardRepository).save(any(ReservationLoyaltyReward.class));
    }

    @Test
    @DisplayName("R12: Successfully awards 2,000 MoodPoints for verified review with image and checked-in booking")
    void completedBookingAlsoAwardsVerifiedReview() {
        reservation.setStatus("COMPLETED");
        when(storeReviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(rewardRepository.findByReservationIdAndRewardType(reservationId, "VERIFIED_REVIEW")).thenReturn(Optional.empty());

        when(customerWalletService.creditRewardPoints(any(RewardPointCommand.class)))
                .thenReturn(PointTransaction.builder().build());

        loyaltyRewardService.awardVerifiedReview(reviewId);

        ArgumentCaptor<RewardPointCommand> cmdCaptor = ArgumentCaptor.forClass(RewardPointCommand.class);
        verify(customerWalletService).creditRewardPoints(cmdCaptor.capture());
        RewardPointCommand cmd = cmdCaptor.getValue();

        assertEquals(userId, cmd.getUserId());
        assertEquals(2000L, cmd.getPoints());
        assertEquals("VERIFIED_REVIEW", cmd.getRewardType());
        assertEquals("RESERVATION_REVIEW_" + reviewId, cmd.getBusinessKey());

        verify(rewardRepository).save(any(ReservationLoyaltyReward.class));
    }

    @Test
    @DisplayName("R12: Anti-duplicate: Should NOT award points if reservation was already rewarded")
    void shouldNotAwardIfAlreadyRewarded() {
        when(storeReviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(rewardRepository.findByReservationIdAndRewardType(reservationId, "VERIFIED_REVIEW"))
                .thenReturn(Optional.of(ReservationLoyaltyReward.builder().build()));

        loyaltyRewardService.awardVerifiedReview(reviewId);

        verify(customerWalletService, never()).creditRewardPoints(any());
        verify(rewardRepository, never()).save(any());
    }

    @Test
    @DisplayName("R12: Anti-abuse: Review without photo should NOT be rewarded")
    void shouldNotAwardReviewWithoutPhotos() {
        review.setImages(Collections.emptyList());

        when(storeReviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

        loyaltyRewardService.awardVerifiedReview(reviewId);

        verify(reservationRepository, never()).findById(any());
        verify(customerWalletService, never()).creditRewardPoints(any());
    }

    @Test
    @DisplayName("R12: Anti-abuse: Booking not checked in (e.g. EXPIRED_NO_SHOW) should NOT be rewarded")
    void shouldNotAwardUncheckedInBooking() {
        reservation.setStatus("EXPIRED_NO_SHOW");

        when(storeReviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

        loyaltyRewardService.awardVerifiedReview(reviewId);

        verify(customerWalletService, never()).creditRewardPoints(any());
        verify(rewardRepository, never()).save(any());
    }

    @Test
    @DisplayName("R12: Anti-abuse: Store mismatch should NOT be rewarded")
    void shouldNotAwardStoreMismatch() {
        reservation.setStoreId(UUID.randomUUID()); // different store

        when(storeReviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

        loyaltyRewardService.awardVerifiedReview(reviewId);

        verify(customerWalletService, never()).creditRewardPoints(any());
        verify(rewardRepository, never()).save(any());
    }
    @Test
    void completedBookingOwnedByAnotherCustomerCannotEarnReviewPoints() {
        reservation.setUserId(UUID.randomUUID());
        when(storeReviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        loyaltyRewardService.awardVerifiedReview(reviewId);
        verifyNoInteractions(customerWalletService);
        verify(rewardRepository, never()).save(any());
    }

}
