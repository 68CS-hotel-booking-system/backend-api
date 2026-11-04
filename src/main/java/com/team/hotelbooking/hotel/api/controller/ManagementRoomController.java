package com.team.hotelbooking.hotel.api.controller;

import com.team.hotelbooking.hotel.api.dto.request.CreateRoomRequest;
import com.team.hotelbooking.hotel.api.dto.response.CreateRoomResponse;
import com.team.hotelbooking.hotel.api.dto.response.ListManagementRoomsResponse;
import com.team.hotelbooking.hotel.application.command.CreateRoomCommand;
import com.team.hotelbooking.hotel.application.command.ListManagementRoomsQuery;
import com.team.hotelbooking.hotel.application.result.CreateRoomResult;
import com.team.hotelbooking.hotel.application.result.ListManagementRoomsResult;
import com.team.hotelbooking.hotel.application.usecase.CreateRoomUseCase;
import com.team.hotelbooking.hotel.application.usecase.ListManagementRoomsUseCase;
import com.team.hotelbooking.shared.pagination.PageQuery;
import com.team.hotelbooking.shared.pagination.PageResult;
import com.team.hotelbooking.shared.web.ApiResponse;
import com.team.hotelbooking.shared.web.PageResponse;
import com.team.hotelbooking.shared.web.PaginationSupport;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/management/hotels/{hotelId}/rooms")
@PreAuthorize("hasAnyRole('ADMIN', 'HOTEL_OWNER')")
@RequiredArgsConstructor
public class ManagementRoomController {

  private final CreateRoomUseCase createRoomUseCase;
  private final ListManagementRoomsUseCase listManagementRoomsUseCase;
  private final PaginationSupport paginationSupport;

  @GetMapping
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

  @PostMapping
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
}
