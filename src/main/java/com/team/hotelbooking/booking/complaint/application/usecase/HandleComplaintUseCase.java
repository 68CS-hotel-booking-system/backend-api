package com.team.hotelbooking.booking.complaint.application.usecase;

import com.team.hotelbooking.booking.complaint.application.command.HandleComplaintCommand;
import com.team.hotelbooking.booking.complaint.application.result.ManagedComplaintResult;

public interface HandleComplaintUseCase {
  ManagedComplaintResult execute(HandleComplaintCommand command);
}
