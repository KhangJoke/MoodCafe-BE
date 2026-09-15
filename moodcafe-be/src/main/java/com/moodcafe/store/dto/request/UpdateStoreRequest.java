package com.moodcafe.store.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateStoreRequest {

    @Size(max = 255, message = "Store name cannot exceed 255 characters")
    private String name;

    private String description;

    @Size(max = 500, message = "Address cannot exceed 500 characters")
    private String address;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private LocalTime openingTime;

    private LocalTime closingTime;

    @Size(max = 50, message = "Price range cannot exceed 50 characters")
    private String priceRange;

    @Size(max = 20, message = "Phone cannot exceed 20 characters")
    private String phone;

    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email cannot exceed 255 characters")
    private String email;
}
