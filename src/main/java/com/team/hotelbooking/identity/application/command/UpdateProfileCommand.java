package com.team.hotelbooking.identity.application.command;

import java.util.UUID;

public record UpdateProfileCommand(
    UUID userId, String fullName, String phone, long expectedVersion) {}
