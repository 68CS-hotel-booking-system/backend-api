package com.team.hotelbooking.identity.infrastructure.integration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.util.HtmlUtils;

final class AccountEmailTemplate {

  private static final String TEMPLATE = loadTemplate();

  private AccountEmailTemplate() {}

  static String render(
      String title,
      String introduction,
      String actionLabel,
      String actionUrl,
      String expiresAt,
      String notice) {
    return TEMPLATE
        .replace("{{title}}", escape(title))
        .replace("{{introduction}}", escape(introduction))
        .replace("{{actionLabel}}", escape(actionLabel))
        .replace("{{actionUrl}}", escape(actionUrl))
        .replace("{{expiresAt}}", escape(expiresAt))
        .replace("{{notice}}", escape(notice));
  }

  private static String escape(String value) {
    return HtmlUtils.htmlEscape(value, StandardCharsets.UTF_8.name());
  }

  private static String loadTemplate() {
    try {
      return new ClassPathResource("mail/account-action.html")
          .getContentAsString(StandardCharsets.UTF_8);
    } catch (IOException exception) {
      throw new IllegalStateException("Không thể tải mẫu email tài khoản.", exception);
    }
  }
}
