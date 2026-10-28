package com.team.hotelbooking.inventory.api.dto.response;

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
    String currency) {}
