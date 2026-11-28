package com.team.hotelbooking.booking;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface BookingRevenueDirectory {
  RevenueSummary summarize(Set<UUID> hotelIds, UUID beneficiaryOwnerId);

  List<ContractRevenue> summarizeContracts(Set<UUID> hotelIds, UUID beneficiaryOwnerId);

  record ContractRevenue(
      UUID contractId,
      UUID hotelId,
      UUID ownerId,
      BigDecimal commissionRate,
      long completedBookingCount,
      long completedAmount,
      long basisAmount,
      long platformAmount,
      long ownerAmount) {}

  record RevenueSummary(
      long bookingCount,
      long bookedAmount,
      long completedAmount,
      long platformAmount,
      long ownerAmount,
      long missingCommissionCount,
      List<UUID> bookingIds) {}
}
