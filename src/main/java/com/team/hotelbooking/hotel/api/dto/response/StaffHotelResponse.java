package com.team.hotelbooking.hotel.api.dto.response;

import com.team.hotelbooking.hotel.application.port.HotelMemberStore.StaffHotelRecord;
import com.team.hotelbooking.shared.pagination.PageResult;
import com.team.hotelbooking.shared.web.PageResponse;
import java.util.UUID;

public record StaffHotelResponse(UUID id, String name, boolean canAssignManager) {

  public static StaffHotelResponse from(StaffHotelRecord record) {
    return new StaffHotelResponse(record.id(), record.name(), record.canAssignManager());
  }

  public static PageResponse<StaffHotelResponse> from(PageResult<StaffHotelRecord> result) {
    return PageResponse.from(result.map(StaffHotelResponse::from));
  }
}
