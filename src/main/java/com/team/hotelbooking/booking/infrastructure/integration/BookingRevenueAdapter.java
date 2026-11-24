package com.team.hotelbooking.booking.infrastructure.integration;

import com.team.hotelbooking.booking.BookingRevenueDirectory;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingRevenueAdapter implements BookingRevenueDirectory {
  private final NamedParameterJdbcTemplate jdbc;

  @Override
  public RevenueSummary summarize(Set<UUID> hotelIds, UUID beneficiaryOwnerId) {
    if (hotelIds.isEmpty() && beneficiaryOwnerId == null)
      return new RevenueSummary(0, 0, 0, 0, 0, 0, List.of());
    var params = parameters(hotelIds, beneficiaryOwnerId);
    String scope = scope(hotelIds, beneficiaryOwnerId);
    var ids =
        jdbc.query(
            "SELECT b.id FROM bookings b LEFT JOIN booking_commissions c ON c.booking_id = b.id WHERE "
                + scope,
            params,
            (row, index) -> row.getObject("id", UUID.class));
    return jdbc.queryForObject(
        """
        SELECT COUNT(*) AS booking_count, COALESCE(SUM(b.total_amount),0) AS booked,
          COALESCE(SUM(b.total_amount) FILTER (WHERE b.status = 'CHECKED_OUT'),0) AS completed,
          COALESCE(SUM(c.platform_amount) FILTER (WHERE b.status = 'CHECKED_OUT'),0) AS platform,
          COALESCE(SUM(c.owner_amount) FILTER (WHERE b.status = 'CHECKED_OUT'),0) AS owner,
          COUNT(*) FILTER (WHERE c.booking_id IS NULL AND b.status = 'CHECKED_OUT') AS missing
        FROM bookings b LEFT JOIN booking_commissions c ON c.booking_id = b.id
        WHERE
        """
            + scope,
        params,
        (row, index) ->
            new RevenueSummary(
                row.getLong("booking_count"),
                row.getLong("booked"),
                row.getLong("completed"),
                row.getLong("platform"),
                row.getLong("owner"),
                row.getLong("missing"),
                ids));
  }

  @Override
  public List<ContractRevenue> summarizeContracts(Set<UUID> hotelIds, UUID beneficiaryOwnerId) {
    if (hotelIds.isEmpty() && beneficiaryOwnerId == null) return List.of();
    return jdbc.query(
        """
        SELECT c.contract_id, b.hotel_id, c.owner_user_id, c.commission_rate, count(*) AS completed_count,
          COALESCE(SUM(b.total_amount),0) AS completed, COALESCE(SUM(c.basis_amount),0) AS basis,
          COALESCE(SUM(c.platform_amount),0) AS platform, COALESCE(SUM(c.owner_amount),0) AS owner
        FROM bookings b JOIN booking_commissions c ON c.booking_id = b.id
        WHERE b.status = 'CHECKED_OUT' AND (
        """
            + scope(hotelIds, beneficiaryOwnerId)
            + """
        ) GROUP BY c.contract_id, b.hotel_id, c.owner_user_id, c.commission_rate
        ORDER BY b.hotel_id, c.contract_id, c.commission_rate
        """,
        parameters(hotelIds, beneficiaryOwnerId),
        (row, index) ->
            new ContractRevenue(
                row.getObject("contract_id", UUID.class),
                row.getObject("hotel_id", UUID.class),
                row.getObject("owner_user_id", UUID.class),
                row.getBigDecimal("commission_rate"),
                row.getLong("completed_count"),
                row.getLong("completed"),
                row.getLong("basis"),
                row.getLong("platform"),
                row.getLong("owner")));
  }

  private static java.util.Map<String, Object> parameters(Set<UUID> hotelIds, UUID ownerId) {
    var params = new java.util.HashMap<String, Object>();
    if (!hotelIds.isEmpty()) params.put("ids", hotelIds);
    if (ownerId != null) params.put("owner", ownerId);
    return params;
  }

  private static String scope(Set<UUID> hotelIds, UUID ownerId) {
    if (ownerId == null) return hotelIds.isEmpty() ? "FALSE" : "b.hotel_id IN (:ids)";
    // Quyền hưởng tiền theo snapshot hợp đồng; đổi chủ khách sạn không chuyển doanh thu cũ.
    return "c.owner_user_id = :owner"
        + (hotelIds.isEmpty() ? "" : " OR (c.booking_id IS NULL AND b.hotel_id IN (:ids))");
  }
}
