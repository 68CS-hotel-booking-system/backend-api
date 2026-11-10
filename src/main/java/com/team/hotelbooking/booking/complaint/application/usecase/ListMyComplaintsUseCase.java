package com.team.hotelbooking.booking.complaint.application.usecase;

import com.team.hotelbooking.booking.complaint.application.command.ListMyComplaintsQuery;
import com.team.hotelbooking.booking.complaint.application.result.ComplaintResult;
import com.team.hotelbooking.shared.pagination.PageResult;

public interface ListMyComplaintsUseCase {

  PageResult<ComplaintResult> execute(ListMyComplaintsQuery query);
}
