package com.moodcafe.voucher.abstraction.repository;

import com.moodcafe.voucher.entity.VoucherTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VoucherTemplateRepository extends JpaRepository<VoucherTemplate, UUID> {

    List<VoucherTemplate> findByIsActiveTrueOrderByDisplayOrderAsc();
}
