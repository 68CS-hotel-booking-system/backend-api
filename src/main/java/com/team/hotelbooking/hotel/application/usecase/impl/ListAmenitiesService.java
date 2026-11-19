package com.team.hotelbooking.hotel.application.usecase.impl;

import com.team.hotelbooking.hotel.application.result.ListAmenitiesResult;
import com.team.hotelbooking.hotel.application.usecase.ListAmenitiesUseCase;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.domain.model.Amenity;
import com.team.hotelbooking.hotel.domain.repository.AmenityRepository;
import java.util.List;

public class ListAmenitiesService implements ListAmenitiesUseCase {

  private final AmenityRepository amenities;

  public ListAmenitiesService(AmenityRepository amenities) {
    this.amenities = amenities;
  }

  @Override
  public List<ListAmenitiesResult> execute(String category) {
    // Không truyền category thì trả toàn bộ; thứ tự category, name, id do repository bảo đảm.
    List<Amenity> result =
        category == null ? amenities.findAll() : amenities.findByCategory(parseCategory(category));

    return result.stream().map(ListAmenitiesResult::from).toList();
  }

  private static Amenity.Category parseCategory(String value) {
    // So khớp chính xác chữ hoa theo contract; "hotel" hoặc chuỗi rỗng đều không hợp lệ.
    for (Amenity.Category category : Amenity.Category.values()) {
      if (category.name().equals(value)) {
        return category;
      }
    }

    throw HotelException.invalid("Loại tiện ích phải là HOTEL hoặc ROOM.");
  }
}
