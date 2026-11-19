package com.team.hotelbooking.hotel.application.usecase.impl;

import com.team.hotelbooking.hotel.application.command.ListPublicHotelsQuery;
import com.team.hotelbooking.hotel.application.result.ListPublicHotelsResult;
import com.team.hotelbooking.hotel.application.usecase.ListPublicHotelsUseCase;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.hotel.domain.repository.HotelRepository;
import com.team.hotelbooking.shared.pagination.PageResult;

public class ListPublicHotelsService implements ListPublicHotelsUseCase {

  private final HotelRepository hotels;
  private static final int MAX_KEYWORD_LENGTH = 100;

  public ListPublicHotelsService(HotelRepository hotels) {
    this.hotels = hotels;
  }

  @Override
  public PageResult<ListPublicHotelsResult> execute(ListPublicHotelsQuery query) {
    if (query == null || query.pagination() == null) {
      throw HotelException.invalid("Phân trang là bắt buộc.");
    }
    if (query.keyword() != null && query.keyword().length() > MAX_KEYWORD_LENGTH) {
      throw HotelException.invalid("Từ khóa tìm kiếm quá dài.");
    }

    PageResult<Hotel> result =
        hotels.findPublicHotels(query.cityId(), query.keyword(), query.pagination());

    return result.map(ListPublicHotelsResult::from);
  }
}
