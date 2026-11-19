package com.team.hotelbooking.inventory.application.usecase;

import com.team.hotelbooking.inventory.application.command.SearchHotelsQuery;
import com.team.hotelbooking.inventory.application.result.HotelSearchItemResult;
import com.team.hotelbooking.shared.pagination.PageResult;

public interface SearchHotelsUseCase {

  PageResult<HotelSearchItemResult> execute(SearchHotelsQuery query);
}
