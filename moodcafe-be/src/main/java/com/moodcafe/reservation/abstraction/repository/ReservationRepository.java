package com.moodcafe.reservation.abstraction.repository;

import com.moodcafe.reservation.entity.Reservation;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    Optional<Reservation> findByPayosOrderCode(Long payosOrderCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reservation r WHERE r.reservationId = :reservationId")
    Optional<Reservation> findByIdWithLock(@Param("reservationId") UUID reservationId);

    Page<Reservation> findByUserIdOrderByBookingTimeDesc(UUID userId, Pageable pageable);

    Page<Reservation> findByStoreIdOrderByBookingTimeDesc(UUID storeId, Pageable pageable);

    Page<Reservation> findByStoreIdAndStatusOrderByBookingTimeDesc(UUID storeId, String status, Pageable pageable);

    @Query("""
        SELECT r FROM Reservation r
        WHERE r.status = 'PAID_CONFIRMED'
          AND r.graceDeadline < :now
    """)
    List<Reservation> findOverdueGraceReservations(@Param("now") Instant now);

    @Query("""
        SELECT r FROM Reservation r
        WHERE (r.status = 'PAID_CONFIRMED' OR r.status = 'TABLE_EXPIRED')
          AND r.serviceClosingDeadline < :now
    """)
    List<Reservation> findClosingNoShowReservations(@Param("now") Instant now);
}
