package com.team.hotelbooking.hotel.api.controller;

import com.team.hotelbooking.hotel.api.dto.request.CreateRoomRequest;
import com.team.hotelbooking.hotel.api.dto.request.UpdateRoomRequest;
import com.team.hotelbooking.hotel.api.dto.response.CreateRoomResponse;
import com.team.hotelbooking.hotel.api.dto.response.ListManagementRoomsResponse;
import com.team.hotelbooking.hotel.api.dto.response.RoomResponse;
import com.team.hotelbooking.hotel.application.command.CreateRoomCommand;
import com.team.hotelbooking.hotel.application.command.ListManagementRoomsQuery;
import com.team.hotelbooking.hotel.application.result.CreateRoomResult;
import com.team.hotelbooking.hotel.application.result.ListManagementRoomsResult;
import com.team.hotelbooking.hotel.application.result.RoomResult;
import com.team.hotelbooking.hotel.application.usecase.CreateRoomUseCase;
import com.team.hotelbooking.hotel.application.usecase.ListManagementRoomsUseCase;
import com.team.hotelbooking.hotel.application.usecase.UpdateRoomUseCase;
import com.team.hotelbooking.shared.pagination.PageQuery;
import com.team.hotelbooking.shared.pagination.PageResult;
import com.team.hotelbooking.shared.security.CurrentActor;
import com.team.hotelbooking.shared.web.ApiResponse;
import com.team.hotelbooking.shared.web.PageResponse;
import com.team.hotelbooking.shared.web.PaginationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/management/hotels/{hotelId}/rooms")
@RequiredArgsConstructor
@Tag(name = "Hotel Management - Rooms", description = "API quản lý phòng vật lý khách sạn")
public class ManagementRoomController {

  private final CreateRoomUseCase createRoomUseCase;
  private final UpdateRoomUseCase updateRoomUseCase;
  private final ListManagementRoomsUseCase listManagementRoomsUseCase;
  private final PaginationSupport paginationSupport;

  // HT21 bỏ @PreAuthorize cấp class, nên đặt lại quyền của HT19 trên chính method này.
  @Operation(summary = "[HT19] Danh sách phòng vật lý quản lý, có phân trang và lọc roomTypeId")
  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'HOTEL_OWNER')")
  public ResponseEntity<ApiResponse<PageResponse<ListManagementRoomsResponse>>> listRooms(
      @PathVariable("hotelId") UUID hotelId,
      @RequestParam(name = "roomTypeId", required = false) UUID roomTypeId,
      @RequestParam(name = "page", required = false) Integer page,
      @RequestParam(name = "size", required = false) Integer size,
      Principal principal) {
    PageQuery pagination = paginationSupport.resolve(page, size);
    ListManagementRoomsQuery query =
        new ListManagementRoomsQuery(
            UUID.fromString(principal.getName()), hotelId, roomTypeId, pagination);

    PageResult<ListManagementRoomsResult> result = listManagementRoomsUseCase.execute(query);

    PageResponse<ListManagementRoomsResponse> response = ListManagementRoomsResponse.from(result);

    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(ApiResponse.success("Lấy danh sách phòng quản lý thành công.", response));
  }

  @Operation(summary = "Tạo mới phòng vật lý cho khách sạn")
  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'HOTEL_OWNER')")
  public ResponseEntity<ApiResponse<CreateRoomResponse>> createRoom(
      @PathVariable("hotelId") UUID hotelId,
      @Valid @RequestBody CreateRoomRequest request,
      Principal principal) {
    CreateRoomCommand command =
        new CreateRoomCommand(
            UUID.fromString(principal.getName()),
            hotelId,
            request.roomTypeId(),
            request.roomNumber(),
            request.floor(),
            request.operationalFrom(),
            request.operationalUntil());

    CreateRoomResult result = createRoomUseCase.execute(command);

    CreateRoomResponse response = CreateRoomResponse.from(result);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Tạo phòng thành công.", response));
  }

  @Operation(
      summary = "[HT21] Cập nhật thông tin hoặc ngừng sử dụng phòng vật lý",
      description =
          "Chỉnh sửa số phòng, tầng, trạng thái buồng phòng (ready, cleaning, maintenance) hoặc ngừng sử dụng phòng (active=false). Chỉ ADMIN, Owner của khách sạn hoặc HOTEL_MANAGER đang hoạt động của đúng khách sạn đó được sửa.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "Cập nhật thành công"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "Thông tin phòng không hợp lệ",
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
        description =
            "Khách sạn không tồn tại, người gọi không thuộc khách sạn, hoặc phòng không thuộc"
                + " khách sạn",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "409",
        description = "Số phòng đã tồn tại trong khách sạn",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  })
  @PatchMapping("/{roomId}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<RoomResponse>> updateRoom(
      @Parameter(description = "ID khách sạn", required = true) @PathVariable UUID hotelId,
      @Parameter(description = "ID phòng vật lý", required = true) @PathVariable UUID roomId,
      @Valid @RequestBody UpdateRoomRequest request,
      Authentication authentication) {

    RoomResult result =
        updateRoomUseCase.execute(
            request.toCommand(
                CurrentActor.userId(authentication),
                CurrentActor.isAdmin(authentication),
                hotelId,
                roomId));

    return ResponseEntity.ok(
        ApiResponse.success("Cập nhật phòng thành công.", RoomResponse.from(result)));
  }
}
