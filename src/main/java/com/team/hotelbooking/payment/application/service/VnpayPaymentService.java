package com.team.hotelbooking.payment.application.service;

import com.team.hotelbooking.payment.application.port.PaymentBookingPort;
import com.team.hotelbooking.payment.application.port.VnpayGateway;
import com.team.hotelbooking.payment.application.result.PaymentResult;
import com.team.hotelbooking.payment.application.result.VnpayIpnResult;
import com.team.hotelbooking.payment.domain.exception.FinanceErrorCode;
import com.team.hotelbooking.payment.domain.exception.FinanceException;
import com.team.hotelbooking.payment.infrastructure.persistence.entity.PaymentTransactionJpaEntity;
import com.team.hotelbooking.payment.infrastructure.persistence.entity.PaymentTransactionJpaEntity.Status;
import com.team.hotelbooking.payment.infrastructure.persistence.repository.PaymentTransactionJpaRepository;
import com.team.hotelbooking.shared.time.BusinessTime;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class VnpayPaymentService {
  private static final DateTimeFormatter PAY_DATE =
      DateTimeFormatter.ofPattern("uuuuMMddHHmmss").withResolverStyle(ResolverStyle.STRICT);
  private final PaymentTransactionJpaRepository payments;
  private final PaymentBookingPort bookings;
  private final VnpayGateway gateway;
  private final Clock clock;
  private final TransactionTemplate writeTx;
  private final TransactionTemplate readTx;

  public VnpayPaymentService(
      PaymentTransactionJpaRepository payments,
      PaymentBookingPort bookings,
      VnpayGateway gateway,
      Clock clock,
      PlatformTransactionManager transactions) {
    this.payments = payments;
    this.bookings = bookings;
    this.gateway = gateway;
    this.clock = clock;
    this.writeTx = new TransactionTemplate(transactions);
    this.readTx = new TransactionTemplate(transactions);
    this.readTx.setReadOnly(true);
  }

  public PaymentResult create(UUID customerId, UUID bookingId, String ipAddress) {
    return create(customerId, bookingId, ipAddress, false);
  }

  public PaymentResult create(UUID customerId, UUID bookingId, String ipAddress, boolean qr) {
    if (!gateway.enabled()) throw new FinanceException(FinanceErrorCode.PAYMENT_UNAVAILABLE);
    return Objects.requireNonNull(
        writeTx.execute(
            tx -> {
              var booking =
                  bookings
                      .lockBookingForPayment(bookingId)
                      .filter(b -> b.customerId().equals(customerId))
                      .orElseThrow(() -> new FinanceException(FinanceErrorCode.PAYMENT_NOT_FOUND));
              Instant now = clock.instant();
              var succeeded =
                  payments.findFirstByBookingIdAndStatusOrderByCreatedAtDesc(
                      bookingId, Status.SUCCEEDED);
              if (succeeded.isPresent()) return PaymentResult.from(succeeded.get(), now);
              if (!"PENDING_PAYMENT".equals(booking.status())
                  || booking.holdExpiresAt() == null
                  || !booking.holdExpiresAt().truncatedTo(ChronoUnit.SECONDS).isAfter(now)) {
                throw new FinanceException(FinanceErrorCode.BOOKING_NOT_PAYABLE);
              }
              long amount = booking.payableAmount();
              if (!"VND".equals(booking.currency()) || amount < 5000 || amount > 9_999_999_999L) {
                throw new FinanceException(FinanceErrorCode.INVALID_PAYMENT_AMOUNT);
              }
              var pending =
                  payments.findFirstByBookingIdAndStatusOrderByCreatedAtDesc(
                      bookingId, Status.PENDING);
              if (pending.isPresent()) {
                var payment = pending.get();
                if (qr && payment.getQrContent() == null) {
                  attachQr(payment, booking.bookingCode(), ipAddress);
                  payments.saveAndFlush(payment);
                }
                return PaymentResult.from(payment, clock.instant());
              }

              var payment = new PaymentTransactionJpaEntity();
              payment.setId(UUID.randomUUID());
              payment.setBookingId(bookingId);
              payment.setCustomerId(customerId);
              payment.setAmountVnd(amount);
              payment.setCurrency("VND");
              payment.setStatus(Status.PENDING);
              payment.setCreatedAt(now);
              payment.setExpiresAt(booking.holdExpiresAt().truncatedTo(ChronoUnit.SECONDS));
              payment.setPaymentUrl(
                  gateway.createUrl(
                      payment.getId(),
                      amount,
                      booking.bookingCode(),
                      now,
                      payment.getExpiresAt(),
                      ipAddress));
              if (qr) attachQr(payment, booking.bookingCode(), ipAddress);
              return PaymentResult.from(payments.saveAndFlush(payment), clock.instant());
            }));
  }

  private void attachQr(PaymentTransactionJpaEntity payment, String bookingCode, String ipAddress) {
    payment.setQrContent(
        gateway.createQr(
            payment.getId(),
            payment.getAmountVnd(),
            bookingCode,
            payment.getCreatedAt(),
            payment.getExpiresAt(),
            ipAddress));
    if (!payment.getExpiresAt().isAfter(clock.instant())) {
      throw new FinanceException(FinanceErrorCode.BOOKING_NOT_PAYABLE);
    }
  }

  public PaymentResult get(UUID customerId, UUID paymentId) {
    return Objects.requireNonNull(
        readTx.execute(
            tx ->
                PaymentResult.from(
                    payments
                        .findByIdAndCustomerId(paymentId, customerId)
                        .orElseThrow(
                            () -> new FinanceException(FinanceErrorCode.PAYMENT_NOT_FOUND)),
                    clock.instant())));
  }

  public VnpayIpnResult receiveIpn(Map<String, List<String>> query) {
    Map<String, String> parameters = new HashMap<>();
    for (var entry : query.entrySet()) {
      if (!entry.getKey().startsWith("vnp_")) continue;
      if (entry.getValue() == null
          || entry.getValue().size() != 1
          || entry.getValue().getFirst() == null) return VnpayIpnResult.of("97");
      parameters.put(entry.getKey(), entry.getValue().getFirst());
    }
    if (!gateway.verify(parameters) || !gateway.matchesMerchant(parameters.get("vnp_TmnCode")))
      return VnpayIpnResult.of("97");
    UUID id;
    try {
      String reference = parameters.get("vnp_TxnRef");
      id = UUID.fromString(reference);
      if (!id.toString().equals(reference)) return VnpayIpnResult.of("01");
    } catch (IllegalArgumentException | NullPointerException exception) {
      return VnpayIpnResult.of("01");
    }
    return Objects.requireNonNull(writeTx.execute(tx -> updateFromIpn(id, parameters)));
  }

  private VnpayIpnResult updateFromIpn(UUID id, Map<String, String> parameters) {
    // Luôn khóa booking trước payment để tránh deadlock với yêu cầu tạo thanh toán.
    var bookingId = payments.findBookingIdById(id);
    if (bookingId.isEmpty()) return VnpayIpnResult.of("01");
    var booking = bookings.lockBookingForPayment(bookingId.get());
    if (booking.isEmpty()) return VnpayIpnResult.of("01");
    var payment = payments.lockById(id).orElseThrow();
    String amount = parameters.get("vnp_Amount");
    if (amount == null
        || !amount.matches("[0-9]{1,12}")
        || Long.parseLong(amount) != Math.multiplyExact(payment.getAmountVnd(), 100))
      return VnpayIpnResult.of("04");
    if (payment.getStatus() != Status.PENDING) return VnpayIpnResult.of("02");

    String response = parameters.get("vnp_ResponseCode");
    String transactionStatus = parameters.get("vnp_TransactionStatus");
    if (response == null
        || !response.matches("[0-9]{2}")
        || transactionStatus == null
        || !transactionStatus.matches("[0-9]{2}")) return VnpayIpnResult.of("99");
    boolean success = "00".equals(response) && "00".equals(transactionStatus);
    String transactionNo = parameters.get("vnp_TransactionNo");
    Instant paidAt = null;
    if (success) {
      if (transactionNo == null
          || !transactionNo.matches("[0-9]{1,15}")
          || Long.parseLong(transactionNo) == 0) return VnpayIpnResult.of("99");
      try {
        paidAt =
            LocalDateTime.parse(parameters.get("vnp_PayDate"), PAY_DATE)
                .atZone(BusinessTime.ZONE)
                .toInstant();
      } catch (java.time.DateTimeException | NullPointerException exception) {
        return VnpayIpnResult.of("99");
      }
    }
    Instant now = clock.instant();
    payment.setStatus(success ? Status.SUCCEEDED : Status.FAILED);
    payment.setResponseCode(response);
    payment.setTransactionStatus(transactionStatus);
    payment.setReceivedAt(now);
    payment.setPaidAt(paidAt);
    payment.setProviderTransactionNo(success ? transactionNo : null);
    // IPN đến muộn vẫn ghi nhận tiền, nhưng không lấy lại tồn đã hết hạn cho khách khác.
    if (success
        && paidAt.isBefore(payment.getExpiresAt())
        && booking.get().payableAmount() == payment.getAmountVnd()
        && booking.get().currency().equals(payment.getCurrency())) {
      payment.setBookingConfirmed(bookings.confirmPayment(payment.getBookingId(), now));
    }
    payments.saveAndFlush(payment);
    return VnpayIpnResult.of("00");
  }
}
