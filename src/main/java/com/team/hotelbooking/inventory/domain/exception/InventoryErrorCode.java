package com.team.hotelbooking.inventory.domain.exception;

import com.team.hotelbooking.shared.exception.ErrorCode;

public enum InventoryErrorCode implements ErrorCode {
  INVALID_DATE_RANGE("Khoảng thời gian lưu trú không hợp lệ."),
  MAX_STAY_EXCEEDED("Thời gian lưu trú tối đa không vượt quá 30 đêm."),
  HOTEL_NOT_FOUND("Không tìm thấy khách sạn hoặc khách sạn chưa mở bán.");

  private final String defaultMessage;

  InventoryErrorCode(String defaultMessage) {
    this.defaultMessage = defaultMessage;
  }

  @Override
  public String code() {
    return name();
  }

  @Override
  public String defaultMessage() {
    return defaultMessage;
  }
}
