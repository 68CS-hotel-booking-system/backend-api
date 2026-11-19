package com.team.hotelbooking.hotel.domain.model;

// Điều kiện mở bán theo HT-03; field là khóa ổn định để client hiển thị từng lý do còn thiếu.
public enum HotelPublishRequirement {
  NAME("name", "Khách sạn chưa có tên."),
  ADDRESS("address", "Khách sạn chưa có địa chỉ."),
  CONTACT("contact", "Khách sạn chưa có số điện thoại hoặc email liên hệ."),
  ROOM_TYPE(
      "roomTypes",
      "Cần ít nhất một loại phòng đang bán có sức chứa hợp lệ (người lớn từ 1 và không vượt"
          + " sức chứa tối đa)."),
  OPERATIONAL_ROOM("rooms", "Loại phòng hợp lệ phải có ít nhất một phòng vật lý đang phục vụ.");

  private final String field;
  private final String message;

  HotelPublishRequirement(String field, String message) {
    this.field = field;
    this.message = message;
  }

  public String field() {
    return field;
  }

  public String message() {
    return message;
  }
}
