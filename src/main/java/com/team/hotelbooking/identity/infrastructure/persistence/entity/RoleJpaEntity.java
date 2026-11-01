package com.team.hotelbooking.identity.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "roles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoleJpaEntity {

  @Id private Integer id;

  @Column(name = "role_key", nullable = false, length = 50)
  private String roleKey;

  @Column(name = "is_active", nullable = false)
  private boolean active;
}
