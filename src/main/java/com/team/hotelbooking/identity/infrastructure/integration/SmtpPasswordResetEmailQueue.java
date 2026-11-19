package com.team.hotelbooking.identity.infrastructure.integration;

import com.team.hotelbooking.identity.application.port.PasswordResetEmailQueue;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class SmtpPasswordResetEmailQueue implements PasswordResetEmailQueue {

  private static final DateTimeFormatter EXPIRY_FORMAT =
      DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy")
          .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

  private final JavaMailSender mailSender;
  private final String resetUrl;
  private final String mailFrom;

  public SmtpPasswordResetEmailQueue(
      JavaMailSender mailSender,
      @Value("${auth.password-reset-url}") String resetUrl,
      @Value("${auth.mail-from}") String mailFrom) {

    this.mailSender = mailSender;
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
            String link =
                UriComponentsBuilder.fromUriString(resetUrl)
                    .queryParam("token", rawToken)
                    .build()
                    .encode()
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
          }
        });
  }
}
