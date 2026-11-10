package com.team.hotelbooking.hotel.domain.model;

import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;
import java.util.regex.Pattern;

public record Hotel(
    UUID id,
    UUID ownerId,
    UUID cityId,
    String name,
    String slug,
    String description,
    String address,
    String contactPhone,
    String contactEmail,
    BigDecimal latitude,
    BigDecimal longitude,
    Short starRating,
    LocalTime checkInTime,
    LocalTime checkOutTime,
    Status status,
    Instant createdAt,
    Instant updatedAt,
    long version) {

  private static final Pattern PHONE = Pattern.compile("\\+?[0-9]{8,15}");
  private static final Pattern EMAIL = Pattern.compile("[^\\s@]+@[^\\s@]+\\.[^\\s@]+");

  /**
   * Vòng đời bán (HT-05, HT-09): chỉ ACTIVE là đang bán.
   *
   * <ul>
   *   <li>DRAFT → ACTIVE: chỉ quản trị hệ thống duyệt (approve) công khai lần đầu.
   *   <li>ACTIVE ↔ SUSPENDED: Owner/HOTEL_MANAGER tạm ngừng và mở lại.
   *   <li>Mọi trạng thái → LOCKED: quản trị hệ thống khóa; chỉ quản trị hệ thống gỡ khóa.
   * </ul>
   */
  public enum Status {
    DRAFT,
    ACTIVE,
    SUSPENDED,
    LOCKED
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
        null,
        null,
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

  // HT-09: chỉ quản trị hệ thống duyệt công khai lần đầu; điều kiện HT-03 kiểm ở application.
  public Hotel activate(Instant now) {
    if (now == null) {
      throw invalid("Thời điểm kích hoạt không được để trống.");
    }

    if (this.status != Status.DRAFT) {
      throw invalid("Chỉ khách sạn ở trạng thái DRAFT mới có thể kích hoạt.");
    }

    return withStatus(Status.ACTIVE, now);
  }

  // Cập nhật một phần: null nghĩa là giữ nguyên. Không đổi slug, Owner, trạng thái hay version;
  // version do persistence kiểm tra với giá trị client gửi lên.
  public Hotel updateDetails(
      String name,
      String description,
      String address,
      String contactPhone,
      String contactEmail,
      BigDecimal latitude,
      BigDecimal longitude,
      Short starRating,
      LocalTime checkInTime,
      LocalTime checkOutTime,
      Instant now) {

    if (now == null) {
      throw invalid("Thời điểm cập nhật là bắt buộc.");
    }
    if (name == null
        && description == null
        && address == null
        && contactPhone == null
        && contactEmail == null
        && latitude == null
        && longitude == null
        && starRating == null
        && checkInTime == null
        && checkOutTime == null) {
      throw new HotelException(
          HotelErrorCode.INVALID_REQUEST, "Phải có ít nhất một thông tin khách sạn cần cập nhật.");
    }

    // Chưa hỗ trợ xóa giá trị: chuỗi rỗng/khoảng trắng là dữ liệu sai, không phải lệnh xóa.
    String nextName = name == null ? this.name : normalizeRequiredText(name, "Tên khách sạn", 255);
    String nextAddress =
        address == null ? this.address : normalizeRequiredText(address, "Địa chỉ", 255);
    if (description != null && description.isBlank()) {
      throw invalid("Mô tả khách sạn không được để trống; chưa hỗ trợ xóa mô tả.");
    }
    String nextPhone = contactPhone == null ? this.contactPhone : normalizePhone(contactPhone);
    String nextEmail = contactEmail == null ? this.contactEmail : normalizeEmail(contactEmail);
    // Tọa độ là một cặp: chỉ đổi vĩ độ hoặc chỉ đổi kinh độ dễ tạo vị trí sai.
    if ((latitude == null) != (longitude == null)) {
      throw invalid("Vĩ độ và kinh độ phải được gửi cùng nhau.");
    }
    validateCoordinate(latitude, "Vĩ độ", BigDecimal.valueOf(-90), BigDecimal.valueOf(90));
    validateCoordinate(longitude, "Kinh độ", BigDecimal.valueOf(-180), BigDecimal.valueOf(180));
    if (starRating != null && (starRating < 1 || starRating > 5)) {
      throw invalid("Số sao khách sạn phải từ 1 đến 5.");
    }

    return new Hotel(
        id,
        ownerId,
        cityId,
        nextName,
        slug,
        description == null ? this.description : description,
        nextAddress,
        nextPhone,
        nextEmail,
        latitude == null ? this.latitude : latitude,
        longitude == null ? this.longitude : longitude,
        starRating == null ? this.starRating : starRating,
        checkInTime == null ? this.checkInTime : checkInTime,
        checkOutTime == null ? this.checkOutTime : checkOutTime,
        status,
        createdAt,
        now,
        version);
  }

  public Hotel suspend(Instant now) {
    requireNotLocked();
    if (status != Status.ACTIVE) {
      throw transition("Chỉ khách sạn đang bán (ACTIVE) mới tạm ngừng bán được.");
    }
    return withStatus(Status.SUSPENDED, now);
  }

  // Giả định chưa chốt: quản lý tự mở lại được sau khi khách sạn bị tạm ngừng (SUSPENDED).
  public Hotel resume(Instant now) {
    requireNotLocked();
    if (status == Status.DRAFT) {
      throw transition(
          "Khách sạn chưa được công khai lần đầu; chỉ quản trị hệ thống được công khai.");
    }
    if (status != Status.SUSPENDED) {
      throw transition("Chỉ khách sạn đang tạm ngừng (SUSPENDED) mới mở bán lại được.");
    }
    return withStatus(Status.ACTIVE, now);
  }

  public Hotel lock(Instant now) {
    if (status == Status.LOCKED) {
      throw transition("Khách sạn đã bị khóa bán.");
    }
    return withStatus(Status.LOCKED, now);
  }

  // Gỡ khóa không mở bán ngay: khách sạn từng công khai về SUSPENDED để quản lý tự mở lại; khách
  // sạn bị khóa khi còn DRAFT về DRAFT vì chưa từng được công khai.
  public Hotel unlock(Status statusBeforeLock, Instant now) {
    if (status != Status.LOCKED) {
      throw transition("Khách sạn không ở trạng thái bị khóa bán.");
    }
    return withStatus(statusBeforeLock == Status.DRAFT ? Status.DRAFT : Status.SUSPENDED, now);
  }

  public boolean onSale() {
    return status == Status.ACTIVE;
  }

  private void requireNotLocked() {
    if (status == Status.LOCKED) {
      throw new HotelException(HotelErrorCode.HOTEL_LOCKED);
    }
  }

  private Hotel withStatus(Status nextStatus, Instant now) {
    if (now == null) {
      throw invalid("Thời điểm đổi trạng thái là bắt buộc.");
    }
    return new Hotel(
        id,
        ownerId,
        cityId,
        name,
        slug,
        description,
        address,
        contactPhone,
        contactEmail,
        latitude,
        longitude,
        starRating,
        checkInTime,
        checkOutTime,
        nextStatus,
        createdAt,
        now,
        version);
  }

  private static String normalizePhone(String value) {
    String normalized = value.trim();
    if (!PHONE.matcher(normalized).matches()) {
      throw invalid("Số điện thoại liên hệ phải gồm 8 đến 15 chữ số, có thể bắt đầu bằng dấu +.");
    }
    return normalized;
  }

  private static String normalizeEmail(String value) {
    String normalized = value.trim();
    if (normalized.length() > 255 || !EMAIL.matcher(normalized).matches()) {
      throw invalid("Email liên hệ không hợp lệ.");
    }
    return normalized;
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

  private static HotelException transition(String message) {
    return new HotelException(HotelErrorCode.INVALID_STATUS_TRANSITION, message);
  }

  private static HotelException invalid(String message) {
    return new HotelException(HotelErrorCode.VALIDATION_FAILED, message);
  }
}
