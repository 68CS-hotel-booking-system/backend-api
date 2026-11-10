package com.team.hotelbooking.payment.api.controller;

import com.team.hotelbooking.payment.api.dto.response.HotelContractDto;
import com.team.hotelbooking.payment.application.command.GetContractsQuery;
import com.team.hotelbooking.payment.application.result.HotelContractResult;
import com.team.hotelbooking.payment.application.usecase.GetContractsUseCase;
import com.team.hotelbooking.payment.domain.model.HotelContract;
import com.team.hotelbooking.shared.persistence.PageResultMapper;
import com.team.hotelbooking.shared.security.CurrentActor;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/finance/hotels")
@RequiredArgsConstructor
@Tag(
    name = "Payment - Finance Contracts",
    description = "API tra cứu hợp đồng hoa hồng dành cho đối tác/tài chính")
public class FinanceContractController {

  private final GetContractsUseCase getContractsUseCase;

  @Operation(
      summary = "[FN01] Tra cứu danh sách thỏa thuận hoa hồng của khách sạn",
      description =
          "Dành cho Owner hoặc người đại diện được ủy quyền tra cứu các hợp đồng hoa hồng áp dụng cho khách sạn.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "Tra cứu thành công"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "Tham số phân trang hoặc lọc không hợp lệ",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "Chưa đăng nhập",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "403",
        description = "Không phải ADMIN, Owner hoặc người có quyền tài chính của khách sạn",
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
      @PageableDefault(size = 10) Pageable pageable,
      Authentication authentication) {

    Page<HotelContractResult> pageResult =
        getContractsUseCase.execute(
            new GetContractsQuery(
                hotelId,
                status,
                CurrentActor.userId(authentication),
                CurrentActor.isAdmin(authentication),
                pageable));

    return ResponseEntity.ok(
        ApiResponse.success(
            "Thành công.",
            PageResponse.from(PageResultMapper.from(pageResult, HotelContractDto::from))));
  }
}
