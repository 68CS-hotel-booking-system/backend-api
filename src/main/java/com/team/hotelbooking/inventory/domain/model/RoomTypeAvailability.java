package com.team.hotelbooking.inventory.domain.model;

import java.util.UUID;

public record RoomTypeAvailability(
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
