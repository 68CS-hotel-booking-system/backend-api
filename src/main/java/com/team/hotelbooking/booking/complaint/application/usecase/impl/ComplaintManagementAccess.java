package com.team.hotelbooking.booking.complaint.application.usecase.impl;

import com.team.hotelbooking.booking.application.port.BookingIdentityPort;
import com.team.hotelbooking.booking.complaint.application.port.ComplaintHotelPort;
import com.team.hotelbooking.booking.complaint.domain.exception.*;
import java.util.*;

final class ComplaintManagementAccess {
  private final BookingIdentityPort accounts;
  private final ComplaintHotelPort hotels;

  ComplaintManagementAccess(BookingIdentityPort accounts, ComplaintHotelPort hotels) {
    this.accounts = accounts;
    this.hotels = hotels;
  }

  Scope require(UUID actorId) {
    var account =
        accounts
            .findById(actorId)
            .filter(a -> a.active() && a.emailVerified())
            .orElseThrow(() -> new ComplaintException(ComplaintErrorCode.ACCESS_DENIED));
    boolean admin = account.roles().contains("ADMIN");
    if (!admin && !account.roles().contains("HOTEL_OWNER"))
      throw new ComplaintException(ComplaintErrorCode.ACCESS_DENIED);
    return new Scope(
        admin ? "ADMIN" : "HOTEL_OWNER", Map.copyOf(hotels.hotels(admin ? null : actorId)));
  }

  record Scope(String role, Map<UUID, String> hotels) {
    void requireHotel(UUID hotelId) {
      if (!hotels.containsKey(hotelId))
        throw new ComplaintException(ComplaintErrorCode.COMPLAINT_NOT_FOUND);
    }
  }
}
