package com.team.hotelbooking.payment.application.service;

import com.team.hotelbooking.hotel.infrastructure.persistence.repository.HotelJpaRepository;
import com.team.hotelbooking.identity.infrastructure.persistence.repository.UserJpaRepository;
import com.team.hotelbooking.payment.application.command.CreateContractCommand;
import com.team.hotelbooking.payment.application.usecase.CreateContractUseCase;
import com.team.hotelbooking.payment.domain.exception.FinanceException;
import com.team.hotelbooking.payment.domain.model.HotelContract;
import com.team.hotelbooking.payment.infrastructure.persistence.entity.HotelContractJpaEntity;
import com.team.hotelbooking.payment.infrastructure.persistence.repository.HotelContractJpaRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateContractService implements CreateContractUseCase {

  private final HotelContractJpaRepository contractJpaRepository;
  private final HotelJpaRepository hotelJpaRepository;
  private final UserJpaRepository userJpaRepository;

  @Override
  @Transactional
  public HotelContract execute(CreateContractCommand command) {
    if (!hotelJpaRepository.existsById(command.hotelId())) {
      throw FinanceException.hotelNotFound("Không tìm thấy khách sạn.");
    }

    if (!userJpaRepository.existsById(command.ownerUserId())) {
      throw FinanceException.userNotFound("Không tìm thấy chủ sở hữu (Owner) được chỉ định.");
    }

    if (command.contractCode() == null || command.contractCode().trim().isBlank()) {
      throw FinanceException.contractCodeExists("Mã hợp đồng không được để trống.");
    }

    String code = command.contractCode().trim();
    if (contractJpaRepository.existsByContractCode(code)) {
      throw FinanceException.contractCodeExists(
          "Mã hợp đồng " + code + " đã tồn tại trên hệ thống.");
    }

    if (command.endsAt() == null
        || command.startsAt() == null
        || !command.endsAt().isAfter(command.startsAt())) {
      throw FinanceException.invalidPeriod(
          "Thời gian kết thúc hợp đồng phải sau thời gian bắt đầu.");
    }

    if (command.commissionRate() == null
        || command.commissionRate().compareTo(BigDecimal.ZERO) < 0
        || command.commissionRate().compareTo(new BigDecimal("100.00")) > 0) {
      throw FinanceException.invalidRate("Tỷ lệ hoa hồng phải nằm trong khoảng từ 0% đến 100%.");
    }

    HotelContractJpaEntity.DbContractStatus status =
        command.status() != null
            ? HotelContractJpaEntity.DbContractStatus.valueOf(command.status().name())
            : HotelContractJpaEntity.DbContractStatus.DRAFT;

    if (status == HotelContractJpaEntity.DbContractStatus.ACTIVE) {
      long overlaps =
          contractJpaRepository.countOverlappingActiveContracts(
              command.hotelId(), command.startsAt(), command.endsAt());
      if (overlaps > 0) {
        throw FinanceException.activeContractOverlap(
            "Khoảng thời gian hiệu lực bị trùng lặp với hợp đồng đang hoạt động khác của khách sạn.");
      }
    }

    HotelContractJpaEntity.DbCommissionBasis commissionBasis =
        command.commissionBasis() != null
            ? HotelContractJpaEntity.DbCommissionBasis.valueOf(command.commissionBasis().name())
            : HotelContractJpaEntity.DbCommissionBasis.AFTER_DISCOUNT;

    HotelContractJpaEntity entity = new HotelContractJpaEntity();
    entity.setContractCode(code);
    entity.setHotelId(command.hotelId());
    entity.setOwnerUserId(command.ownerUserId());
    entity.setCommissionRate(command.commissionRate());
    entity.setCommissionBasis(commissionBasis);
    entity.setStartsAt(command.startsAt());
    entity.setEndsAt(command.endsAt());
    entity.setStatus(status);
    entity.setCreatedByUserId(
        command.currentAdminUserId() != null ? command.currentAdminUserId() : UUID.randomUUID());
    entity.setCreatedAt(Instant.now());
    entity.setUpdatedAt(Instant.now());

    HotelContractJpaEntity saved = contractJpaRepository.save(entity);

    return new HotelContract(
        saved.getId(),
        saved.getContractCode(),
        saved.getHotelId(),
        saved.getOwnerUserId(),
        saved.getCommissionRate(),
        HotelContract.CommissionBasis.valueOf(saved.getCommissionBasis().name()),
        saved.getStartsAt(),
        saved.getEndsAt(),
        HotelContract.ContractStatus.valueOf(saved.getStatus().name()),
        saved.getCreatedByUserId(),
        saved.getCreatedAt(),
        saved.getUpdatedAt());
  }
}
