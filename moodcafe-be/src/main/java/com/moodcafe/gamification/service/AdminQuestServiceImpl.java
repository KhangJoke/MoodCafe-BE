package com.moodcafe.gamification.service;

import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.auth.entity.User;
import com.moodcafe.gamification.abstraction.repository.QuestRepository;
import com.moodcafe.gamification.abstraction.repository.TitleRepository;
import com.moodcafe.gamification.abstraction.repository.UserQuestProgressRepository;
import com.moodcafe.gamification.abstraction.service.AdminQuestService;
import com.moodcafe.gamification.dto.request.CreateQuestAdminRequest;
import com.moodcafe.gamification.dto.request.UpdateQuestAdminRequest;
import com.moodcafe.gamification.dto.response.EquippedTitleDto;
import com.moodcafe.gamification.dto.response.QuestAdminResponse;
import com.moodcafe.gamification.dto.response.QuestRewardDto;
import com.moodcafe.gamification.entity.Quest;
import com.moodcafe.gamification.entity.Title;
import com.moodcafe.gamification.entity.enums.QuestGroup;
import com.moodcafe.gamification.entity.enums.QuestProgressStatus;
import com.moodcafe.gamification.entity.enums.QuestType;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.shared.response.PageResponse;
import com.moodcafe.voucher.abstraction.repository.VoucherTemplateRepository;
import com.moodcafe.voucher.entity.VoucherTemplate;
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
public class AdminQuestServiceImpl implements AdminQuestService {

