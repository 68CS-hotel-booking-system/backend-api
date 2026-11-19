package com.team.hotelbooking.identity.application.command;

import com.team.hotelbooking.shared.pagination.PageQuery;
import java.time.Instant;
import java.util.UUID;

public record ListAdminUsersQuery(
    UUID actorId,
    String search,
    String status,
    String role,
    Instant createdFrom,
    Instant createdTo,
    String sort,
    PageQuery pagination) {}
