package com.team.hotelbooking.payment.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.team.hotelbooking.payment.api.dto.request.CreateContractRequest;
import com.team.hotelbooking.payment.api.exception.FinanceExceptionHandler;
import com.team.hotelbooking.payment.application.result.HotelContractResult;
import com.team.hotelbooking.payment.application.usecase.CreateContractUseCase;
import com.team.hotelbooking.payment.domain.exception.FinanceException;
import com.team.hotelbooking.payment.domain.model.HotelContract;
import com.team.hotelbooking.shared.web.ProblemDetailFactory;
import java.math.BigDecimal;
import java.time.Instant;
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
@DisplayName("Controller quản trị hợp đồng hoa hồng")
class AdminContractControllerTests {

  private MockMvc mvc;
  private ObjectMapper objectMapper;

  @Mock private CreateContractUseCase createContractUseCase;

  @BeforeEach
  void setUp() {
    mvc =
        MockMvcBuilders.standaloneSetup(new AdminContractController(createContractUseCase))
            .setControllerAdvice(new FinanceExceptionHandler(new ProblemDetailFactory()))
            .build();

    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
  }

  @Nested
  @DisplayName("POST /api/v1/admin/hotels/{hotelId}/contracts")
  class CreateContractTests {

    @Test
    @DisplayName("Nên tạo hợp đồng thành công và trả về 201 Created")
    void shouldCreateContractSuccessfully() throws Exception {
      // Given
      UUID hotelId = UUID.randomUUID();
      UUID contractId = UUID.randomUUID();
      UUID ownerId = UUID.randomUUID();
      Instant startsAt = Instant.parse("2026-11-01T00:00:00Z");
      Instant endsAt = Instant.parse("2027-11-01T00:00:00Z");

      CreateContractRequest request =
          new CreateContractRequest(
              "HD-2026-001",
              ownerId,
              new BigDecimal("15.00"),
              HotelContract.CommissionBasis.AFTER_DISCOUNT,
              startsAt,
              endsAt,
              HotelContract.ContractStatus.DRAFT);

      HotelContractResult result =
          new HotelContractResult(
              contractId,
              "HD-2026-001",
              hotelId,
              ownerId,
              new BigDecimal("15.00"),
              HotelContract.CommissionBasis.AFTER_DISCOUNT,
              startsAt,
              endsAt,
              HotelContract.ContractStatus.DRAFT,
              UUID.randomUUID(),
              Instant.now(),
              Instant.now());

      given(createContractUseCase.execute(any())).willReturn(result);

      // When & Then
      mvc.perform(
              post("/api/v1/admin/hotels/{hotelId}/contracts", hotelId)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.message").value("Tạo hợp đồng hoa hồng thành công."))
          .andExpect(jsonPath("$.data.contractCode").value("HD-2026-001"))
          .andExpect(jsonPath("$.data.commissionRate").value(15.00))
          .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    @DisplayName("Nên trả về 409 khi mã hợp đồng đã tồn tại trên hệ thống")
    void shouldReturn409WhenContractCodeAlreadyExists() throws Exception {
      // Given
      UUID hotelId = UUID.randomUUID();
      UUID ownerId = UUID.randomUUID();
      Instant startsAt = Instant.parse("2026-11-01T00:00:00Z");
      Instant endsAt = Instant.parse("2027-11-01T00:00:00Z");

      CreateContractRequest request =
          new CreateContractRequest(
              "HD-DUPLICATE",
              ownerId,
              new BigDecimal("10.00"),
              HotelContract.CommissionBasis.AFTER_DISCOUNT,
              startsAt,
              endsAt,
              HotelContract.ContractStatus.DRAFT);

      given(createContractUseCase.execute(any()))
          .willThrow(FinanceException.contractCodeExists("Mã hợp đồng đã tồn tại trên hệ thống."));

      // When & Then
      mvc.perform(
              post("/api/v1/admin/hotels/{hotelId}/contracts", hotelId)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.detail").value("Mã hợp đồng đã tồn tại trên hệ thống."));
    }
  }
}
