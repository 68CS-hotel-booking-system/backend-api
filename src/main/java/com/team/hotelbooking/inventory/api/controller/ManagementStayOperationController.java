package com.team.hotelbooking.inventory.api.controller;

import com.team.hotelbooking.inventory.api.dto.request.AssignRoomsRequest;
import com.team.hotelbooking.inventory.api.dto.request.MarkNoShowRequest;
import com.team.hotelbooking.inventory.api.dto.request.RecordLateArrivalRequest;
import com.team.hotelbooking.inventory.api.dto.request.UpdateRoomReadinessRequest;
import com.team.hotelbooking.inventory.api.dto.response.BookingRoomAssignmentsResponse;
import com.team.hotelbooking.inventory.api.dto.response.BookingStayOperationResponse;
import com.team.hotelbooking.inventory.api.dto.response.RoomReadinessResponse;
import com.team.hotelbooking.inventory.application.command.CheckInBookingCommand;
import com.team.hotelbooking.inventory.application.command.CheckOutBookingCommand;
import com.team.hotelbooking.inventory.application.command.MarkNoShowCommand;
import com.team.hotelbooking.inventory.application.result.BookingRoomAssignmentsResult;
import com.team.hotelbooking.inventory.application.result.BookingStayOperationResult;
import com.team.hotelbooking.inventory.application.result.RoomReadinessResult;
import com.team.hotelbooking.inventory.application.usecase.AssignRoomsUseCase;
import com.team.hotelbooking.inventory.application.usecase.CheckInBookingUseCase;
import com.team.hotelbooking.inventory.application.usecase.CheckOutBookingUseCase;
import com.team.hotelbooking.inventory.application.usecase.MarkNoShowUseCase;
import com.team.hotelbooking.inventory.application.usecase.RecordLateArrivalUseCase;
import com.team.hotelbooking.inventory.application.usecase.UpdateRoomReadinessUseCase;
import com.team.hotelbooking.shared.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/management/hotels/{hotelId}")
@RequiredArgsConstructor
@Tag(
    name = "Inventory - Stay Operations",
    description =
        "API phân phòng vật lý và vận hành lưu trú (Check-in, Check-out, Late-arrival, No-show)")
public class ManagementStayOperationController {

  private final AssignRoomsUseCase assignRoomsUseCase;
  private final CheckInBookingUseCase checkInBookingUseCase;
  private final CheckOutBookingUseCase checkOutBookingUseCase;
  private final RecordLateArrivalUseCase recordLateArrivalUseCase;
  private final MarkNoShowUseCase markNoShowUseCase;
  private final UpdateRoomReadinessUseCase updateRoomReadinessUseCase;

  @Operation(
      summary = "[BK-15/16/17] Phân công hoặc đổi phòng vật lý cho đơn đặt phòng",
      description =
          "Gán đủ phòng vật lý cụ thể của đúng loại phòng cho từng hạng mục đặt phòng, "
              + "bảo toàn lịch sử khi đổi phòng và không trừ tồn kho lần hai.")
  @PutMapping("/bookings/{bookingId}/room-assignments")
  @PreAuthorize("hasAnyRole('ADMIN', 'HOTEL_OWNER', 'HOTEL_MANAGER', 'RECEPTIONIST')")
  public ResponseEntity<ApiResponse<BookingRoomAssignmentsResponse>> assignRooms(
      @Parameter(description = "ID khách sạn", required = true) @PathVariable UUID hotelId,
      @Parameter(description = "ID đơn đặt phòng", required = true) @PathVariable UUID bookingId,
      @Valid @RequestBody AssignRoomsRequest request,
      Principal principal) {

    UUID actorId = UUID.fromString(principal.getName());
    BookingRoomAssignmentsResult result =
        assignRoomsUseCase.execute(request.toCommand(actorId, hotelId, bookingId));

    return ResponseEntity.ok(
        ApiResponse.success(
            "Phân công phòng vật lý thành công.", BookingRoomAssignmentsResponse.from(result)));
  }

  @Operation(
      summary = "[BK-08] Làm thủ tục nhận phòng (Check-in)",
      description =
          "Xác nhận khách nhận phòng khi đơn đặt phòng đã phân đủ phòng vật lý và tất cả phòng ở trạng thái READY.")
  @PostMapping("/bookings/{bookingId}/check-in")
  @PreAuthorize("hasAnyRole('ADMIN', 'HOTEL_OWNER', 'HOTEL_MANAGER', 'RECEPTIONIST')")
  public ResponseEntity<ApiResponse<BookingStayOperationResponse>> checkIn(
      @Parameter(description = "ID khách sạn", required = true) @PathVariable UUID hotelId,
      @Parameter(description = "ID đơn đặt phòng", required = true) @PathVariable UUID bookingId,
      Principal principal) {

    UUID actorId = UUID.fromString(principal.getName());
    BookingStayOperationResult result =
        checkInBookingUseCase.execute(new CheckInBookingCommand(actorId, hotelId, bookingId));

    return ResponseEntity.ok(
        ApiResponse.success(
            "Làm thủ tục nhận phòng thành công.", BookingStayOperationResponse.from(result)));
  }

