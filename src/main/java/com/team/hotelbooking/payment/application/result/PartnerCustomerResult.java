package com.team.hotelbooking.payment.application.result;

import java.time.Instant;
import java.util.UUID;

public record PartnerCustomerResult(
    UUID id,
    String fullName,
    String email,
    String phone,
    long hotelCount,
    long contractCount,
    long activeContractCount,
    Instant lastContractCreatedAt) {}
