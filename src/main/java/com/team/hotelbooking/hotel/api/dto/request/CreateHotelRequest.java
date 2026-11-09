package com.team.hotelbooking.hotel.api.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

public record CreateHotelRequest(
    UUID ownerId,
    @NotNull(message = "Thành phố không được để trống.") UUID cityId,
    @NotBlank(message = "Tên khách sạn không được để trống.")
        @Size(max = 255, message = "Tên khách sạn không được vượt quá 255 ký tự.")
        String name,
    String description,
    @NotBlank(message = "Địa chỉ không được để trống.")
        @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự.")
        String address,
    @DecimalMin(value = "-90", message = "Vĩ độ phải lớn hơn hoặc bằng -90.")
        @DecimalMax(value = "90", message = "Vĩ độ phải nhỏ hơn hoặc bằng 90.")
        @Digits(integer = 3, fraction = 6, message = "Vĩ độ chỉ được có tối đa 6 chữ số thập phân.")
        BigDecimal latitude,
    @DecimalMin(value = "-180", message = "Kinh độ phải lớn hơn hoặc bằng -180.")
        @DecimalMax(value = "180", message = "Kinh độ phải nhỏ hơn hoặc bằng 180.")
        @Digits(
            integer = 3,
            fraction = 6,
            message = "Kinh độ chỉ được có tối đa 6 chữ số thập phân.")
        BigDecimal longitude,
    @Min(value = 1, message = "Số sao khách sạn phải từ 1 đến 5.")
        @Max(value = 5, message = "Số sao khách sạn phải từ 1 đến 5.")
        Short starRating,
    @NotNull(message = "Giờ nhận phòng không được để trống.") LocalTime checkInTime,
    @NotNull(message = "Giờ trả phòng không được để trống.") LocalTime checkOutTime) {

  public CreateHotelRequest {
    latitude = latitude == null ? null : latitude.stripTrailingZeros();
    longitude = longitude == null ? null : longitude.stripTrailingZeros();
  }
}
