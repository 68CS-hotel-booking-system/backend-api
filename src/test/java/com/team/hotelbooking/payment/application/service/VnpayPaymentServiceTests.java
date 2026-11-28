package com.team.hotelbooking.payment.application.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.team.hotelbooking.payment.application.port.PaymentBookingPort;
import com.team.hotelbooking.payment.application.port.PaymentBookingPort.BookingPaymentSnapshot;
import com.team.hotelbooking.payment.application.port.VnpayGateway;
import com.team.hotelbooking.payment.domain.exception.FinanceErrorCode;
import com.team.hotelbooking.payment.domain.exception.FinanceException;
import com.team.hotelbooking.payment.infrastructure.persistence.entity.PaymentTransactionJpaEntity;
import com.team.hotelbooking.payment.infrastructure.persistence.entity.PaymentTransactionJpaEntity.Status;
import com.team.hotelbooking.payment.infrastructure.persistence.repository.PaymentTransactionJpaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
class VnpayPaymentServiceTests {
  @Mock PaymentTransactionJpaRepository payments;
  @Mock PaymentBookingPort bookings;
  @Mock VnpayGateway gateway;
  @Mock PlatformTransactionManager transactions;
  @Mock com.team.hotelbooking.payment.application.port.RefundRegistrationPort refunds;
  private final UUID customerId = UUID.randomUUID();
  private final UUID bookingId = UUID.randomUUID();
  private final UUID paymentId = UUID.randomUUID();
  private final Instant now = Instant.parse("2026-11-15T03:10:00Z");
  private VnpayPaymentService service;
  private PaymentTransactionJpaEntity payment;

  @BeforeEach
  void setUp() {
    lenient().when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    service =
        new VnpayPaymentService(
            payments, bookings, gateway, Clock.fixed(now, ZoneOffset.UTC), transactions, refunds);
    payment = new PaymentTransactionJpaEntity();
    payment.setId(paymentId);
    payment.setBookingId(bookingId);
    payment.setCustomerId(customerId);
    payment.setAmountVnd(123456);
    payment.setCurrency("VND");
    payment.setStatus(Status.PENDING);
    payment.setCreatedAt(now.minusSeconds(600));
    payment.setExpiresAt(now.plusSeconds(600));
    payment.setPaymentUrl("https://sandbox.vnpayment.vn/test");
  }

  private BookingPaymentSnapshot booking() {
    var booking = mock(BookingPaymentSnapshot.class);
    lenient().when(booking.customerId()).thenReturn(customerId);
    lenient().when(booking.bookingCode()).thenReturn("BK-TEST");
    lenient().when(booking.status()).thenReturn("PENDING_PAYMENT");
    lenient().when(booking.holdExpiresAt()).thenReturn(now.plusSeconds(600));
    lenient().when(booking.payableAmount()).thenReturn(123456L);
    lenient().when(booking.currency()).thenReturn("VND");
    return booking;
  }

  private Map<String, List<String>> signedIpn() {
    when(gateway.verify(anyMap())).thenReturn(true);
    when(gateway.matchesMerchant("TESTCODE")).thenReturn(true);
    return new HashMap<>(
        Map.of(
            "vnp_TxnRef",
            List.of(paymentId.toString()),
            "vnp_Amount",
            List.of("12345600"),
            "vnp_TmnCode",
            List.of("TESTCODE"),
            "vnp_ResponseCode",
            List.of("00"),
            "vnp_TransactionStatus",
            List.of("00"),
            "vnp_TransactionNo",
            List.of("12345"),
            "vnp_PayDate",
            List.of("20261115101000")));
  }

  private void existingPayment() {
    when(payments.findBookingIdById(paymentId)).thenReturn(Optional.of(bookingId));
    doReturn(Optional.of(booking())).when(bookings).lockBookingForPayment(bookingId);
    when(payments.lockById(paymentId)).thenReturn(Optional.of(payment));
  }

