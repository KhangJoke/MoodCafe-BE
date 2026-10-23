package com.moodcafe.voucher.abstraction.repository;

import com.moodcafe.voucher.entity.VoucherRedemption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VoucherRedemptionRepository extends JpaRepository<VoucherRedemption, UUID> {

    Optional<VoucherRedemption> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);
}
