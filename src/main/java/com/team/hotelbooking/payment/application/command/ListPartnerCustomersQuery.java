package com.team.hotelbooking.payment.application.command;

import com.team.hotelbooking.shared.pagination.PageQuery;
import java.util.UUID;

public record ListPartnerCustomersQuery(UUID actorId, String keyword, PageQuery pagination) {}
