package com.team.hotelbooking.booking.complaint.application.usecase.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.team.hotelbooking.booking.complaint.application.command.SubmitComplaintCommand;
import com.team.hotelbooking.booking.complaint.application.result.ComplaintResult;
import com.team.hotelbooking.booking.complaint.domain.exception.ComplaintErrorCode;
import com.team.hotelbooking.booking.complaint.domain.exception.ComplaintException;
import com.team.hotelbooking.booking.complaint.domain.model.Complaint;
import com.team.hotelbooking.booking.complaint.domain.repository.ComplaintRepository;
import com.team.hotelbooking.booking.domain.model.Booking;
import com.team.hotelbooking.booking.domain.repository.BookingRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Dịch vụ gửi khiếu nại (BK-21)")
class SubmitComplaintServiceTests {

  private static final UUID CUSTOMER_ID = UUID.fromString("4a000000-0000-4000-8000-000000000001");
  private static final UUID BOOKING_ID = UUID.fromString("4a000000-0000-4000-8000-000000000101");
  private static final UUID HOTEL_ID = UUID.fromString("4a000000-0000-4000-8000-000000000201");
  private static final UUID OPEN_ID = UUID.fromString("4a000000-0000-4000-8000-000000000301");
  // Có phần nano giây để kiểm việc cắt về micro giây.
  private static final Instant NOW = Instant.parse("2026-11-10T03:00:00.123456789Z");

  @Mock private BookingRepository bookings;
  @Mock private ComplaintRepository complaints;

  private SubmitComplaintService service;

  @BeforeEach
  void setUp() {
    service = new SubmitComplaintService(bookings, complaints, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private void givenBooking(Booking.Status status) {
    Booking booking = mock(Booking.class);
    given(booking.status()).willReturn(status);
    if (status != Booking.Status.PENDING_PAYMENT) {
      given(booking.id()).willReturn(BOOKING_ID);
      given(booking.hotelId()).willReturn(HOTEL_ID);
    }
    given(bookings.findByIdAndCustomerId(BOOKING_ID, CUSTOMER_ID)).willReturn(Optional.of(booking));
  }

  private static SubmitComplaintCommand command() {
    return new SubmitComplaintCommand(
        CUSTOMER_ID, BOOKING_ID, Complaint.Category.BILLING, "Đã bị trừ tiền nhưng đơn hết hạn.");
  }

  private static ComplaintErrorCode codeOf(Throwable exception) {
    return ((ComplaintException) exception).code();
  }

  @Nested
  @DisplayName("SubmitComplaintUseCase.execute")
  class ExecuteTests {

    @ParameterizedTest
    @EnumSource(
        value = Booking.Status.class,
        names = {"CONFIRMED", "CHECKED_IN", "CHECKED_OUT", "CANCELLED", "EXPIRED", "NO_SHOW"})
    @DisplayName("Tạo khiếu nại OPEN cho mọi trạng thái booking trừ PENDING_PAYMENT")
    void shouldCreateOpenComplaintWhenBookingStatusAllowed(Booking.Status status) {
      // Given
      givenBooking(status);
      given(complaints.findOpenComplaintId(BOOKING_ID)).willReturn(Optional.empty());
      given(complaints.addIfNoneOpen(any())).willReturn(true);
      ArgumentCaptor<Complaint> saved = ArgumentCaptor.forClass(Complaint.class);

      // When
      ComplaintResult result = service.execute(command());

      // Then
      assertThat(result.status()).isEqualTo("OPEN");
      assertThat(result.bookingId()).isEqualTo(BOOKING_ID);
      assertThat(result.hotelId()).isEqualTo(HOTEL_ID);
      assertThat(result.createdAt()).isEqualTo(Instant.parse("2026-11-10T03:00:00.123456Z"));
      then(complaints).should().addIfNoneOpen(saved.capture());
      assertThat(saved.getValue().customerId()).isEqualTo(CUSTOMER_ID);
      assertThat(saved.getValue().category()).isEqualTo(Complaint.Category.BILLING);
    }

    @Test
    @DisplayName("Trả 409 BOOKING_NOT_COMPLAINABLE khi booking đang chờ thanh toán")
    void shouldRejectWhenBookingPendingPayment() {
      // Given
      givenBooking(Booking.Status.PENDING_PAYMENT);

      // When & Then
      assertThatThrownBy(() -> service.execute(command()))
          .satisfies(
              ex -> assertThat(codeOf(ex)).isEqualTo(ComplaintErrorCode.BOOKING_NOT_COMPLAINABLE));
      then(complaints).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Trả 404 khi booking không tồn tại hoặc thuộc người khác")
    void shouldThrowBookingNotFoundWhenNotOwnedByCaller() {
      // Given
      given(bookings.findByIdAndCustomerId(BOOKING_ID, CUSTOMER_ID)).willReturn(Optional.empty());

      // When & Then
      assertThatThrownBy(() -> service.execute(command()))
          .satisfies(ex -> assertThat(codeOf(ex)).isEqualTo(ComplaintErrorCode.BOOKING_NOT_FOUND));
      then(complaints).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Trả 409 kèm id khiếu nại đang mở, không chèn, khi đã có khiếu nại đang xử lý")
    void shouldRejectWithOpenIdWhenComplaintAlreadyOpen() {
      // Given
      givenBooking(Booking.Status.CHECKED_OUT);
      given(complaints.findOpenComplaintId(BOOKING_ID)).willReturn(Optional.of(OPEN_ID));

      // When & Then
      assertThatThrownBy(() -> service.execute(command()))
          .satisfies(
              ex -> {
                assertThat(codeOf(ex)).isEqualTo(ComplaintErrorCode.COMPLAINT_ALREADY_OPEN);
                assertThat(((ComplaintException) ex).openComplaintId()).isEqualTo(OPEN_ID);
              });
      then(complaints).should(never()).addIfNoneOpen(any());
    }

    @Test
    @DisplayName("Request đồng thời chèn trước: vẫn trả 409 kèm id, không ném lỗi DB")
    void shouldRejectWithOpenIdWhenConcurrentRequestInsertedFirst() {
      // Given
      givenBooking(Booking.Status.CHECKED_OUT);
      given(complaints.findOpenComplaintId(BOOKING_ID))
          .willReturn(Optional.empty())
          .willReturn(Optional.of(OPEN_ID));
      given(complaints.addIfNoneOpen(any())).willReturn(false);

      // When & Then
      assertThatThrownBy(() -> service.execute(command()))
          .satisfies(
              ex -> {
                assertThat(codeOf(ex)).isEqualTo(ComplaintErrorCode.COMPLAINT_ALREADY_OPEN);
                assertThat(((ComplaintException) ex).openComplaintId()).isEqualTo(OPEN_ID);
              });
    }
  }
}
