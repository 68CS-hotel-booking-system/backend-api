package com.team.hotelbooking.booking.api.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.hotelbooking.booking.api.dto.request.CreateBookingRequest;
import com.team.hotelbooking.booking.api.dto.request.CreateBookingRequest.BookingItemRequest;
import com.team.hotelbooking.booking.api.dto.request.CreateBookingRequest.ContactRequest;
import com.team.hotelbooking.booking.api.exception.BookingExceptionHandler;
import com.team.hotelbooking.booking.application.result.BookingDetailResult;
import com.team.hotelbooking.booking.application.result.BookingDetailResult.BookingItemResult;
import com.team.hotelbooking.booking.application.result.BookingDetailResult.ContactResult;
import com.team.hotelbooking.booking.application.result.BookingDetailResult.NightlyPriceResult;
import com.team.hotelbooking.booking.application.result.BookingDetailResult.PaymentHandoffResult;
import com.team.hotelbooking.booking.application.result.BookingSummaryResult;
import com.team.hotelbooking.booking.application.usecase.CreateBookingUseCase;
import com.team.hotelbooking.booking.application.usecase.GetCustomerBookingDetailUseCase;
import com.team.hotelbooking.booking.application.usecase.GetManagementBookingDetailUseCase;
import com.team.hotelbooking.booking.application.usecase.ListCustomerBookingsUseCase;
import com.team.hotelbooking.booking.application.usecase.ListManagementBookingsUseCase;
import com.team.hotelbooking.booking.domain.exception.BookingErrorCode;
import com.team.hotelbooking.booking.domain.exception.BookingException;
import com.team.hotelbooking.shared.config.JacksonConfig;
import com.team.hotelbooking.shared.config.PaginationProperties;
import com.team.hotelbooking.shared.pagination.PageResult;
import com.team.hotelbooking.shared.web.GlobalExceptionHandler;
import com.team.hotelbooking.shared.web.PaginationSupport;
import com.team.hotelbooking.shared.web.ProblemDetailFactory;
import java.security.Principal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
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
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("Hợp đồng response của BookingController")
class BookingControllerResponseTests {

  private static final UUID CUSTOMER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
  private static final UUID HOTEL_ID = UUID.fromString("30000000-0000-0000-0000-000000000003");
  private static final UUID ROOM_TYPE_ID = UUID.fromString("40000000-0000-0000-0000-000000000004");
  private static final UUID BOOKING_ID = UUID.fromString("50000000-0000-0000-0000-000000000005");
  private static final Principal PRINCIPAL = CUSTOMER_ID::toString;

  @Mock private CreateBookingUseCase createBookingUseCase;
  @Mock private ListCustomerBookingsUseCase listCustomerBookingsUseCase;
  @Mock private GetCustomerBookingDetailUseCase getCustomerBookingDetailUseCase;
  @Mock private ListManagementBookingsUseCase listManagementBookingsUseCase;
  @Mock private GetManagementBookingDetailUseCase getManagementBookingDetailUseCase;

  private MockMvc mvc;
  private JsonMapper jsonMapper;

  @BeforeEach
  void setUp() {
    JsonMapper.Builder mapperBuilder = JsonMapper.builder();
    new JacksonConfig().jsonCustomizer().customize(mapperBuilder);
    jsonMapper = mapperBuilder.build();
    JacksonJsonHttpMessageConverter converter = new JacksonJsonHttpMessageConverter(mapperBuilder);
    PaginationSupport paginationSupport = new PaginationSupport(new PaginationProperties(10, 50));

    mvc =
        MockMvcBuilders.standaloneSetup(
                new BookingController(
                    createBookingUseCase,
                    listCustomerBookingsUseCase,
                    getCustomerBookingDetailUseCase,
                    paginationSupport),
                new ManagementBookingController(
                    listManagementBookingsUseCase,
                    getManagementBookingDetailUseCase,
                    paginationSupport))
            .setMessageConverters(converter)
            .setControllerAdvice(
                new BookingExceptionHandler(new ProblemDetailFactory()),
                new GlobalExceptionHandler(new ProblemDetailFactory()))
            .build();
  }

