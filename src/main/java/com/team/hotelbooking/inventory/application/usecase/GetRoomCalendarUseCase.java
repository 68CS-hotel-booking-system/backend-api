package com.team.hotelbooking.inventory.application.usecase;

import com.team.hotelbooking.inventory.application.command.GetRoomCalendarQuery;
import com.team.hotelbooking.inventory.application.result.RoomCalendarResult;

public interface GetRoomCalendarUseCase {

  RoomCalendarResult execute(GetRoomCalendarQuery query);
}
