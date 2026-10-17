package com.moodcafe.booking.abstraction.repository;

import com.moodcafe.booking.entity.Booking;
import com.moodcafe.booking.entity.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    Optional<Booking> findByBookingCode(String bookingCode);

    Optional<Booking> findByPaymentOrderCode(Long paymentOrderCode);

    List<Booking> findAllByUserUserIdOrderByStartTimeDesc(UUID userId);

    List<Booking> findAllByUserUserIdOrderByCreatedAtDesc(UUID userId);

    List<Booking> findAllByStoreStoreIdOrderByStartTimeDesc(UUID storeId);

    List<Booking> findAllByStoreStoreIdOrderByCreatedAtDesc(UUID storeId);

    List<Booking> findAllByStoreStoreIdAndStartTimeBetweenOrderByStartTimeAsc(
            UUID storeId,
            Instant rangeStart,
            Instant rangeEnd
    );

    List<Booking> findAllByStoreStoreIdAndStartTimeBetweenOrderByCreatedAtDesc(
            UUID storeId,
            Instant rangeStart,
            Instant rangeEnd
    );

    /**
     * Đếm số lượng đơn đang chiếm giữ slot bàn trong khoảng thời gian giao nhau (Overlap).
     */
    @Query("SELECT COUNT(b) FROM Booking b " +
           "WHERE b.tableCategory.categoryId = :categoryId " +
           "AND b.status IN :statuses " +
           "AND b.startTime < :reqEnd " +
           "AND b.endTime > :reqStart")
    long countOverlappingBookings(
            @Param("categoryId") UUID categoryId,
            @Param("statuses") Collection<BookingStatus> statuses,
            @Param("reqStart") Instant reqStart,
            @Param("reqEnd") Instant reqEnd
    );

    /**
     * Lấy toàn bộ đơn đang chiếm giữ bàn trong ngày của quán để tính toán Lịch Trạng Thái Bàn.
     */
    @Query("SELECT b FROM Booking b " +
           "JOIN FETCH b.tableCategory tc " +
           "WHERE b.store.storeId = :storeId " +
           "AND b.status IN :statuses " +
           "AND b.startTime < :dayEnd " +
           "AND b.endTime > :dayStart " +
           "ORDER BY b.startTime ASC")
    List<Booking> findOccupyingBookingsForDay(
            @Param("storeId") UUID storeId,
            @Param("statuses") Collection<BookingStatus> statuses,
            @Param("dayStart") Instant dayStart,
            @Param("dayEnd") Instant dayEnd
    );

    /**
     * Tìm các đơn PENDING_PAYMENT quá hạn để tự động hủy và nhả slot.
     */
    List<Booking> findAllByStatusAndCreatedAtBefore(BookingStatus status, Instant threshold);

    /**
     * Tìm các đơn PAID_CONFIRMED đã quá thời gian giữ bàn mà khách chưa check-in.
     */
    @Query("SELECT b FROM Booking b " +
           "JOIN FETCH b.store s " +
           "WHERE b.status = :status " +
           "AND b.startTime <= :threshold")
    List<Booking> findAllPaidConfirmedPastGracePeriod(
            @Param("status") BookingStatus status,
            @Param("threshold") Instant threshold
    );

    /**
     * Tìm các đơn CHECKED_IN đã quá giờ ngồi quy định (khi quán bật giới hạn thời gian).
     */
    @Query("SELECT b FROM Booking b " +
           "WHERE b.status = :status " +
           "AND b.endTime <= :threshold")
    List<Booking> findAllCheckedInPastEndTime(
            @Param("status") BookingStatus status,
            @Param("threshold") Instant threshold
    );

    /**
     * Tìm các đơn TABLE_EXPIRED của quán để đóng đơn khi hết ngày (store closing time).
     */
    @Query("SELECT b FROM Booking b " +
           "WHERE b.store.storeId = :storeId " +
           "AND b.status = :status " +
           "AND b.startTime <= :dayEnd")
    List<Booking> findAllTableExpiredForClosing(
            @Param("storeId") UUID storeId,
            @Param("status") BookingStatus status,
            @Param("dayEnd") Instant dayEnd
    );
}
