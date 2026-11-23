package com.team.hotelbooking.booking.api.dto.response;

import com.team.hotelbooking.booking.application.result.SaveVoucherResult;
import java.util.UUID;

public record SaveVoucherResponse(UUID id, UUID hotelId, String code, String status) {
  public static SaveVoucherResponse from(SaveVoucherResult result) {
    return new SaveVoucherResponse(result.id(), result.hotelId(), result.code(), result.status());
  }
}
