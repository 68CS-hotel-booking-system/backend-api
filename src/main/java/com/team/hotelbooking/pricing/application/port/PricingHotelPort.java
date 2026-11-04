package com.team.hotelbooking.pricing.application.port;

import java.util.UUID;

public interface PricingHotelPort {

  boolean isHotelActive(UUID hotelId);

  boolean isRoomTypeActiveInHotel(UUID roomTypeId, UUID hotelId);
}
