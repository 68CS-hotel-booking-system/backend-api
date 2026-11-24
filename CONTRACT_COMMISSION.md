# Hợp đồng và doanh thu hoa hồng

## Phạm vi

Mỗi hợp đồng áp dụng cho một khách sạn. Hệ thống kiểm soát điều kiện mở bán và tính hoa hồng; ký kết, đối soát và chi trả owner được xử lý ngoài hệ thống. Không triển khai hủy đơn hoặc hoàn tiền trong tính năng này.

## Hiệu lực và gia hạn

- Có hiệu lực khi `ACTIVE` và `startsAt <= now < endsAt`. Thời gian sử dụng UTC/Clock của backend, không phụ thuộc đồng hồ trình duyệt.
- Hợp đồng mới chỉ dùng `AFTER_DISCOUNT`. Owner phải trùng owner của khách sạn; rate 0–100%, tối đa hai số thập phân. Mã không trùng và các khoảng ACTIVE không chồng nhau.
- Admin tạo dự thảo, kích hoạt, chấm dứt. `POST /api/v1/admin/hotels/{hotelId}/contracts/{contractId}/renewals` tạo hợp đồng kế tiếp và lưu quan hệ gia hạn, không sửa hợp đồng cũ. Body cùng cấu trúc tạo hợp đồng.
- Ngày bắt đầu hợp đồng kế tiếp không trước ngày kết thúc hợp đồng gốc. Có thể chuẩn bị/kích hoạt trước để hiệu lực nối tiếp. Dự thảo chưa mở bán; cần kích hoạt.
- Danh sách hợp đồng trả trạng thái EXPIRED cho ACTIVE đã qua endsAt; bộ lọc EXPIRED dùng cùng quy tắc. Không cần job đổi trạng thái DB để chặn bán đúng mốc.
- Hợp đồng cũ dùng căn cứ khác AFTER_DISCOUNT được giữ nguyên lịch sử nhưng không được chọn cho booking mới. Dashboard báo cần thay hợp đồng; admin chấm dứt và tạo hợp đồng phù hợp nếu cần.

## Khách sạn và booking

- Danh sách công khai và tìm kiếm chỉ chọn khách sạn ACTIVE có hợp đồng hiệu lực. Lọc trước phân trang; tìm kiếm dùng thêm một truy vấn theo lô cho hợp đồng, không N+1 theo khách sạn.
- Link chi tiết khách sạn ACTIVE còn truy cập được và trả `acceptingBookings`; false thì client chỉ thông báo tạm ngừng nhận đặt phòng.
- Backend từ chối tạo booking mới với `409 CONTRACT_NOT_EFFECTIVE` nếu không có hợp đồng phù hợp. Transaction rollback cả booking, snapshot và giữ phòng khi thất bại.
- Không đổi trạng thái nghiệp vụ khách sạn thành DRAFT/LOCKED khi hết hợp đồng. Dashboard quản lý vẫn thấy khách sạn.
- Booking đã tạo tiếp tục được xem và thanh toán trong hạn giữ phòng; không yêu cầu hợp đồng còn hiệu lực tại lúc thanh toán hoặc ngày lưu trú.

## Công thức và dữ liệu

`booking_commissions` lưu snapshot riêng, không xuất hiện trong API booking khách hàng:

```text
căn cứ = max(0, tiền phòng - giảm giá)
hoa hồng nền tảng = căn cứ × rate / 100, HALF_UP đến đồng
khoản owner = tổng tiền booking - hoa hồng nền tảng
```

Phụ thu thuộc khoản owner; giảm giá được khấu trừ vào tiền phòng trước khi tính hoa hồng. Rate, owner và hợp đồng được chụp tại lúc tạo booking. Booking cũ không được backfill bằng rate hiện tại.

## Dashboard

Chỉ admin và owner được truy cập tài chính theo phạm vi của mình. Đã gỡ API GET/PUT `/api/v1/admin/hotels/{hotelId}/financial-access/{userId}` và màn hình cấp quyền tài chính. Danh sách hợp đồng chỉ cho admin hoặc chủ sở hữu hiện tại của khách sạn; quyền cấp thủ công trước đây không còn tác dụng. Các bảng `hotel_financial_access` và `hotel_financial_access_history` được giữ để bảo toàn dữ liệu/lịch sử cũ, không còn được ứng dụng sử dụng để cấp quyền.

