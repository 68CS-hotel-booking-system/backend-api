package com.team.hotelbooking.booking.review.api.dto.request;

import jakarta.validation.constraints.*;

public record SubmitGuestReviewRequest(
    @NotNull @DecimalMin("1") @DecimalMax("5") @Digits(integer = 1, fraction = 0)
        java.math.BigDecimal rating,
    @NotBlank @Size(max = 2000) String content) {}
