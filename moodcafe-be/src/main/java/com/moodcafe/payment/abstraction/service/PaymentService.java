package com.moodcafe.payment.abstraction.service;

import com.moodcafe.booking.entity.Booking;
import com.moodcafe.payment.dto.request.PayOSWebhookRequest;
import com.moodcafe.payment.dto.response.PayOSResponse;
import com.moodcafe.payment.dto.response.PaymentOrderInfoResponse;
import com.moodcafe.sponsored.entity.SponsoredListing;
import com.moodcafe.subscription.entity.SubscriptionPayment;

import java.util.UUID;

public interface PaymentService {

    boolean isConfigured();

    PayOSResponse createPaymentLink(UUID orderId);

    PayOSResponse createPaymentLink(Long orderId);

    PayOSResponse createPaymentLink(String orderId);

    PayOSResponse createPaymentLinkForSubscriptionPayment(SubscriptionPayment payment);

    PayOSResponse createPaymentLinkForSponsoredListing(SponsoredListing campaign);

    PayOSResponse createPaymentLinkForBooking(Booking booking);

    void handlePayOSWebhook(PayOSWebhookRequest request);

    boolean checkPaymentStatus(Long orderCode);

    String sanitizeDescription(String input);

    PaymentOrderInfoResponse getOrderInfo(Long orderCode);
}
