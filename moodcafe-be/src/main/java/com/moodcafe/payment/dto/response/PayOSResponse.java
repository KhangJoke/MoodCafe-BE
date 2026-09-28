package com.moodcafe.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PayOSResponse {
    private String checkoutUrl;    // Đường dẫn thanh toán của PayOS
    private String qrCode;         // URL ảnh VietQR để quét trực tiếp
    private String accountNumber;  // Số tài khoản ngân hàng
    private String accountName;    // Chủ tài khoản
    private String bin;            // Mã BIN ngân hàng (ví dụ: 970422 - MBBank)
    private Long orderCode;        // Mã đơn PayOS
    private Double amount;         // Số tiền
    private String description;    // Nội dung chuyển khoản
    private String status;         // PENDING
}
