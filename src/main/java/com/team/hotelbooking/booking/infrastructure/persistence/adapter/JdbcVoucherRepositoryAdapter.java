package com.team.hotelbooking.booking.infrastructure.persistence.adapter;

import com.team.hotelbooking.booking.domain.exception.BookingErrorCode;
import com.team.hotelbooking.booking.domain.exception.BookingException;
import com.team.hotelbooking.booking.domain.model.Booking;
import com.team.hotelbooking.booking.domain.model.Voucher;
import com.team.hotelbooking.booking.domain.repository.VoucherRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcVoucherRepositoryAdapter implements VoucherRepository {

  private final JdbcClient jdbc;

  @Override
  public Optional<Voucher> findByCode(String code, boolean lock) {
    return jdbc.sql("SELECT * FROM promotions WHERE code = :code" + (lock ? " FOR UPDATE" : ""))
        .param("code", code)
        .query(
            (row, index) ->
                new Voucher(
                    row.getObject("id", UUID.class),
                    row.getObject("hotel_id", UUID.class),
                    row.getString("code"),
                    row.getString("name"),
                    Voucher.DiscountType.valueOf(row.getString("discount_type")),
                    row.getBigDecimal("percentage_off"),
                    row.getObject("fixed_amount", Long.class),
                    row.getObject("max_discount_amount", Long.class),
                    row.getLong("min_booking_amount"),
                    row.getObject("usage_limit", Integer.class),
                    row.getInt("per_user_limit"),
                    row.getObject("starts_at", OffsetDateTime.class).toInstant(),
                    row.getObject("ends_at", OffsetDateTime.class).toInstant(),
                    Voucher.Status.valueOf(row.getString("status")),
                    Voucher.FundingSource.valueOf(row.getString("funding_source"))))
        .optional();
  }

  @Override
  public Usage countUsage(UUID voucherId, UUID userId, Instant now) {
    return jdbc.sql(
            """
        SELECT count(*), count(*) FILTER (WHERE r.user_id = :userId)
        FROM promotion_redemptions r JOIN bookings b ON b.id = r.booking_id
        WHERE r.promotion_id = :voucherId AND r.status = 'APPLIED'
          AND (b.status <> 'PENDING_PAYMENT' OR b.expires_at > :now)
        """)
        .param("voucherId", voucherId)
        .param("userId", userId)
        .param("now", now.atOffset(ZoneOffset.UTC))
        .query((row, index) -> new Usage(row.getLong(1), row.getLong(2)))
        .single();
  }

  @Override
  public void record(Booking booking, Voucher voucher) {
    jdbc.sql(
            """
        INSERT INTO promotion_redemptions
          (booking_id, promotion_id, user_id, code_snapshot, funding_source, discount_amount, redeemed_at)
        VALUES (:booking, :promotion, :userId, :code, :funding, :discount, :now)
        """)
        .param("booking", booking.id())
        .param("promotion", voucher.id())
        .param("userId", booking.customerId())
        .param("code", voucher.code())
        .param("funding", voucher.fundingSource().name())
        .param("discount", booking.discountAmount())
        .param("now", booking.createdAt().atOffset(ZoneOffset.UTC))
        .update();
  }

  @Override
  public void release(UUID bookingId, Instant now) {
    jdbc.sql(
            """
        UPDATE promotion_redemptions SET status = 'CANCELLED', released_at = :now
        WHERE booking_id = :booking AND status = 'APPLIED'
        """)
        .param("booking", bookingId)
        .param("now", now.atOffset(ZoneOffset.UTC))
        .update();
  }

  @Override
  public List<UUID> findExpiredBookingIds(Instant now, int limit) {
    return jdbc.sql(
            """
        SELECT b.id FROM bookings b JOIN promotion_redemptions r ON r.booking_id = b.id
        WHERE b.status = 'PENDING_PAYMENT' AND b.expires_at <= :now AND r.status = 'APPLIED'
        ORDER BY b.expires_at, b.id LIMIT :limit
        """)
        .param("now", now.atOffset(ZoneOffset.UTC))
        .param("limit", limit)
        .query(UUID.class)
        .list();
  }

  @Override
  public void save(Voucher voucher, Instant now, boolean create) {
    String sql =
        create
            ? """
        INSERT INTO promotions (id, hotel_id, code, name, discount_type, percentage_off,
          fixed_amount, max_discount_amount, min_booking_amount, usage_limit, per_user_limit,
          starts_at, ends_at, status, funding_source, created_at, updated_at)
        VALUES (:id, :hotel, :code, :name, :type, :percentage, :fixed, :maxDiscount, :minBooking,
          :usage, :perUser, :starts, :ends, :status, :funding, :now, :now)
        ON CONFLICT (code) DO NOTHING
        """
            : """
        UPDATE promotions SET name = :name, discount_type = :type, percentage_off = :percentage,
          fixed_amount = :fixed, max_discount_amount = :maxDiscount, min_booking_amount = :minBooking,
          usage_limit = :usage, per_user_limit = :perUser, starts_at = :starts, ends_at = :ends,
          status = :status, funding_source = :funding, updated_at = :now
        WHERE id = :id
        """;
    int written =
        jdbc.sql(sql)
            .param("id", voucher.id())
            .param("hotel", voucher.hotelId())
            .param("code", voucher.code())
            .param("name", voucher.name())
            .param("type", voucher.discountType().name())
            .param("percentage", voucher.percentageOff())
            .param("fixed", voucher.fixedAmount())
            .param("maxDiscount", voucher.maxDiscountAmount())
            .param("minBooking", voucher.minBookingAmount())
            .param("usage", voucher.usageLimit())
            .param("perUser", voucher.perUserLimit())
            .param("starts", voucher.startsAt().atOffset(ZoneOffset.UTC))
            .param("ends", voucher.endsAt().atOffset(ZoneOffset.UTC))
            .param("status", voucher.status().name())
            .param("funding", voucher.fundingSource().name())
            .param("now", now.atOffset(ZoneOffset.UTC))
            .update();
    if (written == 0) {
      throw new BookingException(BookingErrorCode.VOUCHER_CODE_EXISTS);
    }
  }
}
