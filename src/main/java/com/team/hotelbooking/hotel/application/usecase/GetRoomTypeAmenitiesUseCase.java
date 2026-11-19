package com.team.hotelbooking.hotel.application.usecase;

import com.team.hotelbooking.hotel.application.command.GetRoomTypeAmenitiesQuery;
import com.team.hotelbooking.hotel.application.result.ListAmenitiesResult;
import java.util.List;

public interface GetRoomTypeAmenitiesUseCase {
  List<ListAmenitiesResult> execute(GetRoomTypeAmenitiesQuery query);
}
