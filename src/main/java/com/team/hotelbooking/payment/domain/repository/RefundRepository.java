package com.team.hotelbooking.payment.domain.repository;

import com.team.hotelbooking.payment.domain.model.*;
import com.team.hotelbooking.shared.pagination.*;
import java.util.*;

public interface RefundRepository {
  Optional<Refund> findById(UUID id);

  Optional<Refund> findByPaymentId(UUID id);

  List<Refund> findByBookingId(UUID id);

  Refund add(Refund refund);

  boolean update(Refund refund, long expectedVersion, RefundAction action);

  List<RefundAction> history(UUID id);

  PageResult<Refund> list(
      UUID customerId,
      Set<UUID> hotels,
      Refund.Status status,
      UUID bookingId,
      String keyword,
      PageQuery pagination);
}
