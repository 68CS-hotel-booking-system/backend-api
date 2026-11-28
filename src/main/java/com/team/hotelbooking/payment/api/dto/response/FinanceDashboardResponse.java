package com.team.hotelbooking.payment.api.dto.response;

import com.team.hotelbooking.payment.application.result.FinanceDashboardResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FinanceDashboardResponse(
    long bookingCount,
    long bookedAmount,
    long collectedAmount,
    long completedAmount,
    long platformAmount,
    long ownerAmount,
    long missingCommissionCount,
    long pendingRefundAmount,
    long refundedAmount,
    long netCollectedAmount,
    List<HotelContractStatusResponse> hotels,
    List<ContractRevenueResponse> contracts) {
  public record ContractRevenueResponse(
      UUID contractId,
      String contractCode,
      UUID hotelId,
      String hotelName,
      UUID ownerId,
      java.math.BigDecimal commissionRate,
      Instant startsAt,
      Instant endsAt,
      long completedBookingCount,
      long completedAmount,
      long basisAmount,
      long platformAmount,
      long ownerAmount,
      boolean canManageHotel) {
    public static ContractRevenueResponse from(FinanceDashboardResult.ContractRevenue revenue) {
      return new ContractRevenueResponse(
          revenue.contractId(),
          revenue.contractCode(),
          revenue.hotelId(),
          revenue.hotelName(),
          revenue.ownerId(),
          revenue.commissionRate(),
          revenue.startsAt(),
          revenue.endsAt(),
          revenue.completedBookingCount(),
          revenue.completedAmount(),
          revenue.basisAmount(),
          revenue.platformAmount(),
          revenue.ownerAmount(),
          revenue.canManageHotel());
    }
  }

  public record HotelContractStatusResponse(
      UUID hotelId,
      String hotelName,
      String contractStatus,
      Instant endsAt,
      boolean acceptingBookings) {}

  public static FinanceDashboardResponse from(FinanceDashboardResult result) {
    return new FinanceDashboardResponse(
        result.bookingCount(),
        result.bookedAmount(),
        result.collectedAmount(),
        result.completedAmount(),
        result.platformAmount(),
        result.ownerAmount(),
        result.missingCommissionCount(),
        result.pendingRefundAmount(),
        result.refundedAmount(),
        result.netCollectedAmount(),
        result.hotels().stream()
            .map(
                h ->
                    new HotelContractStatusResponse(
                        h.hotelId(),
                        h.hotelName(),
                        h.contractStatus(),
                        h.endsAt(),
                        h.acceptingBookings()))
            .toList(),
        result.contracts().stream().map(ContractRevenueResponse::from).toList());
  }
}
