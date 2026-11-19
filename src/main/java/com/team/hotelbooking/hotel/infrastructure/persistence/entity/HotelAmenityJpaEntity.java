package com.team.hotelbooking.hotel.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Không map amenity_category: cột cố định 'HOTEL' do DB điền mặc định để FK kép kiểm tra nhóm.
@Entity
@Table(name = "hotel_amenities")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class HotelAmenityJpaEntity {

  @EmbeddedId private Key id;

  @Embeddable
  @Getter
  @EqualsAndHashCode
  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  @AllArgsConstructor
  public static class Key implements Serializable {

    @Column(name = "hotel_id", nullable = false)
    private UUID hotelId;

    @Column(name = "amenity_id", nullable = false)
    private UUID amenityId;
  }
}
