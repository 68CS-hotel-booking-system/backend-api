package com.team.hotelbooking.pricing.voucher.infrastructure.integration;

import com.team.hotelbooking.hotel.HotelDirectory;
import com.team.hotelbooking.identity.AccountDirectory;
import com.team.hotelbooking.pricing.voucher.application.port.VoucherAccessPort;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VoucherAccessAdapter implements VoucherAccessPort {
  private final HotelDirectory hotels;
  private final AccountDirectory accounts;

  public boolean isCurrentOwner(UUID actor, UUID hotel) {
    return accounts
            .findById(actor)
            .filter(
                a ->
                    a.active()
                        && a.emailVerified()
                        && a.roles().contains("HOTEL_OWNER")
                        && !a.roles().contains("ADMIN"))
            .isPresent()
        && hotels.findById(hotel).filter(h -> actor.equals(h.ownerId())).isPresent();
  }
}
