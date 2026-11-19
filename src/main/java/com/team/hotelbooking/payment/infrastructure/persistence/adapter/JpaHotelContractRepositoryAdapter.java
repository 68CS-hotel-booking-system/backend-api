package com.team.hotelbooking.payment.infrastructure.persistence.adapter;

import com.team.hotelbooking.payment.domain.exception.FinanceException;
import com.team.hotelbooking.payment.domain.model.HotelContract;
import com.team.hotelbooking.payment.domain.repository.HotelContractRepository;
import com.team.hotelbooking.payment.infrastructure.persistence.entity.HotelContractJpaEntity;
import com.team.hotelbooking.payment.infrastructure.persistence.mapper.HotelContractPersistenceMapper;
import com.team.hotelbooking.payment.infrastructure.persistence.repository.HotelContractJpaRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaHotelContractRepositoryAdapter implements HotelContractRepository {

  private static final String ACTIVE_PERIOD_EXCLUSION = "ex_hotel_contracts_active_period";
  private static final String CODE_UNIQUE = "uq_hotel_contracts_code";

  private final HotelContractJpaRepository contractJpaRepository;

  @Override
  public boolean existsByContractCode(String contractCode) {
    return contractJpaRepository.existsByContractCode(contractCode);
  }

  @Override
  public boolean hasOverlappingActive(
      UUID hotelId, Instant startsAt, Instant endsAt, UUID excludedContractId) {
    return contractJpaRepository.countOverlappingActiveContractsExcluding(
            hotelId, startsAt, endsAt, excludedContractId)
        > 0;
  }

  @Override
  public HotelContract add(HotelContract contract) {
    try {
      return HotelContractPersistenceMapper.toDomain(
          contractJpaRepository.saveAndFlush(HotelContractPersistenceMapper.toNewEntity(contract)));
    } catch (DataIntegrityViolationException exception) {
      throw translate(exception);
    }
  }

  @Override
  public Optional<HotelContract> findByIdAndHotelIdForUpdate(UUID contractId, UUID hotelId) {
    return contractJpaRepository
        .findByIdAndHotelIdForUpdate(contractId, hotelId)
        .map(HotelContractPersistenceMapper::toDomain);
  }

  @Override
  public HotelContract updateStatus(HotelContract contract) {
    HotelContractJpaEntity entity =
        contractJpaRepository
            .findById(contract.id())
            .orElseThrow(() -> FinanceException.contractNotFound("Không tìm thấy hợp đồng."));
    entity.setStatus(HotelContractJpaEntity.DbContractStatus.valueOf(contract.status().name()));
    entity.setUpdatedAt(contract.updatedAt());
    try {
      return HotelContractPersistenceMapper.toDomain(contractJpaRepository.saveAndFlush(entity));
    } catch (DataIntegrityViolationException exception) {
      throw translate(exception);
    }
  }

  @Override
  public Optional<HotelContract> findEffective(UUID hotelId, Instant at) {
    return contractJpaRepository.findEffectiveActive(hotelId, at, PageRequest.of(0, 1)).stream()
        .findFirst()
        .map(HotelContractPersistenceMapper::toDomain);
  }

  // Vi phạm ràng buộc khi hai giao dịch cùng vượt qua bước kiểm trước: đổi thành 409 nghiệp vụ.
  private static RuntimeException translate(DataIntegrityViolationException exception) {
    for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
      if (cause instanceof ConstraintViolationException violation) {
        String constraint = violation.getConstraintName();
        String sqlState = violation.getSQLException().getSQLState();
        if (ACTIVE_PERIOD_EXCLUSION.equals(constraint) || "23P01".equals(sqlState)) {
          return FinanceException.activeContractOverlap(
              "Khoảng thời gian hiệu lực bị trùng lặp với hợp đồng đang hoạt động khác của khách"
                  + " sạn.");
        }
        if (CODE_UNIQUE.equals(constraint)) {
          return FinanceException.contractCodeExists("Mã hợp đồng đã tồn tại trên hệ thống.");
        }
      }
    }
    return exception;
  }
}
