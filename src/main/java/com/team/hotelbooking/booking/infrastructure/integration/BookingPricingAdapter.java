package com.team.hotelbooking.booking.infrastructure.integration;

import com.team.hotelbooking.booking.application.port.BookingPricingPort;
import com.team.hotelbooking.booking.domain.exception.BookingErrorCode;
import com.team.hotelbooking.booking.domain.exception.BookingException;
import com.team.hotelbooking.pricing.StayQuoteDirectory;
import com.team.hotelbooking.pricing.StayQuoteDirectory.StayQuoteCalculationException;
import com.team.hotelbooking.pricing.StayQuoteDirectory.StayQuoteItemRequest;
import com.team.hotelbooking.pricing.StayQuoteDirectory.StayQuoteRequest;
import com.team.hotelbooking.pricing.StayQuoteDirectory.StayQuoteSnapshot;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class BookingPricingAdapter implements BookingPricingPort {

  private final StayQuoteDirectory stayQuoteDirectory;

  public BookingPricingAdapter(StayQuoteDirectory stayQuoteDirectory) {
    this.stayQuoteDirectory = stayQuoteDirectory;
  }

  @Override
  public CalculatedStayQuote calculateQuote(StayQuoteCriteria criteria) {
    List<StayQuoteItemRequest> itemRequests =
        criteria.items() == null
            ? List.of()
            : criteria.items().stream()
                .map(
                    i ->
                        new StayQuoteItemRequest(
                            i.roomTypeId(), i.quantity(), i.adults(), i.children()))
                .toList();

    StayQuoteSnapshot snapshot;
    try {
      snapshot =
          stayQuoteDirectory.calculateQuote(
              new StayQuoteRequest(
                  criteria.hotelId(),
                  criteria.checkIn(),
                  criteria.checkOut(),
                  criteria.expectedCheckInTime(),
                  criteria.expectedCheckOutTime(),
                  itemRequests));
    } catch (StayQuoteCalculationException ex) {
      BookingErrorCode errorCode =
          switch (ex.reason()) {
            case INVALID_DATE_RANGE -> BookingErrorCode.INVALID_DATE_RANGE;
            case MAX_STAY_EXCEEDED -> BookingErrorCode.MAX_STAY_EXCEEDED;
            case INVALID_QUOTE_REQUEST -> BookingErrorCode.VALIDATION_FAILED;
            case HOTEL_NOT_FOUND -> BookingErrorCode.HOTEL_NOT_FOUND;
            case ROOM_TYPE_NOT_FOUND -> BookingErrorCode.ROOM_TYPE_NOT_FOUND;
            case INVALID_OCCUPANCY -> BookingErrorCode.INVALID_OCCUPANCY;
            case RATE_NOT_AVAILABLE -> BookingErrorCode.RATE_NOT_AVAILABLE;
          };
      throw new BookingException(errorCode, ex.getMessage());
    }

    return new CalculatedStayQuote(
        snapshot.hotelId(),
        snapshot.checkIn(),
        snapshot.checkOut(),
        snapshot.days(),
        snapshot.nights(),
        snapshot.standardCheckInTime(),
        snapshot.standardCheckOutTime(),
        snapshot.expectedCheckInTime(),
        snapshot.expectedCheckOutTime(),
        snapshot.currency(),
        snapshot.roomSubtotalAmount(),
        snapshot.earlyCheckInSurchargeAmount(),
        snapshot.lateCheckOutSurchargeAmount(),
        snapshot.extraGuestSurchargeAmount(),
        snapshot.surchargeAmount(),
        snapshot.subtotalAmount(),
        snapshot.discountAmount(),
        snapshot.totalAmount(),
        snapshot.items().stream()
            .map(
                item ->
                    new CalculatedItemQuote(
                        item.roomTypeId(),
                        item.roomTypeName(),
                        item.quantity(),
                        item.adults(),
                        item.children(),
                        item.extraGuests(),
                        item.roomSubtotalAmount(),
                        item.earlyCheckInSurchargeAmount(),
                        item.lateCheckOutSurchargeAmount(),
                        item.extraGuestSurchargeAmount(),
                        item.surchargeAmount(),
                        item.subtotalAmount(),
                        item.nightlyPrices().stream()
                            .map(np -> new CalculatedNightlyPrice(np.stayDate(), np.unitPrice()))
                            .toList()))
            .toList());
  }
}
