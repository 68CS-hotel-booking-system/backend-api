package com.team.hotelbooking.payment.application.service;

import com.team.hotelbooking.payment.application.command.GetContractsQuery;
import com.team.hotelbooking.payment.application.port.PaymentHotelPort;
import com.team.hotelbooking.payment.application.result.HotelContractResult;
import com.team.hotelbooking.payment.application.usecase.GetContractsUseCase;
import com.team.hotelbooking.payment.domain.exception.FinanceException;
import com.team.hotelbooking.payment.domain.model.HotelContract;
import com.team.hotelbooking.payment.infrastructure.persistence.entity.HotelContractJpaEntity;
import com.team.hotelbooking.payment.infrastructure.persistence.repository.HotelContractJpaRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;

@RequiredArgsConstructor
public class GetContractsService implements GetContractsUseCase {

  private final HotelContractJpaRepository contractJpaRepository;
  private final PaymentHotelPort paymentHotelPort;

  private final java.time.Clock clock;

  @Override
  public Page<HotelContractResult> execute(GetContractsQuery query) {
    UUID ownerUserId =
        paymentHotelPort
            .findOwnerUserId(query.hotelId())
            .orElseThrow(() -> FinanceException.hotelNotFound("Không tìm thấy khách sạn."));

    // Chỉ admin hoặc chủ sở hữu hiện tại được xem hợp đồng khách sạn.
    boolean isOwner =
        query.requestingUserId() != null && ownerUserId.equals(query.requestingUserId());
    if (!query.isAdmin() && !isOwner) {
      throw FinanceException.accessDenied(
          "Bạn không có quyền xem thông tin hợp đồng của khách sạn này.");
    }

    var now = clock.instant();
    Page<HotelContractJpaEntity> page =
        contractJpaRepository.findAll(
            (root, criteria, cb) -> {
              var hotel = cb.equal(root.get("hotelId"), query.hotelId());
              if (query.status() == null) return hotel;
              var active =
                  cb.equal(root.get("status"), HotelContractJpaEntity.DbContractStatus.ACTIVE);
              var ended = cb.lessThanOrEqualTo(root.<java.time.Instant>get("endsAt"), now);
              if (query.status() == HotelContract.ContractStatus.EXPIRED) {
                return cb.and(
                    hotel,
                    cb.or(
                        cb.equal(
                            root.get("status"), HotelContractJpaEntity.DbContractStatus.EXPIRED),
                        cb.and(active, ended)));
              }
              if (query.status() == HotelContract.ContractStatus.ACTIVE)
                return cb.and(hotel, active, cb.not(ended));
              return cb.and(
                  hotel,
                  cb.equal(
                      root.get("status"),
                      HotelContractJpaEntity.DbContractStatus.valueOf(query.status().name())));
            },
            query.pageable());

    return page.map(
        entity -> {
          HotelContract domain =
              new HotelContract(
                  entity.getId(),
                  entity.getContractCode(),
                  entity.getHotelId(),
                  entity.getOwnerUserId(),
                  entity.getCommissionRate(),
                  HotelContract.CommissionBasis.valueOf(entity.getCommissionBasis().name()),
                  entity.getStartsAt(),
                  entity.getEndsAt(),
                  entity.getStatus() == HotelContractJpaEntity.DbContractStatus.ACTIVE
                          && !entity.getEndsAt().isAfter(now)
                      ? HotelContract.ContractStatus.EXPIRED
                      : HotelContract.ContractStatus.valueOf(entity.getStatus().name()),
                  entity.getCreatedByUserId(),
                  entity.getCreatedAt(),
                  entity.getUpdatedAt());
          return HotelContractResult.from(domain);
        });
  }
}
