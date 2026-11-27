package com.team.hotelbooking.pricing.voucher.application.command;

import com.team.hotelbooking.shared.pagination.PageQuery;
import java.util.UUID;

public record ListVouchersQuery(UUID actorId, UUID hotelId, PageQuery page) {}
