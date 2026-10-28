package com.team.hotelbooking.payment.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.team.hotelbooking.payment.api.dto.request.UpdateFinancialAccessRequest;
import com.team.hotelbooking.payment.api.exception.FinanceExceptionHandler;
import com.team.hotelbooking.payment.application.usecase.GetFinancialAccessUseCase;
import com.team.hotelbooking.payment.application.usecase.UpdateFinancialAccessUseCase;
import com.team.hotelbooking.payment.domain.exception.FinanceException;
import com.team.hotelbooking.payment.domain.model.FinancialAccess;
import com.team.hotelbooking.payment.domain.model.FinancialAccessDetail;
import com.team.hotelbooking.payment.domain.model.FinancialAccessHistory;
import com.team.hotelbooking.shared.web.ProblemDetailFactory;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminFinancialAccessControllerTest {

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

  @Test
  void shouldUpdateFinancialAccessSuccessfully() throws Exception {
    UUID hotelId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID accessId = UUID.randomUUID();

    UpdateFinancialAccessRequest request =
        new UpdateFinancialAccessRequest(true, "FINANCE_READ", "Cấp quyền cho kế toán", 0L);

    FinancialAccess result =
        new FinancialAccess(
            accessId,
            hotelId,
            userId,
            true,
            "FINANCE_READ",
            "Cấp quyền cho kế toán",
            UUID.randomUUID(),
            Instant.now(),
            1L);

    when(updateFinancialAccessUseCase.execute(any())).thenReturn(result);

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
  void shouldReturn409WhenConcurrentModificationDetected() throws Exception {
    UUID hotelId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    UpdateFinancialAccessRequest request =
        new UpdateFinancialAccessRequest(true, "FINANCE_READ", "Cấp quyền", 0L);

    when(updateFinancialAccessUseCase.execute(any()))
        .thenThrow(
            FinanceException.concurrentModification(
                "Dữ liệu phân quyền đã được sửa đổi bởi người khác."));

    mvc.perform(
            put("/api/v1/admin/hotels/{hotelId}/financial-access/{userId}", hotelId, userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.detail").value("Dữ liệu phân quyền đã được sửa đổi bởi người khác."));
  }

  @Test
  void shouldGetFinancialAccessDetailWithHistory() throws Exception {
    UUID hotelId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID accessId = UUID.randomUUID();

    FinancialAccessHistory history1 =
        new FinancialAccessHistory(
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

    FinancialAccessDetail detail =
        new FinancialAccessDetail(
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

    when(getFinancialAccessUseCase.execute(any())).thenReturn(detail);

    mvc.perform(get("/api/v1/admin/hotels/{hotelId}/financial-access/{userId}", hotelId, userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Thành công."))
        .andExpect(jsonPath("$.data.userId").value(userId.toString()))
        .andExpect(jsonPath("$.data.userEmail").value("a@hotel.vn"))
        .andExpect(jsonPath("$.data.currentVersion").value(1))
        .andExpect(jsonPath("$.data.history[0].version").value(0));
  }
}
