package com.team.hotelbooking.payment.application.service;

import com.team.hotelbooking.hotel.infrastructure.persistence.repository.HotelJpaRepository;
import com.team.hotelbooking.identity.infrastructure.persistence.repository.UserJpaRepository;
import com.team.hotelbooking.payment.application.command.UpdateFinancialAccessCommand;
import com.team.hotelbooking.payment.application.usecase.UpdateFinancialAccessUseCase;
import com.team.hotelbooking.payment.domain.exception.FinanceException;
import com.team.hotelbooking.payment.domain.model.FinancialAccess;
import com.team.hotelbooking.payment.infrastructure.persistence.entity.HotelFinancialAccessHistoryJpaEntity;
import com.team.hotelbooking.payment.infrastructure.persistence.entity.HotelFinancialAccessJpaEntity;
import com.team.hotelbooking.payment.infrastructure.persistence.repository.HotelFinancialAccessHistoryJpaRepository;
import com.team.hotelbooking.payment.infrastructure.persistence.repository.HotelFinancialAccessJpaRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateFinancialAccessService implements UpdateFinancialAccessUseCase {

  private final HotelJpaRepository hotelJpaRepository;
  private final UserJpaRepository userJpaRepository;
  private final HotelFinancialAccessJpaRepository accessJpaRepository;
  private final HotelFinancialAccessHistoryJpaRepository historyJpaRepository;

  @Override
  @Transactional
  public FinancialAccess execute(UpdateFinancialAccessCommand command) {
    if (!hotelJpaRepository.existsById(command.hotelId())) {
      throw FinanceException.hotelNotFound("Không tìm thấy khách sạn.");
    }

    if (!userJpaRepository.existsById(command.targetUserId())) {
      throw FinanceException.userNotFound("Không tìm thấy người dùng được chỉ định.");
    }

    UUID adminId =
        command.currentAdminUserId() != null ? command.currentAdminUserId() : UUID.randomUUID();

    Optional<HotelFinancialAccessJpaEntity> optionalAccess =
        accessJpaRepository.findByHotelIdAndUserId(command.hotelId(), command.targetUserId());

    HotelFinancialAccessJpaEntity entity;

    if (optionalAccess.isEmpty()) {
      if (command.expectedVersion() != null && command.expectedVersion() != 0L) {
        throw FinanceException.concurrentModification(
            "Phiên bản kỳ vọng không khớp với dữ liệu hiện tại.");
      }

      entity = new HotelFinancialAccessJpaEntity();
      entity.setHotelId(command.hotelId());
      entity.setUserId(command.targetUserId());
      entity.setGranted(command.isGranted());
      entity.setAccessScope(command.accessScope() != null ? command.accessScope() : "FINANCE_READ");
      entity.setReason(command.reason());
      entity.setGrantedByUserId(adminId);
      entity.setGrantedAt(Instant.now());

      entity = accessJpaRepository.save(entity);

      // Lưu lịch sử bản ghi đầu tiên
      HotelFinancialAccessHistoryJpaEntity history = new HotelFinancialAccessHistoryJpaEntity();
      history.setFinancialAccessId(entity.getId());
      history.setHotelId(entity.getHotelId());
      history.setUserId(entity.getUserId());
      history.setGranted(entity.isGranted());
      history.setAccessScope(entity.getAccessScope());
      history.setReason(entity.getReason());
      history.setVersion(entity.getVersion());
      history.setChangedByUserId(adminId);
      history.setChangedAt(Instant.now());
      historyJpaRepository.save(history);

    } else {
      entity = optionalAccess.get();

      if (command.expectedVersion() != null
          && !entity.getVersion().equals(command.expectedVersion())) {
        throw FinanceException.concurrentModification(
            "Dữ liệu phân quyền đã được sửa đổi bởi người khác. Phiên bản hiện tại là "
                + entity.getVersion()
                + ", phiên bản gửi lên là "
                + command.expectedVersion()
                + ".");
      }

      // Lưu snapshot phiên bản cũ vào history
      HotelFinancialAccessHistoryJpaEntity history = new HotelFinancialAccessHistoryJpaEntity();
      history.setFinancialAccessId(entity.getId());
      history.setHotelId(entity.getHotelId());
      history.setUserId(entity.getUserId());
      history.setGranted(entity.isGranted());
      history.setAccessScope(entity.getAccessScope());
      history.setReason(entity.getReason());
      history.setVersion(entity.getVersion());
      history.setChangedByUserId(adminId);
      history.setChangedAt(Instant.now());
      historyJpaRepository.save(history);

      // Cập nhật giá trị mới
      entity.setGranted(command.isGranted());
      if (command.accessScope() != null) {
        entity.setAccessScope(command.accessScope());
      }
      entity.setReason(command.reason());
      entity.setGrantedByUserId(adminId);
      entity.setGrantedAt(Instant.now());

      entity = accessJpaRepository.save(entity);
    }

    return new FinancialAccess(
        entity.getId(),
        entity.getHotelId(),
        entity.getUserId(),
        entity.isGranted(),
        entity.getAccessScope(),
        entity.getReason(),
        entity.getGrantedByUserId(),
        entity.getGrantedAt(),
        entity.getVersion());
  }
}