    private final QuestRepository questRepository;
    private final TitleRepository titleRepository;
    private final VoucherTemplateRepository voucherTemplateRepository;
    private final UserQuestProgressRepository progressRepository;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<QuestAdminResponse> getAllQuests(
            QuestGroup group,
            QuestType type,
            Boolean isActive,
            String search,
            Pageable pageable
    ) {
        Specification<Quest> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("isDeleted")));

            if (group != null) {
                predicates.add(cb.equal(root.get("questGroup"), group));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("questType"), type));
            }
            if (isActive != null) {
                predicates.add(cb.equal(root.get("active"), isActive));
            }
            if (search != null && !search.trim().isEmpty()) {
                String term = "%" + search.trim().toLowerCase() + "%";
                Predicate codePred = cb.like(cb.lower(root.get("code")), term);
                Predicate namePred = cb.like(cb.lower(root.get("name")), term);
                predicates.add(cb.or(codePred, namePred));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Quest> page = questRepository.findAll(spec, pageable);
        List<QuestAdminResponse> items = page.getContent().stream()
                .map(this::toAdminResponse)
                .toList();

        return PageResponse.<QuestAdminResponse>builder()
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
    public QuestAdminResponse getQuestById(UUID questId) {
        Quest quest = questRepository.findById(questId)
                .filter(q -> !q.isDeleted())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy nhiệm vụ"));
        return toAdminResponse(quest);
    }

    @Override
    @Transactional
    public QuestAdminResponse createQuest(CreateQuestAdminRequest request) {
        User admin = currentUserService.getCurrentUser();
        if (admin == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Vui lòng đăng nhập với quyền Admin");
        }

        String cleanCode = request.getCode().trim().toUpperCase();
        if (questRepository.findByCode(cleanCode).isPresent()) {
            throw new AppException(ErrorCode.CONFLICT, "Mã nhiệm vụ " + cleanCode + " đã tồn tại trong hệ thống");
        }

        VoucherTemplate vt = null;
        if (request.getRewardVoucherTemplateId() != null) {
            vt = voucherTemplateRepository.findById(request.getRewardVoucherTemplateId())
                    .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy mẫu voucher phần thưởng"));
        }

        Title title = null;
        if (request.getRewardTitleId() != null) {
            title = titleRepository.findById(request.getRewardTitleId())
                    .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy danh hiệu phần thưởng"));
        }

        Quest quest = Quest.builder()
                .code(cleanCode)
                .name(request.getName().trim())
                .description(request.getDescription())
                .questGroup(request.getQuestGroup())
                .questType(request.getQuestType())
                .ruleType(request.getRuleType())
                .targetValue(request.getTargetValue())
                .rewardPoints(request.getRewardPoints())
                .rewardVoucherTemplate(vt)
                .rewardTitle(title)
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .active(request.getIsActive() != null ? request.getIsActive() : true)
                .system(false)
                .createdBy(admin)
                .build();

        Quest saved = questRepository.save(quest);
        log.info("Admin {} created quest {}", admin.getUserId(), saved.getCode());
        return toAdminResponse(saved);
    }

    @Override
    @Transactional
    public QuestAdminResponse updateQuest(UUID questId, UpdateQuestAdminRequest request) {
        Quest quest = questRepository.findById(questId)
                .filter(q -> !q.isDeleted())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy nhiệm vụ"));

        quest.setName(request.getName().trim());
        quest.setDescription(request.getDescription());
        quest.setTargetValue(request.getTargetValue());
        quest.setRewardPoints(request.getRewardPoints());

        if (request.getQuestGroup() != null) quest.setQuestGroup(request.getQuestGroup());
        if (request.getQuestType() != null) quest.setQuestType(request.getQuestType());
        if (request.getRuleType() != null) quest.setRuleType(request.getRuleType());
        if (request.getDisplayOrder() != null) quest.setDisplayOrder(request.getDisplayOrder());
        if (request.getIsActive() != null) quest.setActive(request.getIsActive());

        if (request.getRewardVoucherTemplateId() != null) {
            VoucherTemplate vt = voucherTemplateRepository.findById(request.getRewardVoucherTemplateId())
                    .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy mẫu voucher phần thưởng"));
            quest.setRewardVoucherTemplate(vt);
        } else {
            quest.setRewardVoucherTemplate(null);
        }

        if (request.getRewardTitleId() != null) {
            Title title = titleRepository.findById(request.getRewardTitleId())
                    .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy danh hiệu phần thưởng"));
            quest.setRewardTitle(title);
        } else {
            quest.setRewardTitle(null);
        }

        Quest saved = questRepository.save(quest);
        log.info("Admin updated quest {}", saved.getCode());
        return toAdminResponse(saved);
    }

    @Override
    @Transactional
    public void toggleQuestStatus(UUID questId, boolean active) {
        Quest quest = questRepository.findById(questId)
                .filter(q -> !q.isDeleted())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy nhiệm vụ"));
        quest.setActive(active);
        questRepository.save(quest);
        log.info("Toggled quest {} active status to {}", quest.getCode(), active);
    }

    @Override
    @Transactional
    public void deleteQuest(UUID questId) {
        Quest quest = questRepository.findById(questId)
                .filter(q -> !q.isDeleted())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy nhiệm vụ"));

        if (quest.isSystem()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Không được phép xóa nhiệm vụ cốt lõi của hệ thống");
        }

        quest.setDeleted(true);
        questRepository.save(quest);
        log.info("Soft deleted quest {}", quest.getCode());
    }

    private QuestAdminResponse toAdminResponse(Quest q) {
        long completed = progressRepository.countByQuestQuestIdAndStatus(q.getQuestId(), QuestProgressStatus.REWARDED);

        QuestRewardDto.VoucherRewardDto vDto = null;
        if (q.getRewardVoucherTemplate() != null) {
            VoucherTemplate vt = q.getRewardVoucherTemplate();
            vDto = QuestRewardDto.VoucherRewardDto.builder()
                    .templateId(vt.getTemplateId())
                    .title(vt.getTitle())
                    .faceValueVnd(vt.getFaceValueVnd())
                    .build();
        }

        EquippedTitleDto tDto = null;
        if (q.getRewardTitle() != null) {
            Title t = q.getRewardTitle();
            tDto = EquippedTitleDto.builder()
                    .titleId(t.getTitleId())
                    .code(t.getCode())
                    .name(t.getName())
                    .tier(t.getTier().name())
                    .colorHex(t.getColorHex())
                    .iconUrl(t.getIconUrl())
                    .build();
        }

        return QuestAdminResponse.builder()
                .questId(q.getQuestId())
                .code(q.getCode())
                .name(q.getName())
                .description(q.getDescription())
                .questGroup(q.getQuestGroup())
                .questType(q.getQuestType())
                .ruleType(q.getRuleType())
                .targetValue(q.getTargetValue())
                .rewardPoints(q.getRewardPoints())
                .rewardVoucher(vDto)
                .rewardTitle(tDto)
                .displayOrder(q.getDisplayOrder())
                .isActive(q.isActive())
                .isSystem(q.isSystem())
                .totalCompletedUsers(completed)
                .createdAt(q.getCreatedAt())
                .updatedAt(q.getUpdatedAt())
                .build();
    }
}
