package com.team.hotelbooking.hotel.api.controller;

import com.team.hotelbooking.hotel.api.dto.request.CreateHotelRequest;
import com.team.hotelbooking.hotel.api.dto.response.CreateHotelResponse;
import com.team.hotelbooking.hotel.application.command.CreateHotelCommand;
import com.team.hotelbooking.hotel.application.result.CreateHotelResult;
import com.team.hotelbooking.hotel.application.usecase.CreateHotelUseCase;
import com.team.hotelbooking.shared.web.ApiResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/hotels")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminHotelController {

  private final CreateHotelUseCase createHotelUseCase;

  @PostMapping
  public ResponseEntity<ApiResponse<CreateHotelResponse>> createHotel(
      @Valid @RequestBody CreateHotelRequest request, Principal principal) {

    UUID actorId = UUID.fromString(principal.getName());

    CreateHotelCommand command =
        new CreateHotelCommand(
            actorId,
            request.ownerId(),
            request.cityId(),
            request.name(),
            request.description(),
            request.address(),
            request.latitude(),
            request.longitude(),
            request.starRating(),
            request.checkInTime(),
            request.checkOutTime());

    CreateHotelResult result = createHotelUseCase.execute(command);

    CreateHotelResponse response = CreateHotelResponse.from(result);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Tạo khách sạn thành công.", response));
  }
}
