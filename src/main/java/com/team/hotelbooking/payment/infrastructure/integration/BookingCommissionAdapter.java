package com.team.hotelbooking.payment.infrastructure.integration;

import com.team.hotelbooking.booking.BookingCommissionCapture;
import com.team.hotelbooking.booking.BookingCommissionDirectory;
import com.team.hotelbooking.hotel.HotelDirectory;
import com.team.hotelbooking.payment.ContractDirectory;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingCommissionAdapter implements BookingCommissionCapture {
  private final ContractDirectory contracts;
  private final HotelDirectory hotels;
  private final BookingCommissionDirectory snapshots;

  @Override
  public void capture(
      UUID bookingId,
      UUID hotelId,
      long roomAmount,
      long discountAmount,
      long totalAmount,
      Instant at) {
    var contract =
        contracts.findEffective(hotelId, at).orElseThrow(ContractUnavailableException::new);
    if (!hotels.findOwnerUserId(hotelId).filter(contract.ownerUserId()::equals).isPresent())
      throw new ContractUnavailableException();
    snapshots.save(
        bookingId,
        contract.contractId(),
        contract.ownerUserId(),
        contract.commissionRate(),
        roomAmount,
        discountAmount,
        totalAmount,
        at);
  }
}
