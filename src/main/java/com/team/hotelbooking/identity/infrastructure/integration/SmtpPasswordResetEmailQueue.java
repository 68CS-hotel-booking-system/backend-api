package com.team.hotelbooking.identity.infrastructure.integration;

import com.team.hotelbooking.identity.application.port.PasswordResetEmailQueue;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class SmtpPasswordResetEmailQueue implements PasswordResetEmailQueue {

  private static final Logger log = LoggerFactory.getLogger(SmtpPasswordResetEmailQueue.class);

  private static final DateTimeFormatter EXPIRY_FORMAT =
      DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy")
          .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

  private final JavaMailSender mailSender;
  private final TaskExecutor taskExecutor;
  private final String resetUrl;
  private final String mailFrom;

  public SmtpPasswordResetEmailQueue(
      JavaMailSender mailSender,
      @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor,
      @Value("${auth.password-reset-url}") String resetUrl,
      @Value("${auth.mail-from}") String mailFrom) {

    this.mailSender = mailSender;
    this.taskExecutor = taskExecutor;
    this.resetUrl = resetUrl;
    this.mailFrom = mailFrom;
  }

  @Override
  public void enqueue(UUID userId, String recipient, String rawToken, Instant expiresAt) {

    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      throw new IllegalStateException("Gửi email đặt lại mật khẩu yêu cầu transaction.");
    }

    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            // ponytail: async TaskExecutor handoff has no retry or delivery guarantee; upgrade to a
            // transactional outbox if durable delivery is required.
            try {
              taskExecutor.execute(() -> send(userId, recipient, rawToken, expiresAt));
            } catch (RuntimeException exception) {
              log.warn(
                  "Password reset email dispatch failed for userId={} ({}: {})",
                  userId,
                  exception.getClass().getSimpleName(),
                  exception.getMessage());
            }
          }
        });
  }

  private void send(UUID userId, String recipient, String rawToken, Instant expiresAt) {
    String encodedToken = URLEncoder.encode(rawToken, StandardCharsets.UTF_8).replace("+", "%20");
    String baseUrl =
        UriComponentsBuilder.fromUriString(resetUrl).fragment(null).build().encode().toUriString();
    String link =
        UriComponentsBuilder.fromUriString(baseUrl)
            .fragment("token=" + encodedToken)
            .build(true)
            .toUriString();

    String plainText =
        """
        Xin chào,

        Chúng tôi đã nhận được yêu cầu đặt lại mật khẩu cho tài khoản Hotel Booking của bạn.

        Để tạo mật khẩu mới, vui lòng mở liên kết dưới đây:
        %s

        Liên kết có hiệu lực đến %s (giờ Việt Nam, UTC+7).
        Nếu không mở được liên kết, bạn có thể sao chép và dán vào thanh địa chỉ trình duyệt.

        Vui lòng không chia sẻ liên kết này với người khác.
        Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này.
        Mật khẩu hiện tại của bạn sẽ không thay đổi cho đến khi bạn hoàn tất đặt lại mật khẩu.

        Trân trọng,
        Đội ngũ Hotel Booking

        Đây là email tự động. Vui lòng không trả lời email này.
        """
            .formatted(link, EXPIRY_FORMAT.format(expiresAt));

    try {
      mailSender.send(
          mimeMessage -> {
            MimeMessageHelper message =
                new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
            message.setFrom(mailFrom, "Hotel Booking");
            message.setTo(recipient);
            message.setSubject("[Hotel Booking] Yêu cầu đặt lại mật khẩu");
            message.setText(
                plainText,
                AccountEmailTemplate.render(
                    "Đặt lại mật khẩu",
                    "Chúng tôi đã nhận được yêu cầu đặt lại mật khẩu cho tài khoản "
                        + "Hotel Booking của bạn. Chọn nút bên dưới để tạo mật khẩu mới.",
                    "Đặt lại mật khẩu",
                    link,
                    EXPIRY_FORMAT.format(expiresAt),
                    "Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này. "
                        + "Mật khẩu hiện tại chỉ thay đổi khi bạn hoàn tất đặt lại mật khẩu."));
          });
    } catch (Exception exception) {
      log.warn(
          "Password reset email delivery failed for userId={} ({}: {})",
          userId,
          exception.getClass().getSimpleName(),
          exception.getMessage());
    }
  }
}
