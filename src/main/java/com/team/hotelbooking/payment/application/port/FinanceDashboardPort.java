package com.team.hotelbooking.payment.application.port;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FinanceDashboardPort {
  Optional<Viewer> viewer(UUID actorId);

  Totals summarize(List<UUID> hotelIds, UUID beneficiaryOwnerId);

  List<ContractRevenue> contracts(List<UUID> hotelIds, UUID beneficiaryOwnerId);

  List<HotelScope> hotels(UUID ownerId);

  ContractState contractState(UUID hotelId, Instant at);

  record Totals(
      long bookingCount,
      long bookedAmount,
      long collectedAmount,
      long completedAmount,
      long platformAmount,
      long ownerAmount,
      long missingCommissionCount,
      long pendingRefundAmount,
      long refundedAmount,
      long netCollectedAmount) {
    public Totals(
        long bookingCount,
        long bookedAmount,
        long collectedAmount,
        long completedAmount,
        long platformAmount,
        long ownerAmount,
        long missingCommissionCount) {
      this(
          bookingCount,
          bookedAmount,
          collectedAmount,
          completedAmount,
          platformAmount,
          ownerAmount,
          missingCommissionCount,
          0,
          0,
          collectedAmount);
    }
  }

  record HotelScope(UUID id, String name, boolean active) {}

  record ContractState(String status, Instant endsAt) {}

  record Viewer(boolean active, boolean emailVerified, boolean admin, boolean owner) {}

  record ContractRevenue(
      UUID contractId,
      String contractCode,
      UUID hotelId,
      String hotelName,
      UUID ownerId,
      BigDecimal commissionRate,
      Instant startsAt,
      Instant endsAt,
      long completedBookingCount,
      long completedAmount,
      long basisAmount,
      long platformAmount,
      long ownerAmount,
      boolean canManageHotel) {}
}
