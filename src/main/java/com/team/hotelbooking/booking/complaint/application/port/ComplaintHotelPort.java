package com.team.hotelbooking.booking.complaint.application.port;

import java.util.*;

public interface ComplaintHotelPort {
  // ownerId null dành cho admin; trả danh sách cơ sở hiện thuộc phạm vi quản lý.
  Map<UUID, String> hotels(UUID ownerId);
}
