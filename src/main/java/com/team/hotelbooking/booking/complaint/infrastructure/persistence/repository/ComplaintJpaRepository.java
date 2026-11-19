package com.team.hotelbooking.booking.complaint.infrastructure.persistence.repository;

import com.team.hotelbooking.booking.complaint.infrastructure.persistence.entity.ComplaintJpaEntity;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ComplaintJpaRepository extends JpaRepository<ComplaintJpaEntity, UUID> {

  // ON CONFLICT khớp partial unique index uq_complaints_open_per_booking: request đồng thời chờ
  // giao dịch kia rồi bỏ qua (0 dòng) thay vì ném lỗi làm hỏng transaction.
  @Modifying
  @Query(
      value =
          """
          INSERT INTO complaints
            (id, booking_id, customer_id, hotel_id, category, content, status, created_at,
             updated_at, version)
          VALUES
            (:id, :bookingId, :customerId, :hotelId, CAST(:category AS complaint_category__t),
             :content, 'OPEN', :now, :now, 0)
          ON CONFLICT (booking_id) WHERE status IN ('OPEN', 'IN_REVIEW') DO NOTHING
          """,
      nativeQuery = true)
  int insertIfNoneOpen(
      @Param("id") UUID id,
      @Param("bookingId") UUID bookingId,
      @Param("customerId") UUID customerId,
      @Param("hotelId") UUID hotelId,
      @Param("category") String category,
      @Param("content") String content,
      @Param("now") Instant now);

  @Query(
      """
      SELECT c.id FROM ComplaintJpaEntity c
      WHERE c.bookingId = :bookingId AND c.status IN :statuses
      """)
  List<UUID> findIdsByBookingIdAndStatusIn(
      @Param("bookingId") UUID bookingId,
      @Param("statuses") Collection<ComplaintJpaEntity.DbStatus> statuses);

  Optional<ComplaintJpaEntity> findByIdAndCustomerId(UUID id, UUID customerId);

  Page<ComplaintJpaEntity> findByCustomerId(UUID customerId, Pageable pageable);

  Page<ComplaintJpaEntity> findByCustomerIdAndStatus(
      UUID customerId, ComplaintJpaEntity.DbStatus status, Pageable pageable);
}
