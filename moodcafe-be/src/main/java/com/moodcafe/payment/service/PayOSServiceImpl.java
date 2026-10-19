package com.moodcafe.payment.service;

import com.moodcafe.auth.abstraction.repository.UserRepository;
import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.auth.entity.User;
import com.moodcafe.booking.abstraction.repository.BookingRepository;
import com.moodcafe.booking.abstraction.service.BookingService;
import com.moodcafe.booking.entity.Booking;
import com.moodcafe.booking.entity.enums.BookingStatus;
import com.moodcafe.payment.abstraction.service.PayOSService;
import com.moodcafe.payment.config.PayOSConfig;
import com.moodcafe.payment.dto.request.PayOSWebhookRequest;
import com.moodcafe.payment.dto.response.PayOSResponse;
import com.moodcafe.payment.dto.response.PaymentOrderInfoResponse;
import com.moodcafe.sponsored.abstraction.repository.SponsoredListingRepository;
import com.moodcafe.sponsored.abstraction.service.SponsoredListingService;
import com.moodcafe.sponsored.entity.SponsoredListing;
import com.moodcafe.sponsored.entity.enums.SponsoredListingStatus;
import com.moodcafe.subscription.abstraction.repository.SubscriptionPaymentRepository;
import com.moodcafe.subscription.abstraction.service.SubscriptionService;
import com.moodcafe.subscription.entity.SubscriptionPayment;
import com.moodcafe.subscription.entity.enums.SubscriptionPaymentStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.v2.paymentRequests.PaymentLinkItem;
import vn.payos.model.v2.paymentRequests.PaymentLinkStatus;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class PayOSServiceImpl implements PayOSService {

    private final PayOS payOS;
    private final PayOSConfig payOSConfig;
    private final SubscriptionPaymentRepository subscriptionPaymentRepository;
    private final SubscriptionService subscriptionService;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    @Autowired(required = false)
    private SponsoredListingRepository sponsoredListingRepository;

    @Autowired(required = false)
    @Lazy
    private SponsoredListingService sponsoredListingService;

    @Autowired(required = false)
    private BookingRepository bookingRepository;

    @Autowired(required = false)
    @Lazy
    private BookingService bookingService;

    public PayOSServiceImpl(
            @Autowired(required = false) PayOS payOS,
            PayOSConfig payOSConfig,
            SubscriptionPaymentRepository subscriptionPaymentRepository,
            @Lazy SubscriptionService subscriptionService,
            UserRepository userRepository,
            CurrentUserService currentUserService
    ) {
        this.payOS = payOS;
        this.payOSConfig = payOSConfig;
        this.subscriptionPaymentRepository = subscriptionPaymentRepository;
        this.subscriptionService = subscriptionService;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    public void setSponsoredListingRepository(SponsoredListingRepository sponsoredListingRepository) {
        this.sponsoredListingRepository = sponsoredListingRepository;
    }

    public void setSponsoredListingService(SponsoredListingService sponsoredListingService) {
        this.sponsoredListingService = sponsoredListingService;
    }

    public void setBookingRepository(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public void setBookingService(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Override
    public boolean isConfigured() {
        return payOS != null && payOSConfig.isConfigured();
    }

    /**
     * Tạo Link thanh toán PayOS / VietQR cho UUID (Hỗ trợ cả Subscription và Sponsored Campaign)
     */
    @Override
    @Transactional
    public PayOSResponse createPaymentLink(UUID orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("orderId không được null");
        }
        return createPaymentLink(orderId.toString());
    }

    /**
     * 1. Tạo Link thanh toán PayOS / VietQR cho Order ID (Long)
     */
    @Override
    @Transactional
    public PayOSResponse createPaymentLink(Long orderId) {
        return createPaymentLink(String.valueOf(orderId));
    }

    /**
     * 1. Tạo Link thanh toán PayOS / VietQR (Hỗ trợ cả String orderId / UUID PaymentId / CampaignId)
     */
    @Override
    @Transactional
    public PayOSResponse createPaymentLink(String orderId) {
        if (!isConfigured()) {
            throw new IllegalStateException("Cổng thanh toán PayOS chưa được cấu hình Client ID, API Key hoặc Checksum Key.");
        }

        // 1. Tìm kiếm SubscriptionPayment tương ứng (nếu có)
        SubscriptionPayment payment = findPaymentByIdOrTxCode(orderId);
        if (payment != null) {
            return createPaymentLinkForSubscriptionPayment(payment);
        }

        // 2. Tìm kiếm SponsoredListing tương ứng (nếu có)
        SponsoredListing campaign = findCampaignByIdOrTxCode(orderId);
        if (campaign != null) {
            return createPaymentLinkForSponsoredListing(campaign);
        }

        // 3. Fallback cho orderId thông thường
        long amount = 50000;
        String itemName = "Don hang MoodCafe #" + orderId;
        String description = sanitizeDescription("MOOD " + orderId);
        long orderCode = generateOrderCode();

        try {
            PaymentLinkItem item = PaymentLinkItem.builder()
                    .name(itemName.length() > 50 ? itemName.substring(0, 50) : itemName)
                    .quantity(1)
                    .price(amount)
                    .build();

            CreatePaymentLinkRequest paymentData = CreatePaymentLinkRequest.builder()
                    .orderCode(orderCode)
                    .amount(amount)
                    .description(description)
                    .items(Collections.singletonList(item))
                    .returnUrl(payOSConfig.getReturnUrl())
                    .cancelUrl(payOSConfig.getCancelUrl())
                    .build();

            CreatePaymentLinkResponse checkoutData = payOS.paymentRequests().create(paymentData);

            // Tạo link ảnh VietQR để Client có thể hiển thị trực tiếp QR không cần redirect
            String officialQrImageUrl = "https://img.vietqr.io/image/" + checkoutData.getBin() + "-"
                    + checkoutData.getAccountNumber() + "-compact2.png?amount=" + amount
                    + "&addInfo=" + URLEncoder.encode(description, StandardCharsets.UTF_8)
                    + "&accountName=" + URLEncoder.encode(checkoutData.getAccountName(), StandardCharsets.UTF_8);

            savePendingPaymentAnchor(orderCode, amount, checkoutData.getCheckoutUrl(), orderId);

            return PayOSResponse.builder()
                    .checkoutUrl(checkoutData.getCheckoutUrl())
                    .qrCode(officialQrImageUrl)
                    .accountNumber(checkoutData.getAccountNumber())
                    .accountName(checkoutData.getAccountName())
                    .bin(checkoutData.getBin())
                    .orderCode(orderCode)
                    .amount((double) amount)
                    .description(description)
                    .status("PENDING")
                    .build();

        } catch (Exception e) {
            log.error("PayOS createPaymentLink error: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi tạo link thanh toán PayOS: " + e.getMessage(), e);
        }
    }

    /**
     * Tạo Link thanh toán trực tiếp cho một thực thể SubscriptionPayment đã tạo
     */
    @Override
    @Transactional
    public PayOSResponse createPaymentLinkForSubscriptionPayment(SubscriptionPayment payment) {
        if (!isConfigured()) {
            throw new IllegalStateException("Cổng thanh toán PayOS chưa được cấu hình.");
        }

        long amount = payment.getAmount().longValue();
        long orderCode = generateOrderCode();
        String planCodeName = payment.getSubscriptionPlan() != null
                ? payment.getSubscriptionPlan().getPlanCode().name()
                : "PLAN";
        String description = sanitizeDescription("MOOD " + planCodeName);

        String itemName = payment.getSubscriptionPlan() != null
                ? "Goi " + payment.getSubscriptionPlan().getDisplayName()
                : "Subscription MoodCafe";

        PaymentLinkItem item = PaymentLinkItem.builder()
                .name(itemName.length() > 50 ? itemName.substring(0, 50) : itemName)
                .quantity(1)
                .price(amount)
                .build();

        CreatePaymentLinkRequest paymentData = CreatePaymentLinkRequest.builder()
                .orderCode(orderCode)
                .amount(amount)
                .description(description)
                .items(Collections.singletonList(item))
                .returnUrl(appendQueryParam(payOSConfig.getReturnUrl(), "type=SUBSCRIPTION"))
                .cancelUrl(appendQueryParam(payOSConfig.getCancelUrl(), "type=SUBSCRIPTION"))
                .build();

        CreatePaymentLinkResponse checkoutData = payOS.paymentRequests().create(paymentData);

        String officialQrImageUrl = "https://img.vietqr.io/image/" + checkoutData.getBin() + "-"
                + checkoutData.getAccountNumber() + "-compact2.png?amount=" + amount
                + "&addInfo=" + URLEncoder.encode(description, StandardCharsets.UTF_8)
                + "&accountName=" + URLEncoder.encode(checkoutData.getAccountName(), StandardCharsets.UTF_8);

        payment.setTransactionCode(String.valueOf(orderCode));
        payment.setPaymentUrl(checkoutData.getCheckoutUrl());
        payment.setPaymentMethod("PAYOS");
        payment.setStatus(SubscriptionPaymentStatus.PENDING);
        subscriptionPaymentRepository.save(payment);

        return PayOSResponse.builder()
                .checkoutUrl(checkoutData.getCheckoutUrl())
                .qrCode(officialQrImageUrl)
                .accountNumber(checkoutData.getAccountNumber())
                .accountName(checkoutData.getAccountName())
                .bin(checkoutData.getBin())
                .orderCode(orderCode)
                .amount((double) amount)
                .description(description)
                .status("PENDING")
                .build();
    }

    /**
     * Tạo Link thanh toán PayOS / VietQR trực tiếp cho thực thể SponsoredListing đã tạo
     */
    @Override
    @Transactional
    public PayOSResponse createPaymentLinkForSponsoredListing(SponsoredListing campaign) {
        if (!isConfigured()) {
            throw new IllegalStateException("Cổng thanh toán PayOS chưa được cấu hình.");
        }

        long amount = campaign.getAmount().longValue();
        long orderCode = generateOrderCode();
        String placementName = campaign.getPlacement() != null
                ? campaign.getPlacement().name()
                : "SPONSOR";
        String description = sanitizeDescription("MOOD " + placementName);

        String itemName = campaign.getPlacement() != null
                ? "Tai tro " + campaign.getPlacement().getDisplayName()
                : "Tai tro MoodCafe";

        PaymentLinkItem item = PaymentLinkItem.builder()
                .name(itemName.length() > 50 ? itemName.substring(0, 50) : itemName)
                .quantity(1)
                .price(amount)
                .build();

        CreatePaymentLinkRequest paymentData = CreatePaymentLinkRequest.builder()
                .orderCode(orderCode)
                .amount(amount)
                .description(description)
                .items(Collections.singletonList(item))
                .returnUrl(appendQueryParam(payOSConfig.getReturnUrl(), "type=SPONSORED"))
                .cancelUrl(appendQueryParam(payOSConfig.getCancelUrl(), "type=SPONSORED"))
                .build();

        try {
            CreatePaymentLinkResponse checkoutData = payOS.paymentRequests().create(paymentData);

            String officialQrImageUrl = "https://img.vietqr.io/image/" + checkoutData.getBin() + "-"
                    + checkoutData.getAccountNumber() + "-compact2.png?amount=" + amount
                    + "&addInfo=" + URLEncoder.encode(description, StandardCharsets.UTF_8)
                    + "&accountName=" + URLEncoder.encode(checkoutData.getAccountName(), StandardCharsets.UTF_8);

            campaign.setTransactionCode(String.valueOf(orderCode));
            campaign.setPaymentMethod("PAYOS");
            campaign.setPaymentStatus("PENDING");
            if (sponsoredListingRepository != null) {
                sponsoredListingRepository.save(campaign);
            }

            return PayOSResponse.builder()
                    .checkoutUrl(checkoutData.getCheckoutUrl())
                    .qrCode(officialQrImageUrl)
                    .accountNumber(checkoutData.getAccountNumber())
                    .accountName(checkoutData.getAccountName())
                    .bin(checkoutData.getBin())
                    .orderCode(orderCode)
                    .amount((double) amount)
                    .description(description)
                    .status("PENDING")
                    .build();
        } catch (Exception e) {
            log.error("PayOS createPaymentLinkForSponsoredListing error: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi tạo link thanh toán PayOS: " + e.getMessage(), e);
        }
    }

    /**
     * Tạo Link thanh toán PayOS / VietQR cho đơn Đặt chỗ trước (Pre-order Booking)
     */
    @Override
    @Transactional
    public PayOSResponse createPaymentLinkForBooking(Booking booking) {
        if (!isConfigured()) {
            throw new IllegalStateException("Cổng thanh toán PayOS chưa được cấu hình.");
        }

        try {
            long amount = booking.getMinSpendAmount();
            long orderCode = generateOrderCode();
            String description = sanitizeDescription("DAT BAN " + booking.getBookingCode());
            String itemName = "Dat ban " + (booking.getTableCategory() != null ? booking.getTableCategory().getName() : "MoodCafe");

            PaymentLinkItem item = PaymentLinkItem.builder()
                    .name(itemName.length() > 50 ? itemName.substring(0, 50) : itemName)
                    .quantity(1)
                    .price(amount)
                    .build();

            CreatePaymentLinkRequest paymentData = CreatePaymentLinkRequest.builder()
                    .orderCode(orderCode)
                    .amount(amount)
                    .description(description)
                    .items(Collections.singletonList(item))
                    .returnUrl(appendQueryParam(payOSConfig.getReturnUrl(), "type=BOOKING&bookingCode=" + booking.getBookingCode()))
                    .cancelUrl(appendQueryParam(payOSConfig.getCancelUrl(), "type=BOOKING&bookingCode=" + booking.getBookingCode()))
                    .build();

            CreatePaymentLinkResponse checkoutData = payOS.paymentRequests().create(paymentData);

            String officialQrImageUrl = "https://img.vietqr.io/image/" + checkoutData.getBin() + "-"
                    + checkoutData.getAccountNumber() + "-compact2.png?amount=" + amount
                    + "&addInfo=" + URLEncoder.encode(description, StandardCharsets.UTF_8)
                    + "&accountName=" + URLEncoder.encode(checkoutData.getAccountName(), StandardCharsets.UTF_8);

            booking.setPaymentOrderCode(orderCode);
            if (bookingRepository != null) {
                bookingRepository.save(booking);
            }

            return PayOSResponse.builder()
                    .checkoutUrl(checkoutData.getCheckoutUrl())
                    .qrCode(officialQrImageUrl)
                    .accountNumber(checkoutData.getAccountNumber())
                    .accountName(checkoutData.getAccountName())
                    .bin(checkoutData.getBin())
                    .orderCode(orderCode)
                    .amount((double) amount)
                    .description(description)
                    .status("PENDING")
                    .build();
        } catch (Exception e) {
            log.error("PayOS createPaymentLinkForBooking error: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi tạo link thanh toán PayOS cho đơn đặt bàn: " + e.getMessage(), e);
        }
    }

    /**
     * 2. Tiếp nhận Webhook từ PayOS khi khách đã thanh toán thành công
     */
    @Override
    @Transactional
    public void handlePayOSWebhook(PayOSWebhookRequest request) {
        if (request == null) {
            log.warn("PayOS webhook called with null payload");
            return;
        }

        // Xác thực chữ ký webhook với PayOS SDK
        if (isConfigured()) {
            try {
                payOS.webhooks().verify(request);
                log.info("PayOS Webhook signature verified successfully");
            } catch (Exception e) {
                log.warn("PayOS Webhook signature verification warning: {}", e.getMessage());
            }
        }

        if (request.getData() == null) {
            log.info("PayOS Webhook ping / check without data received (webhook URL configuration test)");
            return;
        }

        PayOSWebhookRequest.PayOSWebhookData data = request.getData();
        Long orderCode = data.getOrderCode();
        log.info("PayOS Webhook received: orderCode={}, amount={}, desc={}, code={}",
                orderCode, data.getAmount(), data.getDescription(), request.getCode());

        if (orderCode == null) {
            log.warn("PayOS Webhook data missing orderCode");
            return;
        }

        // Tìm giao dịch theo transactionCode (orderCode)
        String txCode = String.valueOf(orderCode);
        Optional<SubscriptionPayment> paymentOpt = subscriptionPaymentRepository.findByTransactionCode(txCode);

        if (paymentOpt.isEmpty() && data.getDescription() != null) {
            paymentOpt = subscriptionPaymentRepository.findByTransactionCode(data.getDescription().trim());
        }

        if (paymentOpt.isPresent()) {
            SubscriptionPayment payment = paymentOpt.get();
            if (payment.getStatus() != SubscriptionPaymentStatus.SUCCESS) {
                subscriptionService.confirmPayment(payment.getTransactionCode());
                log.info("Subscription payment {} successfully confirmed via webhook for orderCode {}",
                        payment.getPaymentId(), orderCode);
            } else {
                log.info("Subscription payment {} already SUCCESS for orderCode {}", payment.getPaymentId(), orderCode);
            }
            return;
        }

        if (sponsoredListingRepository != null && sponsoredListingService != null) {
            Optional<SponsoredListing> campaignOpt = sponsoredListingRepository.findByTransactionCode(txCode);
            if (campaignOpt.isPresent()) {
                SponsoredListing campaign = campaignOpt.get();
                if (!SponsoredListingStatus.ACTIVE.equals(campaign.getStatus())) {
                    sponsoredListingService.confirmCampaignPayment(campaign.getTransactionCode());
                    log.info("Sponsored campaign {} successfully confirmed via webhook for orderCode {}",
                            campaign.getSponsoredListingId(), orderCode);
                } else {
                    log.info("Sponsored campaign {} already ACTIVE for orderCode {}",
                            campaign.getSponsoredListingId(), orderCode);
                }
                return;
            }
        }

        if (bookingRepository != null && bookingService != null) {
            Optional<Booking> bookingOpt = bookingRepository.findByPaymentOrderCode(orderCode);
            if (bookingOpt.isPresent()) {
                Booking booking = bookingOpt.get();
                if (booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
                    bookingService.confirmBookingPayment(orderCode);
                    log.info("Booking {} successfully confirmed via webhook for orderCode {}",
                            booking.getBookingCode(), orderCode);
                } else {
                    log.info("Booking {} already in status {} for orderCode {}",
                            booking.getBookingCode(), booking.getStatus(), orderCode);
                }
                return;
            }
        }

        log.warn("No SubscriptionPayment, SponsoredListing, or Booking found matching orderCode: {}", orderCode);
    }

    /**
     * 3. Kiểm tra trạng thái đơn chủ động (Polling / Khách ấn 'Tôi đã thanh toán')
     */
    @Override
    @Transactional
    public boolean checkPaymentStatus(Long orderCode) {
        if (orderCode == null) {
            return false;
        }
        if (!isConfigured()) {
            log.warn("PayOS is not configured, cannot check payment status for orderCode: {}", orderCode);
            return false;
        }

        try {
            PaymentLink paymentLink = payOS.paymentRequests().get(orderCode);
            if (paymentLink != null && paymentLink.getStatus() == PaymentLinkStatus.PAID) {
                String txCode = String.valueOf(orderCode);
                Optional<SubscriptionPayment> paymentOpt = subscriptionPaymentRepository.findByTransactionCode(txCode);
                if (paymentOpt.isPresent()) {
                    SubscriptionPayment payment = paymentOpt.get();
                    if (payment.getStatus() != SubscriptionPaymentStatus.SUCCESS) {
                        subscriptionService.confirmPayment(payment.getTransactionCode());
                        log.info("Subscription payment {} confirmed via checkPaymentStatus for orderCode {}",
                                payment.getPaymentId(), orderCode);
                    }
                    return true;
                }

                if (sponsoredListingRepository != null && sponsoredListingService != null) {
                    Optional<SponsoredListing> campaignOpt = sponsoredListingRepository.findByTransactionCode(txCode);
                    if (campaignOpt.isPresent()) {
                        SponsoredListing campaign = campaignOpt.get();
                        if (!SponsoredListingStatus.ACTIVE.equals(campaign.getStatus())) {
                            sponsoredListingService.confirmCampaignPayment(campaign.getTransactionCode());
                            log.info("Sponsored campaign {} confirmed via checkPaymentStatus for orderCode {}",
                                    campaign.getSponsoredListingId(), orderCode);
                        }
                        return true;
                    }
                }

                if (bookingRepository != null && bookingService != null) {
                    Optional<Booking> bookingOpt = bookingRepository.findByPaymentOrderCode(orderCode);
                    if (bookingOpt.isPresent()) {
                        Booking booking = bookingOpt.get();
                        if (booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
                            bookingService.confirmBookingPayment(orderCode);
                            log.info("Booking {} confirmed via checkPaymentStatus for orderCode {}",
                                    booking.getBookingCode(), orderCode);
                        }
                        return true;
                    }
                }
                return true;
            } else {
                log.info("PayOS status for orderCode {}: {}", orderCode, paymentLink != null ? paymentLink.getStatus() : "null");
            }
        } catch (Exception e) {
            log.warn("Check PayOS payment status failed for orderCode {}: {}", orderCode, e.getMessage());
        }
        return false;
    }

    /**
     * Tìm kiếm SubscriptionPayment theo UUID hoặc TransactionCode
     */
    private SubscriptionPayment findPaymentByIdOrTxCode(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return null;
        }
        try {
            UUID uuid = UUID.fromString(identifier);
            Optional<SubscriptionPayment> byId = subscriptionPaymentRepository.findById(uuid);
            if (byId.isPresent()) {
                return byId.get();
            }
        } catch (IllegalArgumentException ignored) {
            // Not a UUID
        }
        return subscriptionPaymentRepository.findByTransactionCode(identifier).orElse(null);
    }

    /**
     * Tìm kiếm SponsoredListing theo UUID hoặc TransactionCode
     */
    private SponsoredListing findCampaignByIdOrTxCode(String identifier) {
        if (identifier == null || identifier.isBlank() || sponsoredListingRepository == null) {
            return null;
        }
        try {
            UUID uuid = UUID.fromString(identifier);
            Optional<SponsoredListing> byId = sponsoredListingRepository.findById(uuid);
            if (byId.isPresent()) {
                return byId.get();
            }
        } catch (IllegalArgumentException ignored) {
            // Not a UUID
        }
        return sponsoredListingRepository.findByTransactionCode(identifier).orElse(null);
    }

    /**
     * Tạo orderCode kiểu Long là số nguyên duy nhất
     */
    private synchronized long generateOrderCode() {
        long code = System.currentTimeMillis() / 1000;
        while (subscriptionPaymentRepository.findByTransactionCode(String.valueOf(code)).isPresent()
                || (sponsoredListingRepository != null && sponsoredListingRepository.findByTransactionCode(String.valueOf(code)).isPresent())) {
            code++;
        }
        return code;
    }

    /**
     * Lưu bản ghi SubscriptionPayment PENDING khi tạo link trực tiếp từ orderId
     */
    private void savePendingPaymentAnchor(long orderCode, long amount, String paymentUrl, String originalOrderId) {
        User user = null;
        if (currentUserService.isAuthenticated()) {
            try {
                user = currentUserService.getCurrentUser();
            } catch (Exception ignored) {
            }
        }
        if (user == null) {
            user = userRepository.findAll().stream().findFirst().orElse(null);
        }

        if (user != null) {
            SubscriptionPayment pending = SubscriptionPayment.builder()
                    .user(user)
                    .transactionCode(String.valueOf(orderCode))
                    .amount(BigDecimal.valueOf(amount))
                    .paymentMethod("PAYOS")
                    .status(SubscriptionPaymentStatus.PENDING)
                    .paymentUrl(paymentUrl)
                    .notes("Đơn thanh toán PayOS #" + originalOrderId)
                    .build();
            subscriptionPaymentRepository.save(pending);
        }
    }

    /**
     * Format description: Tối đa 25 ký tự, chữ và số không dấu, không ký tự đặc biệt
     */
    @Override
    public String sanitizeDescription(String input) {
        if (input == null || input.isBlank()) {
            return "MOODCAFE";
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        normalized = normalized.replace("đ", "d").replace("Đ", "D");
        String cleaned = normalized.replaceAll("[^a-zA-Z0-9 ]", "").replaceAll("\\s+", " ").trim();
        if (cleaned.isEmpty()) {
            cleaned = "MOOD PAYMENT";
        }
        if (cleaned.length() > 25) {
            cleaned = cleaned.substring(0, 25).trim();
        }
        return cleaned;
    }

    /**
     * Lấy thông tin ngữ cảnh của một mã đơn thanh toán PayOS (Booking, Subscription, Sponsored)
     */
    @Override
    @Transactional(readOnly = true)
    public PaymentOrderInfoResponse getOrderInfo(Long orderCode) {
        if (orderCode == null) {
            return PaymentOrderInfoResponse.builder()
                    .paymentType("UNKNOWN")
                    .status("UNKNOWN")
                    .redirectUrl("/")
                    .build();
        }

        String txCode = String.valueOf(orderCode);

        // 1. Kiểm tra Đặt bàn trước (Booking)
        if (bookingRepository != null) {
            Optional<Booking> bookingOpt = bookingRepository.findByPaymentOrderCode(orderCode);
            if (bookingOpt.isPresent()) {
                Booking booking = bookingOpt.get();
                String storeName = booking.getStore() != null ? booking.getStore().getName() : "quán";
                return PaymentOrderInfoResponse.builder()
                        .orderCode(orderCode)
                        .paymentType("BOOKING")
                        .status(booking.getStatus() != null ? booking.getStatus().name() : "PENDING")
                        .amount(booking.getMinSpendAmount())
                        .title("Cọc giữ bàn tại " + storeName)
                        .referenceCode(booking.getBookingCode())
                        .redirectUrl("/customer/bookings")
                        .build();
            }
        }

        // 2. Kiểm tra Gói hội viên (Subscription)
        Optional<SubscriptionPayment> paymentOpt = subscriptionPaymentRepository.findByTransactionCode(txCode);
        if (paymentOpt.isPresent()) {
            SubscriptionPayment payment = paymentOpt.get();
            String planName = payment.getSubscriptionPlan() != null
                    ? payment.getSubscriptionPlan().getDisplayName()
                    : "Gói Hội Viên";
            return PaymentOrderInfoResponse.builder()
                    .orderCode(orderCode)
                    .paymentType("SUBSCRIPTION")
                    .status(payment.getStatus() != null ? payment.getStatus().name() : "PENDING")
                    .amount(payment.getAmount() != null ? payment.getAmount().longValue() : 0L)
                    .title("Gói hội viên: " + planName)
                    .referenceCode(payment.getTransactionCode())
                    .redirectUrl("/merchant/subscription")
                    .build();
        }

        // 3. Kiểm tra Chiến dịch tài trợ (SponsoredListing)
        if (sponsoredListingRepository != null) {
            Optional<SponsoredListing> campaignOpt = sponsoredListingRepository.findByTransactionCode(txCode);
            if (campaignOpt.isPresent()) {
                SponsoredListing campaign = campaignOpt.get();
                String placementName = campaign.getPlacement() != null
                        ? campaign.getPlacement().getDisplayName()
                        : "Chiến dịch quảng cáo";
                return PaymentOrderInfoResponse.builder()
                        .orderCode(orderCode)
                        .paymentType("SPONSORED")
                        .status(campaign.getPaymentStatus() != null ? campaign.getPaymentStatus() : "PENDING")
                        .amount(campaign.getAmount() != null ? campaign.getAmount().longValue() : 0L)
                        .title("Tài trợ: " + placementName)
                        .referenceCode(campaign.getTransactionCode())
                        .redirectUrl("/merchant/sponsored")
                        .build();
            }
        }

        return PaymentOrderInfoResponse.builder()
                .orderCode(orderCode)
                .paymentType("UNKNOWN")
                .status("UNKNOWN")
                .redirectUrl("/")
                .build();
    }

    private String appendQueryParam(String baseUrl, String param) {
        if (baseUrl == null) {
            return "";
        }
        return baseUrl.contains("?") ? baseUrl + "&" + param : baseUrl + "?" + param;
    }
}
