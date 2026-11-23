package com.team.hotelbooking.identity.infrastructure.integration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.mail.MailSendException;
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
            mailSender,
            new SyncTaskExecutor(),
            "http://localhost:5173/reset-password",
            "noreply@hotel.test");

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
            mailSender,
            new SyncTaskExecutor(),
            "noreply@hotel.test",
            "http://localhost:5173/verify-email");

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

  @Test
  @DisplayName(
      "Lỗi SMTP hoặc từ chối TaskExecutor trong afterCommit của quên mật khẩu không lan truyền ra luồng gọi")
  void passwordResetAfterCommitSwallowsSmtpAndExecutorFailures() {
    JavaMailSender mailSender = mock(JavaMailSender.class);
    doThrow(new MailSendException("SMTP down"))
        .when(mailSender)
        .send(any(MimeMessagePreparator.class));

    List<Runnable> submitted = new ArrayList<>();
    TaskExecutor capturingExecutor = submitted::add;

    SmtpPasswordResetEmailQueue queue =
        new SmtpPasswordResetEmailQueue(
            mailSender,
            capturingExecutor,
            "http://localhost:5173/reset-password",
            "noreply@hotel.test");

    TransactionSynchronizationManager.initSynchronization();
    queue.enqueue(
        UUID.randomUUID(), "user@example.com", "reset-tok", Instant.parse("2026-11-23T12:00:00Z"));

    // afterCommit hands off to TaskExecutor and returns without invoking SMTP synchronously.
    assertDoesNotThrow(SmtpEmailQueuesTests::triggerAfterCommit);
    assertEquals(1, submitted.size());

    // Running the background task catches SMTP failure without throwing.
    assertDoesNotThrow(() -> submitted.getFirst().run());
    verify(mailSender).send(any(MimeMessagePreparator.class));

    // Executor rejection also does not propagate out of afterCommit.
    TransactionSynchronizationManager.clearSynchronization();
    TransactionSynchronizationManager.initSynchronization();
    SmtpPasswordResetEmailQueue rejectedQueue =
        new SmtpPasswordResetEmailQueue(
            mailSender,
            task -> {
              throw new TaskRejectedException("Queue full");
            },
            "http://localhost:5173/reset-password",
            "noreply@hotel.test");
    rejectedQueue.enqueue(
        UUID.randomUUID(), "user@example.com", "reset-tok", Instant.parse("2026-11-23T12:00:00Z"));
    assertDoesNotThrow(SmtpEmailQueuesTests::triggerAfterCommit);
  }

  @Test
  @DisplayName(
      "Lỗi SMTP hoặc từ chối TaskExecutor trong afterCommit của email xác minh không lan truyền ra luồng gọi")
  void verificationAfterCommitSwallowsSmtpAndExecutorFailures() {
    JavaMailSender mailSender = mock(JavaMailSender.class);
    doThrow(new RuntimeException("Unexpected SMTP failure"))
        .when(mailSender)
        .send(any(MimeMessagePreparator.class));

    List<Runnable> submitted = new ArrayList<>();
    TaskExecutor capturingExecutor = submitted::add;

    SmtpVerificationEmailQueue queue =
        new SmtpVerificationEmailQueue(
            mailSender,
            capturingExecutor,
            "noreply@hotel.test",
            "http://localhost:5173/verify-email");

    TransactionSynchronizationManager.initSynchronization();
    TransactionSynchronizationManager.setActualTransactionActive(true);
    queue.enqueue(
        UUID.randomUUID(), "user@example.com", "verify-tok", Instant.parse("2026-11-23T12:00:00Z"));

    assertDoesNotThrow(SmtpEmailQueuesTests::triggerAfterCommit);
    assertEquals(1, submitted.size());
    assertDoesNotThrow(() -> submitted.getFirst().run());
    verify(mailSender).send(any(MimeMessagePreparator.class));

    TransactionSynchronizationManager.clearSynchronization();
    TransactionSynchronizationManager.initSynchronization();
    TransactionSynchronizationManager.setActualTransactionActive(true);
    SmtpVerificationEmailQueue rejectedQueue =
        new SmtpVerificationEmailQueue(
            mailSender,
            task -> {
              throw new TaskRejectedException("Queue full");
            },
            "noreply@hotel.test",
            "http://localhost:5173/verify-email");
    rejectedQueue.enqueue(
        UUID.randomUUID(), "user@example.com", "verify-tok", Instant.parse("2026-11-23T12:00:00Z"));
    assertDoesNotThrow(SmtpEmailQueuesTests::triggerAfterCommit);
  }

  @Test
  @DisplayName("Spring Boot tự động tiêm applicationTaskExecutor vào cả hai hàng đợi SMTP")
  void springContextWiresApplicationTaskExecutorIntoBothQueues() {
    new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(TaskExecutionAutoConfiguration.class))
        .withBean(JavaMailSender.class, () -> mock(JavaMailSender.class))
        .withUserConfiguration(SmtpPasswordResetEmailQueue.class, SmtpVerificationEmailQueue.class)
        .withPropertyValues(
            "auth.password-reset-url=http://localhost:5173/reset-password",
            "auth.verification-url=http://localhost:5173/verify-email",
            "auth.mail-from=noreply@hotel.test")
        .run(
            context -> {
              assertTrue(context.containsBean("applicationTaskExecutor"));
              assertTrue(context.getBeansOfType(SmtpPasswordResetEmailQueue.class).size() == 1);
              assertTrue(context.getBeansOfType(SmtpVerificationEmailQueue.class).size() == 1);
            });
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
