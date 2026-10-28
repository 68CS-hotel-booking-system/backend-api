package com.team.hotelbooking.identity.infrastructure.integration;

import com.team.hotelbooking.identity.application.port.VerificationEmailQueue;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class SmtpVerificationEmailQueue implements VerificationEmailQueue {

  private static final Logger log = LoggerFactory.getLogger(SmtpVerificationEmailQueue.class);

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

    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(from);
    message.setTo(recipient);
    message.setSubject("Xác minh tài khoản Hotel Booking");
    message.setText(
        "Mở liên kết để xác minh email:\n" + link + "\n\nLiên kết hết hạn lúc: " + expiresAt);

    try {
      mail.send(message);
    } catch (MailException exception) {
      // User đã commit. Không ném lỗi làm client tưởng
      // toàn bộ đăng ký đã rollback.
      // Không log token, URL hoặc nội dung SMTP exception.
      log.warn("Verification email delivery failed for userId={}", userId);
    }
  }
}
