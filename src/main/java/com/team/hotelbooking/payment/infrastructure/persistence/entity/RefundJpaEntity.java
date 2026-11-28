package com.team.hotelbooking.payment.infrastructure.persistence.entity;

import com.team.hotelbooking.payment.domain.model.Refund;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "payment_refunds")
@Getter
@Setter
@NoArgsConstructor
public class RefundJpaEntity {
  @Id private UUID id;
  private UUID paymentId;
  private UUID bookingId;
  private UUID customerId;
  private UUID hotelId;

  @Column(length = 20)
  private String bookingCode;

  @Column(length = 100)
  private String bookerName;

  @Column(length = 255)
  private String bookerEmail;

  @Column(length = 20)
  private String bookerPhone;

  private long amount;

  @Column(length = 3)
  private String currency;

  @Column(length = 40)
  private String reason;

  @Column(length = 1000)
  private String customerNote;

  @Enumerated(EnumType.STRING)
  @Column(length = 16)
  private Refund.Status status;

  @Column(length = 100)
  private String settlementReference;

  @Column(length = 1000)
  private String note;

  private Instant createdAt;
  private Instant updatedAt;
  private Instant completedAt;
  @Version private Long version;
}
