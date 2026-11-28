package com.team.hotelbooking.booking.infrastructure.integration;

import com.team.hotelbooking.booking.application.port.OperationsReportPort;
import com.team.hotelbooking.booking.application.result.OperationsReportResult;
import com.team.hotelbooking.booking.application.result.OperationsReportResult.HotelRow;
import com.team.hotelbooking.booking.application.result.OperationsReportResult.MonthRow;
import com.team.hotelbooking.booking.application.result.OperationsReportResult.ScopeHotel;
import com.team.hotelbooking.booking.application.result.OperationsReportResult.Totals;
import com.team.hotelbooking.hotel.HotelDirectory;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OperationsReportAdapter implements OperationsReportPort {

  private final HotelDirectory hotels;
  private final NamedParameterJdbcTemplate jdbc;

  @Override
  public List<ScopeHotel> findScopeHotels(UUID ownerId) {
    return hotels.findFinanceHotels(ownerId).stream()
        .map(h -> new ScopeHotel(h.id(), h.name()))
        .toList();
  }

  @Override
  public OperationsReportResult loadReport(
      LocalDate from,
      LocalDate to,
      UUID filterHotelId,
      List<ScopeHotel> scopeHotels,
      List<ScopeHotel> targetHotels) {
    List<UUID> hotelIds = targetHotels.stream().map(ScopeHotel::hotelId).toList();
    var params =
        new MapSqlParameterSource()
            .addValue("from", from)
            .addValue("to", to)
            .addValue("hotelIds", hotelIds);

    Map<String, MonthAccumulator> monthMap = new HashMap<>();
    Map<UUID, HotelAccumulator> hotelMap = new HashMap<>();
    for (ScopeHotel h : targetHotels) {
      hotelMap.put(h.hotelId(), new HotelAccumulator());
    }

    // 1. Đêm phòng khả dụng theo ngày vận hành (giờ Việt Nam), không trừ room-block.
    jdbc.query(
        """
        WITH days AS (
          SELECT generate_series(CAST(:from AS date), CAST(:to AS date), INTERVAL '1 day')::date AS stay_date
        )
        SELECT to_char(d.stay_date, 'YYYY-MM') AS ym,
               r.hotel_id,
               COUNT(*) AS available_room_nights
        FROM days d
        JOIN rooms r ON r.hotel_id IN (:hotelIds)
        WHERE (r.operational_from AT TIME ZONE 'Asia/Ho_Chi_Minh')::date <= d.stay_date
          AND (r.operational_until IS NULL
               OR (r.operational_until AT TIME ZONE 'Asia/Ho_Chi_Minh')::date > d.stay_date)
        GROUP BY 1, 2
        """,
        params,
        rs -> {
          String ym = rs.getString("ym");
          UUID hid = rs.getObject("hotel_id", UUID.class);
          long avail = rs.getLong("available_room_nights");
          monthMap.computeIfAbsent(ym, k -> new MonthAccumulator()).available += avail;
          HotelAccumulator ha = hotelMap.get(hid);
          if (ha != null) {
            ha.available += avail;
          }
        });

    // 2. Đêm phòng đã bán & doanh thu tiền phòng (trước giảm giá) theo booking_night_prices.
    jdbc.query(
        """
        SELECT to_char(bnp.stay_date, 'YYYY-MM') AS ym,
               b.hotel_id,
               COALESCE(SUM(bi.quantity), 0) AS sold_room_nights,
               COALESCE(SUM(bi.quantity::bigint * bnp.unit_price), 0) AS room_revenue
        FROM booking_night_prices bnp
        JOIN booking_items bi ON bi.id = bnp.booking_item_id
        JOIN bookings b ON b.id = bi.booking_id
        WHERE b.hotel_id IN (:hotelIds)
          AND b.status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
          AND bnp.stay_date BETWEEN CAST(:from AS date) AND CAST(:to AS date)
        GROUP BY 1, 2
        """,
        params,
        rs -> {
          String ym = rs.getString("ym");
          UUID hid = rs.getObject("hotel_id", UUID.class);
          long sold = rs.getLong("sold_room_nights");
          long rev = rs.getLong("room_revenue");
          MonthAccumulator ma = monthMap.computeIfAbsent(ym, k -> new MonthAccumulator());
          ma.sold += sold;
          ma.roomRevenue += rev;
          HotelAccumulator ha = hotelMap.get(hid);
          if (ha != null) {
            ha.sold += sold;
            ha.roomRevenue += rev;
          }
        });

    // 3. Doanh thu lưu trú hoàn thành (CHECKED_OUT theo ngày check_out).
    jdbc.query(
        """
        SELECT to_char(b.check_out, 'YYYY-MM') AS ym,
               b.hotel_id,
               COALESCE(SUM(b.total_amount), 0) AS completed_revenue
        FROM bookings b
        WHERE b.hotel_id IN (:hotelIds)
          AND b.status = 'CHECKED_OUT'
          AND b.check_out BETWEEN CAST(:from AS date) AND CAST(:to AS date)
        GROUP BY 1, 2
        """,
        params,
        rs -> {
          String ym = rs.getString("ym");
          UUID hid = rs.getObject("hotel_id", UUID.class);
          long completed = rs.getLong("completed_revenue");
          monthMap.computeIfAbsent(ym, k -> new MonthAccumulator()).completedRevenue += completed;
          HotelAccumulator ha = hotelMap.get(hid);
          if (ha != null) {
            ha.completedRevenue += completed;
          }
        });

    // 4. Đơn mới (theo created_at giờ Việt Nam, bỏ PENDING_PAYMENT và EXPIRED).
    jdbc.query(
        """
        SELECT to_char((b.created_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date, 'YYYY-MM') AS ym,
               COUNT(*) AS cnt
        FROM bookings b
        WHERE b.hotel_id IN (:hotelIds)
          AND b.status NOT IN ('PENDING_PAYMENT', 'EXPIRED')
          AND (b.created_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date
              BETWEEN CAST(:from AS date) AND CAST(:to AS date)
        GROUP BY 1
        """,
        params,
        rs -> {
          String ym = rs.getString("ym");
          monthMap.computeIfAbsent(ym, k -> new MonthAccumulator()).newBookings += rs.getLong("cnt");
        });

    // 5. Đơn hủy (theo booking_status_history to_status = 'CANCELLED', created_at giờ Việt Nam).
    jdbc.query(
        """
        SELECT to_char((h.created_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date, 'YYYY-MM') AS ym,
               COUNT(*) AS cnt
        FROM booking_status_history h
        JOIN bookings b ON b.id = h.booking_id
        WHERE b.hotel_id IN (:hotelIds)
          AND h.to_status = 'CANCELLED'
          AND (h.created_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date
              BETWEEN CAST(:from AS date) AND CAST(:to AS date)
        GROUP BY 1
        """,
        params,
        rs -> {
          String ym = rs.getString("ym");
          monthMap.computeIfAbsent(ym, k -> new MonthAccumulator()).cancelledBookings +=
              rs.getLong("cnt");
        });

    // 6. Đơn no-show (status = 'NO_SHOW' theo check_in).
    jdbc.query(
        """
        SELECT to_char(b.check_in, 'YYYY-MM') AS ym,
               COUNT(*) AS cnt
        FROM bookings b
        WHERE b.hotel_id IN (:hotelIds)
          AND b.status = 'NO_SHOW'
          AND b.check_in BETWEEN CAST(:from AS date) AND CAST(:to AS date)
        GROUP BY 1
        """,
        params,
        rs -> {
          String ym = rs.getString("ym");
          monthMap.computeIfAbsent(ym, k -> new MonthAccumulator()).noShowBookings +=
              rs.getLong("cnt");
        });

    List<MonthRow> months = new ArrayList<>();
    long totalAvailable = 0L;
    long totalSold = 0L;
    long totalRoomRevenue = 0L;
    long totalCompletedRevenue = 0L;
    long totalNew = 0L;
    long totalCancelled = 0L;
    long totalNoShow = 0L;

    YearMonth cursor = YearMonth.from(from);
    YearMonth end = YearMonth.from(to);
    while (!cursor.isAfter(end)) {
      String ym = cursor.toString();
      MonthAccumulator acc = monthMap.getOrDefault(ym, new MonthAccumulator());
      months.add(
          new MonthRow(
              ym,
              acc.available,
              acc.sold,
              acc.roomRevenue,
              OperationsReportResult.occupancyRate(acc.sold, acc.available),
              OperationsReportResult.adr(acc.roomRevenue, acc.sold),
              OperationsReportResult.revpar(acc.roomRevenue, acc.available),
              acc.completedRevenue,
              acc.newBookings,
              acc.cancelledBookings,
              acc.noShowBookings));
      totalAvailable += acc.available;
      totalSold += acc.sold;
      totalRoomRevenue += acc.roomRevenue;
      totalCompletedRevenue += acc.completedRevenue;
      totalNew += acc.newBookings;
      totalCancelled += acc.cancelledBookings;
      totalNoShow += acc.noShowBookings;
      cursor = cursor.plusMonths(1);
    }

    Totals totals =
        new Totals(
            totalAvailable,
            totalSold,
            totalRoomRevenue,
            OperationsReportResult.occupancyRate(totalSold, totalAvailable),
            OperationsReportResult.adr(totalRoomRevenue, totalSold),
            OperationsReportResult.revpar(totalRoomRevenue, totalAvailable),
            totalCompletedRevenue,
            totalNew,
            totalCancelled,
            totalNoShow);

    List<HotelRow> hotelRows =
        targetHotels.stream()
            .map(
                h -> {
                  HotelAccumulator ha = hotelMap.getOrDefault(h.hotelId(), new HotelAccumulator());
                  return new HotelRow(
                      h.hotelId(),
                      h.hotelName(),
                      ha.available,
                      ha.sold,
                      ha.roomRevenue,
                      OperationsReportResult.occupancyRate(ha.sold, ha.available),
                      OperationsReportResult.adr(ha.roomRevenue, ha.sold),
                      OperationsReportResult.revpar(ha.roomRevenue, ha.available),
                      ha.completedRevenue);
                })
            .toList();

    return new OperationsReportResult(
        from, to, filterHotelId, "VND", totals, months, hotelRows, scopeHotels);
  }

  private static final class MonthAccumulator {
    long available;
    long sold;
    long roomRevenue;
    long completedRevenue;
    long newBookings;
    long cancelledBookings;
    long noShowBookings;
  }

  private static final class HotelAccumulator {
    long available;
    long sold;
    long roomRevenue;
    long completedRevenue;
  }
}
