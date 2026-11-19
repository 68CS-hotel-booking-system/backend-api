package com.team.hotelbooking.booking.complaint.domain.repository;

import com.team.hotelbooking.booking.complaint.domain.model.Complaint;
import com.team.hotelbooking.shared.pagination.PageQuery;
import com.team.hotelbooking.shared.pagination.PageResult;
import java.util.Optional;
import java.util.UUID;

public interface ComplaintRepository {

  // Khiếu nại OPEN/IN_REVIEW của booking, nếu có.
  Optional<UUID> findOpenComplaintId(UUID bookingId);

  /**
   * Chèn khi booking chưa có khiếu nại đang xử lý. Trả false (không ném lỗi, không làm hỏng
   * transaction) nếu đã có khiếu nại OPEN/IN_REVIEW, kể cả khi bị request đồng thời chèn trước.
   */
  boolean addIfNoneOpen(Complaint complaint);

  Optional<Complaint> findByIdAndCustomerId(UUID complaintId, UUID customerId);

  PageResult<Complaint> findByCustomerId(
      UUID customerId, Complaint.Status status, PageQuery pagination);
}
