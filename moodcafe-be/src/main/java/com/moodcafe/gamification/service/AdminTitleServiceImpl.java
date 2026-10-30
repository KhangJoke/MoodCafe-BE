package com.moodcafe.gamification.service;

import com.moodcafe.gamification.abstraction.repository.TitleRepository;
import com.moodcafe.gamification.abstraction.service.AdminTitleService;
import com.moodcafe.gamification.dto.request.CreateTitleAdminRequest;
import com.moodcafe.gamification.dto.request.UpdateTitleAdminRequest;
import com.moodcafe.gamification.dto.response.TitleAdminResponse;
import com.moodcafe.gamification.entity.Title;
import com.moodcafe.gamification.entity.enums.TitleTier;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.shared.response.PageResponse;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminTitleServiceImpl implements AdminTitleService {

    private final TitleRepository titleRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TitleAdminResponse> getAllTitles(TitleTier tier, Boolean active, String search, Pageable pageable) {
        Specification<Title> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("isDeleted")));

            if (tier != null) {
                predicates.add(cb.equal(root.get("tier"), tier));
            }
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }
            if (search != null && !search.trim().isEmpty()) {
                String term = "%" + search.trim().toLowerCase() + "%";
                Predicate codePred = cb.like(cb.lower(root.get("code")), term);
                Predicate namePred = cb.like(cb.lower(root.get("name")), term);
                predicates.add(cb.or(codePred, namePred));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Title> page = titleRepository.findAll(spec, pageable);
        List<TitleAdminResponse> items = page.getContent().stream()
                .map(this::toAdminResponse)
                .toList();

        return PageResponse.<TitleAdminResponse>builder()
                .items(items)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TitleAdminResponse> getActiveTitlesForSelection() {
        return titleRepository.findAllByActiveTrueAndIsDeletedFalseOrderByCreatedAtAsc().stream()
                .map(this::toAdminResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TitleAdminResponse getTitleById(UUID titleId) {
        Title title = titleRepository.findById(titleId)
                .filter(t -> !t.isDeleted())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy danh hiệu"));
        return toAdminResponse(title);
    }

    @Override
    @Transactional
    public TitleAdminResponse createTitle(CreateTitleAdminRequest request) {
        String cleanCode = request.getCode().trim().toUpperCase();

        if (titleRepository.findByCode(cleanCode).isPresent()) {
            throw new AppException(ErrorCode.CONFLICT, "Mã danh hiệu " + cleanCode + " đã tồn tại trong hệ thống");
        }

        Title title = Title.builder()
                .code(cleanCode)
                .name(request.getName().trim())
                .description(request.getDescription())
                .tier(request.getTier())
                .colorHex(request.getColorHex().trim())
                .iconUrl(request.getIconUrl())
                .active(request.getIsActive() != null ? request.getIsActive() : true)
                .isDeleted(false)
                .build();

        Title saved = titleRepository.save(title);
        log.info("Admin created title {}", saved.getCode());
        return toAdminResponse(saved);
    }

    @Override
    @Transactional
    public TitleAdminResponse updateTitle(UUID titleId, UpdateTitleAdminRequest request) {
        Title title = titleRepository.findById(titleId)
                .filter(t -> !t.isDeleted())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy danh hiệu"));

        title.setName(request.getName().trim());
        title.setDescription(request.getDescription());
        title.setTier(request.getTier());
        title.setColorHex(request.getColorHex().trim());
        title.setIconUrl(request.getIconUrl());

        if (request.getIsActive() != null) {
            title.setActive(request.getIsActive());
        }

        Title saved = titleRepository.save(title);
        log.info("Admin updated title {}", saved.getCode());
        return toAdminResponse(saved);
    }

    @Override
    @Transactional
    public void toggleTitleStatus(UUID titleId, boolean active) {
        Title title = titleRepository.findById(titleId)
                .filter(t -> !t.isDeleted())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy danh hiệu"));

        title.setActive(active);
        titleRepository.save(title);
        log.info("Admin toggled title {} active={}", title.getCode(), active);
    }

    @Override
    @Transactional
    public void deleteTitle(UUID titleId) {
        Title title = titleRepository.findById(titleId)
                .filter(t -> !t.isDeleted())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy danh hiệu"));

        title.setDeleted(true);
        title.setActive(false);
        titleRepository.save(title);
        log.info("Admin soft-deleted title {}", title.getCode());
    }

    private TitleAdminResponse toAdminResponse(Title t) {
        return TitleAdminResponse.builder()
                .titleId(t.getTitleId())
                .code(t.getCode())
                .name(t.getName())
                .description(t.getDescription())
                .tier(t.getTier() != null ? t.getTier().name() : null)
                .colorHex(t.getColorHex())
                .iconUrl(t.getIconUrl())
                .isActive(t.isActive())
                .isDeleted(t.isDeleted())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}

