package com.team.hotelbooking.hotel.application.usecase;

import com.team.hotelbooking.hotel.application.command.ListHotelReviewsQuery;
import com.team.hotelbooking.hotel.application.result.HotelReviewSummaryResult;
import com.team.hotelbooking.shared.pagination.PageResult;

public interface ListHotelReviewsUseCase {
  PageResult<HotelReviewSummaryResult> execute(ListHotelReviewsQuery query);
}
