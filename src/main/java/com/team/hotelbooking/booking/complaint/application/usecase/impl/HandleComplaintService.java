package com.team.hotelbooking.booking.complaint.application.usecase.impl;

import com.team.hotelbooking.booking.application.port.BookingIdentityPort;
import com.team.hotelbooking.booking.complaint.application.command.HandleComplaintCommand;
import com.team.hotelbooking.booking.complaint.application.port.ComplaintHotelPort;
import com.team.hotelbooking.booking.complaint.application.result.ManagedComplaintResult;
import com.team.hotelbooking.booking.complaint.application.usecase.HandleComplaintUseCase;
import com.team.hotelbooking.booking.complaint.domain.exception.*;
import com.team.hotelbooking.booking.complaint.domain.model.*;
import com.team.hotelbooking.booking.complaint.domain.repository.ComplaintCaseRepository;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public final class HandleComplaintService implements HandleComplaintUseCase {
  private final ComplaintCaseRepository cases;
  private final ComplaintManagementAccess access;
  private final Clock clock;

  public HandleComplaintService(
      ComplaintCaseRepository cases,
      BookingIdentityPort accounts,
      ComplaintHotelPort hotels,
      Clock clock) {
    this.cases = cases;
    this.access = new ComplaintManagementAccess(accounts, hotels);
    this.clock = clock;
  }

  public ManagedComplaintResult execute(HandleComplaintCommand command) {
    if (command == null
        || command.actorId() == null
        || command.complaintId() == null
        || command.expectedVersion() == null
        || command.expectedVersion() < 0)
      throw ComplaintException.invalid("Thiếu mã khiếu nại hoặc phiên bản hợp lệ.");
    var scope = access.require(command.actorId());
    var value =
        cases
            .findCase(command.complaintId())
            .orElseThrow(() -> new ComplaintException(ComplaintErrorCode.COMPLAINT_NOT_FOUND));
    var old = value.complaint();
    scope.requireHotel(old.hotelId());
    if (old.version() != command.expectedVersion())
      throw new ComplaintException(ComplaintErrorCode.COMPLAINT_VERSION_CONFLICT);
    var now = clock.instant().truncatedTo(ChronoUnit.MICROS);
    var changed = old.handle(command.status(), command.response(), now);
    var action =
        new ComplaintAction(
            UUID.randomUUID(),
            old.id(),
            command.actorId(),
            scope.role(),
            old.status(),
            changed.status(),
            command.response().trim(),
            now,
            changed.version());
    if (!cases.update(changed, command.expectedVersion(), action))
      throw new ComplaintException(ComplaintErrorCode.COMPLAINT_VERSION_CONFLICT);
    return ManagedComplaintResult.from(
        new ComplaintCase(
            changed,
            value.bookingCode(),
            value.bookerName(),
            value.bookerEmail(),
            value.bookerPhone()),
        scope.hotels().get(old.hotelId()),
        cases.history(old.id()));
  }
}
