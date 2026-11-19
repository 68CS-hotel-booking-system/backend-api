package com.team.hotelbooking.booking.complaint.application.usecase.impl;

import com.team.hotelbooking.booking.complaint.application.command.ListMyComplaintsQuery;
import com.team.hotelbooking.booking.complaint.application.result.ComplaintResult;
import com.team.hotelbooking.booking.complaint.application.usecase.ListMyComplaintsUseCase;
import com.team.hotelbooking.booking.complaint.domain.exception.ComplaintException;
import com.team.hotelbooking.booking.complaint.domain.repository.ComplaintRepository;
import com.team.hotelbooking.shared.pagination.PageResult;

// Chỉ khiếu nại của chính người gọi, mới nhất trước.
public final class ListMyComplaintsService implements ListMyComplaintsUseCase {

  private final ComplaintRepository complaints;

  public ListMyComplaintsService(ComplaintRepository complaints) {
    this.complaints = complaints;
  }

  @Override
  public PageResult<ComplaintResult> execute(ListMyComplaintsQuery query) {
    if (query == null || query.customerId() == null || query.pagination() == null) {
      throw ComplaintException.invalid("Thiếu người gọi hoặc phân trang.");
    }
    return complaints
        .findByCustomerId(query.customerId(), query.status(), query.pagination())
        .map(ComplaintResult::from);
  }
}
