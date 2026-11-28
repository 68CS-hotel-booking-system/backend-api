package com.team.hotelbooking.booking.review.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "guest_reviews")
@Getter
@Setter
@NoArgsConstructor
public class GuestReviewJpaEntity {
  @Id private UUID id;

  @Column(nullable = false, unique = true)
  private UUID bookingId;

  @Column(nullable = false)
  private UUID customerId;

  @Column(nullable = false)
  private UUID hotelId;

  @Column(nullable = false, length = 20)
  private String bookingCode;

  @Column(nullable = false, length = 110)
  private String authorDisplayName;

  @Column(nullable = false)
  private short rating;

  @Column(nullable = false, length = 2000)
  private String content;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(length = 2000)
  private String reply;

  private UUID repliedBy;
  private Instant repliedAt;
  @Version private Long version;
}
