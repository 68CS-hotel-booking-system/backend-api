package com.team.hotelbooking.hotel.application.usecase;

import com.team.hotelbooking.hotel.application.command.ReplaceHotelAmenitiesCommand;
import com.team.hotelbooking.hotel.application.result.ListAmenitiesResult;
import java.util.List;

public interface ReplaceHotelAmenitiesUseCase {

  List<ListAmenitiesResult> execute(ReplaceHotelAmenitiesCommand command);
}
