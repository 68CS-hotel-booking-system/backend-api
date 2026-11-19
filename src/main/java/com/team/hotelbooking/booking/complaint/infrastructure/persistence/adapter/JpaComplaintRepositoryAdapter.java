package com.team.hotelbooking.booking.complaint.infrastructure.persistence.adapter;

import com.team.hotelbooking.booking.complaint.domain.model.Complaint;
import com.team.hotelbooking.booking.complaint.domain.repository.ComplaintRepository;
import com.team.hotelbooking.booking.complaint.infrastructure.persistence.entity.ComplaintJpaEntity;
import com.team.hotelbooking.booking.complaint.infrastructure.persistence.mapper.ComplaintPersistenceMapper;
import com.team.hotelbooking.booking.complaint.infrastructure.persistence.repository.ComplaintJpaRepository;
import com.team.hotelbooking.shared.pagination.PageQuery;
import com.team.hotelbooking.shared.pagination.PageResult;
import com.team.hotelbooking.shared.persistence.PageResultMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaComplaintRepositoryAdapter implements ComplaintRepository {

  private static final List<ComplaintJpaEntity.DbStatus> OPEN_STATUSES =
      List.of(ComplaintJpaEntity.DbStatus.OPEN, ComplaintJpaEntity.DbStatus.IN_REVIEW);
  private static final Sort NEWEST_FIRST =
      Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

  private final ComplaintJpaRepository complaintJpaRepository;

  @Override
  public Optional<UUID> findOpenComplaintId(UUID bookingId) {
    return complaintJpaRepository.findIdsByBookingIdAndStatusIn(bookingId, OPEN_STATUSES).stream()
        .findFirst();
  }

  @Override
  public boolean addIfNoneOpen(Complaint complaint) {
    return complaintJpaRepository.insertIfNoneOpen(
            complaint.id(),
            complaint.bookingId(),
            complaint.customerId(),
            complaint.hotelId(),
            complaint.category().name(),
            complaint.content(),
            complaint.createdAt())
        == 1;
  }

  @Override
  public Optional<Complaint> findByIdAndCustomerId(UUID complaintId, UUID customerId) {
    return complaintJpaRepository
        .findByIdAndCustomerId(complaintId, customerId)
        .map(ComplaintPersistenceMapper::toDomain);
  }

  @Override
  public PageResult<Complaint> findByCustomerId(
      UUID customerId, Complaint.Status status, PageQuery pagination) {
    Pageable pageable = PageRequest.of(pagination.page(), pagination.size(), NEWEST_FIRST);
    return PageResultMapper.from(
        status == null
            ? complaintJpaRepository.findByCustomerId(customerId, pageable)
            : complaintJpaRepository.findByCustomerIdAndStatus(
                customerId, ComplaintPersistenceMapper.toDbStatus(status), pageable),
        ComplaintPersistenceMapper::toDomain);
  }
}
