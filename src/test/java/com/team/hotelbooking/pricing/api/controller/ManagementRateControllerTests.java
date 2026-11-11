package com.team.hotelbooking.pricing.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.team.hotelbooking.pricing.api.dto.request.SetRoomRatesRequest;
import com.team.hotelbooking.pricing.api.exception.PricingExceptionHandler;
import com.team.hotelbooking.pricing.application.command.GetRoomRatesQuery;
import com.team.hotelbooking.pricing.application.command.SetRoomRatesCommand;
import com.team.hotelbooking.pricing.application.result.DailyRateResult;
import com.team.hotelbooking.pricing.application.result.RoomTypeRatesResult;
import com.team.hotelbooking.pricing.application.usecase.GetRoomRatesUseCase;
import com.team.hotelbooking.pricing.application.usecase.SetRoomRatesUseCase;
import com.team.hotelbooking.pricing.domain.exception.PricingException;
import com.team.hotelbooking.shared.web.ProblemDetailFactory;
import java.time.Instant;
import java.time.LocalDate;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
@DisplayName("Controller tra cứu và thiết lập giá phòng theo ngày")
class ManagementRateControllerTests {

  private static final UUID ACTOR_ID = UUID.fromString("1f2e3d4c-5b6a-4978-8695-a4b3c2d1e0f9");
  private static final UUID ADMIN_ID = UUID.fromString("4c5d6e7f-8a9b-4c0d-9e2f-3a4b5c6d7e8f");
  private static final Authentication OWNER =
      UsernamePasswordAuthenticationToken.authenticated(
          ACTOR_ID.toString(), null, List.of(new SimpleGrantedAuthority("ROLE_HOTEL_OWNER")));
  private static final Authentication ADMIN =
      UsernamePasswordAuthenticationToken.authenticated(
          ADMIN_ID.toString(), null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

  private MockMvc mvc;
  private ObjectMapper objectMapper;

  @Mock private GetRoomRatesUseCase getRoomRatesUseCase;
  @Mock private SetRoomRatesUseCase setRoomRatesUseCase;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    mvc =
        MockMvcBuilders.standaloneSetup(
                new ManagementRateController(getRoomRatesUseCase, setRoomRatesUseCase))
            .setControllerAdvice(new PricingExceptionHandler(new ProblemDetailFactory()))
            .defaultRequest(get("/").principal(OWNER))
            .build();
  }

  @Nested
  @DisplayName("GET /api/v1/management/hotels/{hotelId}/room-types/{roomTypeId}/rates")
  class GetRoomRatesTests {

