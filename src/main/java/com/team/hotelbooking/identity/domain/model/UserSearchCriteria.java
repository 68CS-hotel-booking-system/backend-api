package com.team.hotelbooking.identity.domain.model;

import java.time.Instant;

public record UserSearchCriteria(
    String search,
    String status,
    String role,
    Instant createdFrom,
    Instant createdTo,
    String sort) {}
