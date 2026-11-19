package com.team.hotelbooking.inventory.application.result;

import com.team.hotelbooking.inventory.domain.model.RoomTypeAvailability;
import java.util.UUID;

public record RoomTypeAvailabilityResult(
    UUID roomTypeId,
    String roomTypeName,
    int maxOccupancy,
    int maxAdults,
    int maxChildren,
    int availableQuantity,
    boolean isAvailable,
    Long pricePerNight,
    Long totalEstimatedPrice,
    String currency) {

  public static RoomTypeAvailabilityResult from(RoomTypeAvailability domain) {
    return new RoomTypeAvailabilityResult(
        domain.roomTypeId(),
        domain.roomTypeName(),
        domain.maxOccupancy(),
        domain.maxAdults(),
        domain.maxChildren(),
        domain.availableQuantity(),
        domain.isAvailable(),
        domain.pricePerNight(),
        domain.totalEstimatedPrice(),
        domain.currency());
  }
}
