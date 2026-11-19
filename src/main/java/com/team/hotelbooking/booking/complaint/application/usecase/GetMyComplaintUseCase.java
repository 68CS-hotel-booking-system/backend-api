package com.team.hotelbooking.booking.complaint.application.usecase;

import com.team.hotelbooking.booking.complaint.application.command.GetMyComplaintQuery;
import com.team.hotelbooking.booking.complaint.application.result.ComplaintResult;

public interface GetMyComplaintUseCase {

  ComplaintResult execute(GetMyComplaintQuery query);
}
