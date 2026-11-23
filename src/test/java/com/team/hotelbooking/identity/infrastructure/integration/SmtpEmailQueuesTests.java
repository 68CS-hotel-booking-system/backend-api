package com.team.hotelbooking.identity.infrastructure.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.time.Instant;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@DisplayName("Hàng đợi gửi email xác minh và đặt lại mật khẩu qua SMTP")
class SmtpEmailQueuesTests {

  @AfterEach
  void cleanUpTransactionSynchronization() {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.clearSynchronization();
    }
    TransactionSynchronizationManager.setActualTransactionActive(false);
  }

  @Test
  @DisplayName(
      "Liên kết đặt lại mật khẩu đặt token trong fragment (#token=...), mã hóa ký tự đặc biệt và không dùng query (?token=)")
  void passwordResetLinkUsesEncodedFragmentInsteadOfQuery() throws Exception {
    JavaMailSender mailSender = mock(JavaMailSender.class);
    AtomicReference<MimeMessage> captured = captureSentMessage(mailSender);

    SmtpPasswordResetEmailQueue queue =
        new SmtpPasswordResetEmailQueue(
            mailSender, "http://localhost:5173/reset-password", "noreply@hotel.test");

    String rawToken = "tok+en/1=2&3#4%20end";
    String expectedLink =
        "http://localhost:5173/reset-password#token=tok%2Ben%2F1%3D2%263%234%2520end";

    TransactionSynchronizationManager.initSynchronization();
    queue.enqueue(
        UUID.randomUUID(), "user@example.com", rawToken, Instant.parse("2026-11-23T12:00:00Z"));
    triggerAfterCommit();

    String body = extractAllText(captured.get());
    assertTrue(body.contains(expectedLink), "Expected fragment link in email body: " + body);
    assertFalse(body.contains("?token="), "Token must not appear in query string: " + body);
  }

  @Test
  @DisplayName(
      "Liên kết xác minh email đặt token trong fragment (#token=...), mã hóa ký tự đặc biệt và không dùng query (?token=)")
  void verificationLinkUsesEncodedFragmentInsteadOfQuery() throws Exception {
    JavaMailSender mailSender = mock(JavaMailSender.class);
    AtomicReference<MimeMessage> captured = captureSentMessage(mailSender);

    SmtpVerificationEmailQueue queue =
        new SmtpVerificationEmailQueue(
            mailSender, "noreply@hotel.test", "http://localhost:5173/verify-email");

    String rawToken = "ver+if/a=b&c#d%20z";
    String expectedLink = "http://localhost:5173/verify-email#token=ver%2Bif%2Fa%3Db%26c%23d%2520z";

    TransactionSynchronizationManager.initSynchronization();
    TransactionSynchronizationManager.setActualTransactionActive(true);
    queue.enqueue(
        UUID.randomUUID(), "user@example.com", rawToken, Instant.parse("2026-11-23T12:00:00Z"));
    triggerAfterCommit();

    String body = extractAllText(captured.get());
    assertTrue(body.contains(expectedLink), "Expected fragment link in email body: " + body);
    assertFalse(body.contains("?token="), "Token must not appear in query string: " + body);
  }

  private static AtomicReference<MimeMessage> captureSentMessage(JavaMailSender mailSender) {
    AtomicReference<MimeMessage> captured = new AtomicReference<>();
    doAnswer(
            invocation -> {
              MimeMessagePreparator preparator = invocation.getArgument(0);
              MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
              preparator.prepare(mimeMessage);
              captured.set(mimeMessage);
              return null;
            })
        .when(mailSender)
        .send(any(MimeMessagePreparator.class));
    return captured;
  }

  private static void triggerAfterCommit() {
    for (TransactionSynchronization sync :
        TransactionSynchronizationManager.getSynchronizations()) {
      sync.afterCommit();
    }
  }

  private static String extractAllText(MimeMessage message) throws Exception {
    StringBuilder out = new StringBuilder();
    collectText(message.getContent(), out);
    return out.toString();
  }

  private static void collectText(Object content, StringBuilder out) throws Exception {
    if (content instanceof String text) {
      out.append(text).append('\n');
    } else if (content instanceof MimeMultipart multipart) {
      for (int i = 0; i < multipart.getCount(); i++) {
        collectText(multipart.getBodyPart(i).getContent(), out);
      }
    }
  }
}
