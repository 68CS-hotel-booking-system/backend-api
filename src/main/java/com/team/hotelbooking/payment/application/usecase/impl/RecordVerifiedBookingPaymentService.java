package com.team.hotelbooking.payment.application.usecase.impl;

import com.team.hotelbooking.payment.ContractDirectory;
import com.team.hotelbooking.payment.VerifiedBookingPaymentRecorder;
import com.team.hotelbooking.payment.application.port.PaymentBookingPort;
import com.team.hotelbooking.payment.application.port.VerifiedBookingPaymentRepository;

public final class RecordVerifiedBookingPaymentService implements VerifiedBookingPaymentRecorder {

  private final PaymentBookingPort bookings;
  private final ContractDirectory contracts;
  private final VerifiedBookingPaymentRepository payments;

  public RecordVerifiedBookingPaymentService(
      PaymentBookingPort bookings,
      ContractDirectory contracts,
      VerifiedBookingPaymentRepository payments) {
    this.bookings = bookings;
    this.contracts = contracts;
    this.payments = payments;
  }

  @Override
  public void record(VerifiedCapture capture) {
    var existing = payments.find(capture.bookingId());
    if (existing.isPresent()) {
      if (!existing.get().matches(capture)) {
        throw new IllegalStateException("Đơn đã có giao dịch xác nhận khác.");
      }
      return;
    }

    var booking =
        bookings
            .findBookingForPayment(capture.bookingId())
            .filter(
                order ->
                    order.payableAmount() == capture.amount()
                        && order.currency().equals(capture.currency()))
            .orElseThrow(
                () -> new IllegalArgumentException("Khoản thu không khớp với đơn đặt phòng."));
    if (!"CONFIRMED".equals(booking.status())) {
      throw new IllegalStateException(
          "Chỉ lưu khoản thu xác nhận cho đơn đã chuyển sang CONFIRMED.");
    }
    var contract =
        contracts
            .findEffective(booking.hotelId(), capture.verifiedAt())
            .orElseThrow(
                () ->
                    new IllegalStateException("Khách sạn chưa có hợp đồng áp dụng cho khoản thu."));
    if (!payments.addIfAbsent(capture, contract.contractId(), contract.commissionRate())
        && payments
            .find(capture.bookingId())
            .filter(payment -> payment.matches(capture))
            .isEmpty()) {
      throw new IllegalStateException("Đơn đã có giao dịch xác nhận khác.");
    }
  }
}