  @Test
  void createsUsingServerAmountAndReusesPendingUrl() {
    when(gateway.enabled()).thenReturn(true);
    doReturn(Optional.of(booking())).when(bookings).lockBookingForPayment(bookingId);
    doReturn(Optional.empty())
        .when(payments)
        .findFirstByBookingIdAndStatusOrderByCreatedAtDesc(bookingId, Status.SUCCEEDED);
    when(gateway.createUrl(any(), eq(123456L), eq("BK-TEST"), eq(now), any(), eq("127.0.0.1")))
        .thenReturn(payment.getPaymentUrl());
    when(payments.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
    var result = service.create(customerId, bookingId, "127.0.0.1");
    assertThat(result.amountVnd()).isEqualTo(123456);
    assertThat(result.status()).isEqualTo("PENDING");
    when(payments.findFirstByBookingIdAndStatusOrderByCreatedAtDesc(bookingId, Status.PENDING))
        .thenReturn(Optional.of(payment));
    assertThat(service.create(customerId, bookingId, "127.0.0.1").id()).isEqualTo(paymentId);
    verify(payments, times(1)).saveAndFlush(any());
  }

  @Test
  void rejectsDisabledAndAnotherCustomersBooking() {
    assertThatThrownBy(() -> service.create(customerId, bookingId, "127.0.0.1"))
        .isInstanceOfSatisfying(
            FinanceException.class,
            ex -> assertThat(ex.code()).isEqualTo(FinanceErrorCode.PAYMENT_UNAVAILABLE));
    when(gateway.enabled()).thenReturn(true);
    doReturn(Optional.of(booking())).when(bookings).lockBookingForPayment(bookingId);
    assertThatThrownBy(() -> service.create(UUID.randomUUID(), bookingId, "127.0.0.1"))
        .isInstanceOfSatisfying(
            FinanceException.class,
            ex -> assertThat(ex.code()).isEqualTo(FinanceErrorCode.PAYMENT_NOT_FOUND));
    verifyNoInteractions(payments);
  }

  @Test
  void cachesQrForPendingPaymentAndDoesNotRegenerateItOnRepeatedRequests() {
    when(gateway.enabled()).thenReturn(true);
    doReturn(Optional.of(booking())).when(bookings).lockBookingForPayment(bookingId);
    doReturn(Optional.empty())
        .when(payments)
        .findFirstByBookingIdAndStatusOrderByCreatedAtDesc(bookingId, Status.SUCCEEDED);
    when(payments.findFirstByBookingIdAndStatusOrderByCreatedAtDesc(bookingId, Status.PENDING))
        .thenReturn(Optional.of(payment));
    when(gateway.createQr(
            eq(paymentId),
            eq(123456L),
            eq("BK-TEST"),
            eq(payment.getCreatedAt()),
            eq(payment.getExpiresAt()),
            eq("127.0.0.1")))
        .thenReturn("000201-test-qr");
    assertThat(service.create(customerId, bookingId, "127.0.0.1", true).qrContent())
        .isEqualTo("000201-test-qr");
    assertThat(service.create(customerId, bookingId, "127.0.0.1", true).id()).isEqualTo(paymentId);
    verify(gateway, times(1)).createQr(any(), anyLong(), anyString(), any(), any(), anyString());
    verify(payments, times(1)).saveAndFlush(payment);
    verifyNoInteractions(refunds);
  }

  @Test
  void rejectsExpiredOrNonPayableBooking() {
    when(gateway.enabled()).thenReturn(true);
    var booking = booking();
    when(booking.holdExpiresAt()).thenReturn(now);
    when(bookings.lockBookingForPayment(bookingId)).thenReturn(Optional.of(booking));
    assertThatThrownBy(() -> service.create(customerId, bookingId, "127.0.0.1"))
        .isInstanceOfSatisfying(
            FinanceException.class,
            ex -> assertThat(ex.code()).isEqualTo(FinanceErrorCode.BOOKING_NOT_PAYABLE));
    verify(payments, never()).saveAndFlush(any());
  }

  @ParameterizedTest
  @ValueSource(longs = {4999L, 10_000_000_000L})
  void rejectsAmountsOutsideGatewayRange(long amount) {
    when(gateway.enabled()).thenReturn(true);
    var booking = booking();
    when(booking.payableAmount()).thenReturn(amount);
    when(bookings.lockBookingForPayment(bookingId)).thenReturn(Optional.of(booking));
    assertThatThrownBy(() -> service.create(customerId, bookingId, "127.0.0.1"))
        .isInstanceOfSatisfying(
            FinanceException.class,
            ex -> assertThat(ex.code()).isEqualTo(FinanceErrorCode.INVALID_PAYMENT_AMOUNT));
  }

  @Test
  void onlyReadsOwnedPaymentAndHidesExpiredUrl() {
    when(payments.findByIdAndCustomerId(paymentId, customerId)).thenReturn(Optional.of(payment));
    assertThat(service.get(customerId, paymentId).paymentUrl()).isNotNull();
    payment.setExpiresAt(now);
    assertThat(service.get(customerId, paymentId).paymentUrl()).isNull();
    assertThatThrownBy(() -> service.get(UUID.randomUUID(), paymentId))
        .isInstanceOf(FinanceException.class);
  }

  @Test
  void rejectsBadSignaturesAndDuplicateQueryParametersBeforeDatabaseAccess() {
    assertThat(service.receiveIpn(Map.of("vnp_SecureHash", List.of("invalid"))).code())
        .isEqualTo("97");
    assertThat(
            service
                .receiveIpn(
                    Map.of("vnp_TxnRef", List.of(paymentId.toString(), paymentId.toString())))
                .code())
        .isEqualTo("97");
    verifyNoInteractions(payments, bookings);
  }

  @Test
  void rejectsOtherMerchantBeforeDatabaseAccess() {
    var parameters = signedIpn();
    when(gateway.matchesMerchant("TESTCODE")).thenReturn(false);
    assertThat(service.receiveIpn(parameters).code()).isEqualTo("97");
    verifyNoInteractions(payments, bookings);
  }

  @Test
  void unknownReferenceIsNotFound() {
    assertThat(service.receiveIpn(signedIpn()).code()).isEqualTo("01");
    verify(payments, never()).saveAndFlush(any());
  }

  @Test
  void rejectsWrongAmountWithoutRoundingOrConfirmingBooking() {
    existingPayment();
    var parameters = signedIpn();
    parameters.put("vnp_Amount", List.of("12345601"));
    assertThat(service.receiveIpn(parameters).code()).isEqualTo("04");
    assertThat(payment.getStatus()).isEqualTo(Status.PENDING);
    verify(bookings, never()).confirmPayment(any(), any());
    verify(payments, never()).saveAndFlush(any());
  }

  @Test
  void successConfirmsBookingOnceAndDuplicateCallbackIsAcknowledged() {
    existingPayment();
    when(bookings.confirmPayment(bookingId, now)).thenReturn(true);
    var parameters = signedIpn();
    assertThat(service.receiveIpn(parameters).code()).isEqualTo("00");
    assertThat(payment.getStatus()).isEqualTo(Status.SUCCEEDED);
    assertThat(payment.isBookingConfirmed()).isTrue();
    assertThat(payment.getPaidAt()).isEqualTo(now);
    assertThat(service.receiveIpn(parameters).code()).isEqualTo("02");
    verify(bookings, times(1)).confirmPayment(bookingId, now);
    verify(payments, times(1)).saveAndFlush(payment);
  }

  @Test
  void cancellationDoesNotConfirmBookingAndRetryUsesNewPayment() {
    existingPayment();
    var parameters = signedIpn();
    parameters.put("vnp_ResponseCode", List.of("24"));
    parameters.put("vnp_TransactionStatus", List.of("02"));
    assertThat(service.receiveIpn(parameters).code()).isEqualTo("00");
    assertThat(payment.getStatus()).isEqualTo(Status.FAILED);
    verify(bookings, never()).confirmPayment(any(), any());
  }

  @Test
  void successWithoutAvailableHoldIsRecordedForReview() {
    existingPayment();
    assertThat(service.receiveIpn(signedIpn()).code()).isEqualTo("00");
    assertThat(payment.getStatus()).isEqualTo(Status.SUCCEEDED);
    assertThat(payment.isBookingConfirmed()).isFalse();
    verify(refunds)
        .register(
            eq(paymentId),
            any(),
            eq(123456L),
            eq("VND"),
            eq("UNCONFIRMED_PAYMENT"),
            isNull(),
            eq(now));
    assertThat(service.receiveIpn(signedIpn()).code()).isEqualTo("02");
    verifyNoMoreInteractions(refunds);
  }

  @Test
  void paymentAfterDeadlineIsRecordedWithoutTakingRoomsBack() {
    existingPayment();
    payment.setExpiresAt(now.minusSeconds(1));
    assertThat(service.receiveIpn(signedIpn()).code()).isEqualTo("00");
    assertThat(payment.getStatus()).isEqualTo(Status.SUCCEEDED);
    assertThat(payment.isBookingConfirmed()).isFalse();
    verify(bookings, never()).confirmPayment(any(), any());
    verify(refunds)
        .register(
            eq(paymentId),
            any(),
            eq(123456L),
            eq("VND"),
            eq("UNCONFIRMED_PAYMENT"),
            isNull(),
            eq(now));
  }

  @Test
  void malformedSuccessDateDoesNotMutatePayment() {
    existingPayment();
    var parameters = signedIpn();
    parameters.put("vnp_PayDate", List.of("20260230101000"));
    assertThat(service.receiveIpn(parameters).code()).isEqualTo("99");
    assertThat(payment.getStatus()).isEqualTo(Status.PENDING);
    verify(payments, never()).saveAndFlush(any());
  }
}
