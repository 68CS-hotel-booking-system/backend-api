package com.team.hotelbooking.media.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
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
public class HotelImageId implements Serializable {

  @Column(name = "hotel_id", nullable = false)
  private UUID hotelId;

  @Column(name = "media_object_id", nullable = false)
  private UUID mediaId;
}
