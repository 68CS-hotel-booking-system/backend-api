package com.team.hotelbooking.payment.api.controller;

import com.team.hotelbooking.payment.api.dto.request.UpdateFinancialAccessRequest;
import com.team.hotelbooking.payment.api.dto.response.FinancialAccessDetailResponse;
import com.team.hotelbooking.payment.api.dto.response.FinancialAccessDto;
import com.team.hotelbooking.payment.api.dto.response.FinancialAccessHistoryDto;
import com.team.hotelbooking.payment.application.command.GetFinancialAccessQuery;
import com.team.hotelbooking.payment.application.command.UpdateFinancialAccessCommand;
import com.team.hotelbooking.payment.application.usecase.GetFinancialAccessUseCase;
import com.team.hotelbooking.payment.application.usecase.UpdateFinancialAccessUseCase;
import com.team.hotelbooking.payment.domain.model.FinancialAccess;
import com.team.hotelbooking.payment.domain.model.FinancialAccessDetail;
import com.team.hotelbooking.shared.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/hotels")
@RequiredArgsConstructor
@Tag(
    name = "Finance - Access Control",
    description = "API phân quyền truy cập module tài chính và audit log")
public class AdminFinancialAccessController {

  private final UpdateFinancialAccessUseCase updateFinancialAccessUseCase;
  private final GetFinancialAccessUseCase getFinancialAccessUseCase;

  @Operation(
      summary = "[FN00] Cấp hoặc thu hồi quyền truy cập tài chính của khách sạn",
      description =
          "Quản trị viên thiết lập hoặc thu hồi quyền tài chính cho user. Hỗ trợ khóa lạc quan (Optimistic Concurrency) "
              + "qua tham số expectedVersion và tự động ghi log lịch sử audit.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "Cập nhật quyền thành công"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "Dữ liệu yêu cầu không hợp lệ",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "Không tìm thấy khách sạn hoặc người dùng",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "409",
        description = "Xung đột phiên bản dữ liệu (Optimistic Lock Failure)",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  })
  @PutMapping("/{hotelId}/financial-access/{userId}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<FinancialAccessDto>> updateFinancialAccess(
      @Parameter(description = "ID khách sạn", required = true) @PathVariable UUID hotelId,
      @Parameter(description = "ID người dùng được phân quyền", required = true) @PathVariable
          UUID userId,
      @Valid @RequestBody UpdateFinancialAccessRequest request) {

    FinancialAccess result =
        updateFinancialAccessUseCase.execute(
            new UpdateFinancialAccessCommand(
                hotelId,
                userId,
                request.isGranted(),
                request.accessScope(),
                request.reason(),
                request.expectedVersion(),
                null));

    FinancialAccessDto dto =
        new FinancialAccessDto(
            result.id(),
            result.hotelId(),
            result.userId(),
            result.granted(),
            result.accessScope(),
            result.reason(),
            result.grantedByUserId(),
            result.grantedAt(),
            result.version());

    return ResponseEntity.ok(ApiResponse.success("Cập nhật quyền tài chính thành công.", dto));
  }

  @Operation(
      summary = "[FN16] Tra cứu chi tiết quyền tài chính và lịch sử thay đổi của người dùng",
      description =
          "Lấy thông tin quyền tài chính hiện tại kèm toàn bộ lịch sử phân quyền/thu hồi sắp xếp theo phiên bản mới nhất.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "Lấy dữ liệu thành công"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "Không tìm thấy thông tin phân quyền cho người dùng tại khách sạn này",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  })
  @GetMapping("/{hotelId}/financial-access/{userId}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<FinancialAccessDetailResponse>> getFinancialAccess(
      @Parameter(description = "ID khách sạn", required = true) @PathVariable UUID hotelId,
      @Parameter(description = "ID người dùng", required = true) @PathVariable UUID userId) {

    FinancialAccessDetail detail =
        getFinancialAccessUseCase.execute(new GetFinancialAccessQuery(hotelId, userId));

    List<FinancialAccessHistoryDto> historyDtos =
        detail.history().stream()
            .map(
                h ->
                    new FinancialAccessHistoryDto(
                        h.id(),
                        h.version(),
                        h.granted(),
                        h.accessScope(),
                        h.reason(),
                        h.changedByUserId(),
                        h.changedAt()))
            .toList();

    FinancialAccessDetailResponse response =
        new FinancialAccessDetailResponse(
            detail.id(),
            detail.hotelId(),
            detail.userId(),
            detail.userName(),
            detail.userEmail(),
            detail.granted(),
            detail.accessScope(),
            detail.reason(),
            detail.grantedByUserId(),
            detail.grantedAt(),
            detail.version(),
            historyDtos);

    return ResponseEntity.ok(ApiResponse.success("Thành công.", response));
  }
}
