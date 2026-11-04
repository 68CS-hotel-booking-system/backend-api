package com.team.hotelbooking.payment.application.service;

import com.team.hotelbooking.payment.application.command.GetContractsQuery;
import com.team.hotelbooking.payment.application.port.PaymentHotelPort;
import com.team.hotelbooking.payment.application.result.HotelContractResult;
import com.team.hotelbooking.payment.application.usecase.GetContractsUseCase;
import com.team.hotelbooking.payment.domain.exception.FinanceException;
import com.team.hotelbooking.payment.domain.model.HotelContract;
import com.team.hotelbooking.payment.infrastructure.persistence.entity.HotelContractJpaEntity;
import com.team.hotelbooking.payment.infrastructure.persistence.repository.HotelContractJpaRepository;
import com.team.hotelbooking.payment.infrastructure.persistence.repository.HotelFinancialAccessJpaRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;

@RequiredArgsConstructor
public class GetContractsService implements GetContractsUseCase {

  private final HotelContractJpaRepository contractJpaRepository;
  private final PaymentHotelPort paymentHotelPort;
  private final HotelFinancialAccessJpaRepository financialAccessJpaRepository;

  @Override
  public Page<HotelContractResult> execute(GetContractsQuery query) {
    UUID ownerUserId =
        paymentHotelPort
            .findOwnerUserId(query.hotelId())
            .orElseThrow(() -> FinanceException.hotelNotFound("Không tìm thấy khách sạn."));

    // Kiểm tra quyền: Admin, Chủ sở hữu khách sạn (Owner), hoặc có quyền tài chính
    boolean isOwner =
        query.requestingUserId() != null && ownerUserId.equals(query.requestingUserId());
    boolean hasFinanceAccess =
        query.requestingUserId() != null
            && financialAccessJpaRepository.existsByHotelIdAndUserIdAndGrantedTrue(
                query.hotelId(), query.requestingUserId());

    if (!query.isAdmin() && !isOwner && !hasFinanceAccess) {
      throw FinanceException.accessDenied(
          "Bạn không có quyền xem thông tin hợp đồng của khách sạn này.");
    }

    Page<HotelContractJpaEntity> page;
    if (query.status() != null) {
      HotelContractJpaEntity.DbContractStatus dbStatus =
          HotelContractJpaEntity.DbContractStatus.valueOf(query.status().name());
      page =
          contractJpaRepository.findAllByHotelIdAndStatus(
              query.hotelId(), dbStatus, query.pageable());
    } else {
      page = contractJpaRepository.findAllByHotelId(query.hotelId(), query.pageable());
    }

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
                  HotelContract.ContractStatus.valueOf(entity.getStatus().name()),
                  entity.getCreatedByUserId(),
                  entity.getCreatedAt(),
                  entity.getUpdatedAt());
          return HotelContractResult.from(domain);
        });
  }
}
