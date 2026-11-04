package com.team.hotelbooking.hotel.application.usecase;

import com.team.hotelbooking.hotel.application.result.ListPublicRoomTypesResult;
import java.util.List;
import java.util.UUID;

public interface ListPublicRoomTypesUseCase {

  List<ListPublicRoomTypesResult> execute(UUID hotelId);
}
