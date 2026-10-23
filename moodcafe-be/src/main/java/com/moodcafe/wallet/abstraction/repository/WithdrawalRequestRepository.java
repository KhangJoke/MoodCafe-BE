package com.moodcafe.wallet.abstraction.repository;

import com.moodcafe.wallet.entity.WithdrawalRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM WithdrawalRequest w WHERE w.withdrawalId = :id")
    Optional<WithdrawalRequest> findByIdWithLock(@Param("id") UUID id);

    Page<WithdrawalRequest> findByStoreIdOrderByCreatedAtDesc(UUID storeId, Pageable pageable);

    Page<WithdrawalRequest> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    Page<WithdrawalRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Optional<WithdrawalRequest> findByStoreIdAndIdempotencyKey(UUID storeId, String idempotencyKey);

    boolean existsByTransactionReference(String transactionReference);

    boolean existsByTransactionReferenceIgnoreCase(String transactionReference);

    Optional<WithdrawalRequest> findByTransactionReferenceIgnoreCase(String transactionReference);
}
