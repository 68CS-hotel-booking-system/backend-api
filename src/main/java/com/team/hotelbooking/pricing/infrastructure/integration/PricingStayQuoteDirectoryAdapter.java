package com.team.hotelbooking.pricing.infrastructure.integration;

import com.team.hotelbooking.pricing.StayQuoteDirectory;
import com.team.hotelbooking.pricing.application.command.CalculateStayQuoteQuery;
import com.team.hotelbooking.pricing.application.command.CalculateStayQuoteQuery.QuoteItemInput;
import com.team.hotelbooking.pricing.application.result.StayQuoteResult;
import com.team.hotelbooking.pricing.application.usecase.CalculateStayQuoteUseCase;
import com.team.hotelbooking.pricing.domain.exception.PricingException;
import java.util.List;

public class PricingStayQuoteDirectoryAdapter implements StayQuoteDirectory {

  private final CalculateStayQuoteUseCase calculateStayQuoteUseCase;

  public PricingStayQuoteDirectoryAdapter(CalculateStayQuoteUseCase calculateStayQuoteUseCase) {
    this.calculateStayQuoteUseCase = calculateStayQuoteUseCase;
  }

  @Override
  public StayQuoteSnapshot calculateQuote(StayQuoteRequest request) {
    List<QuoteItemInput> items =
        request.items() == null
            ? List.of()
            : request.items().stream()
                .map(
                    i -> new QuoteItemInput(i.roomTypeId(), i.quantity(), i.adults(), i.children()))
                .toList();

    StayQuoteResult result;
    try {
      result =
          calculateStayQuoteUseCase.execute(
              new CalculateStayQuoteQuery(
                  request.hotelId(),
                  request.checkIn(),
                  request.checkOut(),
                  request.expectedCheckInTime(),
                  request.expectedCheckOutTime(),
                  items));
    } catch (PricingException ex) {
      FailureReason reason =
          switch (ex.code()) {
            case INVALID_DATE_RANGE -> FailureReason.INVALID_DATE_RANGE;
            case MAX_STAY_EXCEEDED, MAX_RANGE_EXCEEDED -> FailureReason.MAX_STAY_EXCEEDED;
            case INVALID_QUOTE_REQUEST, INVALID_AMOUNT -> FailureReason.INVALID_QUOTE_REQUEST;
            case HOTEL_NOT_FOUND -> FailureReason.HOTEL_NOT_FOUND;
            case ROOM_TYPE_NOT_FOUND -> FailureReason.ROOM_TYPE_NOT_FOUND;
            case INVALID_OCCUPANCY -> FailureReason.INVALID_OCCUPANCY;
            case RATE_NOT_AVAILABLE -> FailureReason.RATE_NOT_AVAILABLE;
          };
      throw new StayQuoteCalculationException(reason, ex.getMessage());
    }

    return new StayQuoteSnapshot(
        result.hotelId(),
        result.checkIn(),
        result.checkOut(),
        result.days(),
        result.nights(),
        result.standardCheckInTime(),
        result.standardCheckOutTime(),
        result.expectedCheckInTime(),
        result.expectedCheckOutTime(),
        result.currency(),
        result.roomSubtotalAmount(),
        result.earlyCheckInSurchargeAmount(),
        result.lateCheckOutSurchargeAmount(),
        result.extraGuestSurchargeAmount(),
        result.surchargeAmount(),
        result.subtotalAmount(),
        result.discountAmount(),
        result.totalAmount(),
        result.items().stream()
            .map(
                item ->
                    new StayQuoteItemSnapshot(
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
                            .map(np -> new NightlyPriceSnapshot(np.stayDate(), np.unitPrice()))
                            .toList()))
            .toList());
  }
}
