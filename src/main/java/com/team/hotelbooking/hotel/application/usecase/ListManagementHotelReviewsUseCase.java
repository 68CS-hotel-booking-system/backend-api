package com.team.hotelbooking.hotel.application.usecase;

import com.team.hotelbooking.hotel.application.command.ListManagementHotelReviewsQuery;
import com.team.hotelbooking.hotel.application.result.HotelReviewSummaryResult;
import com.team.hotelbooking.shared.pagination.PageResult;

public interface ListManagementHotelReviewsUseCase {
  PageResult<HotelReviewSummaryResult> execute(ListManagementHotelReviewsQuery query);
}
