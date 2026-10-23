package com.moodcafe.wallet.abstraction.repository;

import com.moodcafe.wallet.entity.CustomerWallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerWalletRepository extends JpaRepository<CustomerWallet, UUID> {

    Optional<CustomerWallet> findByUserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM CustomerWallet w WHERE w.userId = :userId")
    Optional<CustomerWallet> findByUserIdWithLock(@Param("userId") UUID userId);
}
