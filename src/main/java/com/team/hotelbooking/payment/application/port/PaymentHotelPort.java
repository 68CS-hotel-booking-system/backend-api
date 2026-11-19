package com.team.hotelbooking.payment.application.port;

import java.util.Optional;
import java.util.UUID;

public interface PaymentHotelPort {

  boolean existsById(UUID hotelId);

  Optional<UUID> findOwnerUserId(UUID hotelId);
}