  @Operation(
      summary = "[BK-18, IV-10] Làm thủ tục trả phòng (Check-out)",
      description =
          "Xác nhận khách trả phòng, chuyển trạng thái buồng phòng sang DIRTY và không tự động mở bán lại đêm còn lại nếu trả sớm.")
  @PostMapping("/bookings/{bookingId}/check-out")
  @PreAuthorize("hasAnyRole('ADMIN', 'HOTEL_OWNER', 'HOTEL_MANAGER', 'RECEPTIONIST')")
  public ResponseEntity<ApiResponse<BookingStayOperationResponse>> checkOut(
      @Parameter(description = "ID khách sạn", required = true) @PathVariable UUID hotelId,
      @Parameter(description = "ID đơn đặt phòng", required = true) @PathVariable UUID bookingId,
      Principal principal) {

    UUID actorId = UUID.fromString(principal.getName());
    BookingStayOperationResult result =
        checkOutBookingUseCase.execute(new CheckOutBookingCommand(actorId, hotelId, bookingId));

    return ResponseEntity.ok(
        ApiResponse.success(
            "Làm thủ tục trả phòng thành công.", BookingStayOperationResponse.from(result)));
  }

  @Operation(
      summary = "[BK-09] Ghi nhận lịch khách đến nhận phòng muộn",
      description =
          "Ghi nhận thời điểm dự kiến đến muộn và ghi chú để bảo vệ đơn đặt phòng khỏi bị đánh dấu NO_SHOW trước thời điểm đã hẹn.")
  @PostMapping("/bookings/{bookingId}/late-arrival")
  @PreAuthorize("hasAnyRole('ADMIN', 'HOTEL_OWNER', 'HOTEL_MANAGER', 'RECEPTIONIST')")
  public ResponseEntity<ApiResponse<BookingStayOperationResponse>> recordLateArrival(
      @Parameter(description = "ID khách sạn", required = true) @PathVariable UUID hotelId,
      @Parameter(description = "ID đơn đặt phòng", required = true) @PathVariable UUID bookingId,
      @Valid @RequestBody RecordLateArrivalRequest request,
      Principal principal) {

    UUID actorId = UUID.fromString(principal.getName());
    BookingStayOperationResult result =
        recordLateArrivalUseCase.execute(request.toCommand(actorId, hotelId, bookingId));

    return ResponseEntity.ok(
        ApiResponse.success(
            "Ghi nhận lịch đến muộn thành công.", BookingStayOperationResponse.from(result)));
  }

  @Operation(
      summary = "[BK-10, BK-14, IV-11] Đánh dấu khách không đến nhận phòng (NO_SHOW)",
      description =
          "Đánh dấu đơn đặt phòng NO_SHOW sau khi quá hạn nhận phòng, giải phóng phân công phòng và hoàn trả tồn kho các đêm chưa sử dụng từ ngày hiện tại trở đi.")
  @PostMapping("/bookings/{bookingId}/no-show")
  @PreAuthorize("hasAnyRole('ADMIN', 'HOTEL_OWNER', 'HOTEL_MANAGER', 'RECEPTIONIST')")
  public ResponseEntity<ApiResponse<BookingStayOperationResponse>> markNoShow(
      @Parameter(description = "ID khách sạn", required = true) @PathVariable UUID hotelId,
      @Parameter(description = "ID đơn đặt phòng", required = true) @PathVariable UUID bookingId,
      @Valid @RequestBody(required = false) MarkNoShowRequest request,
      Principal principal) {

    UUID actorId = UUID.fromString(principal.getName());
    MarkNoShowCommand command =
        request != null
            ? request.toCommand(actorId, hotelId, bookingId)
            : new MarkNoShowCommand(actorId, hotelId, bookingId, null);
    BookingStayOperationResult result = markNoShowUseCase.execute(command);

    return ResponseEntity.ok(
        ApiResponse.success(
            "Đánh dấu khách không đến thành công.", BookingStayOperationResponse.from(result)));
  }

  @Operation(
      summary = "Cập nhật trạng thái sẵn sàng buồng phòng (READY / DIRTY)",
      description =
          "Cập nhật trạng thái buồng phòng của một phòng vật lý sang READY sau khi dọn xong hoặc DIRTY khi cần dọn dẹp.")
  @PatchMapping("/rooms/{roomId}/readiness")
  @PreAuthorize("hasAnyRole('ADMIN', 'HOTEL_OWNER', 'HOTEL_MANAGER', 'RECEPTIONIST')")
  public ResponseEntity<ApiResponse<RoomReadinessResponse>> updateRoomReadiness(
      @Parameter(description = "ID khách sạn", required = true) @PathVariable UUID hotelId,
      @Parameter(description = "ID phòng vật lý", required = true) @PathVariable UUID roomId,
      @Valid @RequestBody UpdateRoomReadinessRequest request,
      Principal principal) {

    UUID actorId = UUID.fromString(principal.getName());
    RoomReadinessResult result =
        updateRoomReadinessUseCase.execute(request.toCommand(actorId, hotelId, roomId));

    return ResponseEntity.ok(
        ApiResponse.success(
            "Cập nhật trạng thái buồng phòng thành công.", RoomReadinessResponse.from(result)));
  }
}
