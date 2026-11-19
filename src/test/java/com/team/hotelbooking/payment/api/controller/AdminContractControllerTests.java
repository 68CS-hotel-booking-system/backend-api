package com.team.hotelbooking.payment.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.team.hotelbooking.payment.api.dto.request.CreateContractRequest;
import com.team.hotelbooking.payment.api.exception.FinanceExceptionHandler;
import com.team.hotelbooking.payment.application.command.ChangeContractStatusCommand;
import com.team.hotelbooking.payment.application.command.CreateContractCommand;
import com.team.hotelbooking.payment.application.result.HotelContractResult;
import com.team.hotelbooking.payment.application.usecase.ActivateContractUseCase;
import com.team.hotelbooking.payment.application.usecase.CreateContractUseCase;
import com.team.hotelbooking.payment.application.usecase.TerminateContractUseCase;
import com.team.hotelbooking.payment.domain.exception.FinanceErrorCode;
import com.team.hotelbooking.payment.domain.exception.FinanceException;
import com.team.hotelbooking.payment.domain.model.HotelContract;
import com.team.hotelbooking.shared.web.ProblemDetailFactory;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
@DisplayName("Controller quản trị hợp đồng hoa hồng")
class AdminContractControllerTests {

  private MockMvc mvc;
  private ObjectMapper objectMapper;

  private static final UUID ADMIN_ID = UUID.fromString("3e000000-0000-4000-8000-000000000001");
  private static final Authentication ADMIN =
      new UsernamePasswordAuthenticationToken(
          ADMIN_ID.toString(), null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

  @Mock private CreateContractUseCase createContractUseCase;
  @Mock private ActivateContractUseCase activateContractUseCase;
  @Mock private TerminateContractUseCase terminateContractUseCase;

  @BeforeEach
  void setUp() {
    mvc =
        MockMvcBuilders.standaloneSetup(
                new AdminContractController(
                    createContractUseCase, activateContractUseCase, terminateContractUseCase))
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
      ArgumentCaptor<CreateContractCommand> command =
          ArgumentCaptor.forClass(CreateContractCommand.class);

      // When & Then
      mvc.perform(
              post("/api/v1/admin/hotels/{hotelId}/contracts", hotelId)
                  .principal(ADMIN)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.message").value("Tạo hợp đồng hoa hồng thành công."))
          .andExpect(jsonPath("$.data.contractCode").value("HD-2026-001"))
          .andExpect(jsonPath("$.data.commissionRate").value(15.00))
          .andExpect(jsonPath("$.data.status").value("DRAFT"));
      then(createContractUseCase).should().execute(command.capture());
      assertThat(command.getValue().currentAdminUserId()).isEqualTo(ADMIN_ID);
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
                  .principal(ADMIN)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.detail").value("Mã hợp đồng đã tồn tại trên hệ thống."));
    }
  }

  @Nested
  @DisplayName("POST /api/v1/admin/hotels/{hotelId}/contracts/{contractId}/activate và /terminate")
  class ChangeStatusTests {

    private final UUID hotelId = UUID.fromString("3e000000-0000-4000-8000-000000000101");
    private final UUID contractId = UUID.fromString("3e000000-0000-4000-8000-000000000201");

    private HotelContractResult result(HotelContract.ContractStatus status) {
      return new HotelContractResult(
          contractId,
          "HD-PM16",
          hotelId,
          UUID.fromString("3e000000-0000-4000-8000-000000000301"),
          new BigDecimal("12.50"),
          HotelContract.CommissionBasis.BEFORE_DISCOUNT,
          Instant.parse("2026-12-01T00:00:00Z"),
          Instant.parse("2027-12-01T00:00:00Z"),
          status,
          ADMIN_ID,
          Instant.parse("2026-11-01T00:00:00Z"),
          Instant.parse("2026-11-10T00:00:00Z"));
    }

    @Test
    @DisplayName("Kích hoạt trả 200 với trạng thái ACTIVE, truyền người thực hiện từ principal")
    void shouldActivateContractWhenAdmin() throws Exception {
      // Given
      given(
              activateContractUseCase.execute(
                  new ChangeContractStatusCommand(ADMIN_ID, hotelId, contractId)))
          .willReturn(result(HotelContract.ContractStatus.ACTIVE));

      // When & Then
      mvc.perform(
              post(
                      "/api/v1/admin/hotels/{hotelId}/contracts/{contractId}/activate",
                      hotelId,
                      contractId)
                  .principal(ADMIN))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.message").value("Kích hoạt hợp đồng thành công."))
          .andExpect(jsonPath("$.data.id").value(contractId.toString()))
          .andExpect(jsonPath("$.data.status").value("ACTIVE"))
          .andExpect(jsonPath("$.data.commissionRate").value(12.50));
    }

    @Test
    @DisplayName("Kích hoạt trả 409 ProblemDetail khi chồng hiệu lực")
    void shouldReturnConflictWhenActivationOverlaps() throws Exception {
      // Given
      given(activateContractUseCase.execute(any()))
          .willThrow(FinanceException.activeContractOverlap("Chồng hiệu lực."));

      // When & Then
      mvc.perform(
              post(
                      "/api/v1/admin/hotels/{hotelId}/contracts/{contractId}/activate",
                      hotelId,
                      contractId)
                  .principal(ADMIN))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code").value("ACTIVE_CONTRACT_PERIOD_OVERLAP"));
    }

    @Test
    @DisplayName("Chấm dứt trả 200 TERMINATED; chấm dứt lại trả 409")
    void shouldTerminateThenRejectSecondTermination() throws Exception {
      // Given
      given(terminateContractUseCase.execute(any()))
          .willReturn(result(HotelContract.ContractStatus.TERMINATED))
          .willThrow(new FinanceException(FinanceErrorCode.INVALID_CONTRACT_STATUS_TRANSITION));

      // When & Then
      mvc.perform(
              post(
                      "/api/v1/admin/hotels/{hotelId}/contracts/{contractId}/terminate",
                      hotelId,
                      contractId)
                  .principal(ADMIN))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.message").value("Chấm dứt hợp đồng thành công."))
          .andExpect(jsonPath("$.data.status").value("TERMINATED"));
      mvc.perform(
              post(
                      "/api/v1/admin/hotels/{hotelId}/contracts/{contractId}/terminate",
                      hotelId,
                      contractId)
                  .principal(ADMIN))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code").value("INVALID_CONTRACT_STATUS_TRANSITION"));
    }
  }
}
