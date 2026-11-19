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
    return accountDirectory
        .findById(userId)
        .map(
            account ->
                new AccountSnapshot(
                    account.id(), account.active(), account.emailVerified(), account.roles()));
  }
}
