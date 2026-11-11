package com.team.hotelbooking.payment.application.service;

import com.team.hotelbooking.payment.application.command.UpdateFinancialAccessCommand;
import com.team.hotelbooking.payment.application.port.PaymentHotelPort;
import com.team.hotelbooking.payment.application.port.PaymentIdentityPort;
import com.team.hotelbooking.payment.application.result.FinancialAccessResult;
import com.team.hotelbooking.payment.application.usecase.UpdateFinancialAccessUseCase;
import com.team.hotelbooking.payment.domain.exception.FinanceException;
import com.team.hotelbooking.payment.domain.model.FinancialAccess;
import com.team.hotelbooking.payment.infrastructure.persistence.entity.HotelFinancialAccessHistoryJpaEntity;
import com.team.hotelbooking.payment.infrastructure.persistence.entity.HotelFinancialAccessJpaEntity;
import com.team.hotelbooking.payment.infrastructure.persistence.repository.HotelFinancialAccessHistoryJpaRepository;
import com.team.hotelbooking.payment.infrastructure.persistence.repository.HotelFinancialAccessJpaRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UpdateFinancialAccessService implements UpdateFinancialAccessUseCase {

  private final HotelFinancialAccessJpaRepository accessJpaRepository;
  private final HotelFinancialAccessHistoryJpaRepository historyJpaRepository;
  private final PaymentHotelPort paymentHotelPort;
  private final PaymentIdentityPort paymentIdentityPort;
  private final Clock clock;

  @Override
  public FinancialAccessResult execute(UpdateFinancialAccessCommand command) {
    // Người cấp/thu hồi lấy từ principal ADMIN; không tự sinh định danh thay thế (FK tới users).
    if (command == null || command.currentAdminUserId() == null) {
      throw FinanceException.accessDenied("Chỉ quản trị hệ thống được phân quyền tài chính.");
    }
    UUID adminId = command.currentAdminUserId();

    if (!paymentHotelPort.existsById(command.hotelId())) {
      throw FinanceException.hotelNotFound("Không tìm thấy khách sạn.");
    }

    if (!paymentIdentityPort.existsById(command.targetUserId())) {
      throw FinanceException.userNotFound("Không tìm thấy người dùng được chỉ định.");
    }

    Optional<HotelFinancialAccessJpaEntity> optionalAccess =
        accessJpaRepository.findByHotelIdAndUserId(command.hotelId(), command.targetUserId());

    HotelFinancialAccessJpaEntity entity;
    Instant now = clock.instant();

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
      entity.setGrantedAt(now);

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
      history.setChangedAt(now);
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
      history.setChangedAt(now);
      historyJpaRepository.save(history);

      // Cập nhật giá trị mới
      entity.setGranted(command.isGranted());
      if (command.accessScope() != null) {
        entity.setAccessScope(command.accessScope());
      }
      entity.setReason(command.reason());
      entity.setGrantedByUserId(adminId);
      entity.setGrantedAt(now);

      entity = accessJpaRepository.save(entity);
    }

    FinancialAccess domain =
        new FinancialAccess(
            entity.getId(),
            entity.getHotelId(),
            entity.getUserId(),
            entity.isGranted(),
            entity.getAccessScope(),
            entity.getReason(),
            entity.getGrantedByUserId(),
            entity.getGrantedAt(),
            entity.getVersion());

    return FinancialAccessResult.from(domain);
  }
}