  private BookingDetailResult sampleDetailResult() {
    return new BookingDetailResult(
        BOOKING_ID,
        "BK-20261109-ABCD",
        CUSTOMER_ID,
        HOTEL_ID,
        new ContactResult("Nguyễn Văn Đặt", "booker@example.com", "0912345678"),
        new ContactResult("Trần Thị Ở", "guest@example.com", "0987654321"),
        LocalDate.of(2026, 11, 10),
        LocalDate.of(2026, 11, 12),
        3,
        2,
        LocalTime.of(14, 0),
        LocalTime.of(12, 0),
        LocalTime.of(8, 0),
        LocalTime.of(12, 0),
        2,
        1,
        "PENDING_PAYMENT",
        "VND",
        2_000_000L,
        500_000L,
        0L,
        200_000L,
        700_000L,
        2_700_000L,
        0L,
        2_700_000L,
        Instant.parse("2026-11-09T02:15:00Z"),
        Instant.parse("2026-11-09T02:00:00Z"),
        Instant.parse("2026-11-09T02:00:00Z"),
        List.of(
            new BookingItemResult(
                UUID.fromString("60000000-0000-0000-0000-000000000006"),
                ROOM_TYPE_ID,
                "Deluxe King",
                1,
                2,
                1,
                1,
                2_000_000L,
                500_000L,
                0L,
                200_000L,
                700_000L,
                2_700_000L,
                List.of(
                    new NightlyPriceResult(LocalDate.of(2026, 11, 10), 1_000_000L),
                    new NightlyPriceResult(LocalDate.of(2026, 11, 11), 1_000_000L)))),
        new PaymentHandoffResult(
            BOOKING_ID,
            "BK-20261109-ABCD",
            HOTEL_ID,
            CUSTOMER_ID,
            2_000_000L,
            700_000L,
            2_700_000L,
            0L,
            2_700_000L,
            "VND",
            "PENDING_PAYMENT",
            Instant.parse("2026-11-09T02:15:00Z")));
  }

  @Nested
  @DisplayName("POST /api/v1/bookings")
  class CreateBookingApiTests {

