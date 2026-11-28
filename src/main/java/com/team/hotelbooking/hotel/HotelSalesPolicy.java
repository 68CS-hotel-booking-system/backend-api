package com.team.hotelbooking.hotel;

import java.util.UUID;

public interface HotelSalesPolicy {
  boolean hasEffectiveContract(UUID hotelId);

  java.util.Set<UUID> effectiveHotelIds();
}
