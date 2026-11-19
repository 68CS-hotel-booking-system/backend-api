package com.team.hotelbooking.inventory.api.controller;

import com.team.hotelbooking.inventory.api.dto.request.CreateRoomBlockRequest;
import com.team.hotelbooking.inventory.api.dto.request.ReleaseRoomBlockRequest;
import com.team.hotelbooking.inventory.api.dto.request.UpdateRoomBlockRequest;
import com.team.hotelbooking.inventory.api.dto.response.RoomBlockResponse;
import com.team.hotelbooking.inventory.api.dto.response.RoomCalendarResponse;
import com.team.hotelbooking.inventory.application.command.GetRoomCalendarQuery;
import com.team.hotelbooking.inventory.application.command.ListRoomBlocksQuery;
import com.team.hotelbooking.inventory.application.result.RoomBlockResult;
import com.team.hotelbooking.inventory.application.result.RoomCalendarResult;
import com.team.hotelbooking.inventory.application.usecase.CreateRoomBlockUseCase;
import com.team.hotelbooking.inventory.application.usecase.GetRoomCalendarUseCase;
import com.team.hotelbooking.inventory.application.usecase.ListRoomBlocksUseCase;
import com.team.hotelbooking.inventory.application.usecase.ReleaseRoomBlockUseCase;
import com.team.hotelbooking.inventory.application.usecase.UpdateRoomBlockUseCase;
import com.team.hotelbooking.shared.web.ApiResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/management/hotels/{hotelId}")
@RequiredArgsConstructor
public class ManagementRoomBlockController {

  private final CreateRoomBlockUseCase createRoomBlockUseCase;
  private final UpdateRoomBlockUseCase updateRoomBlockUseCase;
  private final ReleaseRoomBlockUseCase releaseRoomBlockUseCase;
  private final ListRoomBlocksUseCase listRoomBlocksUseCase;
  private final GetRoomCalendarUseCase getRoomCalendarUseCase;

  @PostMapping("/room-blocks")
  @PreAuthorize("hasAnyRole('ADMIN', 'HOTEL_OWNER', 'HOTEL_MANAGER')")
  public ResponseEntity<ApiResponse<RoomBlockResponse>> createRoomBlock(
      @PathVariable("hotelId") UUID hotelId,
      @Valid @RequestBody CreateRoomBlockRequest request,
      Principal principal) {
    UUID actorId = UUID.fromString(principal.getName());
    RoomBlockResult result = createRoomBlockUseCase.execute(request.toCommand(actorId, hotelId));
    RoomBlockResponse response = RoomBlockResponse.from(result);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Tạo lịch chặn phòng thành công.", response));
  }

  @PatchMapping("/room-blocks/{blockId}")
  @PreAuthorize("hasAnyRole('ADMIN', 'HOTEL_OWNER', 'HOTEL_MANAGER')")
  public ResponseEntity<ApiResponse<RoomBlockResponse>> updateRoomBlock(
      @PathVariable("hotelId") UUID hotelId,
      @PathVariable("blockId") UUID blockId,
      @Valid @RequestBody UpdateRoomBlockRequest request,
      Principal principal) {
    UUID actorId = UUID.fromString(principal.getName());
    RoomBlockResult result =
        updateRoomBlockUseCase.execute(request.toCommand(actorId, hotelId, blockId));
    RoomBlockResponse response = RoomBlockResponse.from(result);
    return ResponseEntity.ok(ApiResponse.success("Cập nhật lịch chặn phòng thành công.", response));
  }

  @PostMapping("/room-blocks/{blockId}/release")
  @PreAuthorize("hasAnyRole('ADMIN', 'HOTEL_OWNER', 'HOTEL_MANAGER')")
  public ResponseEntity<ApiResponse<RoomBlockResponse>> releaseRoomBlock(
      @PathVariable("hotelId") UUID hotelId,
      @PathVariable("blockId") UUID blockId,
      @Valid @RequestBody ReleaseRoomBlockRequest request,
      Principal principal) {
    UUID actorId = UUID.fromString(principal.getName());
    RoomBlockResult result =
        releaseRoomBlockUseCase.execute(request.toCommand(actorId, hotelId, blockId));
    RoomBlockResponse response = RoomBlockResponse.from(result);
    return ResponseEntity.ok(
        ApiResponse.success("Giải phóng lịch chặn phòng thành công.", response));
  }

  @GetMapping("/room-blocks")
  @PreAuthorize("hasAnyRole('ADMIN', 'HOTEL_OWNER', 'HOTEL_MANAGER', 'RECEPTIONIST')")
  public ResponseEntity<ApiResponse<List<RoomBlockResponse>>> listRoomBlocks(
      @PathVariable("hotelId") UUID hotelId,
      @RequestParam(name = "roomTypeId", required = false) UUID roomTypeId,
      @RequestParam(name = "roomId", required = false) UUID roomId,
      @RequestParam(name = "startDate", required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate startDate,
      @RequestParam(name = "endDate", required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate endDate,
      Principal principal) {
    UUID actorId = principal != null ? UUID.fromString(principal.getName()) : null;
    List<RoomBlockResult> result =
        listRoomBlocksUseCase.execute(
            new ListRoomBlocksQuery(actorId, hotelId, roomTypeId, roomId, startDate, endDate));
    List<RoomBlockResponse> response = RoomBlockResponse.from(result);
    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(ApiResponse.success("Lấy danh sách lịch chặn phòng thành công.", response));
  }

  @GetMapping("/room-calendar")
  @PreAuthorize("hasAnyRole('ADMIN', 'HOTEL_OWNER', 'HOTEL_MANAGER', 'RECEPTIONIST')")
  public ResponseEntity<ApiResponse<RoomCalendarResponse>> getRoomCalendar(
      @PathVariable("hotelId") UUID hotelId,
      @RequestParam(name = "roomTypeId", required = false) UUID roomTypeId,
      @RequestParam(name = "startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate startDate,
      @RequestParam(name = "endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate endDate,
      Principal principal) {
    UUID actorId = principal != null ? UUID.fromString(principal.getName()) : null;
    RoomCalendarResult result =
        getRoomCalendarUseCase.execute(
            new GetRoomCalendarQuery(actorId, hotelId, roomTypeId, startDate, endDate));
    RoomCalendarResponse response = RoomCalendarResponse.from(result);
    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(ApiResponse.success("Lấy lịch phòng thành công.", response));
  }
}
