package com.team.hotelbooking.payment.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "payment_transactions")
@Getter
@Setter
@NoArgsConstructor
public class PaymentTransactionJpaEntity {
  public enum Status {
    PENDING,
    SUCCEEDED,
    FAILED
  }

  @Id private UUID id;

  @Column(nullable = false)
  private UUID bookingId;

  @Column(nullable = false)
  private UUID customerId;

  @Column(nullable = false)
  private long amountVnd;

  @Column(nullable = false, length = 3)
  private String currency;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private Status status;

  @Column(nullable = false, length = 4096)
  private String paymentUrl;

  @Column(length = 2048)
  private String qrContent;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant expiresAt;

  private Instant receivedAt;
  private Instant paidAt;

  @Column(length = 32, unique = true)
  private String providerTransactionNo;

  @Column(length = 2)
  private String responseCode;

  @Column(length = 2)
  private String transactionStatus;

  @Column(nullable = false)
  private boolean bookingConfirmed;
}
