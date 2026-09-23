package com.moodcafe.shared.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // System Config
    SYSTEM_CONFIG_NOT_INITIALIZED(HttpStatus.INTERNAL_SERVER_ERROR, "System configuration not initialized"),
    SYSTEM_CONFIG_NOT_FOUND(HttpStatus.NOT_FOUND, "System configuration not found"),
    INVALID_CONFIG_VALUE(HttpStatus.BAD_REQUEST, "Invalid configuration value"),
    // Token
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Token has expired"),
    TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "Token not found"),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "Token is invalid"),
    TOKEN_REVOKED(HttpStatus.UNAUTHORIZED, "Token has been revoked"),
    MISSING_COOKIE(HttpStatus.BAD_REQUEST, "Missing authentication cookie"),
    // Redis
    REDIS_DATA_NOT_FOUND(HttpStatus.NOT_FOUND, "Redis data not found"),
    OTP_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save OTP"),
    OTP_RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "OTP rate limit exceeded"),
    // Auth
    EMAIL_NOT_FOUND(HttpStatus.NOT_FOUND, "Email not found"),
    EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT, "Email already registered"),
    WRONG_PASSWORD(HttpStatus.UNAUTHORIZED, "Wrong password"),
    WRONG_OTP_CODE(HttpStatus.BAD_REQUEST, "Wrong OTP code"),
    OTP_INVALID(HttpStatus.BAD_REQUEST, "Invalid OTP code"),
    OTP_EXPIRED(HttpStatus.BAD_REQUEST, "OTP has expired or registration session not found"),
    TOO_MANY_FAILED_ATTEMPTS(HttpStatus.TOO_MANY_REQUESTS, "Too many failed attempts"),
    // OTP
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Unauthorized access"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "You do not have permission to perform this action"),
    EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to send email"),
    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),
    USER_ALREADY_REGISTERED(HttpStatus.CONFLICT, "User already registered"),
    USER_IS_BLOCKED(HttpStatus.FORBIDDEN, "User is blocked"),
    // Role
    ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, "Role not found"),
    // Common
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "Bad request"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Validation error"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "Invalid input"),
    // Store
    STORE_NOT_FOUND(HttpStatus.NOT_FOUND, "Store not found"),
    STORE_ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, "Store role not found"),
    STORE_STAFF_NOT_FOUND(HttpStatus.NOT_FOUND, "Store staff not found"),
    STORE_STAFF_ALREADY_EXISTS(HttpStatus.CONFLICT, "User is already assigned to this store"),
    STORE_IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "Store image not found"),
    FAVORITE_STORE_NOT_FOUND(HttpStatus.NOT_FOUND, "Favorite store not found"),
    FAVORITE_STORE_ALREADY_EXISTS(HttpStatus.CONFLICT, "Store is already in user's favorites"),
    FORBIDDEN_STORE_ACCESS(HttpStatus.FORBIDDEN, "You do not have permission to manage this store"),
    TAG_NOT_FOUND(HttpStatus.NOT_FOUND, "Tag not found"),
    TAG_ALREADY_EXISTS(HttpStatus.CONFLICT, "Tag name already exists"),
    TAG_CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "Tag category not found"),
    TAG_CATEGORY_ALREADY_EXISTS(HttpStatus.CONFLICT, "Tag category code or name already exists"),
    ONBOARDING_QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "Onboarding question not found"),
    STORE_TAG_ALREADY_REQUESTED(HttpStatus.CONFLICT, "Tag has already been requested or approved for this store"),
    STORE_TAG_NOT_FOUND(HttpStatus.NOT_FOUND, "Store tag association not found"),
    STORE_RESUBMIT_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "Quán đã bị từ chối và không được phép nộp lại hồ sơ"),
    STORE_NOT_REJECTED(HttpStatus.BAD_REQUEST, "Chỉ có quán ở trạng thái bị từ chối mới có thể nộp lại hồ sơ"),
    STORE_TAG_REQUIRED(HttpStatus.BAD_REQUEST, "Vui lòng chọn ít nhất 1 thẻ vibe khi đăng ký quán"),
    STORE_TAG_PROOF_REQUIRED(HttpStatus.BAD_REQUEST, "Thẻ vibe yêu cầu đính kèm ảnh minh chứng thực tế"),
    STORE_TAG_RESUBMIT_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "Thẻ vibe đã bị từ chối và không được phép nộp lại"),
    // Review
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "Review not found"),
    USER_ALREADY_REVIEWED(HttpStatus.CONFLICT,
            "Bạn đã gửi đánh giá cho quán này rồi. Mỗi người dùng chỉ được đánh giá 1 lần."),
    REVIEW_IMAGE_REQUIRED(HttpStatus.BAD_REQUEST,
            "A live photo of the store taken at the moment is required to submit a review"),
    VIBE_IMAGE_REQUIRED(HttpStatus.BAD_REQUEST,
            "Hình ảnh đại diện cho phong cách không gian (Vibe) là bắt buộc"),
    FORBIDDEN_REVIEW_ACTION(HttpStatus.FORBIDDEN, "You do not have permission to modify this review"),
    MAX_REVIEW_IMAGES_EXCEEDED(HttpStatus.BAD_REQUEST, "Đánh giá chỉ được tải lên tối đa 3 ảnh"),
    REPORT_ALREADY_SUBMITTED(HttpStatus.CONFLICT, "Báo cáo cho đánh giá này đã được gửi trước đó"),
    // Visit Verification & Snap
    LOCATION_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, "Khoảng cách hiện tại vượt quá bán kính cho phép (50m) của quán"),
    STORE_LOCATION_NOT_CONFIGURED(HttpStatus.BAD_REQUEST, "Quán chưa cập nhật tọa độ vị trí GPS"),
    FORBIDDEN_STORE_STAFF_ACTION(HttpStatus.FORBIDDEN, "Chủ quán hoặc nhân viên không được phép thực hiện hành động này trên quán của mình"),
    SNAP_COOLDOWN_ACTIVE(HttpStatus.BAD_REQUEST, "Bạn đã check-in tại quán này trong vòng 24 giờ qua. Vui lòng quay lại sau."),
    VISIT_VERIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy thông tin xác thực lần ghé thăm"),
    VISIT_VERIFICATION_EXPIRED(HttpStatus.BAD_REQUEST, "Xác thực lần ghé thăm đã hết hạn (chỉ có hiệu lực trong 24 giờ)"),
    VISIT_VERIFICATION_ALREADY_USED(HttpStatus.BAD_REQUEST, "Xác thực lần ghé thăm này đã được dùng cho một đánh giá khác"),
    VISIT_VERIFICATION_INVALID(HttpStatus.BAD_REQUEST, "Xác thực lần ghé thăm chưa hợp lệ hoặc đã bị từ chối"),
    VISIT_VERIFICATION_STORE_MISMATCH(HttpStatus.BAD_REQUEST, "Xác thực ghé thăm không thuộc về quán này"),
    VISIT_VERIFICATION_USER_MISMATCH(HttpStatus.FORBIDDEN, "Xác thực ghé thăm không thuộc về tài khoản của bạn"),
    SURVEY_ALREADY_SUBMITTED(HttpStatus.CONFLICT, "Khảo sát cho lần ghé thăm này đã được hoàn thành"),
    // Generic
    FILE_EMPTY(HttpStatus.BAD_REQUEST, "File is empty"),
    FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "File size exceeds the 5MB limit"),
    FILE_TYPE_INVALID(HttpStatus.BAD_REQUEST, "Only JPG, PNG, and WEBP image formats are supported"),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload file to Cloudinary"),
    FORBIDDEN_ACTION(HttpStatus.FORBIDDEN, "Access denied"),
    INVALID_ROLE(HttpStatus.BAD_REQUEST, "Invalid role"),
    DATABASE_CONNECTION_ERROR(HttpStatus.SERVICE_UNAVAILABLE,
            "Cơ sở dữ liệu tạm thời gián đoạn kết nối, vui lòng thử lại sau"),
    UNCATEGORIZED_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "Uncategorized exception");

    private final HttpStatus statusCode;
    private final String message;

    ErrorCode(HttpStatus statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }

    public HttpStatus getStatusCode() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }
}