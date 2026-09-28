package com.moodcafe.payment.controller;

import com.moodcafe.payment.dto.request.PayOSWebhookRequest;
import com.moodcafe.payment.dto.response.PayOSResponse;
import com.moodcafe.payment.service.PayOSService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Payment - PayOS (VietQR)", description = "APIs tích hợp cổng thanh toán VietQR PayOS")
@RestController
@RequestMapping("/api/payment/payos")
@RequiredArgsConstructor
public class PaymentController {

    private final PayOSService payOSService;

    @Operation(summary = "1. Tạo link thanh toán PayOS & mã VietQR")
    @PostMapping("/create-link/{orderId}")
    public ResponseEntity<PayOSResponse> createPaymentLink(@PathVariable String orderId) {
        return ResponseEntity.ok(payOSService.createPaymentLink(orderId));
    }

    @Operation(summary = "2. Webhook PayOS gọi sang khi thanh toán thành công (Server-to-Server)")
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody PayOSWebhookRequest request) {
        payOSService.handlePayOSWebhook(request);
        return ResponseEntity.ok("OK");
    }

    @Operation(summary = "3. Khách ấn 'Xác nhận đã thanh toán' hoặc FE Polling kiểm tra trạng thái đơn")
    @GetMapping("/check/{orderCode}")
    public ResponseEntity<Boolean> checkPaymentStatusGet(@PathVariable Long orderCode) {
        return ResponseEntity.ok(payOSService.checkPaymentStatus(orderCode));
    }

    @Operation(summary = "3. Khách ấn 'Xác nhận đã thanh toán' (Hỗ trợ phương thức POST)")
    @PostMapping("/check/{orderCode}")
    public ResponseEntity<Boolean> checkPaymentStatusPost(@PathVariable Long orderCode) {
        return ResponseEntity.ok(payOSService.checkPaymentStatus(orderCode));
    }
}
