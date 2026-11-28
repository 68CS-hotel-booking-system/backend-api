package com.team.hotelbooking.booking.infrastructure.integration;

import com.team.hotelbooking.booking.application.port.HotelCustomerPort;
import com.team.hotelbooking.booking.application.result.HotelCustomerResult;
import com.team.hotelbooking.hotel.HotelDirectory;
import com.team.hotelbooking.shared.pagination.PageQuery;
import com.team.hotelbooking.shared.pagination.PageResult;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HotelCustomerAdapter implements HotelCustomerPort {
  private final HotelDirectory hotels;
  private final NamedParameterJdbcTemplate jdbc;

  @Override
  public PageResult<HotelCustomerResult> findCustomers(
      UUID ownerId, String keyword, PageQuery pagination) {
    var ids =
        hotels.findFinanceHotels(ownerId).stream().map(HotelDirectory.FinanceHotel::id).toList();
    if (ids.isEmpty())
      return new PageResult<>(
          List.of(), pagination.page(), pagination.size(), 0, 0, pagination.page() == 0, true);
    String pattern =
        "%"
            + keyword
                .toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_")
            + "%";
    var params =
        Map.of(
            "ids",
            ids,
            "pattern",
            pattern,
            "limit",
            pagination.size(),
            "offset",
            (long) pagination.page() * pagination.size());
    // Xếp thông tin liên hệ và tính thống kê sau khi đã giới hạn khách sạn của owner.
    String base =
        """
        WITH scoped AS (SELECT * FROM bookings WHERE hotel_id IN (:ids)),
        ranked AS (SELECT s.*, row_number() OVER (PARTITION BY customer_id ORDER BY created_at DESC, id DESC) AS rn FROM scoped s),
        totals AS (SELECT customer_id, count(*) AS booking_count, count(DISTINCT hotel_id) AS hotel_count,
            count(*) FILTER (WHERE status = 'CHECKED_OUT') AS completed_count, max(created_at) AS last_booked_at
            FROM scoped GROUP BY customer_id),
        matched AS (SELECT r.customer_id, r.booker_name_snapshot, r.booker_email_snapshot, r.booker_phone_snapshot,
            t.booking_count, t.hotel_count, t.completed_count, t.last_booked_at
            FROM ranked r JOIN totals t ON t.customer_id = r.customer_id WHERE r.rn = 1
              AND (lower(COALESCE(r.booker_name_snapshot,'')) LIKE :pattern ESCAPE E'\\\\'
                OR lower(COALESCE(r.booker_email_snapshot,'')) LIKE :pattern ESCAPE E'\\\\'
                OR COALESCE(r.booker_phone_snapshot,'') LIKE :pattern ESCAPE E'\\\\'))
        """;
    long total = jdbc.queryForObject(base + "SELECT count(*) FROM matched", params, Long.class);
    var rows =
        jdbc.query(
            base
                + "SELECT * FROM matched ORDER BY last_booked_at DESC, customer_id LIMIT :limit OFFSET :offset",
            params,
            (row, index) ->
                new HotelCustomerResult(
                    row.getObject("customer_id", UUID.class),
                    row.getString("booker_name_snapshot"),
                    row.getString("booker_email_snapshot"),
                    row.getString("booker_phone_snapshot"),
                    row.getLong("booking_count"),
                    row.getLong("hotel_count"),
                    row.getLong("completed_count"),
                    row.getTimestamp("last_booked_at").toInstant()));
    int pages = (int) ((total + pagination.size() - 1) / pagination.size());
    return new PageResult<>(
        rows,
        pagination.page(),
        pagination.size(),
        total,
        pages,
        pagination.page() == 0,
        (long) pagination.page() + 1 >= pages);
  }
}
