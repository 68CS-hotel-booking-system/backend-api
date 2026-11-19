package com.team.hotelbooking.hotel.application.usecase;

import com.team.hotelbooking.hotel.application.command.ListManagementRoomsQuery;
import com.team.hotelbooking.hotel.application.result.ListManagementRoomsResult;
import com.team.hotelbooking.shared.pagination.PageResult;

public interface ListManagementRoomsUseCase {

  PageResult<ListManagementRoomsResult> execute(ListManagementRoomsQuery query);
}