    @Test
    @DisplayName(
        "Trả về 201 Created cùng chi tiết đơn, người đặt/người ở và thông tin bàn giao thanh toán")
    void shouldReturn201WhenBookingCreated() throws Exception {
      // Given
      CreateBookingRequest request =
          new CreateBookingRequest(
              HOTEL_ID,
              LocalDate.of(2026, 11, 10),
              LocalDate.of(2026, 11, 12),
              LocalTime.of(8, 0),
              LocalTime.of(12, 0),
              new ContactRequest("Nguyễn Văn Đặt", "booker@example.com", "0912345678"),
              new ContactRequest("Trần Thị Ở", "guest@example.com", "0987654321"),
              List.of(new BookingItemRequest(ROOM_TYPE_ID, 1, 2, 1)),
              2_700_000L);

      given(createBookingUseCase.execute(any())).willReturn(sampleDetailResult());

      // When & Then
      mvc.perform(
              post("/api/v1/bookings")
                  .principal(PRINCIPAL)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(jsonMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(header().string("Cache-Control", "no-store"))
          .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.message").value("Tạo đơn đặt phòng thành công."))
          .andExpect(jsonPath("$.data.id").value(BOOKING_ID.toString()))
          .andExpect(jsonPath("$.data.bookingCode").value("BK-20261109-ABCD"))
          .andExpect(jsonPath("$.data.days").value(3))
          .andExpect(jsonPath("$.data.nights").value(2))
          .andExpect(jsonPath("$.data.booker.fullName").value("Nguyễn Văn Đặt"))
          .andExpect(jsonPath("$.data.guest.fullName").value("Trần Thị Ở"))
          .andExpect(jsonPath("$.data.roomSubtotalAmount").value(2000000))
          .andExpect(jsonPath("$.data.surchargeAmount").value(700000))
          .andExpect(jsonPath("$.data.totalAmount").value(2700000))
          .andExpect(jsonPath("$.data.paymentHandoff.payableAmount").value(2700000))
          .andExpect(jsonPath("$.data.items", hasSize(1)));
    }

    @Test
    @DisplayName("Trả về 409 Conflict khi giá tính lại khác với báo giá khách gửi lên")
    void shouldReturn409WhenPriceChanged() throws Exception {
      // Given
      CreateBookingRequest request =
          new CreateBookingRequest(
              HOTEL_ID,
              LocalDate.of(2026, 11, 10),
              LocalDate.of(2026, 11, 12),
              null,
              null,
              new ContactRequest("Nguyễn Văn Đặt", "booker@example.com", "0912345678"),
              null,
              List.of(new BookingItemRequest(ROOM_TYPE_ID, 1, 2, 1)),
              2_500_000L);

      given(createBookingUseCase.execute(any()))
          .willThrow(new BookingException(BookingErrorCode.PRICE_CHANGED));

      // When & Then
      mvc.perform(
              post("/api/v1/bookings")
                  .principal(PRINCIPAL)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(jsonMapper.writeValueAsString(request)))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code").value("PRICE_CHANGED"));
    }
  }

  @Nested
  @DisplayName("GET /api/v1/bookings & GET /api/v1/bookings/{bookingId}")
  class GetBookingsApiTests {

    @Test
    @DisplayName("Trả về 200 kèm danh sách phân trang các đơn đặt phòng của khách")
    void shouldReturnPaginatedCustomerBookings() throws Exception {
      // Given
      BookingSummaryResult item =
          new BookingSummaryResult(
              BOOKING_ID,
              "BK-20261109-ABCD",
              CUSTOMER_ID,
              HOTEL_ID,
              "Nguyễn Văn Đặt",
              "Trần Thị Ở",
              LocalDate.of(2026, 11, 10),
              LocalDate.of(2026, 11, 12),
              3,
              2,
              LocalTime.of(14, 0),
              LocalTime.of(12, 0),
              2,
              1,
              "PENDING_PAYMENT",
              "VND",
              2_000_000L,
              700_000L,
              2_700_000L,
              0L,
              2_700_000L,
              Instant.parse("2026-11-09T02:15:00Z"),
              Instant.parse("2026-11-09T02:00:00Z"));

      given(listCustomerBookingsUseCase.execute(any()))
          .willReturn(new PageResult<>(List.of(item), 0, 10, 1L, 1, true, true));

      // When & Then
      mvc.perform(get("/api/v1/bookings").principal(PRINCIPAL))
          .andExpect(status().isOk())
          .andExpect(header().string("Cache-Control", "no-store"))
          .andExpect(jsonPath("$.message").value("Lấy danh sách đơn đặt phòng thành công."))
          .andExpect(jsonPath("$.data.content", hasSize(1)))
          .andExpect(jsonPath("$.data.content[0].bookingCode").value("BK-20261109-ABCD"))
          .andExpect(jsonPath("$.data.content[0].bookerName").value("Nguyễn Văn Đặt"))
          .andExpect(jsonPath("$.data.content[0].guestName").value("Trần Thị Ở"))
          .andExpect(jsonPath("$.data.content[0].totalAmount").value(2700000));
    }

    @Test
    @DisplayName("Trả về 200 kèm chi tiết đơn đặt phòng của khách")
    void shouldReturnCustomerBookingDetail() throws Exception {
      // Given
      given(getCustomerBookingDetailUseCase.execute(any())).willReturn(sampleDetailResult());

      // When & Then
      mvc.perform(get("/api/v1/bookings/{bookingId}", BOOKING_ID).principal(PRINCIPAL))
          .andExpect(status().isOk())
          .andExpect(header().string("Cache-Control", "no-store"))
          .andExpect(jsonPath("$.message").value("Lấy chi tiết đơn đặt phòng thành công."))
          .andExpect(jsonPath("$.data.id").value(BOOKING_ID.toString()))
          .andExpect(jsonPath("$.data.booker.email").value("booker@example.com"))
          .andExpect(jsonPath("$.data.guest.email").value("guest@example.com"))
          .andExpect(jsonPath("$.data.items[0].nightlyPrices", hasSize(2)));
    }
  }

  @Nested
  @DisplayName(
      "GET /api/v1/management/hotels/{hotelId}/bookings & GET /api/v1/management/hotels/{hotelId}/bookings/{bookingId}")
  class GetManagementBookingsApiTests {

    @Test
    @DisplayName("Trả về 200 kèm danh sách phân trang đơn đặt phòng của khách sạn")
    void shouldReturnPaginatedHotelBookings() throws Exception {
      // Given
      BookingSummaryResult item =
          new BookingSummaryResult(
              BOOKING_ID,
              "BK-20261109-ABCD",
              CUSTOMER_ID,
              HOTEL_ID,
              "Nguyễn Văn Đặt",
              "Trần Thị Ở",
              LocalDate.of(2026, 11, 10),
              LocalDate.of(2026, 11, 12),
              3,
              2,
              LocalTime.of(14, 0),
              LocalTime.of(12, 0),
              2,
              1,
              "PENDING_PAYMENT",
              "VND",
              2_000_000L,
              700_000L,
              2_700_000L,
              0L,
              2_700_000L,
              Instant.parse("2026-11-09T02:15:00Z"),
              Instant.parse("2026-11-09T02:00:00Z"));

      given(listManagementBookingsUseCase.execute(any()))
          .willReturn(new PageResult<>(List.of(item), 0, 10, 1L, 1, true, true));

      // When & Then
      mvc.perform(
              get("/api/v1/management/hotels/{hotelId}/bookings", HOTEL_ID).principal(PRINCIPAL))
          .andExpect(status().isOk())
          .andExpect(header().string("Cache-Control", "no-store"))
          .andExpect(
              jsonPath("$.message").value("Lấy danh sách đơn đặt phòng khách sạn thành công."))
          .andExpect(jsonPath("$.data.content", hasSize(1)))
          .andExpect(jsonPath("$.data.content[0].bookingCode").value("BK-20261109-ABCD"));
    }

    @Test
    @DisplayName("Trả về 200 kèm chi tiết đơn đặt phòng của khách sạn")
    void shouldReturnHotelBookingDetail() throws Exception {
      // Given
      given(getManagementBookingDetailUseCase.execute(any())).willReturn(sampleDetailResult());

      // When & Then
      mvc.perform(
              get("/api/v1/management/hotels/{hotelId}/bookings/{bookingId}", HOTEL_ID, BOOKING_ID)
                  .principal(PRINCIPAL))
          .andExpect(status().isOk())
          .andExpect(header().string("Cache-Control", "no-store"))
          .andExpect(
              jsonPath("$.message").value("Lấy chi tiết đơn đặt phòng khách sạn thành công."))
          .andExpect(jsonPath("$.data.id").value(BOOKING_ID.toString()))
          .andExpect(jsonPath("$.data.paymentHandoff.payableAmount").value(2700000));
    }
  }
}
