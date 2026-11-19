package com.team.hotelbooking.hotel.application.command;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

// actorUserId và admin lấy từ principal đã xác thực. Field null nghĩa là giữ nguyên;
// version là giá trị client đã đọc.
public record UpdateHotelCommand(
    UUID actorUserId,
    boolean admin,
    UUID hotelId,
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
    Long version) {}
