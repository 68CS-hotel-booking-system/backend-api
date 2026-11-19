package com.team.hotelbooking.hotel.application.usecase.impl;

import com.team.hotelbooking.hotel.application.result.ManagementHotelResult;
import com.team.hotelbooking.hotel.application.usecase.ApproveHotelUseCase;
import java.util.UUID;

/** Compatibility adapter; approval always requires a submitted PENDING review. */
public final class ApproveHotelService implements ApproveHotelUseCase {
  private final HotelReviewService reviews;

  public ApproveHotelService(HotelReviewService reviews) {
    this.reviews = reviews;
  }

  @Override
  public ManagementHotelResult execute(UUID hotelId, UUID adminActorId) {
    return reviews.approvePendingForHotel(hotelId, adminActorId);
  }
}
