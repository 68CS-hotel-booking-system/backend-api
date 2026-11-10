package com.team.hotelbooking.booking.infrastructure.integration;

import com.team.hotelbooking.booking.application.port.BookingIdentityPort;
import com.team.hotelbooking.identity.AccountDirectory;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class BookingIdentityAdapter implements BookingIdentityPort {

  private final AccountDirectory accountDirectory;

  public BookingIdentityAdapter(AccountDirectory accountDirectory) {
    this.accountDirectory = accountDirectory;
  }

  @Override
  public Optional<AccountSnapshot> findById(UUID userId) {
    return accountDirectory
        .findById(userId)
        .map(
            account ->
                new AccountSnapshot(
                    account.id(),
                    account.fullName(),
                    account.email(),
                    account.active(),
                    account.emailVerified(),
                    account.roles()));
  }
}
