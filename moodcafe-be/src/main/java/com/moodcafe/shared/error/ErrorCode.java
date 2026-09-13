package com.moodcafe.shared.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // System Config
    SYSTEM_CONFIG_NOT_INITIALIZED(HttpStatus.INTERNAL_SERVER_ERROR, "System configuration not initialized"),
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
    AMENITY_NOT_FOUND(HttpStatus.NOT_FOUND, "Amenity not found"),
    AMENITY_ALREADY_EXISTS(HttpStatus.CONFLICT, "Amenity already exists"),
    STORE_AMENITY_NOT_FOUND(HttpStatus.NOT_FOUND, "Amenity is not associated with this store"),
    STORE_AMENITY_ALREADY_EXISTS(HttpStatus.CONFLICT, "Amenity is already added to this store"),
    STORE_IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "Store image not found"),
    FAVORITE_STORE_NOT_FOUND(HttpStatus.NOT_FOUND, "Favorite store not found"),
    FAVORITE_STORE_ALREADY_EXISTS(HttpStatus.CONFLICT, "Store is already in user's favorites"),
    FORBIDDEN_STORE_ACCESS(HttpStatus.FORBIDDEN, "You do not have permission to manage this store"),
    TAG_NOT_FOUND(HttpStatus.NOT_FOUND, "Tag not found"),
    TAG_ALREADY_EXISTS(HttpStatus.CONFLICT, "Tag name already exists"),
    // Generic
    FILE_EMPTY(HttpStatus.BAD_REQUEST, "File is empty"),
    FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "File size exceeds the 5MB limit"),
    FILE_TYPE_INVALID(HttpStatus.BAD_REQUEST, "Only JPG, PNG, and WEBP image formats are supported"),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload file to Cloudinary"),
    FORBIDDEN_ACTION(HttpStatus.FORBIDDEN, "Access denied"),
    INVALID_ROLE(HttpStatus.BAD_REQUEST, "Invalid role"),
    DATABASE_CONNECTION_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "Cơ sở dữ liệu tạm thời gián đoạn kết nối, vui lòng thử lại sau"),
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