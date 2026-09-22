package com.moodcafe.store.controller;

import com.moodcafe.shared.response.ApiResponse;
import com.moodcafe.store.abstraction.service.VisitVerificationService;
import com.moodcafe.store.dto.request.CreateVisitSnapRequest;
import com.moodcafe.store.dto.request.SubmitMicroSurveyRequest;
import com.moodcafe.store.dto.response.ActiveVisitStatusResponse;
import com.moodcafe.store.dto.response.MicroSurveyQuestionDto;
import com.moodcafe.store.dto.response.SurveySubmissionResponse;
import com.moodcafe.store.dto.response.VisitVerificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Vibe Snap & Micro-Survey", description = "Endpoints cho chụp ảnh realtime xác thực tại quán (Vibe Snap), Micro-Survey và mở khóa Verified Review")
public class VisitVerificationController {

    private final VisitVerificationService visitVerificationService;

    @Operation(summary = "Chụp ảnh realtime xác thực tại quán (Vibe Snap)",
            description = "Yêu cầu ảnh chụp trực tiếp từ Camera và tọa độ GPS của người dùng. Hệ thống sẽ kiểm tra bán kính <= 50m. Nếu hợp lệ, lưu xác thực và trả về câu hỏi Micro-Survey.")
    @PostMapping(value = "/api/stores/{storeId}/snap", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<VisitVerificationResponse>> snapVisitByStore(
            @PathVariable UUID storeId,
            @RequestParam("image") MultipartFile image,
            @Valid @ModelAttribute CreateVisitSnapRequest request
    ) {
        VisitVerificationResponse response = visitVerificationService.snapVisit(storeId, request, image);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Xác thực vị trí ghé quán thành công"));
    }

    @Operation(summary = "Chụp ảnh realtime xác thực tại quán (Alias /api/visits/snap)")
    @PostMapping(value = "/api/visits/snap", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<VisitVerificationResponse>> snapVisit(
            @RequestParam("storeId") UUID storeId,
            @RequestParam("image") MultipartFile image,
            @Valid @ModelAttribute CreateVisitSnapRequest request
    ) {
        VisitVerificationResponse response = visitVerificationService.snapVisit(storeId, request, image);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Xác thực vị trí ghé quán thành công"));
    }

    @Operation(summary = "Lấy câu hỏi Micro-Survey cho lượt ghé thăm (1 câu hỏi / 3 lựa chọn)")
    @GetMapping("/api/visits/{visitVerificationId}/survey")
    public ResponseEntity<ApiResponse<MicroSurveyQuestionDto>> getVisitSurvey(
            @PathVariable UUID visitVerificationId
    ) {
        MicroSurveyQuestionDto question = visitVerificationService.getVisitMicroSurveyQuestion(visitVerificationId);
        return ResponseEntity.ok(ApiResponse.success(question));
    }

    @Operation(summary = "Gửi phản hồi Micro-Survey (Mở khóa Verified Review)",
            description = "Gửi lựa chọn CORRECT / NEUTRAL / WRONG cho tag được khảo sát. Sau khi gửi thành công, người dùng được mở khóa viết Verified Review.")
    @PostMapping(value = "/api/visits/{visitVerificationId}/survey", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<SurveySubmissionResponse>> submitMicroSurvey(
            @PathVariable UUID visitVerificationId,
            @Valid @RequestBody SubmitMicroSurveyRequest request
    ) {
        SurveySubmissionResponse response = visitVerificationService.submitMicroSurvey(visitVerificationId, request);
        return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
    }

    @Operation(summary = "Kiểm tra xem user hiện tại có lượt Snap hợp lệ nào chưa dùng tại quán hay không",
            description = "Dùng cho Mobile kiểm tra để quyết định hiển thị nút 'Chụp ảnh tại quán' hay 'Viết đánh giá xác thực'.")
    @GetMapping("/api/stores/{storeId}/visits/active")
    public ResponseEntity<ApiResponse<ActiveVisitStatusResponse>> getActiveVisitStatus(
            @PathVariable UUID storeId
    ) {
        ActiveVisitStatusResponse response = visitVerificationService.getActiveVisitStatus(storeId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Lấy chi tiết lượt xác thực ghé thăm theo ID")
    @GetMapping("/api/visits/{visitVerificationId}")
    public ResponseEntity<ApiResponse<VisitVerificationResponse>> getVisitVerificationById(
            @PathVariable UUID visitVerificationId
    ) {
        VisitVerificationResponse response = visitVerificationService.getVisitVerificationById(visitVerificationId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