`GET /api/v1/finance/dashboard`: ADMIN xem toàn bộ khách sạn; HOTEL_OWNER xem khoản tiền thuộc mình theo snapshot người hưởng trong hợp đồng, kể cả khách sạn đã đổi chủ. Phạm vi lấy từ tài khoản đăng nhập, đối chiếu tài khoản đang hoạt động/đã xác minh email và role hiện tại trong DB. Không nhận ownerId từ client để mở rộng phạm vi. Header `Cache-Control: no-store`.

- Tiền đã thu: tổng giao dịch SUCCEEDED, kể cả tiền thanh toán chưa đủ điều kiện ghi nhận doanh thu.
- Doanh thu lưu trú hoàn thành: tổng giá trị đơn CHECKED_OUT.
- Hoa hồng và khoản owner: chỉ cộng snapshot của đơn CHECKED_OUT.
- Tổng khoản chia được cộng từ số tiền đã làm tròn riêng cho từng booking; không lấy tổng tiền phòng nhân rate hiện tại.
- `contracts`: bảng phân chia theo hợp đồng, khách sạn và rate trong snapshot; trả số đơn hoàn thành, tổng giá trị đơn, tiền phòng sau giảm, khoản nền tảng và owner. Mã và thời hạn hợp đồng được đọc theo contractId đã lưu; không lấy hợp đồng đang mở bán để tính lại.
- Khi khách sạn đổi chủ, chủ mới không kế thừa khoản chia/tiền đã thu của booking có snapshot thuộc chủ cũ. Chủ cũ vẫn thấy phần doanh thu của mình; `canManageHotel=false` để FE không mở liên kết quản lý khách sạn/hợp đồng ngoài phạm vi hiện tại.
- Booking cũ thiếu snapshot chỉ được tính vào doanh thu gộp và cảnh báo ở khách sạn owner đang quản lý; không tự phân bổ khoản chia cho owner hiện tại hay owner cũ khi chưa có bằng chứng.
- `missingCommissionCount`: số đơn hoàn thành chưa có snapshot; doanh thu vẫn được ghi nhận nhưng không suy diễn khoản chia.
- `hotels` giữ danh sách khách sạn trong phạm vi quản lý hiện tại, kèm trạng thái hợp đồng và điều kiện mở bán. Cảnh báo sắp hết hạn trong 7 ngày được hiển thị ở frontend. Danh sách này tách khỏi bảng doanh thu hợp đồng lịch sử.
- Báo cáo là toàn bộ thời gian, chưa có điều chỉnh hoàn tiền hay ghi nhận chi trả owner; không phải báo cáo lợi nhuận hoặc số dư ngân hàng.

## Migration và tích hợp

Mã hợp đồng mới do BE tự sinh theo dạng `HD-2026-000001`, gồm năm tạo theo múi giờ Việt Nam và số thứ tự toàn hệ thống (không reset theo năm). Tạo mới và gia hạn không nhận `contractCode` trong request; response vẫn trả mã để hiển thị. Mã hợp đồng cũ giữ nguyên.

Migration `V20261124120000000__generate_contract_codes.sql` khởi tạo sequence sau số lớn nhất của mã cũ cùng định dạng. Sequence tránh trùng khi tạo đồng thời; số đã cấp không được tái sử dụng sau rollback nên có thể có khoảng trống.

Migration mới `V20261122120000000__booking_commission_snapshots.sql` tạo bảng snapshot và quan hệ gia hạn; không tạo hợp đồng sản xuất tự động. Khi triển khai, khách sạn chưa có hợp đồng hiệu lực sẽ bị ẩn khỏi danh sách bán. Admin cần tạo/kích hoạt hợp đồng theo thỏa thuận thực tế.

Payment triển khai các interface công khai `HotelSalesPolicy` và `BookingCommissionCapture`; Hotel/Booking không phụ thuộc ngược vào Payment. Booking tự lưu và tổng hợp bảng snapshot của mình qua interface công khai. Không bỏ kiểm tra vòng phụ thuộc của Spring Modulith.

Kiểm thử chính: `ContractCommissionIntegrationTests`, `ContractLifecycleIntegrationTests`, `HotelSearchIntegrationTests`, `PublicHotelIntegrationTests`, `ModularityTests`; frontend có test API/dashboard và kiểm tra Chrome với API giả lập trong scratch.
