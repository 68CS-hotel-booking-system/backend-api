package com.team.hotelbooking.identity.infrastructure.integration;

import com.team.hotelbooking.identity.application.port.VerificationEmailQueue;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class SmtpVerificationEmailQueue implements VerificationEmailQueue {

  private static final Logger log = LoggerFactory.getLogger(SmtpVerificationEmailQueue.class);

  private static final DateTimeFormatter EXPIRY_FORMAT =
      DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy")
          .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

  private final JavaMailSender mail;
  private final String from;
  private final URI verificationUrl;

  public SmtpVerificationEmailQueue(
      JavaMailSender mail,
      @Value("${auth.mail-from}") String from,
      @Value("${auth.verification-url}") String verificationUrl) {

    URI uri = URI.create(verificationUrl);

    if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
        || uri.getHost() == null
        || uri.getRawQuery() != null
        || uri.getRawFragment() != null
        || uri.getUserInfo() != null) {

      throw new IllegalArgumentException("Invalid auth.verification-url");
    }

    this.mail = mail;
    this.from = from;
    this.verificationUrl = uri;
  }

  @Override
  public void enqueue(UUID userId, String recipient, String rawToken, Instant expiresAt) {

    if (!TransactionSynchronizationManager.isActualTransactionActive()) {

      throw new IllegalStateException("Verification email requires an active transaction.");
    }

    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            send(userId, recipient, rawToken, expiresAt);
          }
        });
  }

  private void send(UUID userId, String recipient, String rawToken, Instant expiresAt) {

    String link =
        UriComponentsBuilder.fromUri(verificationUrl)
            .queryParam("token", rawToken)
            .build()
            .encode()
            .toUriString();

    String plainText =
        """
        Xin chào,

        Cảm ơn bạn đã đăng ký tài khoản tại Hotel Booking.

        Để hoàn tất đăng ký và xác minh địa chỉ email, vui lòng mở liên kết dưới đây:
        %s

        Liên kết có hiệu lực đến %s (giờ Việt Nam, UTC+7).
        Nếu không mở được liên kết, bạn có thể sao chép và dán vào thanh địa chỉ trình duyệt.

        Vui lòng không chia sẻ liên kết này với người khác.
        Nếu bạn không thực hiện đăng ký, bạn có thể bỏ qua email này.

        Trân trọng,
        Đội ngũ Hotel Booking

        Đây là email tự động. Vui lòng không trả lời email này.
        """
            .formatted(link, EXPIRY_FORMAT.format(expiresAt));

    try {
      mail.send(
          mimeMessage -> {
            MimeMessageHelper message =
                new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
            message.setFrom(from, "Hotel Booking");
            message.setTo(recipient);
            message.setSubject("[Hotel Booking] Xác minh địa chỉ email của bạn");
            message.setText(
                plainText,
                AccountEmailTemplate.render(
                    "Xác minh email",
                    "Cảm ơn bạn đã đăng ký tài khoản tại Hotel Booking. "
                        + "Vui lòng xác minh địa chỉ email để hoàn tất đăng ký.",
                    "Xác minh email",
                    link,
                    EXPIRY_FORMAT.format(expiresAt),
                    "Nếu bạn không thực hiện đăng ký, bạn có thể bỏ qua email này."));
          });
    } catch (MailException exception) {
      // User đã commit. Không ném lỗi làm client tưởng
      // toàn bộ đăng ký đã rollback.
      // Không log token, URL hoặc nội dung SMTP exception.
      log.warn("Verification email delivery failed for userId={}", userId);
    }
  }
}
