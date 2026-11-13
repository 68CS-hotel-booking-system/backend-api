package com.team.hotelbooking.hotel.infrastructure.persistence.adapter;

import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.domain.model.HotelReviewRequest;
import com.team.hotelbooking.hotel.domain.model.HotelReviewSummary;
import com.team.hotelbooking.hotel.domain.repository.HotelReviewRepository;
import com.team.hotelbooking.hotel.infrastructure.persistence.entity.HotelReviewRequestJpaEntity;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.HotelReviewRequestJpaRepository;
import com.team.hotelbooking.shared.pagination.PageQuery;
import com.team.hotelbooking.shared.pagination.PageResult;
import com.team.hotelbooking.shared.persistence.PageResultMapper;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaHotelReviewRepositoryAdapter implements HotelReviewRepository {
  private final HotelReviewRequestJpaRepository repository;
  private final JdbcTemplate jdbc;

  @Override
  public HotelReviewRequest add(HotelReviewRequest request) {
    var entity = new HotelReviewRequestJpaEntity();
    entity.setId(request.id());
    entity.setHotelId(request.hotelId());
    entity.setSnapshot(request.snapshot());
    entity.setStatus(request.status());
    entity.setSubmittedBy(request.submittedBy());
    entity.setSubmittedAt(request.submittedAt());
    try {
      HotelReviewRequest saved = toDomain(repository.saveAndFlush(entity));
      // Pin media vĩnh viễn theo lịch sử, độc lập với liên kết ảnh/album hiện tại.
      for (var image : saved.snapshot().images()) {
        jdbc.update(
            "INSERT INTO hotel_review_request_images (request_id, media_id) VALUES (?, ?)",
            saved.id(),
            image.mediaId());
      }
      return saved;
    } catch (DataIntegrityViolationException exception) {
      for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
        if (cause instanceof ConstraintViolationException violation
            && "uq_hotel_review_pending".equals(violation.getConstraintName())) {
          throw new HotelException(HotelErrorCode.REVIEW_PENDING);
        }
      }
      throw exception;
    }
  }

  @Override
  public Optional<HotelReviewRequest> findById(UUID id) {
    return repository.findById(id).map(JpaHotelReviewRepositoryAdapter::toDomain);
  }

  @Override
  public Optional<UUID> findHotelId(UUID id) {
    return repository.findHotelId(id);
  }

  @Override
  public Optional<HotelReviewRequest> findPendingByHotelId(UUID hotelId) {
    return repository
        .findByHotelIdAndStatus(hotelId, HotelReviewRequest.Status.PENDING)
        .map(JpaHotelReviewRepositoryAdapter::toDomain);
  }

  @Override
  public HotelReviewRequest updateDecision(HotelReviewRequest request) {
    var entity =
        repository
            .findById(request.id())
            .orElseThrow(() -> new HotelException(HotelErrorCode.REVIEW_NOT_FOUND));
    if (entity.getVersion() != request.version()) {
      throw new HotelException(HotelErrorCode.REVIEW_CONCURRENT_UPDATE);
    }
    if (entity.getStatus() != HotelReviewRequest.Status.PENDING) {
      throw new HotelException(HotelErrorCode.REVIEW_ALREADY_DECIDED);
    }
    entity.setStatus(request.status());
    entity.setReviewedBy(request.reviewedBy());
    entity.setReviewedAt(request.reviewedAt());
    entity.setReason(request.reason());
    return toDomain(repository.saveAndFlush(entity));
  }

  @Override
  public PageResult<HotelReviewSummary> findAll(
      HotelReviewRequest.Status status, UUID hotelId, PageQuery pagination) {
    var pageable =
        PageRequest.of(
            pagination.page(),
            pagination.size(),
            Sort.by(Sort.Order.desc("submittedAt"), Sort.Order.desc("id")));
    return PageResultMapper.from(
        repository.findSummaries(status, hotelId, pageable),
        r ->
            new HotelReviewSummary(
                r.getId(),
                r.getHotelId(),
                r.getHotelName(),
                r.getStatus(),
                r.getSubmittedBy(),
                r.getSubmittedAt(),
                r.getReviewedBy(),
                r.getReviewedAt(),
                r.getReason(),
                r.getVersion()));
  }

  private static HotelReviewRequest toDomain(HotelReviewRequestJpaEntity entity) {
    return new HotelReviewRequest(
        entity.getId(),
        entity.getHotelId(),
        entity.getSnapshot(),
        entity.getStatus(),
        entity.getSubmittedBy(),
        entity.getSubmittedAt(),
        entity.getReviewedBy(),
        entity.getReviewedAt(),
        entity.getReason(),
        entity.getVersion());
  }
}
