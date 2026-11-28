package com.team.hotelbooking.booking.complaint.domain.repository;

import com.team.hotelbooking.booking.complaint.domain.model.*;
import com.team.hotelbooking.shared.pagination.*;
import java.util.*;

public interface ComplaintCaseRepository {
  PageResult<ComplaintCase> findCases(
      Set<UUID> hotelIds, Complaint.Status status, String keyword, PageQuery page);

  Optional<ComplaintCase> findCase(UUID id);

  List<ComplaintAction> history(UUID id);

  boolean update(Complaint complaint, long expectedVersion, ComplaintAction action);
}
