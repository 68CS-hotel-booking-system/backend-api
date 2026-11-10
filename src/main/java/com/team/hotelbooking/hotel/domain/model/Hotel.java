package com.team.hotelbooking.hotel.domain.model;

import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

public record Hotel(
    UUID id,
    UUID ownerId,
    UUID cityId,
    String name,
    String slug,
    String description,
    String address,
    BigDecimal latitude,
    BigDecimal longitude,
    Short starRating,
    LocalTime checkInTime,
    LocalTime checkOutTime,
    Status status,
    Instant createdAt,
    Instant updatedAt,
    long version) {

  public enum Status {
    DRAFT,
    ACTIVE
  }

  public static Hotel createDraft(
      UUID id,
      UUID ownerId,
      UUID cityId,
      String name,
      String slug,
      String description,
      String address,
      BigDecimal latitude,
      BigDecimal longitude,
      Short starRating,
      LocalTime checkInTime,
      LocalTime checkOutTime,
      Instant now) {

    if (id == null || ownerId == null || cityId == null || now == null) {
      throw invalid("Định danh khách sạn, Owner, thành phố và thời điểm tạo là bắt buộc.");
    }

    String normalizedName = normalizeRequiredText(name, "Tên khách sạn", 255);
    String normalizedAddress = normalizeRequiredText(address, "Địa chỉ", 255);
    String normalizedSlug = normalizeRequiredText(slug, "Slug", 255);

    validateCoordinate(latitude, "Vĩ độ", BigDecimal.valueOf(-90), BigDecimal.valueOf(90));

    validateCoordinate(longitude, "Kinh độ", BigDecimal.valueOf(-180), BigDecimal.valueOf(180));

    if (starRating != null && (starRating < 1 || starRating > 5)) {
      throw invalid("Số sao khách sạn phải từ 1 đến 5.");
    }

    if (checkInTime == null || checkOutTime == null) {
      throw invalid("Giờ nhận phòng và giờ trả phòng không được để trống.");
    }

    return new Hotel(
        id,
        ownerId,
        cityId,
        normalizedName,
        normalizedSlug,
        description,
        normalizedAddress,
        latitude,
        longitude,
        starRating,
        checkInTime,
        checkOutTime,
        Status.DRAFT,
        now,
        now,
        0L);
  }

  public Hotel activate(Instant now) {
    if (now == null) {
      throw invalid("Thời điểm kích hoạt không được để trống.");
    }

    if (this.status != Status.DRAFT) {
      throw invalid("Chỉ khách sạn ở trạng thái DRAFT mới có thể kích hoạt.");
    }

    // Trả về bản sao mới của record với status là ACTIVE và updatedAt là now
    return new Hotel(
        this.id,
        this.ownerId,
        this.cityId,
        this.name,
        this.slug,
        this.description,
        this.address,
        this.latitude,
        this.longitude,
        this.starRating,
        this.checkInTime,
        this.checkOutTime,
        Status.ACTIVE, // <--- Trạng thái mới
        this.createdAt,
        now, // <--- Cập nhật thời điểm updatedAt
        this.version);
  }

  private static String normalizeRequiredText(String value, String fieldName, int maxLength) {

    String normalized = value == null ? "" : value.trim();

    if (normalized.isBlank() || normalized.length() > maxLength) {
      throw invalid(fieldName + " phải có từ 1 đến " + maxLength + " ký tự.");
    }

    return normalized;
  }

  private static void validateCoordinate(
      BigDecimal value, String fieldName, BigDecimal min, BigDecimal max) {

    if (value == null) {
      return;
    }

    if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
      throw invalid(fieldName + " phải nằm trong khoảng " + min + " đến " + max + ".");
    }

    if (value.stripTrailingZeros().scale() > 6) {
      throw invalid(fieldName + " chỉ được có tối đa 6 chữ số thập phân.");
    }
  }

  private static HotelException invalid(String message) {
    return new HotelException(HotelErrorCode.VALIDATION_FAILED, message);
  }
}
