package com.team.hotelbooking.hotel.infrastructure.integration;

import com.team.hotelbooking.hotel.application.port.IdentityAccounts;
import com.team.hotelbooking.identity.AccountDirectory;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class IdentityAccountsAdapter implements IdentityAccounts {

  private final AccountDirectory accountDirectory;

  public IdentityAccountsAdapter(AccountDirectory accountDirectory) {
    this.accountDirectory = accountDirectory;
  }

  @Override
  public Optional<AccountSnapshot> findById(UUID userId) {
    return accountDirectory.findById(userId).map(IdentityAccountsAdapter::toSnapshot);
  }

  @Override
  public Optional<AccountSnapshot> findByEmail(String email) {
    return accountDirectory.findByEmail(email).map(IdentityAccountsAdapter::toSnapshot);
  }

  private static AccountSnapshot toSnapshot(AccountDirectory.AccountInfo account) {
    return new AccountSnapshot(
        account.id(),
        account.fullName(),
        account.email(),
        account.active(),
        account.emailVerified(),
        account.roles());
  }
}