    @Test
    @DisplayName("Nên trả về 200 và danh sách giá theo ngày khi tra cứu thành công")
    void shouldReturnRatesSuccessfully() throws Exception {
      // Given
      UUID hotelId = UUID.randomUUID();
      UUID roomTypeId = UUID.randomUUID();
      LocalDate startDate = LocalDate.of(2026, 11, 1);
      LocalDate endDate = LocalDate.of(2026, 11, 3);

      List<DailyRateResult> rates =
          List.of(
              new DailyRateResult(LocalDate.of(2026, 11, 1), 1200000L, true, Instant.now()),
              new DailyRateResult(LocalDate.of(2026, 11, 2), null, false, null),
              new DailyRateResult(LocalDate.of(2026, 11, 3), 1500000L, true, Instant.now()));

      RoomTypeRatesResult result =
          new RoomTypeRatesResult(hotelId, roomTypeId, "VND", startDate, endDate, rates);

      given(getRoomRatesUseCase.execute(any())).willReturn(result);

      // When & Then
      mvc.perform(
              get(
                      "/api/v1/management/hotels/{hotelId}/room-types/{roomTypeId}/rates",
                      hotelId,
                      roomTypeId)
                  .param("startDate", "2026-11-01")
                  .param("endDate", "2026-11-03"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.message").value("Thành công."))
          .andExpect(jsonPath("$.data.hotelId").value(hotelId.toString()))
          .andExpect(jsonPath("$.data.roomTypeId").value(roomTypeId.toString()))
          .andExpect(jsonPath("$.data.rates[0].price").value(1200000))
          .andExpect(jsonPath("$.data.rates[0].hasRate").value(true))
          .andExpect(jsonPath("$.data.rates[1].hasRate").value(false))
          .andExpect(jsonPath("$.data.rates[2].price").value(1500000));
    }

    @Test
    @DisplayName("Nên trả về 404 khi loại phòng không thuộc khách sạn")
    void shouldReturn404WhenRoomTypeNotFound() throws Exception {
      // Given
      UUID hotelId = UUID.randomUUID();
      UUID roomTypeId = UUID.randomUUID();

      given(getRoomRatesUseCase.execute(any()))
          .willThrow(
              PricingException.roomTypeNotFound(
                  "Không tìm thấy loại phòng hoặc loại phòng không thuộc khách sạn."));

      // When & Then
      mvc.perform(
              get(
                      "/api/v1/management/hotels/{hotelId}/room-types/{roomTypeId}/rates",
                      hotelId,
                      roomTypeId)
                  .param("startDate", "2026-11-01")
                  .param("endDate", "2026-11-03"))
          .andExpect(status().isNotFound())
          .andExpect(
              jsonPath("$.detail")
                  .value("Không tìm thấy loại phòng hoặc loại phòng không thuộc khách sạn."));
    }
  }

  @Nested
  @DisplayName("PUT /api/v1/management/hotels/{hotelId}/room-types/{roomTypeId}/rates")
  class SetRoomRatesTests {

    @Test
    @DisplayName("Nên trả về 200 và kết quả cập nhật khi thiết lập giá thành công")
    void shouldSetRoomRatesSuccessfully() throws Exception {
      // Given
      UUID hotelId = UUID.randomUUID();
      UUID roomTypeId = UUID.randomUUID();
      LocalDate startDate = LocalDate.of(2026, 11, 10);
      LocalDate endDateExclusive = LocalDate.of(2026, 11, 12);

      SetRoomRatesRequest request =
          new SetRoomRatesRequest(startDate, endDateExclusive, 1500000L, "VND");

      List<DailyRateResult> rates =
          List.of(
              new DailyRateResult(LocalDate.of(2026, 11, 10), 1500000L, true, Instant.now()),
              new DailyRateResult(LocalDate.of(2026, 11, 11), 1500000L, true, Instant.now()));

      RoomTypeRatesResult result =
          new RoomTypeRatesResult(
              hotelId, roomTypeId, "VND", startDate, LocalDate.of(2026, 11, 11), rates);

      given(setRoomRatesUseCase.execute(any())).willReturn(result);

      // When & Then
      mvc.perform(
              put(
                      "/api/v1/management/hotels/{hotelId}/room-types/{roomTypeId}/rates",
                      hotelId,
                      roomTypeId)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.message").value("Thiết lập giá thành công."))
          .andExpect(jsonPath("$.data.hotelId").value(hotelId.toString()))
          .andExpect(jsonPath("$.data.roomTypeId").value(roomTypeId.toString()))
          .andExpect(jsonPath("$.data.currency").value("VND"))
          .andExpect(jsonPath("$.data.rates[0].price").value(1500000))
          .andExpect(jsonPath("$.data.rates[1].price").value(1500000));
    }

    @Test
    @DisplayName("Nên trả về 400 khi số tiền <= 0 trong request body")
    void shouldReturn400WhenAmountIsZeroOrNegative() throws Exception {
      // Given
      UUID hotelId = UUID.randomUUID();
      UUID roomTypeId = UUID.randomUUID();
      SetRoomRatesRequest invalidRequest =
          new SetRoomRatesRequest(LocalDate.of(2026, 11, 10), null, 0L, "VND");

      // When & Then
      mvc.perform(
              put(
                      "/api/v1/management/hotels/{hotelId}/room-types/{roomTypeId}/rates",
                      hotelId,
                      roomTypeId)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(invalidRequest)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Nên trả về 400 khi ngày áp dụng trong quá khứ hoặc dải ngày không hợp lệ")
    void shouldReturn400WhenDateRangeIsInvalid() throws Exception {
      // Given
      UUID hotelId = UUID.randomUUID();
      UUID roomTypeId = UUID.randomUUID();
      SetRoomRatesRequest request =
          new SetRoomRatesRequest(
              LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 5), 1500000L, "VND");

      given(setRoomRatesUseCase.execute(any()))
          .willThrow(
              PricingException.invalidDateRange(
                  "Ngày kết thúc (không bao gồm) phải sau ngày bắt đầu."));

      // When & Then
      mvc.perform(
              put(
                      "/api/v1/management/hotels/{hotelId}/room-types/{roomTypeId}/rates",
                      hotelId,
                      roomTypeId)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(
              jsonPath("$.detail").value("Ngày kết thúc (không bao gồm) phải sau ngày bắt đầu."));
    }

    @Test
    @DisplayName("Nên trả về 404 khi loại phòng hoặc khách sạn không tồn tại")
    void shouldReturn404WhenRoomTypeNotFound() throws Exception {
      // Given
      UUID hotelId = UUID.randomUUID();
      UUID roomTypeId = UUID.randomUUID();
      SetRoomRatesRequest request =
          new SetRoomRatesRequest(LocalDate.of(2026, 11, 10), null, 1500000L, "VND");

      given(setRoomRatesUseCase.execute(any()))
          .willThrow(
              PricingException.roomTypeNotFound(
                  "Không tìm thấy loại phòng hoặc loại phòng không thuộc khách sạn."));

      // When & Then
      mvc.perform(
              put(
                      "/api/v1/management/hotels/{hotelId}/room-types/{roomTypeId}/rates",
                      hotelId,
                      roomTypeId)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isNotFound())
          .andExpect(
              jsonPath("$.detail")
                  .value("Không tìm thấy loại phòng hoặc loại phòng không thuộc khách sạn."));
    }
  }

  @Nested
  @DisplayName("Người gọi lấy từ principal khi tra cứu và đặt giá")
  class ActorTests {

    @Test
    void shouldPassActorAndAdminFlagFromPrincipalWhenReadingRates() throws Exception {
      // Given
      UUID hotelId = UUID.randomUUID();
      UUID roomTypeId = UUID.randomUUID();
      LocalDate start = LocalDate.of(2026, 11, 1);
      LocalDate end = LocalDate.of(2026, 11, 2);
      given(getRoomRatesUseCase.execute(any()))
          .willReturn(new RoomTypeRatesResult(hotelId, roomTypeId, "VND", start, end, List.of()));

      // When
      mvc.perform(
              get(
                      "/api/v1/management/hotels/{hotelId}/room-types/{roomTypeId}/rates",
                      hotelId,
                      roomTypeId)
                  .principal(ADMIN)
                  .param("startDate", "2026-11-01")
                  .param("endDate", "2026-11-02"))
          .andExpect(status().isOk());

      // Then
      then(getRoomRatesUseCase)
          .should()
          .execute(new GetRoomRatesQuery(ADMIN_ID, true, hotelId, roomTypeId, start, end));
    }

    @Test
    void shouldPassNonAdminFlagWhenPrincipalIsHotelOwnerSettingRates() throws Exception {
      // Given
      UUID hotelId = UUID.randomUUID();
      UUID roomTypeId = UUID.randomUUID();
      LocalDate start = LocalDate.of(2026, 12, 1);
      given(setRoomRatesUseCase.execute(any()))
          .willReturn(new RoomTypeRatesResult(hotelId, roomTypeId, "VND", start, start, List.of()));

      // When
      mvc.perform(
              put(
                      "/api/v1/management/hotels/{hotelId}/room-types/{roomTypeId}/rates",
                      hotelId,
                      roomTypeId)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"startDate\": \"2026-12-01\", \"amount\": 1500000}"))
          .andExpect(status().isOk());

      // Then
      then(setRoomRatesUseCase)
          .should()
          .execute(
              new SetRoomRatesCommand(
                  ACTOR_ID, false, hotelId, roomTypeId, start, null, 1_500_000L, "VND"));
    }
  }
}
