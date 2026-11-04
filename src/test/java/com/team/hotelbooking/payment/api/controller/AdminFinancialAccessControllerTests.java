package com.team.hotelbooking.payment.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.team.hotelbooking.payment.api.dto.request.UpdateFinancialAccessRequest;
import com.team.hotelbooking.payment.api.exception.FinanceExceptionHandler;
import com.team.hotelbooking.payment.application.result.FinancialAccessDetailResult;
import com.team.hotelbooking.payment.application.result.FinancialAccessHistoryResult;
import com.team.hotelbooking.payment.application.result.FinancialAccessResult;
import com.team.hotelbooking.payment.application.usecase.GetFinancialAccessUseCase;
import com.team.hotelbooking.payment.application.usecase.UpdateFinancialAccessUseCase;
import com.team.hotelbooking.payment.domain.exception.FinanceException;
import com.team.hotelbooking.shared.web.ProblemDetailFactory;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
@DisplayName("Controller quản trị phân quyền tài chính")
class AdminFinancialAccessControllerTests {

  private MockMvc mvc;
  private ObjectMapper objectMapper;

  @Mock private UpdateFinancialAccessUseCase updateFinancialAccessUseCase;
  @Mock private GetFinancialAccessUseCase getFinancialAccessUseCase;

  @BeforeEach
  void setUp() {
    mvc =
        MockMvcBuilders.standaloneSetup(
                new AdminFinancialAccessController(
                    updateFinancialAccessUseCase, getFinancialAccessUseCase))
            .setControllerAdvice(new FinanceExceptionHandler(new ProblemDetailFactory()))
            .build();

    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
  }

  @Nested
  @DisplayName("PUT /api/v1/admin/hotels/{hotelId}/financial-access/{userId}")
  class UpdateFinancialAccessTests {

    @Test
    @DisplayName("Nên cập nhật quyền tài chính thành công")
    void shouldUpdateFinancialAccessSuccessfully() throws Exception {
      // Given
      UUID hotelId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      UUID accessId = UUID.randomUUID();

      UpdateFinancialAccessRequest request =
          new UpdateFinancialAccessRequest(true, "FINANCE_READ", "Cấp quyền cho kế toán", 0L);

      FinancialAccessResult result =
          new FinancialAccessResult(
              accessId,
              hotelId,
              userId,
              true,
              "FINANCE_READ",
              "Cấp quyền cho kế toán",
              UUID.randomUUID(),
              Instant.now(),
              1L);

      given(updateFinancialAccessUseCase.execute(any())).willReturn(result);

      // When & Then
      mvc.perform(
              put("/api/v1/admin/hotels/{hotelId}/financial-access/{userId}", hotelId, userId)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.message").value("Cập nhật quyền tài chính thành công."))
          .andExpect(jsonPath("$.data.isGranted").value(true))
          .andExpect(jsonPath("$.data.version").value(1));
    }

    @Test
    @DisplayName("Nên trả về 409 khi phát hiện xung đột phiên bản")
    void shouldReturn409WhenConcurrentModificationDetected() throws Exception {
      // Given
      UUID hotelId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();

      UpdateFinancialAccessRequest request =
          new UpdateFinancialAccessRequest(true, "FINANCE_READ", "Cấp quyền", 0L);

      given(updateFinancialAccessUseCase.execute(any()))
          .willThrow(
              FinanceException.concurrentModification(
                  "Dữ liệu phân quyền đã được sửa đổi bởi người khác."));

      // When & Then
      mvc.perform(
              put("/api/v1/admin/hotels/{hotelId}/financial-access/{userId}", hotelId, userId)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isConflict())
          .andExpect(
              jsonPath("$.detail").value("Dữ liệu phân quyền đã được sửa đổi bởi người khác."));
    }
  }

  @Nested
  @DisplayName("GET /api/v1/admin/hotels/{hotelId}/financial-access/{userId}")
  class GetFinancialAccessTests {

    @Test
    @DisplayName("Nên lấy thông tin chi tiết quyền tài chính kèm lịch sử")
    void shouldGetFinancialAccessDetailWithHistory() throws Exception {
      // Given
      UUID hotelId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      UUID accessId = UUID.randomUUID();

      FinancialAccessHistoryResult history1 =
          new FinancialAccessHistoryResult(
              UUID.randomUUID(),
              accessId,
              hotelId,
              userId,
              false,
              null,
              "Khởi tạo",
              0L,
              UUID.randomUUID(),
              Instant.now());

      FinancialAccessDetailResult detail =
          new FinancialAccessDetailResult(
              accessId,
              hotelId,
              userId,
              "Nguyễn Văn A",
              "a@hotel.vn",
              true,
              "FINANCE_READ",
              "Cấp quyền",
              UUID.randomUUID(),
              Instant.now(),
              1L,
              List.of(history1));

      given(getFinancialAccessUseCase.execute(any())).willReturn(detail);

      // When & Then
      mvc.perform(get("/api/v1/admin/hotels/{hotelId}/financial-access/{userId}", hotelId, userId))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.message").value("Thành công."))
          .andExpect(jsonPath("$.data.userId").value(userId.toString()))
          .andExpect(jsonPath("$.data.userEmail").value("a@hotel.vn"))
          .andExpect(jsonPath("$.data.currentVersion").value(1))
          .andExpect(jsonPath("$.data.history[0].version").value(0));
    }
  }
}
