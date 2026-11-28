package com.team.hotelbooking.payment.infrastructure.persistence.entity;

import com.team.hotelbooking.payment.domain.model.Refund;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "payment_refund_actions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RefundActionJpaEntity {
  @Id private UUID id;
  private UUID refundId;
  private UUID actorId;

  @Enumerated(EnumType.STRING)
  @Column(length = 16)
  private Refund.Status fromStatus;

  @Enumerated(EnumType.STRING)
  @Column(length = 16)
  private Refund.Status toStatus;

  @Column(length = 1000)
  private String note;

  @Column(length = 100)
  private String settlementReference;

  private Instant createdAt;
  private long version;
}
