package com.team.hotelbooking.shared.time;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/** Ngày nghiệp vụ cho các khách sạn tại Việt Nam; thời điểm lưu trữ vẫn dùng Instant/UTC. */
public final class BusinessTime {

  public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

  private BusinessTime() {}

  public static LocalDate today(Clock clock) {
    return LocalDate.now(clock.withZone(ZONE));
  }
}
