package com.team.hotelbooking.identity.infrastructure.integration;

import com.team.hotelbooking.identity.application.port.PasswordResetEmailQueue;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class SmtpPasswordResetEmailQueue implements PasswordResetEmailQueue {

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

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(recipient);
            message.setSubject("Đặt lại mật khẩu");
            message.setText(
                "Mở liên kết sau để đặt lại mật khẩu:\n"
                    + link
                    + "\n\nLiên kết hết hạn lúc: "
                    + expiresAt);

            mailSender.send(message);
          }
        });
  }
}
