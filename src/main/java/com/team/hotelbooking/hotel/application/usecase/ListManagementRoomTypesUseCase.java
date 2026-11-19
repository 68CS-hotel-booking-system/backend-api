package com.team.hotelbooking.hotel.application.usecase;

import com.team.hotelbooking.hotel.application.command.ListManagementRoomTypesQuery;
import com.team.hotelbooking.hotel.application.result.ListManagementRoomTypesResult;
import java.util.List;

public interface ListManagementRoomTypesUseCase {

  List<ListManagementRoomTypesResult> execute(ListManagementRoomTypesQuery query);
}
