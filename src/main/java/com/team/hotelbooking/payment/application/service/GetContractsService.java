package com.team.hotelbooking.payment.application.service;

import com.team.hotelbooking.hotel.infrastructure.persistence.entity.HotelJpaEntity;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.HotelJpaRepository;
import com.team.hotelbooking.payment.application.command.GetContractsQuery;
import com.team.hotelbooking.payment.application.usecase.GetContractsUseCase;
import com.team.hotelbooking.payment.domain.exception.FinanceException;
import com.team.hotelbooking.payment.domain.model.HotelContract;
import com.team.hotelbooking.payment.infrastructure.persistence.entity.HotelContractJpaEntity;
import com.team.hotelbooking.payment.infrastructure.persistence.repository.HotelContractJpaRepository;
import com.team.hotelbooking.payment.infrastructure.persistence.repository.HotelFinancialAccessJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetContractsService implements GetContractsUseCase {

  private final HotelContractJpaRepository contractJpaRepository;
  private final HotelJpaRepository hotelJpaRepository;
  private final HotelFinancialAccessJpaRepository financialAccessJpaRepository;

  @Override
  public Page<HotelContract> execute(GetContractsQuery query) {
    HotelJpaEntity hotel =
        hotelJpaRepository
            .findById(query.hotelId())
            .orElseThrow(() -> FinanceException.hotelNotFound("Không tìm thấy khách sạn."));

    // Kiểm tra quyền: Admin, Chủ sở hữu khách sạn (Owner), hoặc có quyền tài chính
    boolean isOwner =
        query.requestingUserId() != null && hotel.getOwnerUserId().equals(query.requestingUserId());
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
        entity ->
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
                entity.getUpdatedAt()));
  }
}
