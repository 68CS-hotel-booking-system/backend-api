package com.team.hotelbooking.payment.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.hotelbooking.payment.api.exception.FinanceExceptionHandler;
import com.team.hotelbooking.payment.application.command.GetContractsQuery;
import com.team.hotelbooking.payment.application.result.HotelContractResult;
import com.team.hotelbooking.payment.application.usecase.GetContractsUseCase;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
@DisplayName("Controller tra cứu hợp đồng tài chính")
class FinanceContractControllerTests {

  private static final UUID ACTOR_ID = UUID.fromString("1f2e3d4c-5b6a-4978-8695-a4b3c2d1e0f9");
  private static final UUID ADMIN_ID = UUID.fromString("4c5d6e7f-8a9b-4c0d-9e2f-3a4b5c6d7e8f");
  private static final Authentication OWNER =
      UsernamePasswordAuthenticationToken.authenticated(
          ACTOR_ID.toString(), null, List.of(new SimpleGrantedAuthority("ROLE_HOTEL_OWNER")));
  private static final Authentication ADMIN =
      UsernamePasswordAuthenticationToken.authenticated(
          ADMIN_ID.toString(), null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

  private MockMvc mvc;

  @Mock private GetContractsUseCase getContractsUseCase;

  @BeforeEach
  void setUp() {
    mvc =
        MockMvcBuilders.standaloneSetup(new FinanceContractController(getContractsUseCase))
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .setControllerAdvice(new FinanceExceptionHandler(new ProblemDetailFactory()))
            .defaultRequest(get("/").principal(OWNER))
            .build();
  }

  @Nested
  @DisplayName("GET /api/v1/finance/hotels/{hotelId}/contracts")
  class GetContractsTests {

    @Test
    @DisplayName("Nên trả về danh sách hợp đồng phân trang thành công")
    void shouldReturnContractsPageSuccessfully() throws Exception {
      // Given
      UUID hotelId = UUID.randomUUID();
      UUID contractId = UUID.randomUUID();

      HotelContractResult contract =
          new HotelContractResult(
              contractId,
              "HD-ACTIVE-01",
              hotelId,
              UUID.randomUUID(),
              new BigDecimal("12.50"),
              HotelContract.CommissionBasis.AFTER_DISCOUNT,
              Instant.now(),
              Instant.now().plusSeconds(86400),
              HotelContract.ContractStatus.ACTIVE,
              UUID.randomUUID(),
              Instant.now(),
              Instant.now());

      PageImpl<HotelContractResult> page =
          new PageImpl<>(List.of(contract), PageRequest.of(0, 10), 1);

      given(getContractsUseCase.execute(any())).willReturn(page);

      // When & Then
      mvc.perform(get("/api/v1/finance/hotels/{hotelId}/contracts", hotelId))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.message").value("Thành công."))
          .andExpect(jsonPath("$.data.content[0].contractCode").value("HD-ACTIVE-01"))
          .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("Nên trả về 403 khi không có quyền truy cập thông tin hợp đồng")
    void shouldReturn403WhenAccessDenied() throws Exception {
      // Given
      UUID hotelId = UUID.randomUUID();

      given(getContractsUseCase.execute(any()))
          .willThrow(
              FinanceException.accessDenied(
                  "Bạn không có quyền xem thông tin hợp đồng của khách sạn này."));

      // When & Then
      mvc.perform(get("/api/v1/finance/hotels/{hotelId}/contracts", hotelId))
          .andExpect(status().isForbidden())
          .andExpect(
              jsonPath("$.detail")
                  .value("Bạn không có quyền xem thông tin hợp đồng của khách sạn này."));
    }
  }

  @Nested
  @DisplayName("Người gọi lấy từ principal khi tra cứu hợp đồng")
  class ActorTests {

    @Test
    void shouldPassActorAndAdminFlagFromPrincipalInsteadOfHardcodedAdmin() throws Exception {
      // Given
      UUID hotelId = UUID.randomUUID();
      given(getContractsUseCase.execute(any())).willReturn(Page.empty());
      ArgumentCaptor<GetContractsQuery> query = ArgumentCaptor.forClass(GetContractsQuery.class);

      // When
      mvc.perform(get("/api/v1/finance/hotels/{hotelId}/contracts", hotelId).principal(ADMIN))
          .andExpect(status().isOk());

      // Then
      then(getContractsUseCase).should().execute(query.capture());
      assertEquals(ADMIN_ID, query.getValue().requestingUserId());
      assertTrue(query.getValue().isAdmin());
    }

    @Test
    void shouldPassNonAdminFlagWhenPrincipalIsNotAdmin() throws Exception {
      // Given
      UUID hotelId = UUID.randomUUID();
      given(getContractsUseCase.execute(any())).willReturn(Page.empty());
      ArgumentCaptor<GetContractsQuery> query = ArgumentCaptor.forClass(GetContractsQuery.class);

      // When
      mvc.perform(get("/api/v1/finance/hotels/{hotelId}/contracts", hotelId))
          .andExpect(status().isOk());

      // Then
      then(getContractsUseCase).should().execute(query.capture());
      assertEquals(ACTOR_ID, query.getValue().requestingUserId());
      assertFalse(query.getValue().isAdmin());
      assertEquals(hotelId, query.getValue().hotelId());
    }
  }
}
