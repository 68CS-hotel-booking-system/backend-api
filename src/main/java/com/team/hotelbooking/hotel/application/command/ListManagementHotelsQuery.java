package com.team.hotelbooking.hotel.application.command;

import com.team.hotelbooking.shared.pagination.PageQuery;
import java.util.UUID;

public record ListManagementHotelsQuery(UUID actorId, PageQuery pagination) {}
