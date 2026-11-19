package com.team.hotelbooking.hotel.application.usecase;

import com.team.hotelbooking.hotel.application.command.ListManagementHotelsQuery;
import com.team.hotelbooking.hotel.application.result.ManagementHotelResult;
import com.team.hotelbooking.shared.pagination.PageResult;

public interface ListManagementHotelsUseCase {

  PageResult<ManagementHotelResult> execute(ListManagementHotelsQuery query);
}
