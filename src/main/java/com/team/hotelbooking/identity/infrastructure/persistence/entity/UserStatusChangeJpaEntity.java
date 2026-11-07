package com.team.hotelbooking.identity.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_status_changes")
@Getter
@Setter
@NoArgsConstructor
public class UserStatusChangeJpaEntity {

  @Id private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "actor_id", nullable = false)
  private UUID actorId;

  @Column(name = "previous_status", nullable = false, length = 20)
  private String previousStatus;

  @Column(name = "new_status", nullable = false, length = 20)
  private String newStatus;

  @Column(length = 500)
  private String reason;

  @Column(name = "changed_at", nullable = false)
  private Instant changedAt;
}
