package com.team.hotelbooking.booking.complaint.application.usecase;

import com.team.hotelbooking.booking.complaint.application.command.GetManagedComplaintQuery;
import com.team.hotelbooking.booking.complaint.application.result.ManagedComplaintResult;

public interface GetManagedComplaintUseCase {
  ManagedComplaintResult execute(GetManagedComplaintQuery query);
}
