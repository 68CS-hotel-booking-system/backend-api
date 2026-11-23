package com.team.hotelbooking.booking.infrastructure.integration;

import com.team.hotelbooking.booking.domain.model.Booking;
import com.team.hotelbooking.shared.time.BusinessTime;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.support.TransactionTemplate;

public final class BookingEmailNotifications {

  private static final Logger log = LoggerFactory.getLogger(BookingEmailNotifications.class);
  private static final DateTimeFormatter STAY_DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu");
  private static final DateTimeFormatter STAY_TIME = DateTimeFormatter.ofPattern("HH:mm");

  private final JdbcClient jdbc;
  private final JavaMailSender mail;
  private final String from;
  private final Clock clock;
  private final TransactionTemplate transaction;

  public BookingEmailNotifications(
      JdbcClient jdbc,
      JavaMailSender mail,
      String from,
      Clock clock,
      TransactionTemplate transaction) {
    this.jdbc = jdbc;
    this.mail = mail;
    this.from = from;
    this.clock = clock;
    this.transaction = transaction;
  }

  public void enqueueConfirmation(Booking booking, Instant now) {
    Instant checkIn =
        booking
            .checkIn()
            .atTime(booking.expectedCheckInTime())
            .atZone(BusinessTime.ZONE)
            .toInstant();
    jdbc.sql(
            """
            INSERT INTO booking_email_notifications (booking_id, is_reminder, due_at)
            VALUES (:id, FALSE, :now), (:id, TRUE, GREATEST(:now, :remindAt))
            ON CONFLICT (booking_id, is_reminder) DO NOTHING
            """)
        .param("id", booking.id())
        .param("now", now.atOffset(ZoneOffset.UTC))
        .param("remindAt", checkIn.minus(Duration.ofHours(24)).atOffset(ZoneOffset.UTC))
        .update();
  }

  @Scheduled(
      fixedDelayString = "${booking.email-poll-delay:PT1M}",
      initialDelayString = "${booking.email-poll-delay:PT1M}")
  public void deliverDueEmails() {
    for (int i = 0; i < 20; i++) {
      if (!Boolean.TRUE.equals(transaction.execute(status -> deliverNext()))) {
        return;
      }
    }
  }

  private boolean deliverNext() {
    var now = clock.instant().atOffset(ZoneOffset.UTC);
    var next =
        jdbc.sql(
                """
                SELECT n.booking_id, n.is_reminder, b.booking_code, b.check_in, b.check_out,
                       b.expected_check_in_time, b.total_amount, b.currency,
                       CASE WHEN n.is_reminder THEN b.guest_email_snapshot
                            ELSE b.booker_email_snapshot END AS recipient,
                       b.status = 'CONFIRMED'
                         AND (b.check_in + b.expected_check_in_time)
                             AT TIME ZONE 'Asia/Ho_Chi_Minh' > :now AS can_remind
                FROM booking_email_notifications n
                JOIN bookings b ON b.id = n.booking_id
                WHERE n.due_at <= :now
                ORDER BY n.due_at, n.booking_id, n.is_reminder
                LIMIT 1 FOR UPDATE OF n, b SKIP LOCKED
                """)
            .param("now", now)
            .query(
                (rs, row) ->
                    new Notification(
                        rs.getObject("booking_id", UUID.class),
                        rs.getBoolean("is_reminder"),
                        rs.getBoolean("can_remind"),
                        rs.getString("recipient"),
                        rs.getString("booking_code"),
                        rs.getObject("check_in", LocalDate.class),
                        rs.getObject("check_out", LocalDate.class),
                        rs.getObject("expected_check_in_time", LocalTime.class),
                        rs.getLong("total_amount"),
                        rs.getString("currency")))
            .optional();
    if (next.isEmpty()) {
      return false;
    }

    var notification = next.get();
    try {
      if (!notification.reminder() || notification.canRemind()) {
        send(notification);
      }
    } catch (MailException exception) {
      log.warn("Could not send booking email for {}; will retry", notification.bookingId());
      jdbc.sql(
              """
              UPDATE booking_email_notifications SET due_at = :retryAt
              WHERE booking_id = :id AND is_reminder = :reminder
              """)
          .param("retryAt", now.plusMinutes(5))
          .param("id", notification.bookingId())
          .param("reminder", notification.reminder())
          .update();
      return true;
    }

    jdbc.sql(
            """
            DELETE FROM booking_email_notifications WHERE booking_id = :id AND is_reminder = :reminder
            """)
        .param("id", notification.bookingId())
        .param("reminder", notification.reminder())
        .update();
    return true;
  }

  private void send(Notification notification) {
    var message = new SimpleMailMessage();
    message.setFrom(from);
    message.setTo(notification.recipient());
    String stay =
        """
        Mã đặt phòng: %s
        Nhận phòng: %s ngày %s (giờ Việt Nam)
        Trả phòng: %s
        """
            .formatted(
                notification.code(),
                STAY_TIME.format(notification.checkInTime()),
                STAY_DATE.format(notification.checkIn()),
                STAY_DATE.format(notification.checkOut()));
    if (notification.reminder()) {
      message.setSubject("[Hotel Booking] Nhắc nhận phòng " + notification.code());
      message.setText("Lịch nhận phòng của bạn sắp đến.\n\n" + stay);
    } else {
      message.setSubject("[Hotel Booking] Xác nhận đặt phòng và thanh toán " + notification.code());
      message.setText(
          "Đơn đặt phòng của bạn đã được thanh toán và xác nhận.\n\n"
              + stay
              + "Đã thanh toán: %d %s\n".formatted(notification.amount(), notification.currency()));
    }
    // shortcut: SMTP có thể gửi lặp nếu commit thất bại sau khi máy chủ nhận thư, dùng nhà cung cấp
    // có khóa chống lặp nếu cần.
    mail.send(message);
  }

  private record Notification(
      UUID bookingId,
      boolean reminder,
      boolean canRemind,
      String recipient,
      String code,
      LocalDate checkIn,
      LocalDate checkOut,
      LocalTime checkInTime,
      long amount,
      String currency) {}
}
