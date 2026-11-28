package com.team.hotelbooking.payment.api.dto.response;

import com.team.hotelbooking.payment.application.result.PartnerCustomerResult;
import com.team.hotelbooking.shared.pagination.PageResult;
import com.team.hotelbooking.shared.web.PageResponse;
import java.time.Instant;
import java.util.UUID;

public record PartnerCustomerResponse(
    UUID id,
    String fullName,
    String email,
    String phone,
    long hotelCount,
    long contractCount,
    long activeContractCount,
    Instant lastContractCreatedAt) {
  public static PartnerCustomerResponse from(PartnerCustomerResult result) {
    return new PartnerCustomerResponse(
        result.id(),
        result.fullName(),
        result.email(),
        result.phone(),
        result.hotelCount(),
        result.contractCount(),
        result.activeContractCount(),
        result.lastContractCreatedAt());
  }

  public static PageResponse<PartnerCustomerResponse> from(
      PageResult<PartnerCustomerResult> result) {
    return PageResponse.from(result.map(PartnerCustomerResponse::from));
  }
}
