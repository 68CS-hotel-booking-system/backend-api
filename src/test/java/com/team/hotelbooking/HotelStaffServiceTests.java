package com.team.hotelbooking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.team.hotelbooking.hotel.application.port.HotelMemberStore;
import com.team.hotelbooking.hotel.application.port.HotelMemberStore.MemberRecord;
import com.team.hotelbooking.hotel.application.port.IdentityAccounts;
import com.team.hotelbooking.hotel.application.port.IdentityAccounts.AccountSnapshot;
import com.team.hotelbooking.hotel.application.usecase.impl.HotelStaffService;
import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.hotel.domain.model.HotelMemberRole;
import com.team.hotelbooking.hotel.domain.repository.HotelRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("HotelStaffService unit tests")
class HotelStaffServiceTests {

  private static final Instant NOW = Instant.parse("2027-02-01T08:00:00Z");

  private HotelRepository hotels;
  private HotelMemberStore members;
  private IdentityAccounts accounts;
  private HotelStaffService service;

  private UUID ownerId, managerId, receptionistId, targetId, adminId, hotelId;
  private Hotel hotel;

  @BeforeEach
  void setUp() {
    hotels = mock(HotelRepository.class);
    members = mock(HotelMemberStore.class);
    accounts = mock(IdentityAccounts.class);
    service =
        new HotelStaffService(hotels, members, accounts, Clock.fixed(NOW, ZoneOffset.UTC), 100);

    ownerId = UUID.randomUUID();
    managerId = UUID.randomUUID();
    receptionistId = UUID.randomUUID();
    targetId = UUID.randomUUID();
    adminId = UUID.randomUUID();
    hotelId = UUID.randomUUID();

    hotel =
        new Hotel(
            hotelId,
            ownerId,
            UUID.randomUUID(),
            "StayHub Test",
            "stayhub-test",
            null,
            "123 Street",
            null,
            null,
            null,
            null,
            (short) 4,
            LocalTime.of(14, 0),
            LocalTime.of(12, 0),
            Hotel.Status.ACTIVE,
            NOW,
            NOW,
            0L);

    when(hotels.findById(hotelId)).thenReturn(Optional.of(hotel));
    when(hotels.findByIdForUpdate(hotelId)).thenReturn(Optional.of(hotel));
    when(accounts.findById(ownerId))
        .thenReturn(
            Optional.of(
                new AccountSnapshot(
                    ownerId, "Owner", "owner@test.vn", true, true, Set.of("HOTEL_OWNER"))));
    when(accounts.findById(managerId))
        .thenReturn(
            Optional.of(
                new AccountSnapshot(
                    managerId, "Manager", "mgr@test.vn", true, true, Set.of("CUSTOMER"))));
    when(accounts.findById(adminId))
        .thenReturn(
            Optional.of(
                new AccountSnapshot(
                    adminId, "Admin", "admin@test.vn", true, true, Set.of("ADMIN"))));
    when(accounts.findByEmail("staff@test.vn"))
        .thenReturn(
            Optional.of(
                new AccountSnapshot(
                    targetId, "Staff", "staff@test.vn", true, true, Set.of("CUSTOMER"))));
  }

  @Test
  void ownerCanAddNewReceptionist() {
    MemberRecord inserted =
        new MemberRecord(
            UUID.randomUUID(),
            hotelId,
            targetId,
            "Staff",
            "staff@test.vn",
            HotelMemberRole.RECEPTIONIST,
            true,
            NOW,
            NOW,
            0L);
    when(members.hasOtherActiveMembership(hotelId, targetId, null)).thenReturn(false);
    when(members.findByHotelUserRole(hotelId, targetId, HotelMemberRole.RECEPTIONIST))
        .thenReturn(Optional.empty());
    when(members.insert(hotelId, targetId, HotelMemberRole.RECEPTIONIST, ownerId, NOW))
        .thenReturn(inserted);

    MemberRecord result =
        service.addMember(ownerId, hotelId, "staff@test.vn", HotelMemberRole.RECEPTIONIST);

    assertThat(result.userId()).isEqualTo(targetId);
    assertThat(result.active()).isTrue();
  }

  @Test
  void managerCannotAssignAnotherManagerAndAdminIsForbidden() {
    when(hotels.findActiveMemberRoles(hotelId, managerId))
        .thenReturn(Set.of(HotelMemberRole.HOTEL_MANAGER));

    assertThatThrownBy(
            () ->
                service.addMember(
                    managerId, hotelId, "staff@test.vn", HotelMemberRole.HOTEL_MANAGER))
        .isInstanceOf(HotelException.class)
        .extracting("code")
        .isEqualTo(HotelErrorCode.ACCESS_DENIED);

    assertThatThrownBy(
            () ->
                service.addMember(adminId, hotelId, "staff@test.vn", HotelMemberRole.RECEPTIONIST))
        .isInstanceOf(HotelException.class)
        .extracting("code")
        .isEqualTo(HotelErrorCode.ACCESS_DENIED);
  }

  @Test
  void unverifiedEmailReturnsFieldErrorAndStaleVersionReturnsConflict() {
    when(accounts.findByEmail("unverified@test.vn"))
        .thenReturn(
            Optional.of(
                new AccountSnapshot(
                    targetId, "U", "unverified@test.vn", true, false, Set.of("CUSTOMER"))));

    assertThatThrownBy(
            () ->
                service.addMember(
                    ownerId, hotelId, "unverified@test.vn", HotelMemberRole.RECEPTIONIST))
        .isInstanceOf(HotelException.class)
        .satisfies(
            ex -> {
              HotelException he = (HotelException) ex;
              assertThat(he.code()).isEqualTo(HotelErrorCode.STAFF_ACCOUNT_NOT_ELIGIBLE);
              assertThat(he.fieldIssues()).hasSize(1);
              assertThat(he.fieldIssues().getFirst().field()).isEqualTo("email");
            });

    UUID memberId = UUID.randomUUID();
    when(members.findById(hotelId, memberId))
        .thenReturn(
            Optional.of(
                new MemberRecord(
                    memberId,
                    hotelId,
                    receptionistId,
                    "R",
                    "r@test.vn",
                    HotelMemberRole.RECEPTIONIST,
                    true,
                    NOW,
                    NOW,
                    1L)));
    when(members.updateStatus(eq(memberId), eq(false), eq(0L), eq(ownerId), any())).thenReturn(0);

    assertThatThrownBy(() -> service.updateMemberStatus(ownerId, hotelId, memberId, "REVOKED", 0L))
        .isInstanceOf(HotelException.class)
        .extracting("code")
        .isEqualTo(HotelErrorCode.HOTEL_MEMBER_VERSION_CONFLICT);
  }
}
