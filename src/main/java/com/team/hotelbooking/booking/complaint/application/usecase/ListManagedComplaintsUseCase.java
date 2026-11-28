package com.team.hotelbooking.booking.complaint.application.usecase;

import com.team.hotelbooking.booking.complaint.application.command.ListManagedComplaintsQuery;
import com.team.hotelbooking.booking.complaint.application.result.ManagedComplaintResult;
import com.team.hotelbooking.shared.pagination.PageResult;

public interface ListManagedComplaintsUseCase {
  PageResult<ManagedComplaintResult> execute(ListManagedComplaintsQuery query);
}
