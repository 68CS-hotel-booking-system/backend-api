package com.team.hotelbooking.booking.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
public class BookingJpaEntity {

  public enum DbStatus {
    PENDING_PAYMENT,
    CONFIRMED,
    CHECKED_IN,
    CHECKED_OUT,
    CANCELLED,
    EXPIRED,
    NO_SHOW
  }

  @Id private UUID id;

  @Column(name = "booking_code", nullable = false, length = 20, unique = true)
  private String bookingCode;

  @Column(name = "customer_id", nullable = false)
  private UUID customerId;

  @Column(name = "hotel_id", nullable = false)
  private UUID hotelId;

  @Column(name = "booker_name_snapshot", nullable = false, length = 100)
  private String bookerNameSnapshot;

  @Column(name = "booker_email_snapshot", nullable = false, length = 255)
  private String bookerEmailSnapshot;

  @Column(name = "booker_phone_snapshot", nullable = false, length = 20)
  private String bookerPhoneSnapshot;

  @Column(name = "guest_name_snapshot", nullable = false, length = 100)
  private String guestNameSnapshot;

  @Column(name = "guest_email_snapshot", nullable = false, length = 255)
  private String guestEmailSnapshot;

  @Column(name = "guest_phone_snapshot", nullable = false, length = 20)
  private String guestPhoneSnapshot;

  @Column(name = "check_in", nullable = false)
  private LocalDate checkIn;

  @Column(name = "check_out", nullable = false)
  private LocalDate checkOut;

  @Column(nullable = false)
  private short days;

  @Column(nullable = false)
  private short nights;

  @Column(name = "check_in_time_snapshot", nullable = false)
  private LocalTime checkInTimeSnapshot;

  @Column(name = "check_out_time_snapshot", nullable = false)
  private LocalTime checkOutTimeSnapshot;

  @Column(name = "expected_check_in_time", nullable = false)
  private LocalTime expectedCheckInTime;

  @Column(name = "expected_check_out_time", nullable = false)
  private LocalTime expectedCheckOutTime;

  @Column(nullable = false)
  private short adults;

  @Column(nullable = false)
  private short children;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false, columnDefinition = "booking_status__t")
  private DbStatus status;

  @Column(nullable = false, length = 3)
  private String currency = "VND";

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

  @Column(name = "discount_amount", nullable = false)
  private long discountAmount;

  @Column(name = "total_amount", nullable = false)
  private long totalAmount;

  @Column(name = "expires_at")
  private Instant expiresAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version
  @Column(nullable = false)
  private Long version;
}
