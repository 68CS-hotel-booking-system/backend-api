package com.team.hotelbooking.booking.complaint.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.team.hotelbooking.booking.complaint.domain.exception.ComplaintErrorCode;
import com.team.hotelbooking.booking.complaint.domain.exception.ComplaintException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Quy tắc tạo khiếu nại (BK-21)")
class ComplaintTests {

  private static final Instant NOW = Instant.parse("2026-11-10T03:00:00Z");

  private static Complaint submit(Complaint.Category category, String content) {
    return Complaint.submit(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        category,
        content,
        NOW);
  }

  private static ComplaintErrorCode codeOf(Throwable exception) {
    return ((ComplaintException) exception).code();
  }

  @Nested
  @DisplayName("Complaint.submit")
  class SubmitTests {

    @Test
    @DisplayName("Tạo ở OPEN, cắt khoảng trắng nội dung, version 0")
    void shouldCreateOpenComplaintWithTrimmedContent() {
      Complaint complaint = submit(Complaint.Category.ROOM_CONDITION, "  Phòng có mùi ẩm mốc.  ");

      assertThat(complaint.status()).isEqualTo(Complaint.Status.OPEN);
      assertThat(complaint.content()).isEqualTo("Phòng có mùi ẩm mốc.");
      assertThat(complaint.createdAt()).isEqualTo(NOW);
      assertThat(complaint.updatedAt()).isEqualTo(NOW);
      assertThat(complaint.version()).isZero();
    }

    @Test
    @DisplayName("Từ chối nội dung rỗng, quá 2000 ký tự hoặc thiếu loại")
    void shouldRejectBlankTooLongOrMissingCategory() {
      assertThatThrownBy(() -> submit(Complaint.Category.BILLING, "   "))
          .satisfies(ex -> assertThat(codeOf(ex)).isEqualTo(ComplaintErrorCode.VALIDATION_FAILED));
      assertThatThrownBy(() -> submit(Complaint.Category.BILLING, "a".repeat(2001)))
          .satisfies(ex -> assertThat(codeOf(ex)).isEqualTo(ComplaintErrorCode.VALIDATION_FAILED));
      assertThatThrownBy(() -> submit(null, "Nội dung"))
          .satisfies(ex -> assertThat(codeOf(ex)).isEqualTo(ComplaintErrorCode.VALIDATION_FAILED));
    }

    @Test
    @DisplayName("OTHER cần tối thiểu 20 ký tự sau khi cắt khoảng trắng")
    void shouldRequireTwentyCharactersWhenCategoryIsOther() {
      assertThatThrownBy(() -> submit(Complaint.Category.OTHER, "  quá ngắn  "))
          .satisfies(ex -> assertThat(codeOf(ex)).isEqualTo(ComplaintErrorCode.VALIDATION_FAILED));
      assertThat(submit(Complaint.Category.OTHER, "a".repeat(20)).content()).hasSize(20);
    }
  }
}
