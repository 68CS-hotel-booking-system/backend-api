package com.team.hotelbooking.hotel.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalTime;

// PATCH: field null hoặc không gửi nghĩa là giữ nguyên; version bắt buộc để chống ghi đè.
@Schema(
    description =
        "Cập nhật một phần thông tin khách sạn. Field không gửi hoặc null được giữ nguyên;"
            + " chưa hỗ trợ xóa giá trị. Chuỗi rỗng hoặc chỉ có khoảng trắng bị từ chối (400),"
            + " không được coi là xóa. Phải gửi ít nhất một field ngoài version.")
public record UpdateHotelRequest(
    @Schema(
            description = "Tên khách sạn mới, 1–255 ký tự sau khi bỏ khoảng trắng đầu/cuối.",
            example = "Khách sạn Biển Xanh")
        @Size(max = 255, message = "Tên khách sạn không được vượt quá 255 ký tự.")
        @Pattern(regexp = "(?s).*\\S.*", message = "Tên khách sạn không được để trống.")
        String name,
    @Schema(
            description = "Mô tả mới; không nhận chuỗi rỗng/khoảng trắng (chưa hỗ trợ xóa mô tả).",
            example = "Gần biển Mỹ Khê")
        @Pattern(regexp = "(?s).*\\S.*", message = "Mô tả khách sạn không được để trống.")
        String description,
    @Schema(description = "Địa chỉ mới, 1–255 ký tự.", example = "12 Võ Nguyên Giáp, Đà Nẵng")
        @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự.")
        @Pattern(regexp = "(?s).*\\S.*", message = "Địa chỉ không được để trống.")
        String address,
    @Schema(
            description =
                "Số điện thoại liên hệ (HT-03), 8–15 chữ số, có thể bắt đầu bằng +. Không trả ra"
                    + " API công khai.",
            example = "+84905123456")
        @Pattern(
            regexp = "\\s*\\+?[0-9]{8,15}\\s*",
            message = "Số điện thoại liên hệ phải gồm 8 đến 15 chữ số, có thể bắt đầu bằng dấu +.")
        String contactPhone,
    @Schema(
            description = "Email liên hệ (HT-03), tối đa 255 ký tự. Không trả ra API công khai.",
            example = "lienhe@bienxanh.vn")
        @Size(max = 255, message = "Email liên hệ không được vượt quá 255 ký tự.")
        @Email(message = "Email liên hệ không hợp lệ.")
        @Pattern(regexp = "(?s).*\\S.*", message = "Email liên hệ không được để trống.")
        String contactEmail,
    @Schema(
            description =
                "Vĩ độ từ -90 đến 90, tối đa 6 chữ số thập phân. Phải gửi cùng longitude.",
            example = "16.061400")
        @DecimalMin(value = "-90", message = "Vĩ độ phải lớn hơn hoặc bằng -90.")
        @DecimalMax(value = "90", message = "Vĩ độ phải nhỏ hơn hoặc bằng 90.")
        @Digits(integer = 3, fraction = 6, message = "Vĩ độ chỉ được có tối đa 6 chữ số thập phân.")
        BigDecimal latitude,
    @Schema(
            description =
                "Kinh độ từ -180 đến 180, tối đa 6 chữ số thập phân. Phải gửi cùng latitude.",
            example = "108.247800")
        @DecimalMin(value = "-180", message = "Kinh độ phải lớn hơn hoặc bằng -180.")
        @DecimalMax(value = "180", message = "Kinh độ phải nhỏ hơn hoặc bằng 180.")
        @Digits(
            integer = 3,
            fraction = 6,
            message = "Kinh độ chỉ được có tối đa 6 chữ số thập phân.")
        BigDecimal longitude,
    @Schema(description = "Số sao từ 1 đến 5.", example = "4", minimum = "1", maximum = "5")
        @Min(value = 1, message = "Số sao khách sạn phải từ 1 đến 5.")
        @Max(value = 5, message = "Số sao khách sạn phải từ 1 đến 5.")
        Short starRating,
    @Schema(description = "Giờ nhận phòng, định dạng HH:mm:ss.", example = "14:00:00")
        LocalTime checkInTime,
    @Schema(description = "Giờ trả phòng, định dạng HH:mm:ss.", example = "12:00:00")
        LocalTime checkOutTime,
    @Schema(
            description = "Version đã đọc từ API quản lý; lệch với dữ liệu hiện tại thì trả 409.",
            example = "3",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Version của khách sạn là bắt buộc.")
        @PositiveOrZero(message = "Version của khách sạn không được âm.")
        Long version) {

  public UpdateHotelRequest {
    latitude = latitude == null ? null : latitude.stripTrailingZeros();
    longitude = longitude == null ? null : longitude.stripTrailingZeros();
  }
}
