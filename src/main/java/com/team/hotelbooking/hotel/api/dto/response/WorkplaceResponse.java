package com.team.hotelbooking.hotel.api.dto.response;

import com.team.hotelbooking.hotel.application.port.HotelMemberStore.WorkplaceRecord;
import com.team.hotelbooking.hotel.domain.model.HotelMemberRole;
import java.util.UUID;

public record WorkplaceResponse(UUID hotelId, String hotelName, HotelMemberRole memberRole) {

  public static WorkplaceResponse from(WorkplaceRecord record) {
    return new WorkplaceResponse(record.hotelId(), record.hotelName(), record.memberRole());
  }
}
