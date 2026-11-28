package com.team.hotelbooking.booking.review.api.dto.request;

import jakarta.validation.constraints.*;

public record ReplyGuestReviewRequest(
    @NotBlank @Size(max = 2000) String content, @NotNull @Min(0) Long expectedVersion) {}
