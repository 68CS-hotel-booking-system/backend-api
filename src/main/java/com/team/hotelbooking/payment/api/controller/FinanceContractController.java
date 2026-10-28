package com.team.hotelbooking.payment.api.controller;

import com.team.hotelbooking.payment.api.dto.response.HotelContractDto;
import com.team.hotelbooking.payment.application.command.GetContractsQuery;
import com.team.hotelbooking.payment.application.usecase.GetContractsUseCase;
import com.team.hotelbooking.payment.domain.model.HotelContract;
import com.team.hotelbooking.shared.web.ApiResponse;
import com.team.hotelbooking.shared.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/finance/hotels")
@RequiredArgsConstructor
@Tag(name = "Finance - Contracts", description = "API tra cứu danh sách hợp đồng tài chính")
public class FinanceContractController {

  private final GetContractsUseCase getContractsUseCase;

  @Operation(
      summary = "[FN01] Tra cứu danh sách hợp đồng hoa hồng theo khách sạn",
      description =
          "Lấy danh sách hợp đồng phân trang cho Admin, Chủ khách sạn (Owner) hoặc người được cấp quyền tài chính. "
              + "Hỗ trợ lọc theo trạng thái hợp đồng (DRAFT, ACTIVE, EXPIRED, TERMINATED).")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "Lấy danh sách thành công"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "403",
        description = "Không có quyền truy cập dữ liệu tài chính của khách sạn này",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "Không tìm thấy khách sạn",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  })
  @GetMapping("/{hotelId}/contracts")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<PageResponse<HotelContractDto>>> getContracts(
      @Parameter(description = "ID khách sạn", required = true) @PathVariable UUID hotelId,
      @Parameter(description = "Lọc theo trạng thái hợp đồng") @RequestParam(required = false)
          HotelContract.ContractStatus status,
      @PageableDefault(size = 10) Pageable pageable) {

    Page<HotelContract> pageResult =
        getContractsUseCase.execute(new GetContractsQuery(hotelId, status, null, true, pageable));

    Page<HotelContractDto> dtoPage =
        pageResult.map(
            c ->
                new HotelContractDto(
                    c.id(),
                    c.contractCode(),
                    c.hotelId(),
                    c.ownerUserId(),
                    c.commissionRate(),
                    c.commissionBasis(),
                    c.startsAt(),
                    c.endsAt(),
                    c.status(),
                    c.createdByUserId(),
                    c.createdAt(),
                    c.updatedAt()));

    return ResponseEntity.ok(ApiResponse.success("Thành công.", PageResponse.from(dtoPage)));
  }
}
