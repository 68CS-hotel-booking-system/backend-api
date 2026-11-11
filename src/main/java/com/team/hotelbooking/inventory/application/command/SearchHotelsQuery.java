package com.team.hotelbooking.inventory.application.command;

import com.team.hotelbooking.shared.pagination.PageQuery;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * Tìm kiếm khách sạn công khai. checkIn/checkOut cùng có hoặc cùng không: không có ngày thì chỉ lọc
 * tĩnh (thành phố, số sao, tiện ích, sức chứa, giá cơ bản).
 */
public record SearchHotelsQuery(
    UUID cityId,
    String citySlug,
    LocalDate checkIn,
    LocalDate checkOut,
    Integer guests,
    Long minPrice,
    Long maxPrice,
    Integer minStarRating,
    Set<UUID> amenityIds,
    Sort sort,
    PageQuery pagination) {

  public SearchHotelsQuery {
    amenityIds = amenityIds == null ? Set.of() : Set.copyOf(amenityIds);
  }

  public enum Sort {
    PRICE,
    RATING,
    NAME
  }
}
