package com.team.hotelbooking.payment.application.result;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FinanceDashboardResult(
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
    List<HotelContractStatus> hotels,
    List<ContractRevenue> contracts) {
  public record ContractRevenue(
      java.util.UUID contractId,
      String contractCode,
      java.util.UUID hotelId,
      String hotelName,
      java.util.UUID ownerId,
      java.math.BigDecimal commissionRate,
      Instant startsAt,
      Instant endsAt,
      long completedBookingCount,
      long completedAmount,
      long basisAmount,
      long platformAmount,
      long ownerAmount,
      boolean canManageHotel) {
    public static ContractRevenue from(
        com.team.hotelbooking.payment.application.port.FinanceDashboardPort.ContractRevenue
            revenue) {
      return new ContractRevenue(
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

  public record HotelContractStatus(
      UUID hotelId,
      String hotelName,
      String contractStatus,
      Instant endsAt,
      boolean acceptingBookings) {}
}
