package com.team.hotelbooking.payment.domain.repository;

import com.team.hotelbooking.payment.domain.model.HotelContract;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface HotelContractRepository {

  String nextContractCode(int year);

  // Có hợp đồng ACTIVE khác (bỏ qua excludedContractId, có thể null) chồng [startsAt, endsAt).
  boolean hasOverlappingActive(
      UUID hotelId, Instant startsAt, Instant endsAt, UUID excludedContractId);

  // Ghi ngay để vi phạm mã trùng hoặc chồng hiệu lực đổi thành lỗi nghiệp vụ trong use case.
  HotelContract add(HotelContract contract);

  // Khóa ghi để kích hoạt/chấm dứt cùng hợp đồng chạy tuần tự.
  Optional<HotelContract> findByIdAndHotelIdForUpdate(UUID contractId, UUID hotelId);

  // Chỉ ghi trạng thái và updated_at; điều khoản không đổi.
  HotelContract updateStatus(HotelContract contract);

  // Hợp đồng ACTIVE có hiệu lực tại thời điểm at: starts_at <= at < ends_at.
  Optional<HotelContract> findEffective(UUID hotelId, Instant at);

  Set<UUID> findEffectiveHotelIds(Instant at);

  void recordRenewal(UUID previousId, UUID nextId);
}
