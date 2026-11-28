package com.team.hotelbooking.pricing.voucher.application.command;

import java.util.UUID;

public record SetVoucherEnabledCommand(
    UUID actorId, UUID hotelId, UUID voucherId, boolean enabled, long expectedVersion) {}
