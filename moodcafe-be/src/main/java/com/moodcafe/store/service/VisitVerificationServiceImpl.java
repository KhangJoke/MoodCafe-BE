package com.moodcafe.store.service;

import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.auth.entity.User;
import com.moodcafe.shared.abstraction.service.FileStorageService;
import com.moodcafe.shared.dto.UploadImageResponse;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.shared.util.GeoUtils;
import com.moodcafe.store.abstraction.repository.StoreRepository;
import com.moodcafe.store.abstraction.repository.StoreStaffRepository;
import com.moodcafe.store.abstraction.repository.VibeSurveyLogRepository;
import com.moodcafe.store.abstraction.repository.VisitVerificationRepository;
import com.moodcafe.store.abstraction.service.VisitVerificationService;
import com.moodcafe.store.dto.request.CreateVisitSnapRequest;
import com.moodcafe.store.dto.request.SubmitMicroSurveyRequest;
import com.moodcafe.store.dto.response.ActiveVisitStatusResponse;
import com.moodcafe.store.dto.response.MicroSurveyQuestionDto;
import com.moodcafe.store.dto.response.SurveySubmissionResponse;
import com.moodcafe.store.dto.response.VisitVerificationResponse;
import com.moodcafe.store.entity.Store;
import com.moodcafe.store.entity.VibeSurveyLog;
import com.moodcafe.store.entity.VisitVerification;
import com.moodcafe.store.entity.enums.VisitVerificationStatus;
import com.moodcafe.tag.abstraction.repository.StoreTagRepository;
import com.moodcafe.tag.abstraction.repository.TagRepository;
import com.moodcafe.tag.entity.StoreTag;
import com.moodcafe.tag.entity.Tag;
import com.moodcafe.tag.entity.enums.StoreTagStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VisitVerificationServiceImpl implements VisitVerificationService {

    private final VisitVerificationRepository visitVerificationRepository;
    private final VibeSurveyLogRepository vibeSurveyLogRepository;
    private final StoreRepository storeRepository;
    private final StoreStaffRepository storeStaffRepository;
    private final StoreTagRepository storeTagRepository;
    private final TagRepository tagRepository;
    private final CurrentUserService currentUserService;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public VisitVerificationResponse snapVisit(UUID storeId, CreateVisitSnapRequest request, MultipartFile image) {
        User currentUser = currentUserService.getCurrentUser();

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new AppException(ErrorCode.STORE_NOT_FOUND));

        // 1. Business Rule: Owner or Staff cannot snap their own store
        if (storeStaffRepository.existsByStoreStoreIdAndUserUserId(storeId, currentUser.getUserId())) {
            throw new AppException(ErrorCode.FORBIDDEN_STORE_STAFF_ACTION,
                    "Chủ quán hoặc nhân viên không được phép tự chụp ảnh xác thực tại quán của mình");
        }

        // 2. Validate Store GPS configuration
        if (store.getLatitude() == null || store.getLongitude() == null) {
            throw new AppException(ErrorCode.STORE_LOCATION_NOT_CONFIGURED,
                    "Quán chưa cập nhật tọa độ vị trí GPS, không thể xác thực vị trí");
        }

        // 3. Validate image provided
        if (image == null || image.isEmpty()) {
            throw new AppException(ErrorCode.REVIEW_IMAGE_REQUIRED, "Vui lòng chụp ảnh trực tiếp tại quán");
        }

        // 4. Validate user coordinates
        if (request.getLatitude() == null || request.getLongitude() == null) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Tọa độ GPS hiện tại là bắt buộc");
        }

        // 5. Calculate Haversine distance and enforce <= 50m BEFORE image upload (No Orphan Files)
        double distance = GeoUtils.calculateDistanceMeters(
                request.getLatitude(), request.getLongitude(),
                store.getLatitude(), store.getLongitude()
        );
        double roundedDistance = GeoUtils.roundToOneDecimal(distance);

        if (distance > GeoUtils.MAX_VERIFICATION_DISTANCE_METERS) {
            Map<String, Object> distanceDetails = Map.of(
                    "currentDistanceMeters", roundedDistance,
                    "maxAllowedDistanceMeters", GeoUtils.MAX_VERIFICATION_DISTANCE_METERS
            );
            log.warn("User {} snap rejected: distance {}m exceeds {}m for store {}",
                    currentUser.getUserId(), roundedDistance, GeoUtils.MAX_VERIFICATION_DISTANCE_METERS, storeId);
            throw new AppException(
                    ErrorCode.LOCATION_OUT_OF_RANGE,
                    String.format("Bạn đang cách quán %.1fm, vượt quá bán kính cho phép (50m). Vui lòng di chuyển lại gần quán hoặc khu vực cửa sổ để bắt GPS tốt hơn.", roundedDistance),
                    distanceDetails
            );
        }

        // 6. Upload live moment photo to Cloudinary
        UploadImageResponse uploadResponse = fileStorageService.uploadImage(image, "visits");
        String imageUrl = uploadResponse.getImageUrl();

        // 7. Save VisitVerification to DB with orphan cleanup on failure
        VisitVerification visitVerification;
        try {
            Instant serverTimestamp = Instant.now();
            Instant expiresAt = serverTimestamp.plus(24, ChronoUnit.HOURS);

            visitVerification = VisitVerification.builder()
                    .store(store)
                    .user(currentUser)
                    .imageUrl(imageUrl)
                    .latitude(request.getLatitude())
                    .longitude(request.getLongitude())
                    .capturedAt(serverTimestamp)
                    .verifiedAt(serverTimestamp)
                    .expiresAt(expiresAt)
                    .distanceFromStoreMeters(BigDecimal.valueOf(roundedDistance))
                    .status(VisitVerificationStatus.VERIFIED)
                    .isUsed(false)
                    .build();

            visitVerification = visitVerificationRepository.save(visitVerification);
        } catch (Exception ex) {
            log.error("Failed to save VisitVerification for user {} and store {}. Cleaning up Cloudinary image {}: {}",
                    currentUser.getUserId(), storeId, imageUrl, ex.getMessage());
            try {
                fileStorageService.deleteImageByUrl(imageUrl);
            } catch (Exception cleanupEx) {
                log.warn("Failed to delete orphan image {} from Cloudinary: {}", imageUrl, cleanupEx.getMessage());
            }
            throw ex;
        }

        log.info("VisitVerification {} created successfully for user {} at store {} (distance: {}m)",
                visitVerification.getVisitVerificationId(), currentUser.getUserId(), storeId, roundedDistance);

        MicroSurveyQuestionDto surveyQuestion = buildMicroSurveyQuestionForStore(storeId);

        return toResponse(visitVerification, surveyQuestion);
    }

    @Override
    @Transactional(readOnly = true)
    public MicroSurveyQuestionDto getStoreMicroSurveyQuestion(UUID storeId) {
        if (!storeRepository.existsById(storeId)) {
            throw new AppException(ErrorCode.STORE_NOT_FOUND);
        }
        return buildMicroSurveyQuestionForStore(storeId);
    }

    @Override
    @Transactional(readOnly = true)
    public MicroSurveyQuestionDto getVisitMicroSurveyQuestion(UUID visitVerificationId) {
        VisitVerification visit = visitVerificationRepository.findById(visitVerificationId)
                .orElseThrow(() -> new AppException(ErrorCode.VISIT_VERIFICATION_NOT_FOUND));

        return buildMicroSurveyQuestionForStore(visit.getStore().getStoreId());
    }

    @Override
    @Transactional
    public SurveySubmissionResponse submitMicroSurvey(UUID visitVerificationId, SubmitMicroSurveyRequest request) {
        User currentUser = currentUserService.getCurrentUser();

        VisitVerification visit = visitVerificationRepository.findById(visitVerificationId)
                .orElseThrow(() -> new AppException(ErrorCode.VISIT_VERIFICATION_NOT_FOUND));

        if (!visit.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new AppException(ErrorCode.VISIT_VERIFICATION_USER_MISMATCH);
        }

        UUID storeId = visit.getStore().getStoreId();
        if (storeStaffRepository.existsByStoreStoreIdAndUserUserId(storeId, currentUser.getUserId())) {
            throw new AppException(ErrorCode.FORBIDDEN_STORE_STAFF_ACTION);
        }

        if (visit.getStatus() != VisitVerificationStatus.VERIFIED) {
            throw new AppException(ErrorCode.VISIT_VERIFICATION_INVALID);
        }

        if (visit.getExpiresAt().isBefore(Instant.now())) {
            throw new AppException(ErrorCode.VISIT_VERIFICATION_EXPIRED);
        }

        if (vibeSurveyLogRepository.existsByVisitVerificationVisitVerificationId(visitVerificationId)) {
            throw new AppException(ErrorCode.SURVEY_ALREADY_SUBMITTED);
        }

        Tag tag = tagRepository.findById(request.getTagId())
                .orElseThrow(() -> new AppException(ErrorCode.TAG_NOT_FOUND));

        VibeSurveyLog surveyLog = VibeSurveyLog.builder()
                .visitVerification(visit)
                .tag(tag)
                .response(request.getResponse())
                .build();

        vibeSurveyLogRepository.save(surveyLog);

        log.info("User {} submitted survey response {} for tag {} on visit {}",
                currentUser.getUserId(), request.getResponse(), tag.getName(), visitVerificationId);

        return SurveySubmissionResponse.builder()
                .visitVerificationId(visitVerificationId)
                .tagId(tag.getTagId())
                .tagName(tag.getName())
                .response(request.getResponse())
                .verifiedReviewUnlocked(true)
                .message("Khảo sát hoàn tất! Bạn đã mở khóa quyền viết Đánh giá xác thực (Verified Review).")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ActiveVisitStatusResponse getActiveVisitStatus(UUID storeId) {
        User currentUser = currentUserService.getCurrentUser();

        var activeVisitOpt = visitVerificationRepository
                .findFirstByStoreStoreIdAndUserUserIdAndStatusAndIsUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                        storeId,
                        currentUser.getUserId(),
                        VisitVerificationStatus.VERIFIED,
                        Instant.now()
                );

        if (activeVisitOpt.isEmpty()) {
            return ActiveVisitStatusResponse.builder()
                    .hasActiveVisit(false)
                    .build();
        }

        VisitVerification visit = activeVisitOpt.get();
        boolean surveyCompleted = vibeSurveyLogRepository
                .existsByVisitVerificationVisitVerificationId(visit.getVisitVerificationId());

        long remainingMinutes = Math.max(0, Duration.between(Instant.now(), visit.getExpiresAt()).toMinutes());

        return ActiveVisitStatusResponse.builder()
                .hasActiveVisit(true)
                .visitVerificationId(visit.getVisitVerificationId())
                .surveyCompleted(surveyCompleted)
                .expiresAt(visit.getExpiresAt())
                .remainingMinutes(remainingMinutes)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public VisitVerificationResponse getVisitVerificationById(UUID visitVerificationId) {
        User currentUser = currentUserService.getCurrentUser();
        VisitVerification visit = visitVerificationRepository.findById(visitVerificationId)
                .orElseThrow(() -> new AppException(ErrorCode.VISIT_VERIFICATION_NOT_FOUND));

        boolean isAuthor = visit.getUser().getUserId().equals(currentUser.getUserId());
        boolean isAdmin = currentUser.getRole() != null && "ADMIN".equalsIgnoreCase(currentUser.getRole().getName());
        if (!isAuthor && !isAdmin) {
            throw new AppException(ErrorCode.FORBIDDEN_ACTION);
        }

        MicroSurveyQuestionDto surveyQuestion = buildMicroSurveyQuestionForStore(visit.getStore().getStoreId());
        return toResponse(visit, surveyQuestion);
    }

    private MicroSurveyQuestionDto buildMicroSurveyQuestionForStore(UUID storeId) {
        List<StoreTag> approvedStoreTags = storeTagRepository.findAllByStoreIdAndStatus(storeId, StoreTagStatus.APPROVED);

        StoreTag candidateTag = approvedStoreTags.stream()
                .filter(st -> st.getTag() != null && st.getTag().isActive())
                .max(Comparator.comparing(StoreTag::isHighlighted)
                        .thenComparing(st -> st.getAvgScore() != null ? st.getAvgScore() : 0.0)
                        .thenComparing(st -> st.getReviewCount() != null ? st.getReviewCount() : 0))
                .orElse(null);

        if (candidateTag == null || candidateTag.getTag() == null) {
            return null;
        }

        Tag tag = candidateTag.getTag();
        String categoryName = tag.getCategory() != null ? tag.getCategory().getName() : null;

        return MicroSurveyQuestionDto.builder()
                .tagId(tag.getTagId())
                .tagName(tag.getName())
                .categoryName(categoryName)
                .imageUrl(tag.getImageUrl())
                .questionText(String.format("Không gian quán hôm nay có đúng với phong cách \"%s\" không?", tag.getName()))
                .options(List.of("CORRECT", "NEUTRAL", "WRONG"))
                .build();
    }

    private VisitVerificationResponse toResponse(VisitVerification visit, MicroSurveyQuestionDto surveyQuestion) {
        Double distance = visit.getDistanceFromStoreMeters() != null
                ? visit.getDistanceFromStoreMeters().doubleValue()
                : null;

        return VisitVerificationResponse.builder()
                .visitVerificationId(visit.getVisitVerificationId())
                .storeId(visit.getStore().getStoreId())
                .storeName(visit.getStore().getName())
                .imageUrl(visit.getImageUrl())
                .latitude(visit.getLatitude())
                .longitude(visit.getLongitude())
                .distanceFromStoreMeters(distance)
                .status(visit.getStatus())
                .capturedAt(visit.getCapturedAt())
                .verifiedAt(visit.getVerifiedAt())
                .expiresAt(visit.getExpiresAt())
                .isUsed(visit.isUsed())
                .surveyQuestion(surveyQuestion)
                .build();
    }
}
