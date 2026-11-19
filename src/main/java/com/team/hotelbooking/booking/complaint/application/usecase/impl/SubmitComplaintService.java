package com.team.hotelbooking.booking.complaint.application.usecase.impl;

import com.team.hotelbooking.booking.complaint.application.command.SubmitComplaintCommand;
import com.team.hotelbooking.booking.complaint.application.result.ComplaintResult;
import com.team.hotelbooking.booking.complaint.application.usecase.SubmitComplaintUseCase;
import com.team.hotelbooking.booking.complaint.domain.exception.ComplaintErrorCode;
import com.team.hotelbooking.booking.complaint.domain.exception.ComplaintException;
import com.team.hotelbooking.booking.complaint.domain.model.Complaint;
import com.team.hotelbooking.booking.complaint.domain.repository.ComplaintRepository;
import com.team.hotelbooking.booking.domain.model.Booking;
import com.team.hotelbooking.booking.domain.repository.BookingRepository;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * BK-21: chỉ chủ booking gửi khiếu nại; booking của người khác hoặc không tồn tại đều trả 404 để
 * không lộ booking. Mọi trạng thái trừ PENDING_PAYMENT được gửi (EXPIRED vẫn được, vì kết quả cổng
 * thanh toán có thể về trễ). Mỗi booking tối đa một khiếu nại đang xử lý.
 */
public final class SubmitComplaintService implements SubmitComplaintUseCase {

  private final BookingRepository bookings;
  private final ComplaintRepository complaints;
  private final Clock clock;

  public SubmitComplaintService(
      BookingRepository bookings, ComplaintRepository complaints, Clock clock) {
    this.bookings = bookings;
    this.complaints = complaints;
    this.clock = clock;
  }

  @Override
  public ComplaintResult execute(SubmitComplaintCommand command) {
    if (command == null || command.customerId() == null || command.bookingId() == null) {
      throw ComplaintException.invalid("Thiếu thông tin đơn đặt phòng hoặc người gửi khiếu nại.");
    }

    // Đọc qua repository của module booking, không đi qua service booking.
    Booking booking =
        bookings
            .findByIdAndCustomerId(command.bookingId(), command.customerId())
            .orElseThrow(() -> new ComplaintException(ComplaintErrorCode.BOOKING_NOT_FOUND));

    if (booking.status() == Booking.Status.PENDING_PAYMENT) {
      throw new ComplaintException(ComplaintErrorCode.BOOKING_NOT_COMPLAINABLE);
    }

    Complaint complaint =
        Complaint.submit(
            UUID.randomUUID(),
            booking.id(),
            command.customerId(),
            booking.hotelId(),
            command.category(),
            command.content(),
            // PostgreSQL lưu tới micro giây: cắt trước để response POST trùng với lúc đọc lại.
            clock.instant().truncatedTo(ChronoUnit.MICROS));

    // Kiểm trước để trả id đang mở; request đồng thời vượt qua bước này vẫn bị index unique chặn.
    complaints
        .findOpenComplaintId(booking.id())
        .ifPresent(
            openId -> {
              throw ComplaintException.alreadyOpen(openId);
            });
    if (!complaints.addIfNoneOpen(complaint)) {
      throw ComplaintException.alreadyOpen(
          complaints.findOpenComplaintId(booking.id()).orElse(null));
    }

    return ComplaintResult.from(complaint);
  }
}
