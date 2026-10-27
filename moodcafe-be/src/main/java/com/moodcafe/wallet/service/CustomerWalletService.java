package com.moodcafe.wallet.service;

import com.moodcafe.wallet.abstraction.repository.CustomerWalletRepository;
import com.moodcafe.wallet.abstraction.repository.PointLotRepository;
import com.moodcafe.wallet.abstraction.repository.PointTransactionRepository;
import org.springframework.stereotype.Service;

/**
 * Legacy service name kept for backwards compatibility.
 * Use {@link CustomerLoyaltyService} for all new implementations.
 */
@Service
@Deprecated
public class CustomerWalletService extends CustomerLoyaltyService {

    public CustomerWalletService(CustomerWalletRepository customerWalletRepository,
                                 PointLotRepository pointLotRepository,
                                 PointTransactionRepository pointTransactionRepository) {
        super(customerWalletRepository, pointLotRepository, pointTransactionRepository);
    }
}
