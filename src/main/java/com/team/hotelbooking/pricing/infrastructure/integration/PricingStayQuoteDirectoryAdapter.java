package com.team.hotelbooking.pricing.infrastructure.integration;

import com.team.hotelbooking.pricing.StayQuoteDirectory;
import com.team.hotelbooking.pricing.application.command.CalculateStayQuoteQuery;
import com.team.hotelbooking.pricing.application.command.CalculateStayQuoteQuery.QuoteItemInput;
import com.team.hotelbooking.pricing.application.command.ListNightlyRatesQuery;
import com.team.hotelbooking.pricing.application.result.RoomTypeNightlyRatesResult;
import com.team.hotelbooking.pricing.application.result.StayQuoteResult;
import com.team.hotelbooking.pricing.application.usecase.CalculateStayQuoteUseCase;
import com.team.hotelbooking.pricing.application.usecase.ListNightlyRatesUseCase;
import com.team.hotelbooking.pricing.domain.exception.PricingException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PricingStayQuoteDirectoryAdapter implements StayQuoteDirectory {

  private final CalculateStayQuoteUseCase calculateStayQuoteUseCase;
  private final ListNightlyRatesUseCase listNightlyRatesUseCase;

  public PricingStayQuoteDirectoryAdapter(
      CalculateStayQuoteUseCase calculateStayQuoteUseCase,
      ListNightlyRatesUseCase listNightlyRatesUseCase) {
    this.calculateStayQuoteUseCase = calculateStayQuoteUseCase;
    this.listNightlyRatesUseCase = listNightlyRatesUseCase;
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
      throw toCalculationException(ex);
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

  @Override
  public Map<UUID, NightlyRatesSnapshot> findNightlyRates(
      Collection<UUID> roomTypeIds, LocalDate checkIn, LocalDate checkOut) {
    List<RoomTypeNightlyRatesResult> results;
    try {
      results =
          listNightlyRatesUseCase.execute(
              new ListNightlyRatesQuery(
                  roomTypeIds == null ? Set.of() : Set.copyOf(roomTypeIds), checkIn, checkOut));
    } catch (PricingException ex) {
      throw toCalculationException(ex);
    }

    Map<UUID, NightlyRatesSnapshot> snapshots = new LinkedHashMap<>();
    for (RoomTypeNightlyRatesResult result : results) {
      snapshots.put(
          result.roomTypeId(),
          new NightlyRatesSnapshot(
              result.roomTypeId(),
              result.hotelId(),
              result.sellable(),
              result.totalAmount(),
              result.nightlyPrices().stream()
                  .map(np -> new NightlyPriceSnapshot(np.stayDate(), np.unitPrice()))
                  .toList()));
    }
    return snapshots;
  }

  // Dùng chung cho calculateQuote và findNightlyRates để hai hàm báo cùng lý do lỗi.
  private static StayQuoteCalculationException toCalculationException(PricingException ex) {
    FailureReason reason =
        switch (ex.code()) {
          case INVALID_DATE_RANGE -> FailureReason.INVALID_DATE_RANGE;
          case MAX_STAY_EXCEEDED, MAX_RANGE_EXCEEDED -> FailureReason.MAX_STAY_EXCEEDED;
          case INVALID_QUOTE_REQUEST, INVALID_AMOUNT -> FailureReason.INVALID_QUOTE_REQUEST;
          case HOTEL_NOT_FOUND -> FailureReason.HOTEL_NOT_FOUND;
          case ROOM_TYPE_NOT_FOUND -> FailureReason.ROOM_TYPE_NOT_FOUND;
          case INVALID_OCCUPANCY -> FailureReason.INVALID_OCCUPANCY;
          case RATE_NOT_AVAILABLE -> FailureReason.RATE_NOT_AVAILABLE;
          // Chỉ thuộc API quản lý giá (PricingManagementAccess), báo giá không ném hai mã này;
          // vẫn liệt kê để switch đủ mọi mã khi thêm mã mới.
          case HOTEL_LOCKED -> FailureReason.HOTEL_NOT_FOUND;
          case ACCESS_DENIED -> FailureReason.INVALID_QUOTE_REQUEST;
        };
    return new StayQuoteCalculationException(reason, ex.getMessage());
  }
}
