package com.team.hotelbooking.booking.application.usecase.impl;

import com.team.hotelbooking.booking.application.command.CancelHotelBookingCommand;
import com.team.hotelbooking.booking.application.port.BookingHotelPort;
import com.team.hotelbooking.booking.application.port.BookingIdentityPort;
import com.team.hotelbooking.booking.application.result.CancelBookingResult;
import com.team.hotelbooking.booking.application.usecase.CancelHotelBookingUseCase;

public final class CancelHotelBookingService implements CancelHotelBookingUseCase {

  private final CancelBookingService cancellation;
  private final BookingIdentityPort accounts;
  private final BookingHotelPort hotels;

  public CancelHotelBookingService(
      CancelBookingService cancellation, BookingIdentityPort accounts, BookingHotelPort hotels) {
    this.cancellation = cancellation;
    this.accounts = accounts;
    this.hotels = hotels;
  }

  @Override
  public CancelBookingResult execute(CancelHotelBookingCommand command) {
    ListManagementBookingsService.verifyHotelManagementAccess(
        command.actorId(), command.hotelId(), accounts, hotels);
    return cancellation.cancelByHotel(
        command.hotelId(), command.bookingId(), command.actorId(), command.reason());
  }
}
