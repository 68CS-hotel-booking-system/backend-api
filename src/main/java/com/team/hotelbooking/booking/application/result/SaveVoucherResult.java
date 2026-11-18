package com.team.hotelbooking.booking.application.result;

import java.util.UUID;

public record SaveVoucherResult(UUID id, UUID hotelId, String code, String status) {}
