package com.team.hotelbooking.booking.complaint.application.usecase.impl;

import com.team.hotelbooking.booking.application.port.BookingIdentityPort;
import com.team.hotelbooking.booking.complaint.application.command.ListManagedComplaintsQuery;
import com.team.hotelbooking.booking.complaint.application.port.ComplaintHotelPort;
import com.team.hotelbooking.booking.complaint.application.result.ManagedComplaintResult;
import com.team.hotelbooking.booking.complaint.application.usecase.ListManagedComplaintsUseCase;
import com.team.hotelbooking.booking.complaint.domain.exception.ComplaintException;
import com.team.hotelbooking.booking.complaint.domain.repository.ComplaintCaseRepository;
import com.team.hotelbooking.shared.pagination.PageResult;
import java.util.*;

public final class ListManagedComplaintsService implements ListManagedComplaintsUseCase {
  private final ComplaintCaseRepository cases;
  private final ComplaintManagementAccess access;
  private final int maxSize;

  public ListManagedComplaintsService(
      ComplaintCaseRepository cases,
      BookingIdentityPort accounts,
      ComplaintHotelPort hotels,
      int maxSize) {
    this.cases = cases;
    this.access = new ComplaintManagementAccess(accounts, hotels);
    this.maxSize = maxSize;
  }

  public PageResult<ManagedComplaintResult> execute(ListManagedComplaintsQuery query) {
    if (query == null
        || query.actorId() == null
        || query.pagination() == null
        || query.pagination().page() < 0
        || query.pagination().size() < 1
        || query.pagination().size() > maxSize)
      throw ComplaintException.invalid("Tham số phân trang không hợp lệ.");
    String keyword = query.keyword() == null ? "" : query.keyword().trim();
    if (keyword.length() > 100) throw ComplaintException.invalid("Từ khóa tối đa 100 ký tự.");
    var scope = access.require(query.actorId());
    if (query.hotelId() != null) scope.requireHotel(query.hotelId());
    var ids = query.hotelId() == null ? scope.hotels().keySet() : Set.of(query.hotelId());
    return cases
        .findCases(ids, query.status(), keyword, query.pagination())
        .map(
            value ->
                ManagedComplaintResult.from(
                    value, scope.hotels().get(value.complaint().hotelId()), List.of()));
  }
}
