package com.team.hotelbooking.booking.infrastructure.integration;

import com.team.hotelbooking.booking.complaint.application.port.ComplaintHotelPort;
import com.team.hotelbooking.hotel.HotelDirectory;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ComplaintHotelAdapter implements ComplaintHotelPort {
  private final HotelDirectory hotels;

  public Map<UUID, String> hotels(UUID ownerId) {
    return hotels.findFinanceHotels(ownerId).stream()
        .collect(
            Collectors.toMap(HotelDirectory.FinanceHotel::id, HotelDirectory.FinanceHotel::name));
  }
}
