package com.moodcafe.wallet.abstraction.repository;

import com.moodcafe.wallet.entity.StoreBankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreBankAccountRepository extends JpaRepository<StoreBankAccount, UUID> {

    List<StoreBankAccount> findByStoreId(UUID storeId);

    List<StoreBankAccount> findByStoreIdAndIsDeletedFalse(UUID storeId);

    Optional<StoreBankAccount> findByStoreIdAndIsDefaultTrue(UUID storeId);

    Optional<StoreBankAccount> findByStoreIdAndIsDefaultTrueAndIsDeletedFalse(UUID storeId);
}
