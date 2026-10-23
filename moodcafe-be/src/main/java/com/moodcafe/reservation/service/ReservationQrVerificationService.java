package com.moodcafe.reservation.service;

import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Service
public class ReservationQrVerificationService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final byte[] secretKeyBytes;
    private final boolean insecureSecret;

    public ReservationQrVerificationService(
            @Value("${moodcafe.qr.secret:moodcafe-super-secret-hmac-key-for-physical-store-qr-validation-2026}") String secret
    ) {
        String s = secret != null ? secret.trim() : "";
        this.insecureSecret = s.isEmpty() || "moodcafe-super-secret-hmac-key-for-physical-store-qr-validation-2026".equals(s);
        this.secretKeyBytes = s.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Generates a tamper-proof daily physical QR token for a store table.
     * Format: STORE_QR:<storeId>:<yyyyMMdd>:<hmacHex>
     */
    public String generateDailyStoreQrToken(UUID storeId, LocalDate date) {
        if (storeId == null) {
            throw new IllegalArgumentException("storeId cannot be null");
        }
        LocalDate effectiveDate = date != null ? date : LocalDate.now(VIETNAM_ZONE);
        String dateStr = effectiveDate.format(DATE_FORMATTER);
        String payload = storeId + ":" + dateStr;
        String hmac = computeHmac(payload);
        return "STORE_QR:" + storeId + ":" + dateStr + ":" + hmac;
    }

    /**
     * Backward-compatible generation for today.
     */
    public String generateDailyStoreQrToken(UUID storeId) {
        return generateDailyStoreQrToken(storeId, LocalDate.now(VIETNAM_ZONE));
    }

    /**
     * Validates that the scanned QR token belongs to the specified store,
     * has not been forged, and was issued for today (or yesterday to tolerate midnight shifts).
     */
    public void validateStoreQrToken(UUID expectedStoreId, String qrToken) {
        if (insecureSecret) {
            throw new AppException(ErrorCode.FORBIDDEN_ACTION, "Hệ thống bảo mật QR chưa được cấu hình khóa an toàn");
        }
        if (expectedStoreId == null) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Store ID không hợp lệ");
        }
        if (qrToken == null || qrToken.trim().isEmpty()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Vui lòng quét mã QR tại bàn để xác nhận");
        }

        String token = qrToken.trim();
        String[] parts = token.split(":");
        if (parts.length != 4 || !"STORE_QR".equals(parts[0])) {
            log.warn("Rejected malformed or forged QR token format for store {}: {}", expectedStoreId, token);
            throw new AppException(ErrorCode.BAD_REQUEST, "Mã QR tại bàn không đúng định dạng bảo mật của quán");
        }

        UUID tokenStoreId;
        try {
            tokenStoreId = UUID.fromString(parts[1]);
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Mã QR chứa thông tin cơ sở không hợp lệ");
        }

        if (!expectedStoreId.equals(tokenStoreId)) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Mã QR không khớp với cơ sở đặt bàn này");
        }

        String dateStr = parts[2];
        String signature = parts[3];

        LocalDate tokenDate;
        try {
            tokenDate = LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (Exception e) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Mã QR có ngày hiệu lực không hợp lệ");
        }

        LocalDate today = LocalDate.now(VIETNAM_ZONE);
        LocalDate yesterday = today.minusDays(1);

        // Allow today and yesterday (to accommodate overnight shifts closing after midnight)
        if (tokenDate.isBefore(yesterday) || tokenDate.isAfter(today.plusDays(1))) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Mã QR tại bàn đã hết hạn hiệu lực theo ngày");
        }

        String expectedPayload = tokenStoreId + ":" + dateStr;
        String expectedHmac = computeHmac(expectedPayload);

        if (!MessageDigest.isEqual(expectedHmac.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8))) {
            log.warn("HMAC signature mismatch on QR token for store {}: provided={}, expected={}", expectedStoreId, signature, expectedHmac);
            throw new AppException(ErrorCode.BAD_REQUEST, "Mã QR không hợp lệ hoặc đã bị giả mạo");
        }
    }

    private String computeHmac(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secretKeyBytes, HMAC_ALGORITHM));
            byte[] rawHmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(rawHmac);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate HMAC-SHA256 for QR verification", e);
        }
    }
}
