package com.team.hotelbooking.booking.complaint.application.usecase;

import com.team.hotelbooking.booking.complaint.application.command.SubmitComplaintCommand;
import com.team.hotelbooking.booking.complaint.application.result.ComplaintResult;

public interface SubmitComplaintUseCase {

  ComplaintResult execute(SubmitComplaintCommand command);
}
