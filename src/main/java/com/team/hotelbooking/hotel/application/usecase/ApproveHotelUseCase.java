package com.team.hotelbooking.hotel.application.usecase;

import com.team.hotelbooking.hotel.application.result.ManagementHotelResult;
import java.util.UUID;

public interface ApproveHotelUseCase {

  ManagementHotelResult execute(UUID hotelId, UUID adminActorId);
}
