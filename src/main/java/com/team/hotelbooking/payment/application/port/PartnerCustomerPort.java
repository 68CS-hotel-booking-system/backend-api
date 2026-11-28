package com.team.hotelbooking.payment.application.port;

import com.team.hotelbooking.payment.application.result.PartnerCustomerResult;
import com.team.hotelbooking.shared.pagination.PageQuery;
import com.team.hotelbooking.shared.pagination.PageResult;
import java.time.Instant;
import java.util.UUID;

public interface PartnerCustomerPort {
  boolean isActiveAdmin(UUID actorId);

  PageResult<PartnerCustomerResult> findCustomers(
      String keyword, PageQuery pagination, Instant now);
}
