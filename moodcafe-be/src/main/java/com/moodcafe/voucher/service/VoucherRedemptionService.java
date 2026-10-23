package com.moodcafe.voucher.service;

import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.voucher.abstraction.repository.CustomerVoucherRepository;
import com.moodcafe.voucher.abstraction.repository.VoucherPointAllocationRepository;
import com.moodcafe.voucher.abstraction.repository.VoucherRedemptionRepository;
import com.moodcafe.voucher.abstraction.repository.VoucherTemplateRepository;
import com.moodcafe.voucher.entity.CustomerVoucher;
import com.moodcafe.voucher.entity.VoucherPointAllocation;
import com.moodcafe.voucher.entity.VoucherRedemption;
import com.moodcafe.voucher.entity.VoucherTemplate;
import com.moodcafe.wallet.dto.PointAllocation;
import com.moodcafe.wallet.service.CustomerWalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherRedemptionService {

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final VoucherTemplateRepository voucherTemplateRepository;
    private final CustomerVoucherRepository customerVoucherRepository;
    private final VoucherPointAllocationRepository allocationRepository;
    private final VoucherRedemptionRepository voucherRedemptionRepository;
    private final CustomerWalletService customerWalletService;

    /**
     * Atomically redeems points for a personal voucher:
     * 1. Checks idempotency if businessKey provided.
     * 2. Validates template.
     * 3. Allocates MoodPoints from customer wallet (REFUND lots prioritized first).
     * 4. Creates CustomerVoucher (AVAILABLE, 60 days validity).
     * 5. Persists lot allocations for financial backing traceability.
     * 6. Records redemption entry for idempotency deduplication.
     */
    @Transactional
    public CustomerVoucher redeemVoucher(UUID userId, UUID templateId, String businessKey) {
        if (userId == null || templateId == null) {
            throw new AppException(ErrorCode.BAD_REQUEST, "User ID và Template ID không được để trống");
        }

        // Idempotency check: if already redeemed with same intent key, return existing voucher
        if (businessKey != null && !businessKey.trim().isEmpty()) {
            String cleanKey = businessKey.trim();
            Optional<VoucherRedemption> existingOpt = voucherRedemptionRepository.findByUserIdAndIdempotencyKey(userId, cleanKey);
            if (existingOpt.isPresent()) {
                VoucherRedemption existing = existingOpt.get();
                log.info("Idempotent redeemVoucher replay for user {} with key {}, returning voucher {}",
                        userId, cleanKey, existing.getVoucherId());
                return customerVoucherRepository.findById(existing.getVoucherId())
                        .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy voucher đã đổi"));
            }
        }

        VoucherTemplate template = voucherTemplateRepository.findById(templateId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy mẫu voucher"));

        if (!Boolean.TRUE.equals(template.getIsActive())) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Mẫu voucher này hiện không còn áp dụng");
        }

        // 1. Atomically deduct and allocate points
        String key = (businessKey != null && !businessKey.trim().isEmpty())
                ? businessKey.trim()
                : "REDEEM_" + userId + "_" + System.currentTimeMillis();

        PointAllocation allocation = customerWalletService.allocatePoints(
                userId,
                template.getPointCost(),
                key,
                "Đổi điểm lấy " + template.getTitle()
        );

        // 2. Generate unique voucher code
        String code = generateUniqueVoucherCode();

        // 3. Save voucher
        Instant now = Instant.now();
        Instant expiresAt = now.plus(60, ChronoUnit.DAYS); // 60 days validity

        CustomerVoucher voucher = CustomerVoucher.builder()
                .userId(userId)
                .templateId(template.getTemplateId())
                .code(code)
                .faceValueVnd(template.getFaceValueVnd())
                .pointCost(template.getPointCost())
                .status("AVAILABLE")
                .expiresAt(expiresAt)
                .build();

        CustomerVoucher savedVoucher = customerVoucherRepository.save(voucher);

        // 4. Record allocation items for subsidy tracking
        for (PointAllocation.LotAllocationItem item : allocation.getItems()) {
            VoucherPointAllocation alloc = VoucherPointAllocation.builder()
                    .voucherId(savedVoucher.getVoucherId())
                    .lotId(item.getLotId())
                    .allocatedPoints(item.getAllocatedPoints())
                    .fundingType(item.getFundingType())
                    .build();
            allocationRepository.save(alloc);
        }

        // 5. Record redemption for idempotency
        if (businessKey != null && !businessKey.trim().isEmpty()) {
            VoucherRedemption redemption = VoucherRedemption.builder()
                    .userId(userId)
                    .idempotencyKey(businessKey.trim())
                    .payloadHash(computePayloadHash(userId, templateId))
                    .templateId(template.getTemplateId())
                    .voucherId(savedVoucher.getVoucherId())
                    .build();
            voucherRedemptionRepository.save(redemption);
        }

        log.info("Voucher {} (code {}) redeemed by user {} for {} points (REFUND: {}, REWARD: {})",
                savedVoucher.getVoucherId(), code, userId, template.getPointCost(),
                allocation.getRefundPointsAllocated(), allocation.getRewardPointsAllocated());

        return savedVoucher;
    }

    /**
     * Computes a deterministic SHA-256 payload hash for voucher redemption idempotency.
     * Hash input: userId + ":" + templateId → 64-char hex string.
     */
    static String computePayloadHash(UUID userId, UUID templateId) {
        try {
            String payload = userId.toString() + ":" + templateId.toString();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private String generateUniqueVoucherCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder("MCV-");
            for (int i = 0; i < 8; i++) {
                sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
            }
            String candidate = sb.toString();
            if (customerVoucherRepository.findByCode(candidate).isEmpty()) {
                return candidate;
            }
        }
        return "MCV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
