package com.moodcafe.wallet.abstraction.repository;

import com.moodcafe.wallet.entity.MerchantWalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MerchantWalletTransactionRepository extends JpaRepository<MerchantWalletTransaction, UUID> {

    Page<MerchantWalletTransaction> findByWalletIdOrderByCreatedAtDesc(UUID walletId, Pageable pageable);
}
