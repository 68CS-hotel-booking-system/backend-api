package com.team.hotelbooking.inventory.api.dto.response;

import com.team.hotelbooking.inventory.application.result.RoomTypeAvailabilityResult;
import java.util.UUID;

public record RoomTypeAvailabilityDto(
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

  public static RoomTypeAvailabilityDto from(RoomTypeAvailabilityResult result) {
    return new RoomTypeAvailabilityDto(
        result.roomTypeId(),
        result.roomTypeName(),
        result.maxOccupancy(),
        result.maxAdults(),
        result.maxChildren(),
        result.availableQuantity(),
        result.isAvailable(),
        result.pricePerNight(),
        result.totalEstimatedPrice(),
        result.currency());
  }
}
