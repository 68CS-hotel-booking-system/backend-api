package com.team.hotelbooking.booking.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "booking_items")
@Getter
@Setter
@NoArgsConstructor
public class BookingItemJpaEntity {

  @Id private UUID id;

  @Column(name = "booking_id", nullable = false)
  private UUID bookingId;

  @Column(name = "hotel_id", nullable = false)
  private UUID hotelId;

  @Column(name = "room_type_id", nullable = false)
  private UUID roomTypeId;

  @Column(name = "room_type_name_snapshot", nullable = false, length = 100)
  private String roomTypeNameSnapshot;

  @Column(nullable = false)
  private short quantity;

  @Column(nullable = false)
  private short adults;

  @Column(nullable = false)
  private short children;

  @Column(name = "extra_guests", nullable = false)
  private short extraGuests;

  @Column(name = "room_subtotal_amount", nullable = false)
  private long roomSubtotalAmount;

  @Column(name = "early_check_in_surcharge_amount", nullable = false)
  private long earlyCheckInSurchargeAmount;

  @Column(name = "late_check_out_surcharge_amount", nullable = false)
  private long lateCheckOutSurchargeAmount;

  @Column(name = "extra_guest_surcharge_amount", nullable = false)
  private long extraGuestSurchargeAmount;

  @Column(name = "surcharge_amount", nullable = false)
  private long surchargeAmount;

  @Column(name = "subtotal_amount", nullable = false)
  private long subtotalAmount;
}
