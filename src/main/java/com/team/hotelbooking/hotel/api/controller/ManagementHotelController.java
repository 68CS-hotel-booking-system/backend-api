package com.team.hotelbooking.hotel.api.controller;

import com.team.hotelbooking.hotel.api.dto.request.UpdateHotelRequest;
import com.team.hotelbooking.hotel.api.dto.response.ManagementHotelResponse;
import com.team.hotelbooking.hotel.application.command.GetManagementHotelQuery;
import com.team.hotelbooking.hotel.application.command.ListManagementHotelsQuery;
import com.team.hotelbooking.hotel.application.command.UpdateHotelCommand;
import com.team.hotelbooking.hotel.application.result.ManagementHotelResult;
import com.team.hotelbooking.hotel.application.usecase.GetManagementHotelUseCase;
import com.team.hotelbooking.hotel.application.usecase.ListManagementHotelsUseCase;
import com.team.hotelbooking.hotel.application.usecase.UpdateHotelUseCase;
import com.team.hotelbooking.shared.pagination.PageQuery;
import com.team.hotelbooking.shared.pagination.PageResult;
import com.team.hotelbooking.shared.security.CurrentActor;
import com.team.hotelbooking.shared.web.ApiResponse;
import com.team.hotelbooking.shared.web.PageResponse;
import com.team.hotelbooking.shared.web.PaginationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Quyền đặt theo từng method: PATCH mở cho HOTEL_MANAGER, mà quản lý không phải role hệ thống
// (lấy từ hotel_members), nên không thể giữ hasAnyRole cấp class như trước.
@RestController
@RequestMapping("/api/v1/management/hotels")
@RequiredArgsConstructor
public class ManagementHotelController {

  private final GetManagementHotelUseCase getManagementHotelUseCase;
  private final ListManagementHotelsUseCase listManagementHotelsUseCase;
  private final UpdateHotelUseCase updateHotelUseCase;
  private final PaginationSupport paginationSupport;

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'HOTEL_OWNER')")
  public ResponseEntity<ApiResponse<PageResponse<ManagementHotelResponse>>> listHotels(
      @RequestParam(name = "page", required = false) Integer page,
      @RequestParam(name = "size", required = false) Integer size,
      Principal principal) {
    PageQuery pagination = paginationSupport.resolve(page, size);
    ListManagementHotelsQuery query =
        new ListManagementHotelsQuery(UUID.fromString(principal.getName()), pagination);

    PageResult<ManagementHotelResult> result = listManagementHotelsUseCase.execute(query);

    PageResponse<ManagementHotelResponse> response = ManagementHotelResponse.from(result);

    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(ApiResponse.success("Lấy danh sách khách sạn quản lý thành công.", response));
  }

  @GetMapping("/{hotelId}")
  @PreAuthorize("hasAnyRole('ADMIN', 'HOTEL_OWNER')")
  public ResponseEntity<ApiResponse<ManagementHotelResponse>> getHotel(
      @PathVariable("hotelId") UUID hotelId, Principal principal) {
    GetManagementHotelQuery query =
        new GetManagementHotelQuery(UUID.fromString(principal.getName()), hotelId);

    ManagementHotelResult result = getManagementHotelUseCase.execute(query);

    ManagementHotelResponse response = ManagementHotelResponse.from(result);

    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(ApiResponse.success("Lấy thông tin quản lý khách sạn thành công.", response));
  }

  // ADMIN, Owner hoặc HOTEL_MANAGER của đúng hotel; quyền theo hotel kiểm trong use case.
  @Operation(
      summary = "Sửa thông tin khách sạn",
      description =
          "Cập nhật một phần: field không gửi hoặc null được giữ nguyên, chưa hỗ trợ xóa giá trị."
              + " Chuỗi rỗng/khoảng trắng trả 400, không coi là xóa. latitude và longitude phải"
              + " gửi cùng nhau, trong miền [-90, 90] và [-180, 180]; starRating từ 1 đến 5."
              + " Body chỉ có version trả 400 INVALID_REQUEST. Chỉ ADMIN, Owner của khách sạn hoặc"
              + " HOTEL_MANAGER đang hoạt động của đúng khách sạn đó được sửa. Thành viên khách sạn"
              + " không đủ quyền (RECEPTIONIST) nhận 403; người không thuộc khách sạn hoặc khách"
              + " sạn không tồn tại nhận 404. Version lệch trả 409 CONCURRENT_UPDATE.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "Cập nhật thành công, trả version mới"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "Dữ liệu không hợp lệ hoặc không có field cần cập nhật",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "Chưa đăng nhập",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "403",
        description = "Là thành viên khách sạn nhưng không đủ quyền sửa (RECEPTIONIST)",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "Khách sạn không tồn tại hoặc người gọi không thuộc khách sạn",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "409",
        description = "Version đã cũ, dữ liệu vừa được người khác sửa",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  })
  @PatchMapping("/{hotelId}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<ManagementHotelResponse>> updateHotel(
      @PathVariable("hotelId") UUID hotelId,
      @Valid @RequestBody UpdateHotelRequest request,
      Authentication authentication) {
    UpdateHotelCommand command =
        new UpdateHotelCommand(
            CurrentActor.userId(authentication),
            CurrentActor.isAdmin(authentication),
            hotelId,
            request.name(),
            request.description(),
            request.address(),
            request.contactPhone(),
            request.contactEmail(),
            request.latitude(),
            request.longitude(),
            request.starRating(),
            request.checkInTime(),
            request.checkOutTime(),
            request.version());

    ManagementHotelResult result = updateHotelUseCase.execute(command);

    ManagementHotelResponse response = ManagementHotelResponse.from(result);

    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(ApiResponse.success("Cập nhật thông tin khách sạn thành công.", response));
  }
}
