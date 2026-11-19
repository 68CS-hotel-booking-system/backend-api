package com.team.hotelbooking.booking.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class BookingNightPriceIdEmbeddable implements Serializable {

  @Column(name = "booking_item_id", nullable = false)
  private UUID bookingItemId;

  @Column(name = "stay_date", nullable = false)
  private LocalDate stayDate;
}
