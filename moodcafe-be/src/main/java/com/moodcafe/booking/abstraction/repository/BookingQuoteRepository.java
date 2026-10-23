package com.moodcafe.booking.abstraction.repository;

import com.moodcafe.booking.entity.BookingQuote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingQuoteRepository extends JpaRepository<BookingQuote, UUID> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    Optional<BookingQuote> findByQuoteIdAndUserUserId(UUID quoteId, UUID userId);
}
