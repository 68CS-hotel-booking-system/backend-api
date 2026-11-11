package com.team.hotelbooking.payment.application.service;

import com.team.hotelbooking.payment.application.command.CreateContractCommand;
import com.team.hotelbooking.payment.application.port.PaymentHotelPort;
import com.team.hotelbooking.payment.application.port.PaymentIdentityPort;
import com.team.hotelbooking.payment.application.result.HotelContractResult;
import com.team.hotelbooking.payment.application.usecase.CreateContractUseCase;
import com.team.hotelbooking.payment.domain.exception.FinanceException;
import com.team.hotelbooking.payment.domain.model.HotelContract;
import com.team.hotelbooking.payment.domain.repository.HotelContractRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreateContractService implements CreateContractUseCase {

  private final HotelContractRepository contractRepository;
  private final PaymentHotelPort paymentHotelPort;
  private final PaymentIdentityPort paymentIdentityPort;
  private final Clock clock;

  @Override
  public HotelContractResult execute(CreateContractCommand command) {
    // Người tạo lấy từ principal ADMIN; không tự sinh định danh thay thế (FK tới users).
    if (command.currentAdminUserId() == null) {
      throw FinanceException.accessDenied("Chỉ quản trị hệ thống được tạo hợp đồng hoa hồng.");
    }

    if (!paymentHotelPort.existsById(command.hotelId())) {
      throw FinanceException.hotelNotFound("Không tìm thấy khách sạn.");
    }

    if (!paymentIdentityPort.existsById(command.ownerUserId())) {
      throw FinanceException.userNotFound("Không tìm thấy chủ sở hữu (Owner) được chỉ định.");
    }

    if (command.contractCode() == null || command.contractCode().trim().isBlank()) {
      throw FinanceException.contractCodeExists("Mã hợp đồng không được để trống.");
    }

    String code = command.contractCode().trim();
    if (contractRepository.existsByContractCode(code)) {
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

    HotelContract.ContractStatus status =
        command.status() != null ? command.status() : HotelContract.ContractStatus.DRAFT;

    // Kiểm trước để trả lỗi rõ; hai yêu cầu song song vẫn bị ràng buộc exclusion của DB chặn.
    if (status == HotelContract.ContractStatus.ACTIVE
        && contractRepository.hasOverlappingActive(
            command.hotelId(), command.startsAt(), command.endsAt(), null)) {
      throw FinanceException.activeContractOverlap(
          "Khoảng thời gian hiệu lực bị trùng lặp với hợp đồng đang hoạt động khác của khách sạn.");
    }

    HotelContract.CommissionBasis commissionBasis =
        command.commissionBasis() != null
            ? command.commissionBasis()
            : HotelContract.CommissionBasis.AFTER_DISCOUNT;

    Instant now = clock.instant();

    HotelContract saved =
        contractRepository.add(
            new HotelContract(
                null,
                code,
                command.hotelId(),
                command.ownerUserId(),
                command.commissionRate(),
                commissionBasis,
                command.startsAt(),
                command.endsAt(),
                status,
                command.currentAdminUserId(),
                now,
                now));

    return HotelContractResult.from(saved);
  }
}
