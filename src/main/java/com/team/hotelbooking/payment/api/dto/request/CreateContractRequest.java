package com.team.hotelbooking.payment.api.dto.request;

import com.team.hotelbooking.payment.domain.model.HotelContract;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreateContractRequest(
    @NotBlank(message = "Mã hợp đồng không được để trống.")
        @Size(max = 50, message = "Mã hợp đồng không vượt quá 50 ký tự.")
        String contractCode,
    @NotNull(message = "Chủ sở hữu (Owner) không được để trống.") UUID ownerUserId,
    @NotNull(message = "Tỷ lệ hoa hồng không được để trống.")
        @DecimalMin(value = "0.00", message = "Tỷ lệ hoa hồng tối thiểu là 0.00%.")
        @DecimalMax(value = "100.00", message = "Tỷ lệ hoa hồng tối đa là 100.00%.")
        BigDecimal commissionRate,
    HotelContract.CommissionBasis commissionBasis,
    @NotNull(message = "Thời gian bắt đầu hợp đồng không được để trống.") Instant startsAt,
    @NotNull(message = "Thời gian kết thúc hợp đồng không được để trống.") Instant endsAt,
    HotelContract.ContractStatus status) {}
