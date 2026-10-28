package com.team.hotelbooking.payment.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateFinancialAccessRequest(
    @NotNull(message = "Trạng thái cấp quyền không được để trống.") Boolean isGranted,
    @Size(max = 50, message = "Phạm vi quyền không vượt quá 50 ký tự.") String accessScope,
    @Size(max = 255, message = "Lý do thay đổi không vượt quá 255 ký tự.") String reason,
    Long expectedVersion) {}
