package com.team.hotelbooking.payment.infrastructure.integration;

import com.team.hotelbooking.booking.BookingRevenueDirectory;
import com.team.hotelbooking.hotel.HotelDirectory;
import com.team.hotelbooking.identity.AccountDirectory;
import com.team.hotelbooking.payment.application.port.FinanceDashboardPort;
import com.team.hotelbooking.payment.infrastructure.persistence.repository.HotelContractJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FinanceDashboardAdapter implements FinanceDashboardPort {
  private final BookingRevenueDirectory bookings;
  private final HotelDirectory directory;
  private final HotelContractJpaRepository contracts;
  private final NamedParameterJdbcTemplate jdbc;
  private final AccountDirectory accounts;

  @Override
  public java.util.Optional<Viewer> viewer(UUID actorId) {
    return accounts
        .findById(actorId)
        .map(
            a ->
                new Viewer(
                    a.active(),
                    a.emailVerified(),
                    a.roles().contains("ADMIN"),
                    a.roles().contains("HOTEL_OWNER")));
  }

  @Override
  public List<HotelScope> hotels(UUID ownerId) {
    return directory.findFinanceHotels(ownerId).stream()
        .map(h -> new HotelScope(h.id(), h.name(), h.active()))
        .toList();
  }

  @Override
  public Totals summarize(List<UUID> hotelIds, UUID beneficiaryOwnerId) {
    var summary = bookings.summarize(Set.copyOf(hotelIds), beneficiaryOwnerId);
    long collected =
        summary.bookingIds().isEmpty()
            ? 0
            : jdbc.queryForObject(
                """
        SELECT COALESCE(SUM(amount_vnd),0) FROM payment_transactions
        WHERE status = 'SUCCEEDED' AND booking_id IN (:ids)
        """,
                Map.of("ids", summary.bookingIds()),
                Long.class);
    return new Totals(
        summary.bookingCount(),
        summary.bookedAmount(),
        collected,
        summary.completedAmount(),
        summary.platformAmount(),
        summary.ownerAmount(),
        summary.missingCommissionCount());
  }

  @Override
  public List<ContractRevenue> contracts(List<UUID> hotelIds, UUID beneficiaryOwnerId) {
    var rows = bookings.summarizeContracts(Set.copyOf(hotelIds), beneficiaryOwnerId);
    if (rows.isEmpty()) return List.of();
    var terms =
        contracts
            .findAllById(
                rows.stream()
                    .map(BookingRevenueDirectory.ContractRevenue::contractId)
                    .collect(java.util.stream.Collectors.toSet()))
            .stream()
            .collect(java.util.stream.Collectors.toMap(c -> c.getId(), c -> c));
    var hotelNames =
        directory
            .findFinanceHotelsByIds(
                rows.stream()
                    .map(BookingRevenueDirectory.ContractRevenue::hotelId)
                    .collect(java.util.stream.Collectors.toSet()))
            .stream()
            .collect(java.util.stream.Collectors.toMap(HotelDirectory.FinanceHotel::id, h -> h));
    return rows.stream()
        .map(
            row -> {
              var contract = terms.get(row.contractId());
              var hotel = hotelNames.get(row.hotelId());
              return new ContractRevenue(
                  row.contractId(),
                  contract.getContractCode(),
                  row.hotelId(),
                  hotel.name(),
                  row.ownerId(),
                  row.commissionRate(),
                  contract.getStartsAt(),
                  contract.getEndsAt(),
                  row.completedBookingCount(),
                  row.completedAmount(),
                  row.basisAmount(),
                  row.platformAmount(),
                  row.ownerAmount(),
                  beneficiaryOwnerId == null || beneficiaryOwnerId.equals(hotel.ownerId()));
            })
        .toList();
  }

  @Override
  public ContractState contractState(UUID hotelId, Instant at) {
    var effective = contracts.findEffectiveActive(hotelId, at, PageRequest.of(0, 1));
    if (!effective.isEmpty())
      return new ContractState("EFFECTIVE", effective.getFirst().getEndsAt());
    var recent =
        contracts.findAllByHotelId(
            hotelId, PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "endsAt")));
    if (recent.isEmpty()) return new ContractState("MISSING", null);
    var last = recent.getContent().getFirst();
    // Có dự thảo mới không che khuất việc hợp đồng bán đã hết hạn.
    var previous =
        contracts.findAllByHotelIdAndStatus(
            hotelId,
            com.team.hotelbooking.payment.infrastructure.persistence.entity.HotelContractJpaEntity
                .DbContractStatus.ACTIVE,
            PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "endsAt")));
    if (!previous.isEmpty()) {
      var active = previous.getContent().getFirst();
      if (active.getCommissionBasis()
          != com.team.hotelbooking.payment.infrastructure.persistence.entity.HotelContractJpaEntity
              .DbCommissionBasis.AFTER_DISCOUNT)
        return new ContractState("UNSUPPORTED", active.getEndsAt());
      return new ContractState(
          active.getStartsAt().isAfter(at) ? "SCHEDULED" : "EXPIRED", active.getEndsAt());
    }
    return new ContractState(last.getStatus().name(), last.getEndsAt());
  }
}
