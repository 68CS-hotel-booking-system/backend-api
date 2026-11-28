package com.team.hotelbooking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.team.hotelbooking.booking.application.command.GetOperationsReportQuery;
import com.team.hotelbooking.booking.application.port.BookingIdentityPort;
import com.team.hotelbooking.booking.application.port.BookingIdentityPort.AccountSnapshot;
import com.team.hotelbooking.booking.application.port.OperationsReportPort;
import com.team.hotelbooking.booking.application.result.OperationsReportResult;
import com.team.hotelbooking.booking.application.result.OperationsReportResult.ScopeHotel;
import com.team.hotelbooking.booking.application.usecase.impl.GetOperationsReportService;
import com.team.hotelbooking.booking.domain.exception.BookingErrorCode;
import com.team.hotelbooking.booking.domain.exception.BookingException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GetOperationsReportService unit tests")
class GetOperationsReportServiceTests {

  private BookingIdentityPort accounts;
  private OperationsReportPort port;
  private GetOperationsReportService service;
  private UUID ownerId, customerId, hotelId, foreignHotelId;

  @BeforeEach
  void setUp() {
    accounts = mock(BookingIdentityPort.class);
    port = mock(OperationsReportPort.class);
    service = new GetOperationsReportService(accounts, port);
    ownerId = UUID.randomUUID();
    customerId = UUID.randomUUID();
    hotelId = UUID.randomUUID();
    foreignHotelId = UUID.randomUUID();

    when(accounts.findById(ownerId))
        .thenReturn(
            Optional.of(
                new AccountSnapshot(
                    ownerId, "Owner", "o@test.vn", true, true, Set.of("HOTEL_OWNER"))));
    when(accounts.findById(customerId))
        .thenReturn(
            Optional.of(
                new AccountSnapshot(
                    customerId, "Cust", "c@test.vn", true, true, Set.of("CUSTOMER"))));
  }

  @Test
  void returnsEmptyMonthSeriesWhenOwnerHasNoHotels() {
    when(port.findScopeHotels(ownerId)).thenReturn(List.of());

    OperationsReportResult result =
        service.execute(
            new GetOperationsReportQuery(
                ownerId, null, LocalDate.of(2027, 1, 15), LocalDate.of(2027, 3, 10)));

    assertThat(result.currency()).isEqualTo("VND");
    assertThat(result.totals().availableRoomNights()).isZero();
    assertThat(result.totals().occupancyRate()).isNull();
    assertThat(result.totals().adr()).isNull();
    assertThat(result.totals().revpar()).isNull();
    assertThat(result.months()).extracting("month").containsExactly("2027-01", "2027-02", "2027-03");
  }

  @Test
  void rejectsInvalidDatesForeignHotelAndUnauthorizedCustomer() {
    when(port.findScopeHotels(ownerId)).thenReturn(List.of(new ScopeHotel(hotelId, "Hotel A")));

    assertThatThrownBy(
            () ->
                service.execute(
                    new GetOperationsReportQuery(
                        ownerId, null, LocalDate.of(2027, 4, 1), LocalDate.of(2027, 3, 1))))
        .isInstanceOf(BookingException.class)
        .extracting("code")
        .isEqualTo(BookingErrorCode.INVALID_DATE_RANGE);

    assertThatThrownBy(
            () ->
                service.execute(
                    new GetOperationsReportQuery(
                        ownerId, null, LocalDate.of(2026, 1, 1), LocalDate.of(2027, 3, 1))))
        .isInstanceOf(BookingException.class)
        .extracting("code")
        .isEqualTo(BookingErrorCode.INVALID_DATE_RANGE);

    assertThatThrownBy(
            () ->
                service.execute(
                    new GetOperationsReportQuery(
                        ownerId,
                        foreignHotelId,
                        LocalDate.of(2027, 3, 1),
                        LocalDate.of(2027, 3, 31))))
        .isInstanceOf(BookingException.class)
        .extracting("code")
        .isEqualTo(BookingErrorCode.HOTEL_NOT_FOUND);

    assertThatThrownBy(
            () ->
                service.execute(
                    new GetOperationsReportQuery(
                        customerId, null, LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 31))))
        .isInstanceOf(BookingException.class)
        .extracting("code")
        .isEqualTo(BookingErrorCode.ACCESS_DENIED);
  }

  @Test
  void calculatesOccupancyAdrAndRevparAccurately() {
    assertThat(OperationsReportResult.occupancyRate(3, 62)).isEqualTo(0.0484);
    assertThat(OperationsReportResult.occupancyRate(0, 0)).isNull();
    assertThat(OperationsReportResult.adr(1_800_000, 3)).isEqualTo(600_000L);
    assertThat(OperationsReportResult.adr(0, 0)).isNull();
    assertThat(OperationsReportResult.revpar(1_800_000, 62)).isEqualTo(29_032L);
    assertThat(OperationsReportResult.revpar(0, 0)).isNull();
  }
}
