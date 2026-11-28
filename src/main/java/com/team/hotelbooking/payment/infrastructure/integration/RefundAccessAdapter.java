package com.team.hotelbooking.payment.infrastructure.integration;

import com.team.hotelbooking.hotel.HotelDirectory;
import com.team.hotelbooking.identity.AccountDirectory;
import com.team.hotelbooking.payment.application.port.RefundAccessPort;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefundAccessAdapter implements RefundAccessPort {
  private final AccountDirectory accounts;
  private final HotelDirectory hotels;

  public Optional<Viewer> viewer(UUID id) {
    return accounts
        .findById(id)
        .map(
            a ->
                new Viewer(
                    a.active(),
                    a.emailVerified(),
                    a.roles().contains("ADMIN"),
                    a.roles().contains("HOTEL_OWNER")));
  }

  public Map<UUID, String> hotels(UUID ownerId) {
    return hotels.findFinanceHotels(ownerId).stream()
        .collect(
            Collectors.toMap(HotelDirectory.FinanceHotel::id, HotelDirectory.FinanceHotel::name));
  }
}
