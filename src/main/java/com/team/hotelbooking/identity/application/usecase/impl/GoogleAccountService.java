package com.team.hotelbooking.identity.application.usecase.impl;

import com.team.hotelbooking.identity.application.port.OpaqueTokens;
import com.team.hotelbooking.identity.application.port.PasswordHasher;
import com.team.hotelbooking.identity.application.result.LoginResult;
import com.team.hotelbooking.identity.domain.exception.IdentityErrorCode;
import com.team.hotelbooking.identity.domain.exception.IdentityException;
import com.team.hotelbooking.identity.domain.model.User;
import com.team.hotelbooking.identity.domain.repository.GoogleIdentityRepository;
import com.team.hotelbooking.identity.domain.repository.UserRepository;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GoogleAccountService {
  private final GoogleIdentityRepository identities;
  private final UserRepository users;
  private final OpaqueTokens tokens;
  private final PasswordHasher passwords;
  private final LoginSessionIssuer sessions;
  private final Clock clock;

  @Transactional
  public LoginResult signIn(VerifiedAccount account, String rawPhone) {
    var existingUserId = identities.findUserId(account.subject());
    if (existingUserId.isPresent()) {
      User user =
          users
              .findByIdForUpdate(existingUserId.get())
              .orElseThrow(() -> new IdentityException(IdentityErrorCode.ACCOUNT_UNAVAILABLE));
      return sessions.issue(user, false);
    }

    String email = User.normalizeEmail(account.email());
    // Google is authoritative for verified Gmail and Google Workspace addresses.
    if (!account.authoritativeEmail()) {
      throw IdentityException.invalid(
          "Hãy dùng Gmail hoặc Google Workspace, hoặc đăng ký bằng email để xác minh địa chỉ này.");
    }
    if (users.existsByEmail(email)) {
      throw new IdentityException(
          IdentityErrorCode.EMAIL_ALREADY_USED,
          "Email này đã có tài khoản StayHub. Hãy đăng nhập bằng mật khẩu hoặc chọn Quên mật khẩu.");
    }
    if (rawPhone == null || rawPhone.isBlank()) {
      throw new IdentityException(IdentityErrorCode.GOOGLE_PHONE_REQUIRED);
    }
    String phone = User.normalizePhone(rawPhone);
    if (users.existsByPhone(phone)) {
      throw new IdentityException(IdentityErrorCode.PHONE_ALREADY_USED);
    }

    var now = clock.instant();
    String name = account.name() == null ? "" : account.name().strip();
    if (name.isBlank()) {
      name = "Khách StayHub";
    }
    if (name.length() > 100) {
      name = name.substring(0, Character.isHighSurrogate(name.charAt(99)) ? 99 : 100);
    }
    String unusablePassword = passwords.hash(tokens.generate().raw());
    User user =
        users.add(
            User.register(UUID.randomUUID(), email, unusablePassword, name, phone, now)
                .verifyEmail(now));
    if (!identities.add(account.subject(), user.id())) {
      throw new IdentityException(
          IdentityErrorCode.CONFLICT, "Tài khoản Google đã được tạo. Vui lòng thử lại.");
    }
    return sessions.issue(user, false);
  }

  public record VerifiedAccount(
      String subject, String email, String name, boolean authoritativeEmail) {}
}
