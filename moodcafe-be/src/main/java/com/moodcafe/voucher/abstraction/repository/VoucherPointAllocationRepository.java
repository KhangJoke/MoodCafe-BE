package com.moodcafe.voucher.abstraction.repository;

import com.moodcafe.voucher.entity.VoucherPointAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VoucherPointAllocationRepository extends JpaRepository<VoucherPointAllocation, UUID> {

    List<VoucherPointAllocation> findByVoucherId(UUID voucherId);

    @Query("SELECT COALESCE(SUM(a.allocatedPoints), 0) FROM VoucherPointAllocation a WHERE a.voucherId = :voucherId AND a.fundingType = :fundingType")
    Long sumAllocatedPointsByVoucherIdAndFundingType(@Param("voucherId") UUID voucherId, @Param("fundingType") String fundingType);
}
