package com.moodcafe.config;

import com.moodcafe.auth.abstraction.repository.UserRepository;
import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.finance.service.ReservationPricingService;
import com.moodcafe.loyalty.service.LoyaltyRewardService;
import com.moodcafe.payment.config.PayOSConfig;
import com.moodcafe.payment.service.BookingPaymentCoordinator;
import com.moodcafe.payment.service.OrderCodeGeneratorService;
import com.moodcafe.payment.abstraction.service.PayOSService;
import com.moodcafe.payment.service.PaymentAttemptService;
import com.moodcafe.reservation.abstraction.repository.ReservationRepository;
import com.moodcafe.reservation.abstraction.repository.TableCategoryRepository;
import com.moodcafe.reservation.controller.ReservationController;
import com.moodcafe.reservation.service.ReservationPaymentCaptureService;
import com.moodcafe.reservation.service.ReservationQrVerificationService;
import com.moodcafe.reservation.service.ReservationService;
import com.moodcafe.reservation.service.ServiceDayDeadlineResolver;
import com.moodcafe.store.abstraction.repository.StoreRepository;
import com.moodcafe.store.abstraction.service.StoreStaffService;
import com.moodcafe.subscription.abstraction.repository.SubscriptionPaymentRepository;
import com.moodcafe.subscription.abstraction.service.SubscriptionService;
import com.moodcafe.voucher.abstraction.repository.CustomerVoucherRepository;
import com.moodcafe.voucher.abstraction.repository.VoucherPointAllocationRepository;
import com.moodcafe.voucher.service.VoucherLifecycleService;
import com.moodcafe.wallet.service.MerchantWalletService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class FinancialServiceWiringTest {

    @Configuration
    static class TestConfig {

        @Bean
        public PayOSConfig payOSConfig() {
            return mock(PayOSConfig.class);
        }

        @Bean
        public SubscriptionPaymentRepository subscriptionPaymentRepository() {
            return mock(SubscriptionPaymentRepository.class);
        }

        @Bean
        public SubscriptionService subscriptionService() {
            return mock(SubscriptionService.class);
        }

        @Bean
        public UserRepository userRepository() {
            return mock(UserRepository.class);
        }

        @Bean
        public CurrentUserService currentUserService() {
            return mock(CurrentUserService.class);
        }

        @Bean
        public ReservationRepository reservationRepository() {
            return mock(ReservationRepository.class);
        }

        @Bean
        public ReservationPaymentCaptureService reservationPaymentCaptureService() {
            return mock(ReservationPaymentCaptureService.class);
        }

        @Bean
        public PaymentAttemptService paymentAttemptService() {
            return mock(PaymentAttemptService.class);
        }

        @Bean
        public TableCategoryRepository tableCategoryRepository() {
            return mock(TableCategoryRepository.class);
        }

        @Bean
        public StoreRepository storeRepository() {
            return mock(StoreRepository.class);
        }

        @Bean
        public ServiceDayDeadlineResolver serviceDayDeadlineResolver() {
            return mock(ServiceDayDeadlineResolver.class);
        }

        @Bean
        public OrderCodeGeneratorService orderCodeGeneratorService() {
            return mock(OrderCodeGeneratorService.class);
        }

        @Bean
        public ReservationPricingService reservationPricingService() {
            return mock(ReservationPricingService.class);
        }

        @Bean
        public CustomerVoucherRepository customerVoucherRepository() {
            return mock(CustomerVoucherRepository.class);
        }

        @Bean
        public VoucherPointAllocationRepository voucherPointAllocationRepository() {
            return mock(VoucherPointAllocationRepository.class);
        }

        @Bean
        public VoucherLifecycleService voucherLifecycleService() {
            return mock(VoucherLifecycleService.class);
        }

        @Bean
        public MerchantWalletService merchantWalletService() {
            return mock(MerchantWalletService.class);
        }

        @Bean
        public LoyaltyRewardService loyaltyRewardService() {
            return mock(LoyaltyRewardService.class);
        }

        @Bean
        public StoreStaffService storeStaffService() {
            return mock(StoreStaffService.class);
        }

        @Bean
        public ReservationQrVerificationService reservationQrVerificationService() {
            return mock(ReservationQrVerificationService.class);
        }

        @Bean
        public com.moodcafe.finance.service.ReservationRefundService reservationRefundService() {
            return mock(com.moodcafe.finance.service.ReservationRefundService.class);
        }
    }

    @Test
    @DisplayName("Financial services and controllers wire without circular dependency or ambiguous constructors")
    void testSpringContextWiring() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(TestConfig.class);
        context.register(com.moodcafe.payment.service.PayOSServiceImpl.class);
        context.register(ReservationService.class);
        context.register(BookingPaymentCoordinator.class);
        context.register(ReservationController.class);
        context.refresh();

        PayOSService payOSService = context.getBean(PayOSService.class);
        ReservationService reservationService = context.getBean(ReservationService.class);
        BookingPaymentCoordinator coordinator = context.getBean(BookingPaymentCoordinator.class);
        ReservationController controller = context.getBean(ReservationController.class);

        assertThat(payOSService).isNotNull();
        assertThat(reservationService).isNotNull();
        assertThat(coordinator).isNotNull();
        assertThat(controller).isNotNull();

        context.close();
    }
}
