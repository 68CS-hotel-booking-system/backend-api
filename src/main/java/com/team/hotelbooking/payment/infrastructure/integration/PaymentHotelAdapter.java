package com.team.hotelbooking.payment.infrastructure.integration;

import com.team.hotelbooking.hotel.HotelDirectory; // Import duy nhất từ hotel
import com.team.hotelbooking.payment.application.port.PaymentHotelPort;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentHotelAdapter implements PaymentHotelPort {

  private final HotelDirectory hotelDirectory;

  @Override
  public boolean existsById(UUID hotelId) {
    return hotelDirectory.existsById(hotelId);
  }

  @Override
  public Optional<UUID> findOwnerUserId(UUID hotelId) {
    return hotelDirectory.findOwnerUserId(hotelId);
  }
}
