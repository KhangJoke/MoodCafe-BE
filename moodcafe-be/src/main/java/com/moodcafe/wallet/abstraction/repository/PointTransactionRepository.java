package com.moodcafe.wallet.abstraction.repository;

import com.moodcafe.wallet.entity.PointTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PointTransactionRepository extends JpaRepository<PointTransaction, UUID> {

    Optional<PointTransaction> findByBusinessKey(String businessKey);

    Page<PointTransaction> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
