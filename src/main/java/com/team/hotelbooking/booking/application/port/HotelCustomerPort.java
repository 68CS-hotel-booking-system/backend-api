package com.team.hotelbooking.booking.application.port;

import com.team.hotelbooking.booking.application.result.HotelCustomerResult;
import com.team.hotelbooking.shared.pagination.PageQuery;
import com.team.hotelbooking.shared.pagination.PageResult;
import java.util.UUID;

public interface HotelCustomerPort {
  PageResult<HotelCustomerResult> findCustomers(UUID ownerId, String keyword, PageQuery pagination);
}
