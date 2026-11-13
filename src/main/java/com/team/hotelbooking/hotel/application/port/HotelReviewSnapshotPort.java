package com.team.hotelbooking.hotel.application.port;

import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.hotel.domain.model.HotelReviewSnapshot;

public interface HotelReviewSnapshotPort {
  // Gọi sau khi khóa ghi hotel; mọi thao tác sửa hồ sơ đều lấy cùng khóa này trước.
  HotelReviewSnapshot capture(Hotel hotel);
}
