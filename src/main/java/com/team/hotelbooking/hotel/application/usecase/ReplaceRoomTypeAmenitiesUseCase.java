package com.team.hotelbooking.hotel.application.usecase;

import com.team.hotelbooking.hotel.application.command.ReplaceRoomTypeAmenitiesCommand;
import com.team.hotelbooking.hotel.application.result.ListAmenitiesResult;
import java.util.List;

public interface ReplaceRoomTypeAmenitiesUseCase {

  List<ListAmenitiesResult> execute(ReplaceRoomTypeAmenitiesCommand command);
}
