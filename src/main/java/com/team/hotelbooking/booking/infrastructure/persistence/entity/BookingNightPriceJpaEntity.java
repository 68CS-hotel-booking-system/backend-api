package com.team.hotelbooking.booking.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "booking_night_prices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingNightPriceJpaEntity {

  @EmbeddedId private BookingNightPriceIdEmbeddable id;

  @Column(name = "unit_price", nullable = false)
  private long unitPrice;
}
