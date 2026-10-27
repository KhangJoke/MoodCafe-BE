package com.moodcafe.gamification.service;

import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.auth.entity.User;
import com.moodcafe.gamification.abstraction.repository.TitleRepository;
import com.moodcafe.gamification.abstraction.repository.UserTitleRepository;
import com.moodcafe.gamification.abstraction.service.CustomerTitleService;
import com.moodcafe.gamification.dto.response.EquippedTitleDto;
import com.moodcafe.gamification.dto.response.TitleItemDto;
import com.moodcafe.gamification.dto.response.UserTitleShowcaseResponse;
import com.moodcafe.gamification.entity.Title;
import com.moodcafe.gamification.entity.UserTitle;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerTitleServiceImpl implements CustomerTitleService {

    private final TitleRepository titleRepository;
    private final UserTitleRepository userTitleRepository;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional(readOnly = true)
    public UserTitleShowcaseResponse getMyTitleShowcase() {
        User user = currentUserService.getCurrentUser();
        if (user == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Vui lòng đăng nhập để xem danh hiệu");
        }
        UUID userId = user.getUserId();

        List<Title> allTitles = titleRepository.findAllByActiveTrueAndIsDeletedFalseOrderByCreatedAtAsc();
        List<UserTitle> userTitles = userTitleRepository.findAllByUserUserIdAndIsDeletedFalseOrderByUnlockedAtDesc(userId);

        Map<UUID, UserTitle> ownedMap = userTitles.stream()
                .collect(Collectors.toMap(ut -> ut.getTitle().getTitleId(), ut -> ut, (a, b) -> a));

        EquippedTitleDto equipped = null;
        List<TitleItemDto> items = new ArrayList<>();

        for (Title t : allTitles) {
            UserTitle ut = ownedMap.get(t.getTitleId());
            boolean isUnlocked = (ut != null);
            boolean isEquipped = (ut != null && ut.isEquipped());

            if (isEquipped) {
                equipped = EquippedTitleDto.builder()
                        .titleId(t.getTitleId())
                        .code(t.getCode())
                        .name(t.getName())
                        .tier(t.getTier().name())
                        .colorHex(t.getColorHex())
                        .iconUrl(t.getIconUrl())
                        .build();
            }

            items.add(TitleItemDto.builder()
                    .titleId(t.getTitleId())
                    .code(t.getCode())
                    .name(t.getName())
                    .description(t.getDescription())
                    .tier(t.getTier().name())
                    .colorHex(t.getColorHex())
                    .iconUrl(t.getIconUrl())
                    .isUnlocked(isUnlocked)
                    .isEquipped(isEquipped)
                    .unlockedAt(ut != null ? ut.getUnlockedAt() : null)
                    .unlockCondition(t.getDescription())
                    .build());
        }

        return UserTitleShowcaseResponse.builder()
                .equippedTitle(equipped)
                .totalUnlocked(userTitles.size())
                .totalAvailable(allTitles.size())
                .titles(items)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public EquippedTitleDto getEquippedTitle(UUID userId) {
        if (userId == null) return null;
        return userTitleRepository.findByUserUserIdAndEquippedTrueAndIsDeletedFalse(userId)
                .map(ut -> {
                    Title t = ut.getTitle();
                    return EquippedTitleDto.builder()
                            .titleId(t.getTitleId())
                            .code(t.getCode())
                            .name(t.getName())
                            .tier(t.getTier().name())
                            .colorHex(t.getColorHex())
                            .iconUrl(t.getIconUrl())
                            .build();
                })
                .orElse(null);
    }

    @Override
    @Transactional
    public EquippedTitleDto equipTitle(UUID titleId) {
        User user = currentUserService.getCurrentUser();
        if (user == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Vui lòng đăng nhập");
        }
        UUID userId = user.getUserId();

        UserTitle target = userTitleRepository.findByUserUserIdAndTitleTitleId(userId, titleId)
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST, "Bạn chưa sở hữu danh hiệu này để trang bị"));

        if (target.isEquipped()) {
            Title t = target.getTitle();
            return EquippedTitleDto.builder()
                    .titleId(t.getTitleId())
                    .code(t.getCode())
                    .name(t.getName())
                    .tier(t.getTier().name())
                    .colorHex(t.getColorHex())
                    .iconUrl(t.getIconUrl())
                    .build();
        }

        userTitleRepository.unequipAllByUserId(userId);
        target.setEquipped(true);
        userTitleRepository.save(target);

        Title t = target.getTitle();
        log.info("User {} equipped title {}", userId, t.getName());
        return EquippedTitleDto.builder()
                .titleId(t.getTitleId())
                .code(t.getCode())
                .name(t.getName())
                .tier(t.getTier().name())
                .colorHex(t.getColorHex())
                .iconUrl(t.getIconUrl())
                .build();
    }

    @Override
    @Transactional
    public void unequipTitle() {
        User user = currentUserService.getCurrentUser();
        if (user == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Vui lòng đăng nhập");
        }
        userTitleRepository.unequipAllByUserId(user.getUserId());
        log.info("User {} unequipped all titles", user.getUserId());
    }
}
