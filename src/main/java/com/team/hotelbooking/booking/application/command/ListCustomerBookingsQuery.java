package com.team.hotelbooking.booking.application.command;

import com.team.hotelbooking.shared.pagination.PageQuery;
import java.util.UUID;

public record ListCustomerBookingsQuery(UUID customerId, String status, PageQuery pagination) {}
