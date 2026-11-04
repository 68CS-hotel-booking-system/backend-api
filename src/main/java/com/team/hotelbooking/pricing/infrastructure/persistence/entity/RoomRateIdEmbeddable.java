package com.team.hotelbooking.pricing.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoomRateIdEmbeddable implements Serializable {

  @Column(name = "room_type_id", nullable = false)
  private UUID roomTypeId;

  @Column(name = "rate_date", nullable = false)
  private LocalDate rateDate;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    RoomRateIdEmbeddable that = (RoomRateIdEmbeddable) o;
    return Objects.equals(roomTypeId, that.roomTypeId) && Objects.equals(rateDate, that.rateDate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(roomTypeId, rateDate);
  }
}
