package com.team.hotelbooking.hotel.api.controller;

import com.team.hotelbooking.hotel.api.dto.request.CreateRoomTypeRequest;
import com.team.hotelbooking.hotel.api.dto.response.CreateRoomTypeResponse;
import com.team.hotelbooking.hotel.api.dto.response.ListManagementRoomTypesResponse;
import com.team.hotelbooking.hotel.application.command.CreateRoomTypeCommand;
import com.team.hotelbooking.hotel.application.command.ListManagementRoomTypesQuery;
import com.team.hotelbooking.hotel.application.result.CreateRoomTypeResult;
import com.team.hotelbooking.hotel.application.result.ListManagementRoomTypesResult;
import com.team.hotelbooking.hotel.application.usecase.CreateRoomTypeUseCase;
import com.team.hotelbooking.hotel.application.usecase.ListManagementRoomTypesUseCase;
import com.team.hotelbooking.shared.web.ApiResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/management/hotels/{hotelId}/room-types")
@PreAuthorize("hasAnyRole('ADMIN', 'HOTEL_OWNER')")
@RequiredArgsConstructor
public class ManagementRoomTypeController {

  private final CreateRoomTypeUseCase createRoomTypeUseCase;
  private final ListManagementRoomTypesUseCase listManagementRoomTypesUseCase;

  @GetMapping
  public ResponseEntity<ApiResponse<List<ListManagementRoomTypesResponse>>> listRoomTypes(
      @PathVariable("hotelId") UUID hotelId, Principal principal) {
    ListManagementRoomTypesQuery query =
        new ListManagementRoomTypesQuery(UUID.fromString(principal.getName()), hotelId);

    List<ListManagementRoomTypesResult> result = listManagementRoomTypesUseCase.execute(query);

    List<ListManagementRoomTypesResponse> response = ListManagementRoomTypesResponse.from(result);

    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(ApiResponse.success("Lấy danh sách loại phòng quản lý thành công.", response));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<CreateRoomTypeResponse>> createRoomType(
      @PathVariable("hotelId") UUID hotelId,
      @Valid @RequestBody CreateRoomTypeRequest request,
      Principal principal) {
    CreateRoomTypeCommand command =
        new CreateRoomTypeCommand(
            UUID.fromString(principal.getName()),
            hotelId,
            request.name(),
            request.description(),
            request.maxAdults(),
            request.maxChildren());

    CreateRoomTypeResult result = createRoomTypeUseCase.execute(command);

    CreateRoomTypeResponse response = CreateRoomTypeResponse.from(result);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Tạo loại phòng thành công.", response));
  }
}
