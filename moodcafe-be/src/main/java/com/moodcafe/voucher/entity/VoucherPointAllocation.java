package com.moodcafe.voucher.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "voucher_point_allocations", indexes = {
        @Index(name = "idx_voucher_allocations_voucher", columnList = "voucher_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherPointAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "allocation_id", nullable = false, updatable = false)
    private UUID allocationId;

    @Column(name = "voucher_id", nullable = false)
    private UUID voucherId;

    @Column(name = "lot_id", nullable = false)
    private UUID lotId;

    @Column(name = "allocated_points", nullable = false)
    private Long allocatedPoints;

    /**
     * REFUND or REWARD
     */
    @Column(name = "funding_type", nullable = false, length = 30)
    private String fundingType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
