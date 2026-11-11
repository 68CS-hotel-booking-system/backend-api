package com.team.hotelbooking.hotel.domain.repository;

import com.team.hotelbooking.hotel.domain.model.HotelReviewRequest;
import com.team.hotelbooking.hotel.domain.model.HotelReviewSummary;
import com.team.hotelbooking.shared.pagination.PageQuery;
import com.team.hotelbooking.shared.pagination.PageResult;
import java.util.Optional;
import java.util.UUID;

public interface HotelReviewRepository {
  HotelReviewRequest add(HotelReviewRequest request);

  Optional<HotelReviewRequest> findById(UUID id);

  Optional<UUID> findHotelId(UUID id);

  Optional<HotelReviewRequest> findPendingByHotelId(UUID hotelId);

  HotelReviewRequest updateDecision(HotelReviewRequest request);

  PageResult<HotelReviewSummary> findAll(
      HotelReviewRequest.Status status, UUID hotelId, PageQuery pagination);
}
