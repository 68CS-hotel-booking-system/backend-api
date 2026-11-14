package com.team.hotelbooking.pricing.api.dto.response;

import com.team.hotelbooking.pricing.application.result.DailyRateResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;

public record DailyRateDto(
    LocalDate date,
    Long price,
    boolean hasRate,
    Instant updatedAt,
    @Schema(description = "Giá hiệu lực mỗi phòng mỗi đêm; null khi không có giá hợp lệ")
        Long effectivePrice,
    @Schema(
            description = "Nguồn giá hiệu lực: giá riêng, giá cơ bản hoặc không có giá hợp lệ",
            allowableValues = {"RATE", "BASE", "NONE"})
        String source) {

  public static DailyRateDto from(DailyRateResult result) {
    return new DailyRateDto(
        result.date(),
        result.price(),
        result.hasRate(),
        result.updatedAt(),
        result.effectivePrice(),
        result.source());
  }
}
