package com.team.hotelbooking.hotel.application.usecase;

import com.team.hotelbooking.hotel.application.command.ListPublicHotelsQuery;
import com.team.hotelbooking.hotel.application.result.ListPublicHotelsResult;
import com.team.hotelbooking.shared.pagination.PageResult;

public interface ListPublicHotelsUseCase {

  PageResult<ListPublicHotelsResult> execute(ListPublicHotelsQuery query);
}
