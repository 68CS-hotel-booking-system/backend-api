# Phạm vi, vai trò và quy tắc nghiệp vụ theo module

> Bản nháp v0.8 — ngày 22/10/2026 — đồng bộ DBML 40 bảng/65 FK: hotel_contracts, booking_financials, hotel_payouts và promotions.funding_source. Snapshot bên tài trợ ở promotion_redemptions còn thiếu; các ràng buộc tài chính và chính sách vận hành chưa được coi đã triển khai. Vẫn trả trước 100% để xác nhận, chưa đặt cọc một phần.
>
> Tên 7 module lấy từ cấu trúc repo hiện tại. Nội dung ghi “Đã chốt” phản ánh quyết định của người phụ trách nghiệp vụ; các mục còn lại tiếp tục là đề xuất để thảo luận. Tài liệu mô tả yêu cầu, không khẳng định chức năng đã được triển khai.

## 1. Mục tiêu và phạm vi chung

Xây dựng hệ thống giúp khách tìm khách sạn, xem phòng còn trống và giá theo ngày, đặt phòng, thanh toán, theo dõi và hủy đơn theo chính sách. Khách đặt theo loại phòng và số lượng; nhân sự quản lý phòng vật lý, gán phòng cụ thể và tiếp nhận khách.

### 1.1. Phạm vi và trạng thái thống nhất

| Mã | Phạm vi đang dùng | Trạng thái và hệ quả |
| --- | --- | --- |
| PV-01 | Hệ thống phục vụ nhiều khách sạn | Đề xuất. Mỗi khách sạn có nhân sự và dữ liệu quản lý riêng; chưa mở quy trình chủ khách sạn tự đăng ký tham gia. |
| PV-02 | Khách đặt theo loại phòng và số lượng; nhân viên gán phòng vật lý | Đã chốt. Ví dụ khách đặt 2 phòng Deluxe, nhân viên gán 301 và 302 trước hoặc khi check-in. Khách không chọn số phòng khi đặt. |
| PV-03 | Một đơn thuộc một khách sạn, có thể gồm nhiều loại phòng | Đề xuất. Đặt ở hai khách sạn phải tạo hai đơn; tất cả hạng mục trong một đơn cùng ngày nhận và trả. |
| PV-04 | Đặt phòng cần đăng ký, đăng nhập và xác minh email một lần | Đã chốt. Khách chưa xác minh email không được tạo đơn; không yêu cầu xác minh lại ở mỗi lần đặt. |
| PV-05 | Trả trước toàn bộ để xác nhận đơn | Đã chốt thanh toán 100% trước khi xác nhận. Hạn giữ chỗ độc lập với giờ check-in; thời lượng 15 phút vẫn là đề xuất, được giải thích ở IV-06. |
| PV-06 | Chỉ dùng VND, ngày kinh doanh theo giờ Việt Nam | Đề xuất. Chưa có chuyển đổi ngoại tệ hoặc khách sạn ở nhiều múi giờ. |
| PV-07 | Có giờ nhận/trả phòng mặc định theo khách sạn | Đề xuất mặc định nhận từ 14:00, trả trước hoặc lúc 12:00. Giờ check-in không phải hạn đặt phòng trong ngày hoặc hạn thanh toán. Hạn đặt trong ngày cần chốt riêng; khách báo đến muộn theo BK-13. |
| PV-08 | Chính sách hủy và khách không đến | Đã chốt NO_SHOW giữ toàn bộ tiền đã thu, không hoàn và không thu thêm. Mốc hủy miễn phí 24 giờ và phí hủy muộn vẫn là đề xuất riêng. |
| PV-09 | Giá phòng trọn gói, có voucher cơ bản | Theo yêu cầu mới: có voucher, điều kiện áp dụng, cách giảm và trần giảm; thay quyết định không voucher trước đó. Một đơn dùng tối đa một mã theo UNIQUE booking_id trong DBML. |
| PV-10 | Quản lý phòng vật lý và khả năng bàn giao | Đã chốt danh mục số phòng, tầng, loại phòng; kiểm tra trùng lịch và bảo trì; gán đủ phòng để check-in; trả phòng chuyển chờ dọn, xác nhận sẵn sàng trước khi giao khách tiếp theo. |
| PV-11 | Đặc tả chặn phòng, đánh giá và voucher | Chặn phòng thuộc inventory; voucher cơ bản đã được yêu cầu trong phạm vi mới. Đánh giá vẫn là phần đề xuất cần duyệt vào MVP; các chính sách chưa xác nhận được ghi riêng. |
| PV-12 | Hạn chế giao dịch ngoài hệ thống | Đã chọn: trả trước 100% qua hệ thống, chỉ xác nhận bằng kết quả cổng đã xác thực; hạn chế liên hệ trước CONFIRMED ở cả API; kiểm duyệt nội dung; lưu lịch sử hủy/hoàn, theo dõi hủy bất thường; có voucher, lịch sử đặt và hỗ trợ khiếu nại. Nền tảng thu hộ, giữ khoản chưa chi và trả Owner đã được chọn; xem PM-15 đến PM-20. |
| PV-13 | Tài chính nền tảng và Owner | Đã có trong DBML 40 bảng: hotel_contracts quản lý thỏa thuận hoa hồng riêng, booking_financials lưu phần từng bên theo booking, hotel_payouts lưu lần chi và promotions.funding_source là PLATFORM/HOTEL. Lưu lại bên tài trợ khi áp mã đã được chốt về nghiệp vụ nhưng promotion_redemptions chưa có cột này. Có thiết kế không đồng nghĩa đã chạy migration/API. |

### 1.2. Ngoài phạm vi phiên bản đầu

- Chủ khách sạn tự đăng ký và duyệt đối tác. Thu hộ, hợp đồng hoa hồng và chi trả đã thuộc phạm vi; chưa mở ví Owner, rút tiền tùy ý hoặc hệ thống kế toán đầy đủ.
- Giao diện sơ đồ tầng trực quan, khách tự chọn số phòng khi đặt, chuyển phòng trong kỳ lưu trú và nâng hạng phòng. Danh mục phòng vật lý, gán phòng, ghi nhận chờ dọn/sẵn sàng và lịch ngừng phục vụ đã thuộc phạm vi.
- Quy trình phân công nhân viên buồng phòng, checklist dọn phòng và quản lý công việc bảo trì chi tiết; bản đầu chỉ ghi nhận tình trạng và khoảng thời gian phòng không phục vụ.
- Đổi ngày, đổi loại phòng, hủy một phần đơn hoặc rút ngắn kỳ lưu trú.
- Đặt hộ qua điện thoại tại quầy, khách đoàn có hợp đồng và công nợ doanh nghiệp.
- Đặt cọc, trả tại khách sạn, trả góp, thu thêm dịch vụ và hóa đơn thuế.
- Điểm thưởng và chat.
- Đánh giá khách sạn chưa được chốt đưa vào MVP; xem phần 10. Voucher cơ bản đã thuộc phạm vi theo yêu cầu mới ở phần 11; chưa làm cộng dồn nhiều mã, điểm thưởng hoặc chiến dịch phức tạp.

Các mục này có thể bổ sung sau khi phạm vi được duyệt; chưa mặc định dùng chúng để mở rộng API hoặc phạm vi triển khai. Đặc tả mở rộng có thể được dùng để rà thiết kế database, nhưng không chứng minh tính năng đã được duyệt hoặc triển khai.

## 2. Vai trò và giới hạn quyền

| Vai trò | Mục tiêu và quyền chính | Giới hạn |
| --- | --- | --- |
| Khách truy cập | Xem khách sạn, loại phòng, ảnh, giá và khả dụng | Chỉ xem thông tin công khai; phải đăng ký, đăng nhập và xác minh email một lần trước khi đặt. |
| Khách hàng — CUSTOMER | Quản lý hồ sơ; đặt, xem và hủy đơn của mình; xem thanh toán và hoàn tiền | Chỉ tạo đơn khi email đã xác minh; không xem đơn của khách khác hoặc tự thay đổi giá, trạng thái thanh toán. |
| Chủ/đại diện khách sạn — Owner | Ký thỏa thuận hoa hồng, xem phần được hưởng/chờ nhận và lịch sử chi của đúng khách sạn/thỏa thuận liên quan | Phân quyền tài chính riêng; không tự đổi snapshot hoặc đánh dấu lần chi thành công. DBML liên kết Owner qua users, không chứng minh quyền đã triển khai. |
| Quản lý khách sạn — HOTEL_MANAGER | Quản lý nội dung, loại phòng, danh mục phòng vật lý, lịch ngừng phục vụ, tồn phòng, giá và nhân sự thuộc khách sạn được giao; gán phòng và xử lý vận hành | Không quản lý khách sạn khác; không tự cấp quyền quản trị hệ thống hoặc sửa giao dịch thành công. |
| Lễ tân — RECEPTIONIST | Xem đơn, gán phòng vật lý, xác nhận nhận/trả phòng, ghi nhận chờ dọn/sẵn sàng, khách không đến và hỗ trợ hủy theo chính sách | Chỉ khách sạn được giao; không chỉnh giá, thêm/bớt phòng vật lý hoặc cấp quyền. |
| Quản trị hệ thống — ADMIN | Tạo khách sạn, phân công quản lý, quản lý tài khoản, khóa/mở bán; quản lý thỏa thuận, xem báo cáo nền tảng và thực hiện chi Owner theo quyền | Can thiệp có lý do/lịch sử; không vượt tồn, tự ghi khách đã trả khi thiếu xác thực cổng hoặc sửa mất giao dịch. |
| Cổng thanh toán | Cung cấp kết quả thu tiền và hoàn tiền | Là hệ thống bên ngoài; không có quyền quản lý đơn hoặc dữ liệu khách sạn. |

Một tài khoản có thể đồng thời là khách hàng và nhân sự. Quyền nhân sự luôn gắn với từng khách sạn; ví dụ quản lý khách sạn A không mặc nhiên có quyền tại B. Quản trị hệ thống cấp quyền quản lý; quản lý chỉ được phân công hoặc thu hồi vai trò lễ tân tại khách sạn của mình.

**Owner** là chủ/đại diện khách sạn; hotels.owner_user_id xác định chủ hiện tại, hotel_contracts.owner_user_id giữ bên ký cho thỏa thuận. Không tự suy quyền tài chính từ quyền vận hành HOTEL_MANAGER hoặc chỉ biết user_id. Chưa xác nhận vai trò/quyền tài chính đã triển khai. Owner và nhân sự không tự xác nhận đã thu tiền hoặc yêu cầu khách hủy để trả riêng; quản trị tiếp nhận khiếu nại và kiểm tra dấu hiệu hủy bất thường.

**Quyền voucher:** Quản trị tạo/quản lý mã toàn hệ thống hoặc của khách sạn; quản lý chỉ tạo/quản lý mã của khách sạn được giao; khách áp dụng mã khi đặt. Lễ tân không tự cấp/sửa mức giảm trong đơn. **Quyền đánh giá còn đề xuất:** Khách hàng viết/sửa của đơn mình; quản lý xem/báo cáo; quản trị ẩn/hiện có lý do, áp dụng khi reviews được duyệt.

## 3. Module identity — Tài khoản và phân quyền

**Phạm vi:** Đăng ký, xác minh email một lần bằng URL gửi qua email, đăng nhập, đăng xuất, hồ sơ cá nhân, đổi mật khẩu, reset password bằng URL gửi qua email, khóa tài khoản và phân quyền theo khách sạn.

**Vai trò:** Khách truy cập, khách hàng, quản lý khách sạn và quản trị hệ thống.

**Đã chốt:** Phải đăng ký trước khi đặt phòng; email phải được xác minh một lần. Khôi phục mật khẩu thực hiện bằng URL gửi đến email của tài khoản.

| Mã | Quy tắc nghiệp vụ |
| --- | --- |
| ID-01 | Mỗi địa chỉ email chỉ thuộc một tài khoản; email được so sánh không phân biệt chữ hoa, chữ thường và bỏ khoảng trắng đầu/cuối. |
| ID-02 | Tự đăng ký chỉ tạo quyền khách hàng; người dùng không tự chọn quyền quản lý, lễ tân hoặc quản trị hệ thống. |
| ID-03 | Quyền xem hoặc sửa dữ liệu phải đồng thời phù hợp vai trò và khách sạn được phân công. Biết mã đơn không đồng nghĩa được xem đơn. |
| ID-04 | Tài khoản bị khóa không được đăng nhập hoặc tiếp tục thực hiện hành động được bảo vệ; đơn và giao dịch đã phát sinh vẫn được giữ. |
| ID-05 | Người dùng chỉ sửa hồ sơ của mình; thay đổi thông tin liên hệ không tự sửa thông tin người đặt đã lưu trong đơn cũ. |
| ID-06 | Đổi mật khẩu yêu cầu mật khẩu hiện tại. Khi quên mật khẩu, khách nhập email đã đăng ký; hệ thống gửi URL đặt lại mật khẩu tới email đó. URL chứa mã khôi phục có thời hạn, dùng một lần; thời hạn 15 phút vẫn là đề xuất. |
| ID-07 | Khôi phục mật khẩu thành công làm mất hiệu lực các phiên đăng nhập trước đó. Email hồ sơ chưa được đổi trong bản đầu. |
| ID-08 | Không xóa tài khoản theo cách làm mất đơn, giao dịch hoặc dấu vết người thực hiện. Khi cần ngừng sử dụng, khóa tài khoản. |
| ID-09 | Sau khi đăng ký, tài khoản chưa xác minh email. Hệ thống gửi URL xác minh tới email đăng ký; xác minh hợp lệ một lần thì lưu kết quả và không bắt xác minh lại mỗi lần đăng nhập hoặc đặt phòng. Email hồ sơ chưa được đổi trong bản đầu theo ID-07. |
| ID-10 | Tạo đơn yêu cầu tài khoản đã đăng ký, đang đăng nhập, không bị khóa và đã xác minh email. Tài khoản chưa xác minh vẫn được xem thông tin công khai; gửi lại email xác minh không tạo thêm tài khoản. |
| ID-11 | URL đặt lại mật khẩu và URL xác minh email phục vụ hai mục đích riêng; không dùng lẫn nhau. Reset password không tự chuyển tài khoản chưa xác minh thành đã xác minh. |
| ID-12 | Trước khi đơn CONFIRMED, không trả liên hệ riêng của khách/chủ khách sạn qua API, thông báo hoặc chức năng xuất dữ liệu không phù hợp. Sau xác nhận chỉ cung cấp thông tin cần cho lưu trú theo quyền và khách sạn; không công khai danh sách liên hệ khách. |

**Luồng reset password đã chốt:** Nhập email → nhận URL qua email → mở trang đặt mật khẩu mới → gửi mật khẩu mới với mã khôi phục → hệ thống kiểm tra mã, cập nhật mật khẩu và vô hiệu hóa mã đã dùng. URL hết hạn hoặc đã dùng không được đặt lại lần nữa. Đề xuất thu hồi các phiên cũ theo ID-07.

**Luồng xác minh email đã chốt:** Đăng ký → nhận URL xác minh qua email → mở URL hợp lệ → email được đánh dấu đã xác minh → đăng nhập và được phép đặt phòng. Đặt những lần sau không cần mở URL xác minh mới.

**Còn cần chốt:** Thời hạn URL xác minh email; thời hạn 15 phút của URL reset password; dịch vụ gửi email. Yêu cầu đăng ký và xác minh một lần đã được chấp thuận.

## 4. Module hotel — Khách sạn, loại phòng và phòng vật lý

**Phạm vi:** Thông tin khách sạn, địa chỉ, tiện ích, chính sách hiển thị, loại phòng, sức chứa, danh mục phòng vật lý và mở/ngừng bán. Phòng vật lý có số phòng, tầng, khách sạn và loại phòng. Ảnh được liên kết từ module media; lịch sử dụng, bảo trì và khả năng bàn giao phòng thuộc inventory.

**Vai trò:** Khách truy cập, khách hàng, quản lý khách sạn và quản trị hệ thống.

**Đã chốt:** Quản trị hệ thống mở công khai lần đầu; quản lý được sửa nội dung và tạm ngừng bán khách sạn của mình. Khóa bán bởi quản trị chỉ quản trị mới được gỡ.

**Đã chốt bổ sung:** Quản lý danh mục phòng vật lý; khách vẫn mua theo loại phòng, nhân viên gán số phòng trước hoặc khi check-in.

| Mã | Quy tắc nghiệp vụ |
| --- | --- |
| HT-01 | Một loại phòng thuộc đúng một khách sạn; loại phòng đã được đặt không được chuyển sang khách sạn khác. |
| HT-02 | Loại phòng mô tả hạng phòng, sức chứa và tiện ích. Số lượng còn bán theo từng ngày thuộc inventory; giá theo ngày thuộc pricing. |
| HT-03 | Khách sạn chỉ được mở bán khi có tên, địa chỉ, thông tin liên hệ và ít nhất một loại phòng có sức chứa hợp lệ cùng phòng vật lý đang phục vụ. Một kỳ chỉ đặt được nếu còn khả năng phân bổ phòng theo IV-13 và mọi đêm có giá hợp lệ. |
| HT-04 | Sức chứa tối đa tính cho một phòng, là số nguyên dương. Bản đầu tính mọi người lưu trú vào sức chứa, chưa có chính sách riêng theo tuổi trẻ em. |
| HT-05 | Khách chỉ thấy khách sạn đang công khai. Khách sạn hoặc loại phòng ngừng bán không nhận đơn mới; đơn hiện hữu vẫn được phục vụ. |
| HT-06 | Ngừng bán là chặn đơn mới, không tự hủy đơn cũ hoặc tự hoàn tiền. Nếu khách sạn không thể phục vụ đơn đã xác nhận, phải xử lý riêng theo BK-09. |
| HT-07 | Thay đổi tên, mô tả hoặc chính sách không được làm thay đổi thông tin và điều khoản đã ghi nhận trong đơn trước đó. |
| HT-08 | Khách sạn hoặc loại phòng đã có lịch sử đặt được ngừng sử dụng thay vì xóa mất lịch sử. |
| HT-09 | Quản lý không tự công khai khách sạn lần đầu. Quản lý được sửa nội dung và tạm ngừng bán khách sạn được giao; không được gỡ khóa bán do quản trị hệ thống áp dụng. |
| HT-10 | Mỗi phòng vật lý thuộc đúng một khách sạn và một loại phòng của chính khách sạn đó. Số phòng duy nhất trong cùng khách sạn; hai khách sạn khác nhau có thể cùng có phòng 301. |
| HT-11 | Phòng vật lý lưu số phòng, tầng và loại phòng. Không chuyển sang khách sạn khác khi đã có lịch sử; thay đổi loại phòng hoặc ngừng sử dụng phải bảo toàn khả năng phục vụ các đơn đang giữ/đã xác nhận và không làm sai lịch sử phòng đã gán. |
| HT-12 | Phòng vật lý đã có lịch sử không bị xóa mất dữ liệu; dùng ngừng sử dụng với thời điểm hiệu lực. Phòng ngừng sử dụng không tạo thêm tồn bán kể từ thời điểm đó; phải kiểm tra ảnh hưởng theo IV-08 và IV-13. |
| HT-13 | Kiểm duyệt số điện thoại, QR nhận tiền và liên kết thanh toán/đặt trực tiếp trong nội dung hoặc ảnh khách sạn trước công khai. Không đưa hướng dẫn giao dịch ngoài; vẫn cung cấp tên, địa chỉ và thông tin lưu trú chính xác. Bản đầu kiểm tra thủ công; OCR tự động chưa thuộc phạm vi. |

**Còn cần chốt:** Quản lý có được tự mở bán lại sau khi chính mình tạm ngừng hay cần quản trị duyệt lại? Quyền công khai lần đầu và quyền gỡ khóa của quản trị đã được chốt.

## 5. Module inventory — Tồn phòng và lịch phòng vật lý

**Phạm vi:** Khả dụng theo loại phòng/ngày, lịch sử dụng và phân bổ phòng vật lý, khoảng ngừng phục vụ/bảo trì, tình trạng chờ dọn/sẵn sàng; giữ chỗ, xác nhận và trả tồn. Danh mục số phòng/tầng/loại phòng do hotel quản lý.

**Vai trò:** Quản lý khách sạn quản lý lịch ngừng phục vụ; lễ tân/quản lý kiểm tra lịch và ghi nhận chờ dọn/sẵn sàng. Khách chỉ xem khả dụng theo loại phòng. Hệ thống giữ, xác nhận, trả tồn và kiểm tra xung đột khi booking yêu cầu gán phòng.

**Đã chốt:** Không cho bán vượt số lượng phòng, kể cả thao tác của quản trị hệ thống.

**Đã chốt bổ sung:** Khả dụng phải phù hợp phòng vật lý thực tế; phòng được gán phải phù hợp liên tục cho toàn bộ kỳ ở và không trùng lịch. Gán số phòng không trừ tồn lần thứ hai.

| Mã | Quy tắc nghiệp vụ |
| --- | --- |
| IV-01 | Khách xem và đặt tồn theo loại phòng/ngày. Tổng số phòng được tính từ danh mục phòng vật lý đang phục vụ theo thời gian hiệu lực; không nhập một tổng độc lập có thể lệch với danh mục. Kiểm tra khả năng phân bổ liên tục theo IV-13 trước khi giữ chỗ. |
| IV-02 | Khoảng lưu trú bao gồm ngày nhận và không bao gồm ngày trả. Nhận 10/11, trả 12/11 sử dụng tồn các đêm 10/11 và 11/11. |
| IV-03 | Khả dụng theo ngày = tổng phòng vật lý đang phục vụ − số phòng bị chặn theo lịch − số đang giữ hợp lệ − số đã xác nhận, bao gồm đơn đang lưu trú. Không tính trùng số đã gán với số đã xác nhận, hoặc tính trùng nhiều lịch chặn của cùng phòng; phòng đã ngừng sử dụng không bị trừ thêm lần nữa như phòng bảo trì. |
| IV-04 | Muốn đặt q phòng trong nhiều đêm thì mọi đêm đều phải còn ít nhất q phòng. Giá trị nhỏ nhất theo ngày chỉ là giới hạn trên; số lượng có thể bán cho cả kỳ còn phải bảo đảm có phương án phân bổ liên tục theo IV-13. |
| IV-05 | Xem kết quả tìm kiếm không giữ phòng. Tạo đơn chờ thanh toán thành công mới giữ đủ tất cả loại phòng và tất cả đêm trong đơn. Nếu thiếu bất kỳ hạng mục nào thì không tạo đơn có giữ chỗ một phần. |
| IV-06 | Trong lúc chờ thanh toán, giữ chỗ đến thời điểm hết hạn của đơn. Đề xuất hết hạn sau 15 phút kể từ khi tạo đơn, độc lập với giờ check-in; thử thanh toán lại không gia hạn. Tại hoặc sau thời điểm hết hạn, giữ chỗ không còn hợp lệ. |
| IV-07 | Khi xác nhận thanh toán, phần đang giữ chuyển thành phần đã xác nhận; không trừ tồn lần thứ hai. Hết hạn hoặc hủy giải phóng phần đang chiếm đúng một lần. |
| IV-08 | Không được bán vượt tồn hoặc thao tác khiến khả dụng âm trên bất kỳ ngày nào, kể cả bởi quản trị hệ thống. Không cho thêm lịch bảo trì, ngừng sử dụng hoặc thay đổi loại phòng làm mất khả năng phục vụ đơn đang giữ hợp lệ/đã xác nhận. Hai khách tranh phòng cuối chỉ một khách được giữ thành công. |
| IV-09 | Loại phòng/ngày chưa được thiết lập tồn được xem là chưa sẵn sàng bán; không tự suy ra còn phòng từ ngày khác. |
| IV-10 | Đơn đã nhận phòng tiếp tục chiếm tồn cho các đêm đã đặt. Trả phòng đúng lịch kết thúc kỳ lưu trú; không tự mở bán thêm đêm vì khách trả trước giờ dự kiến. |
| IV-11 | Khi nhân viên đánh dấu NO_SHOW hợp lệ, giải phóng tồn của các đêm chưa sử dụng còn lại theo BK-14; chỉ giải phóng một lần. Không thay đổi tồn lịch sử của những đêm đã qua. |
| IV-12 | Mỗi phòng vật lý có lịch sử dụng và khoảng chặn phục vụ rõ ràng. Không được gán cho hai đơn có kỳ lưu trú chồng nhau hoặc gán vào khoảng bảo trì/ngừng phục vụ. Hai kỳ liên tiếp được phép dùng cùng phòng nếu trả/nhận đúng lịch và phòng đã sẵn sàng khi bàn giao. |
| IV-13 | Trước khi giữ chỗ, xác nhận đơn, gán/đổi phân công trước check-in hoặc sửa lịch ngừng phục vụ, phải bảo đảm vẫn có phương án phân bổ đủ phòng vật lý cùng loại, liên tục cả kỳ, cho mọi đơn đang giữ hợp lệ/đã xác nhận liên quan. Một phòng có thể trống từng đêm khác nhau mà không đáp ứng cả kỳ; không được chỉ dựa vào tổng theo ngày. Việc kiểm tra hoặc dành năng lực phân bổ nội bộ không phải cam kết số phòng cho khách lúc đặt. |
| IV-14 | Gán phòng chuyển phần năng lực đã dành cho booking thành lịch phòng cụ thể; không tạo thêm một phần chiếm tồn. Hủy, hết hạn hoặc NO_SHOW giải phóng năng lực và lịch gán tương ứng đúng một lần; NO_SHOW chỉ giải phóng các đêm chưa sử dụng theo BK-14. |
| IV-15 | Tình trạng chờ dọn/sẵn sàng để bàn giao tách biệt với lịch trống trong tương lai. Sau check-out, phòng chuyển chờ dọn; lễ tân/quản lý chỉ đánh dấu sẵn sàng khi đã xác nhận dọn xong. Không check-in vào phòng chưa sẵn sàng. Chờ dọn không tự chặn mọi ngày tương lai; nếu không thể phục vụ đúng lịch phải xử lý khoảng chặn và các đơn bị ảnh hưởng. |

**Ví dụ tồn theo ngày:** Deluxe ngày 10/11 có 10 phòng, 1 phòng ngừng phục vụ, 2 phòng đang giữ và 5 phòng đã xác nhận thì còn 2 phòng. Nếu ngày 11/11 chỉ còn 1 phòng thì khách ở hai đêm này đặt được tối đa 1 phòng Deluxe, với điều kiện vẫn có phương án phân bổ phòng liên tục.

**Ví dụ không có phòng liên tục:** Deluxe chỉ có 301 và 302. Đêm 10/11 phòng 301 bảo trì, đêm 11/11 phòng 302 bảo trì. Mỗi đêm còn 1 phòng nhưng không có cùng một phòng dùng được cả hai đêm. Vì chuyển phòng trong kỳ nằm ngoài phạm vi, hệ thống không nhận đơn mới ở cả hai đêm này.

### Giải thích IV-06 — Giữ chỗ khi chờ thanh toán

Giữ chỗ là dành tạm số lượng phòng cho khách đang trả tiền, để khách khác không đặt mất số lượng đó. Đây là hạn thanh toán của đơn chưa xác nhận; đơn đã trả đủ và được xác nhận tiếp tục chiếm tồn cho kỳ lưu trú, không hết hạn sau 15 phút.

| Tình huống | Kết quả theo đề xuất hiện tại |
| --- | --- |
| Tạo đơn lúc 10:00 ngày 01/11, nhận phòng ngày 10/11 | Giữ chỗ đến 10:15 ngày 01/11. |
| Khách thử thanh toán lại lúc 10:12 | Vẫn hết hạn lúc 10:15, không kéo dài đến 10:27. |
| Nhận kết quả thanh toán hợp lệ lúc 10:10 | Xác nhận đơn; chuyển phần tồn đang giữ sang đã xác nhận, không trừ tồn thêm lần nữa. |
| Đến 10:15 vẫn chưa xác nhận được thanh toán | Giữ chỗ hết hiệu lực, đơn hết hạn và tồn được trả lại. Không phụ thuộc việc khách có đóng trình duyệt hay không. |
| Tạo đơn lúc 13:55 để nhận phòng cùng ngày, nếu chính sách đặt trong ngày cho phép | Hạn giữ chỗ là 14:10; giờ bắt đầu nhận phòng 14:00 không cắt ngắn hạn thanh toán. |
| Ghi nhận thu tiền sau khi đơn đã hết hạn | Ghi nhận khoản tiền đã thu và xử lý hoàn theo PM-07; không tự lấy lại phòng đã trả tồn. |

Hệ thống phải kiểm tra thời điểm hết hạn khi xử lý giữ chỗ và xác nhận thanh toán, kể cả khi tác vụ dọn đơn hết hạn chưa kịp chạy. Việc thử lại hoặc tải lại trang không được tạo thêm phần tồn đang giữ.

**Còn cần chốt:** Có dùng thời gian 15 phút như đề xuất không? Hạn đặt trong ngày là chính sách riêng, không dùng giờ check-in để mặc định cắt hạn thanh toán. Chính sách không bán vượt số lượng đã chốt. Điều chỉnh lịch ngừng phục vụ cần ghi lý do; quy trình sự cố bảo trì khẩn cấp ảnh hưởng đơn đã xác nhận cần đặc tả riêng.

### 5.1. Chặn phòng — room_blocks

**Phạm vi:** Tạo, xem, sửa thời gian và giải phóng lịch chặn của từng phòng vật lý, ví dụ bảo trì hoặc tạm thời không thể phục vụ. Đây là nghiệp vụ thuộc inventory, khác với giữ chỗ cho khách chờ thanh toán.

**Trạng thái duyệt:** Chặn phòng theo lịch đã thuộc phạm vi; các quy tắc chi tiết BL bên dưới là đề xuất để nhóm duyệt, được viết theo phương án một bản ghi chặn một phòng cụ thể.

**Vai trò:** Quản lý khách sạn hoặc quản trị có quyền tạo/sửa/giải phóng; lễ tân xem lịch và báo tình trạng phòng, chưa được tự tạo hoặc gỡ lịch chặn theo đề xuất này.

| Mã | Quy tắc đề xuất |
| --- | --- |
| BL-01 | Chỉ thao tác lịch chặn của khách sạn thuộc quyền quản lý; mọi thay đổi phải ghi người thực hiện, thời điểm và lý do. |
| BL-02 | Một lịch chặn gắn với một phòng vật lý. Loại phòng và khách sạn phải khớp phòng đó; quantity bằng 1. Chặn ba phòng tạo ba bản ghi, không vừa chặn từng phòng vừa trừ thêm một số lượng tổng cho cùng sự việc. |
| BL-03 | start_date là đêm đầu bị chặn, end_date là đêm cuối bị chặn, bao gồm cả hai ngày. Phải có start_date <= end_date. Khi so sánh với kỳ lưu trú dạng không bao gồm ngày trả, quy đổi lịch chặn thành khoảng [start_date, end_date + 1 ngày). |
| BL-04 | Phải có lý do và khoảng ngày chặn; không tạo hoặc sửa lịch làm thay đổi tồn lịch sử của các đêm đã qua. Phòng ngừng sử dụng dài hạn còn phải tuân thủ thời gian hiệu lực ở HT-12. |
| BL-05 | Lịch ACTIVE chỉ tác động lên các đêm trong khoảng chặn; lịch tương lai không làm giảm tồn của hôm nay. Lịch RELEASED không tiếp tục chặn các đêm chưa qua; phần lịch sử trước lúc giải phóng vẫn được giữ. |
| BL-06 | Trước khi tạo/gia hạn lịch chặn, kiểm tra lại khả năng phân bổ cho mọi đơn đang giữ hợp lệ, đã xác nhận và đang ở. Không được làm mất khả năng phục vụ các đơn này, kể cả khi chưa gán số phòng. |
| BL-07 | Nếu phòng đã gán cho đơn chưa check-in, nhân viên chỉ đổi phân công sang phòng cùng loại phù hợp theo BK-17 trước khi tạo lịch chặn. Không tự bỏ phân công, nâng hạng hoặc hủy đơn để tạo lịch chặn. Lịch bảo trì thông thường không được chồng việc lưu trú đang diễn ra. |
| BL-08 | Khi có sự cố khẩn cấp ảnh hưởng khách đang ở hoặc đơn không còn phục vụ được, chuyển quản lý xử lý ngoại lệ và ghi nhận sự cố; không dùng thao tác chặn để tự hủy đơn/thu tiền/hoàn tiền. Trường hợp khách sạn hủy áp dụng BK-09; chuyển phòng trong kỳ cần quy trình riêng vì đang ngoài phạm vi. |
| BL-09 | Đề xuất không tạo hai lịch ACTIVE chồng đêm cho cùng phòng. Khi cần đổi phạm vi phải sửa/gia hạn có lịch sử; dữ liệu trùng do lỗi cũng không được trừ tồn một phòng hai lần. |
| BL-10 | Giải phóng lịch chặn phải có lý do, người thực hiện và released_at, chỉ có tác động một lần. Không xóa bản ghi để mở lại phòng; gọi lại cùng thao tác không cộng tồn thêm lần nữa. |
| BL-11 | Hết khoảng ngày thì lịch không chặn các đêm sau đó, nhưng không đồng nghĩa phòng đã sẵn sàng bàn giao. Nếu bảo trì chưa xong, quản lý phải gia hạn và kiểm tra ảnh hưởng; gỡ lịch chặn không tự chuyển phòng DIRTY thành READY theo IV-15. |
| BL-12 | Thêm, sửa hoặc giải phóng lịch chặn phải cập nhật khả dụng nhất quán với phòng thực tế, giữ chỗ và phân công; không thay đổi tiền hoặc điều khoản booking hiện hữu. |

**Luồng đề xuất:** Chọn phòng và các đêm → nhập lý do → kiểm tra quyền, lịch phòng và các đơn bị ảnh hưởng → tạo ACTIVE → khi đủ điều kiện mở lại, giải phóng thành RELEASED → kiểm tra tình trạng bàn giao trước khi nhận khách.

**Ví dụ:** Phòng 301 bị chặn các đêm 10 và 11/11 thì start_date = 10/11, end_date = 11/11; đơn nhận 10/11, trả 12/11 không được gán 301. Đơn nhận 12/11 có thể được gán nếu không còn xung đột và phòng sẵn sàng tại lúc nhận. Chặn phòng 301 không tự chặn toàn bộ loại Deluxe.

**Còn cần chốt:** Quyền lễ tân đối với lịch chặn; giới hạn sửa/gia hạn; quy trình khẩn cấp khi đang có khách; có chấp nhận quy ước end_date bao gồm đêm cuối như BL-03 không. Các ranh giới ngày này phải thống nhất trước khi viết API/migration.

## 6. Module pricing — Giá phòng và báo giá

**Phạm vi:** Giá phòng trọn gói theo loại phòng/ngày, báo giá trước và sau voucher cơ bản. Ưu tiên hoàn thiện chính xác luồng đặt phòng và thanh toán; cách giảm ở phần 11.

**Vai trò:** Quản lý khách sạn thiết lập giá; khách xem giá; lễ tân xem giá đã ghi trong đơn.

**Đã chốt theo yêu cầu mới:** Giá phòng trọn gói trước giảm, cho áp tối đa một voucher hợp lệ theo điều kiện và trần giảm. Không cộng dồn mã hoặc dùng điểm thưởng.

| Mã | Quy tắc nghiệp vụ |
| --- | --- |
| PR-01 | Giá tính theo một phòng cho một đêm, bằng VND. Giá bán phải lớn hơn 0 và là số nguyên đồng. |
| PR-02 | Giá riêng của loại phòng/ngày được ưu tiên hơn giá cơ bản đang có hiệu lực. Không có giá hợp lệ thì không bán đêm đó. |
| PR-03 | Thành tiền một hạng mục = số lượng phòng × tổng giá từng đêm. Tổng đơn = tổng thành tiền các hạng mục. |
| PR-04 | Giá phòng công bố là giá trọn gói, đã gồm các khoản bắt buộc của phòng trong phạm vi đặt. Tổng đơn không cộng thêm thuế hoặc phí bắt buộc ở bước thanh toán. Việc phát hành hóa đơn thuế ngoài phạm vi. |
| PR-05 | Hệ thống tính lại giá lúc tạo đơn. Nếu khác báo giá khách vừa chấp nhận, trả lại báo giá mới để khách xác nhận; không tự tạo đơn với mức giá cao hơn. |
| PR-06 | Khi tạo đơn thành công, lưu giá từng đêm, tổng tiền và điều khoản áp dụng. Giá thay đổi sau đó không làm đổi tiền của đơn đang giữ chỗ hoặc đã xác nhận. |
| PR-07 | Giá phòng không tăng theo số người miễn là không vượt sức chứa; chưa áp dụng giường phụ hoặc phụ thu trẻ em. |
| PR-08 | Sửa giá chỉ áp dụng cho đơn được tạo sau thời điểm sửa. Mọi điều chỉnh giá của nhân sự phải xác định được người thực hiện. |
| PR-09 | Một đơn áp tối đa một voucher hợp lệ theo PRO-02 đến PRO-06; không cộng dồn mã hoặc điểm thưởng. Tổng cần trả bằng giá trọn gói trước giảm trừ khoản giảm đã lưu; payment thu đúng tổng sau giảm. |

**Ví dụ:** 2 phòng, 2 đêm; đêm đầu 800.000 đồng/phòng, đêm sau 1.000.000 đồng/phòng: tổng đơn = 2 × (800.000 + 1.000.000) = 3.600.000 đồng.

**Ưu tiên đã chốt:** Hoàn thiện báo giá → giữ tồn → tạo đơn → thanh toán đủ tiền → xác nhận đơn với cùng một tổng tiền. Các nhánh lỗi phải được kiểm soát, đặc biệt thay đổi giá lúc tạo đơn, hết hạn giữ chỗ và thông báo thanh toán lặp.

## 7. Module booking — Đặt phòng và lưu trú

**Phạm vi:** Tạo đơn theo loại phòng/số lượng, ghi nhận người đặt/người lưu trú, xem lịch sử, hủy toàn bộ đơn, hết hạn giữ chỗ, gán phòng vật lý trước hoặc khi check-in, nhận phòng, trả phòng và ghi nhận không đến.

**Vai trò:** Khách hàng, lễ tân, quản lý khách sạn và quản trị hệ thống.

**Đã chốt:** Khách trả đủ 100% rồi đơn được xác nhận. Khách không đến vẫn giữ CONFIRMED cho tới khi nhân viên đánh dấu NO_SHOW hợp lệ; NO_SHOW không hoàn tiền, không thu thêm và giải phóng tồn chưa sử dụng. Khách đã thông báo đến muộn không bị đánh dấu NO_SHOW theo mốc cũ.

**Đã chốt bổ sung:** Khách chọn loại phòng/số lượng; lễ tân hoặc quản lý gán đủ phòng vật lý đúng loại, cùng khách sạn, trước hoặc khi check-in. Chưa cho khách tự chọn số phòng, chuyển phòng trong kỳ hoặc nâng hạng.

| Mã | Quy tắc nghiệp vụ |
| --- | --- |
| BK-01 | Mỗi đơn có một người đặt chịu trách nhiệm, một khách sạn, một ngày nhận, một ngày trả và ít nhất một hạng mục loại phòng. Người đặt có thể khác người đại diện lưu trú; phải có thông tin liên hệ của cả hai nếu khác nhau. |
| BK-02 | Ngày trả phải sau ngày nhận; không nhận ngày đã qua. Đặt cho hôm nay theo hạn đặt trong ngày được chốt riêng, không mặc định dừng lúc giờ check-in. Kỳ ở đề xuất tối đa 30 đêm. |
| BK-03 | Mỗi hạng mục có số lượng phòng và số người lưu trú là số nguyên dương; số người không vượt số phòng × sức chứa của loại phòng. Mỗi phòng đặt phải có ít nhất một người, nên số người không nhỏ hơn số phòng. |
| BK-04 | Đơn chỉ được tạo khi người đặt đã đăng ký, đăng nhập, xác minh email, khách sạn/loại phòng còn mở bán, mọi đêm có giá và hệ thống giữ đủ tồn. Giá trị đơn và thời điểm hết hạn phải hiển thị trước khi khách thanh toán. |
| BK-05 | Đơn chờ thanh toán chưa được nhận phòng. Chỉ xác nhận đơn khi thu đủ tiền đúng yêu cầu và giữ chỗ còn hợp lệ theo thời điểm hệ thống ghi nhận kết quả đã xác thực. |
| BK-06 | Khách được hủy toàn bộ đơn chưa nhận phòng. Đơn đang chờ thanh toán hủy không mất phí; với đơn đã xác nhận, áp dụng chính sách được lưu lúc đặt. |
| BK-07 | Đề xuất hủy đơn đã xác nhận: trước hoặc đúng mốc 24 giờ trước giờ nhận phòng được lưu trong đơn thì hoàn 100%; sau mốc đó hoàn 0%. Mức hoàn căn cứ số tiền đã thực thu, không chỉ căn cứ giá trị đơn. |
| BK-08 | Lễ tân chỉ xác nhận nhận phòng cho đơn CONFIRMED đã được gán đủ phòng hợp lệ/sẵn sàng, theo lịch nhận phòng hoặc lịch đến muộn đã được ghi nhận và sau khi đối chiếu thông tin người đại diện. Khách đã được đánh dấu NO_SHOW không được check-in trực tiếp theo luồng này. Chỉ trả phòng cho đơn đã nhận phòng; thủ tục giấy tờ chi tiết cần được người phụ trách vận hành cung cấp. |
| BK-09 | Khi khách sạn không thể phục vụ đơn đã xác nhận, quản lý hoặc quản trị được hủy với lý do và hoàn 100% số tiền đã thu, không áp dụng phí hủy của khách. |
| BK-10 | Khi quá thời hạn đến nhận phòng đã áp dụng cho đơn, lễ tân có thể đánh dấu NO_SHOW nếu đơn vẫn CONFIRMED, chưa check-in và không có thông báo đến muộn đang được ghi nhận theo BK-13. Hệ thống không tự đánh dấu NO_SHOW chỉ vì khách chưa xuất hiện. Khách đã trả 100% nên giữ nguyên khoản đã thu, không hoàn và không phát sinh khoản thu mới. Mốc 00:00 ngày kế tiếp ngày nhận vẫn là đề xuất chưa chốt. |
| BK-11 | Đơn đã nhận phòng không được hủy theo luồng khách tự thao tác. Đơn đã hủy/hết hạn không được tự khôi phục khi có thông báo thanh toán đến muộn. |
| BK-12 | Một yêu cầu tạo đơn được gửi lại do mất kết nối phải trả về cùng kết quả, không tạo thêm đơn hoặc giữ thêm phòng. Khách không được tự thay đổi tổng tiền, khách sạn hoặc người sở hữu đơn. |
| BK-13 | Nếu khách thông báo đến muộn trước khi bị đánh dấu NO_SHOW, nhân viên ghi nhận thông báo và thời điểm dự kiến đến; giữ đơn CONFIRMED và giữ tồn cho kỳ lưu trú còn lại, không đánh dấu NO_SHOW theo mốc cũ. Không tự mở rộng kỳ lưu trú hoặc dời ngày trả. Cách xử lý khi khách tiếp tục không đến theo lịch mới cần chốt riêng. |
| BK-14 | Sau khi đánh dấu NO_SHOW, giải phóng phần tồn của các đêm chưa sử dụng còn lại theo ngày kinh doanh tại thời điểm ghi nhận. Không hồi tố tồn những đêm đã qua; lưu người thực hiện, thời điểm và lý do để tránh giải phóng lặp. |
| BK-15 | Booking xác nhận cam kết loại phòng và số lượng, chưa cam kết số phòng cụ thể với khách. Nhân viên chỉ gán phòng cho đơn CONFIRMED; có thể gán trước hoặc khi check-in và phải gán đủ số phòng khác nhau cho từng hạng mục trước khi hoàn tất check-in toàn bộ đơn. |
| BK-16 | Phòng được gán phải thuộc cùng khách sạn, đúng loại đã đặt và phù hợp toàn bộ kỳ lưu trú còn lại; không trùng lịch hoặc bảo trì. Phòng gán trước có thể chưa dọn xong ở thời điểm phân công nhưng bắt buộc sẵn sàng lúc bàn giao; kiểm tra lại tại check-in theo IV-12 đến IV-15. |
| BK-17 | Nhân viên có thể đổi phân công trước check-in sang phòng cùng loại còn phù hợp; lưu người thực hiện, thời điểm và lịch sử thay đổi. Gán hoặc đổi phân công không đổi giá, không thu thêm và không trừ tồn lần nữa. Chuyển phòng sau check-in/nâng hạng ngoài phạm vi bản đầu. |
| BK-18 | Khi check-out, kết thúc việc sử dụng phòng vật lý, giữ lịch sử người ở/phòng đã gán và chuyển phòng sang chờ dọn theo IV-15. Khi hủy hoặc NO_SHOW, bỏ phần phân công còn hiệu lực tương ứng; không xóa lịch sử và không tự chuyển một phòng chưa có khách sử dụng sang chờ dọn. |
| BK-19 | Mỗi khách sạn có giờ nhận/trả phòng mặc định; đề xuất nhận từ 14:00, trả chậm nhất 12:00. Booking lưu giờ áp dụng lúc đặt để thay đổi cấu hình không làm đổi điều khoản đơn cũ. Giờ bắt đầu check-in khác hạn đặt trong ngày, hạn thanh toán và hạn đánh dấu NO_SHOW. |
| BK-20 | Hủy đơn phải lưu người thao tác, thời điểm, lý do, trạng thái trước/sau và lịch sử; phân biệt khách hủy với khách sạn không phục vụ được. Theo dõi tỷ lệ/số lần khách sạn hủy và phản ánh bị yêu cầu hủy để trả riêng. Đây là tín hiệu kiểm tra, không tự kết luận gian lận hoặc chặn hoàn tiền hợp lệ. |
| BK-21 | Khách có thể gửi khiếu nại gắn booking, lý do và bằng chứng; quản trị tiếp nhận, ghi quá trình và kết quả xử lý. Nếu cần hoàn tiền thì payment thực hiện theo chính sách và kết quả thực tế; không hứa tự động bồi thường hoặc hoàn mọi yêu cầu. Bản đầu xử lý thủ công, chưa bổ sung chat hay module riêng. |

### Luồng gán phòng vật lý và tiếp nhận khách đã chốt

1. Khách đặt 2 phòng Deluxe từ 10/11 đến 12/11; hệ thống kiểm tra đủ năng lực phân bổ liên tục và giữ tồn cho hai đêm 10 và 11/11.
2. Khách trả đủ tiền; booking chuyển CONFIRMED. Số phòng cụ thể chưa được khách lựa chọn.
3. Trước hoặc khi khách đến, lễ tân/quản lý gán phòng 301 và 302, cùng loại Deluxe, còn phù hợp toàn bộ kỳ lưu trú. Gán phòng không trừ thêm tồn và không đổi tiền.
4. Khi khách đến, nhân viên đối chiếu thông tin, kiểm tra lại lịch và tình trạng sẵn sàng của cả hai phòng, rồi hoàn tất check-in.
5. Khi khách trả phòng, nhân viên check-out; phòng 301 và 302 chuyển chờ dọn. Nhân viên xác nhận dọn xong trước khi đánh dấu sẵn sàng và giao cho khách tiếp theo.

Nếu không gán được phòng phù hợp dù khách đã thanh toán, không được check-in vào phòng sai loại, bảo trì hoặc đã có khách. Nhân viên chuyển quản lý xử lý; nếu khách sạn không phục vụ được thì theo BK-09. Hệ thống phải kiểm soát khả năng phân bổ từ lúc giữ chỗ để tránh tạo tình huống này trong hoạt động bình thường.

### Giải thích BK-10 — Khách không đến nhận phòng

NO_SHOW là kết quả xử lý đơn đã được trả đủ nhưng khách không đến. “Trừ tiền no-show” trong phạm vi này nghĩa là khách mất quyền nhận lại khoản tiền đã thanh toán; không gọi thêm một giao dịch thu tiền lần hai. Lịch sử thanh toán thành công vẫn được giữ nguyên.

| Tình huống | Xử lý đơn | Tiền và tồn phòng |
| --- | --- | --- |
| Khách đã thanh toán đủ, đơn được xác nhận | CONFIRMED | Tiền đã thu đủ; tồn được giữ cho kỳ lưu trú. |
| Khách đến theo lịch và nhân viên check-in | Chuyển sang đã nhận phòng | Tiếp tục chiếm tồn cho các đêm đã đặt. |
| Khách chưa đến, chưa có thao tác của nhân viên | Vẫn CONFIRMED | Không tự chuyển NO_SHOW, không tự giải phóng tồn đã xác nhận. |
| Quá hạn nhận phòng, chưa check-in, không có thông báo đến muộn; nhân viên đánh dấu | Chuyển CONFIRMED → NO_SHOW | Không hoàn tiền, không thu thêm; trả tồn các đêm chưa sử dụng còn lại. |
| Khách đã báo đến muộn, nhân viên ghi nhận trước khi đánh dấu NO_SHOW | Giữ CONFIRMED | Giữ tồn kỳ lưu trú còn lại; không áp dụng mốc no-show cũ. |

**Ví dụ trả tồn:** Đặt 1 phòng từ 10/11 đến 13/11, gồm các đêm 10, 11 và 12/11; đã trả 3.000.000 đồng. Giả sử dùng mốc no-show đề xuất và nhân viên đánh dấu lúc 00:10 ngày 11/11: đơn chuyển NO_SHOW, giữ nguyên 3.000.000 đồng đã thu, thu thêm 0 đồng; trả tồn đêm 11 và 12/11, không sửa lại tồn lịch sử đêm 10/11. Đơn chỉ đặt một đêm và đã qua đêm đó thì không còn đêm tương lai để trả tồn.

**Ví dụ đến muộn:** Khách có lịch nhận ngày 10/11 nhưng đã báo sẽ đến lúc 02:00 ngày 11/11; nhân viên ghi nhận trước khi đánh dấu NO_SHOW. Đơn vẫn CONFIRMED và tồn/lịch phòng đã gán vẫn được giữ, không đánh dấu NO_SHOW lúc 00:00 ngày 11/11. Đây là tiếp nhận khách đến muộn trong kỳ đã đặt, không phải tạo đơn mới.

**Ví dụ chính sách hủy:** Nhận phòng lúc 14:00 ngày 10/11/2026. Khách hủy lúc 14:00 ngày 09/11 được hoàn toàn bộ; hủy lúc 14:01 ngày 09/11 không được hoàn theo đề xuất hiện tại. Khách sạn chủ động hủy vì không phục vụ được vẫn phải hoàn toàn bộ tiền đã thu.

**Còn cần chốt:** Giờ mặc định 14:00/12:00, hạn đặt trong ngày, mốc giờ được phép đánh dấu NO_SHOW (đề xuất 00:00 ngày kế tiếp ngày nhận); xử lý khi khách tiếp tục không đến theo giờ đã báo muộn hoặc đến sau khi đã NO_SHOW. Mốc hủy miễn phí 24 giờ, phí hủy muộn 100% và giới hạn 30 đêm vẫn chưa được duyệt. Chính sách không hoàn/không thu thêm khi NO_SHOW và luồng gán phòng vật lý đã chốt.

## 8. Module payment — Thu/hoàn tiền, hoa hồng và chi trả

**Phạm vi:** Thu toàn bộ tiền đơn, xác thực kết quả cổng, hoàn tiền; nền tảng thu hộ/giữ khoản chưa chi, quản lý hợp đồng hoa hồng riêng, tài chính hai bên theo booking và lần chi trả Owner. Các nhóm này thuộc payment, chưa tách module kế toán/ví.

**Vai trò:** Khách thanh toán; cổng báo kết quả; quản trị quản lý thỏa thuận, báo cáo toàn hệ thống và thực hiện chi trả theo quyền. Owner/đại diện được cấp quyền tài chính xem đúng khách sạn và thỏa thuận liên quan; không mặc định mọi nhân viên quản lý có quyền xem doanh thu hoặc nhận tiền. Lễ tân chỉ xem thông tin thu cần cho vận hành.

| Mã | Quy tắc nghiệp vụ |
| --- | --- |
| PM-01 | Mỗi yêu cầu thu tiền gắn với đúng một đơn, bằng đúng tổng tiền của đơn và cùng loại tiền. Khách không nhập số tiền tùy ý. |
| PM-02 | Chỉ tạo yêu cầu thanh toán khi đơn còn chờ thanh toán và chưa hết hạn. Cho thử lại nếu lần trước thất bại hoặc đã được xác định là không thể thu tiền; không tạo đồng thời nhiều yêu cầu thu đang có hiệu lực cho cùng đơn. |
| PM-03 | Chỉ ghi nhận thành công từ kết quả được xác thực với cổng thanh toán. Trang trình duyệt báo thành công hoặc ảnh chụp chuyển khoản không đủ để xác nhận đã thu tiền. |
| PM-04 | Kết quả phải khớp đơn, yêu cầu thu tiền, số tiền và loại tiền. Trường hợp không khớp phải được ghi nhận để đối soát; không xác nhận đơn. |
| PM-05 | Cùng một giao dịch được báo nhiều lần chỉ ghi nhận tác động tài chính một lần; không xác nhận đơn hoặc phát sinh hoàn tiền lặp lại. |
| PM-06 | Nếu cùng đơn thực tế bị thu tiền hai lần qua hai giao dịch khác nhau, chỉ một khoản hợp lệ được dùng để xác nhận đơn; khoản thu thừa phải được theo dõi để hoàn. |
| PM-07 | Nếu hệ thống ghi nhận thanh toán thành công sau khi đơn hết hạn hoặc đã hủy, vẫn phải lưu khoản đã thu, không khôi phục đơn và tạo nghĩa vụ hoàn toàn bộ khoản thu đó. Quy tắc này áp dụng cả khi khách đã trả trước hạn nhưng thông báo đến muộn. |
| PM-08 | Hủy đơn và hoàn tiền là hai việc riêng. Đơn có thể đã hủy trong khi khoản hoàn còn chờ hoặc bị lỗi; phải thể hiện đúng tình hình cho khách và nhân sự. |
| PM-09 | Tổng số tiền đã hoàn và đang được yêu cầu hoàn không được vượt số thực thu. Các khoản hoàn phải chỉ rõ giao dịch gốc và lý do. |
| PM-10 | Giao dịch đã thành công không bị sửa thành thất bại hoặc xóa lịch sử. Kết quả hoàn tiền cũng chỉ hoàn tất khi có bằng chứng xác thực từ cổng thanh toán. |
| PM-11 | Trường hợp hủy được miễn phí hoặc khách sạn hủy tạo nghĩa vụ hoàn theo BK-07/BK-09. Khi cổng chưa hỗ trợ tự động, nhân sự có quyền theo dõi và xử lý; không đánh dấu đã hoàn chỉ vì đã tạo yêu cầu. |
| PM-12 | NO_SHOW của đơn đã thanh toán 100% không tạo giao dịch thu tiền hay hoàn tiền mới. Khoản thu thành công giữ nguyên; booking ghi nhận kết quả không đến, inventory trả tồn chưa sử dụng theo BK-14. |
| PM-13 | Gán hoặc đổi phân công phòng vật lý trước check-in, đúng loại phòng và kỳ đã đặt, không thay đổi số tiền phải thu và không tạo giao dịch thanh toán mới. |
| PM-14 | Khách trả trước 100% tổng sau giảm qua luồng chính thức. Owner, lễ tân và quản trị không tự đánh dấu đã thu bằng tiền mặt/ảnh chuyển khoản; chỉ hệ thống áp dụng kết quả đã xác thực từ cổng. Hoàn tiền lưu người khởi tạo, thời điểm, lý do, giao dịch gốc và lịch sử thay đổi; nghi vấn giao dịch ngoài không tự làm mất quyền hoàn hợp lệ. |
| PM-15 | Đã chọn nền tảng thu hộ và giữ khoản chưa chi trả, sau đó thanh toán cho Owner khi đủ điều kiện. Khi booking CONFIRMED, lưu thỏa thuận hoa hồng áp dụng và khoản phí của từng booking; hủy/hoàn ghi điều chỉnh riêng, không sửa mất bản gốc. Thu của khách, hoàn khách và chi Owner có trạng thái/lịch sử riêng; không chi trùng. Mức phí, căn cứ tính, điều kiện và lịch chi trả còn cần chốt. |
| PM-16 | hotel_contracts đã có trong DBML: mã hợp đồng, hotel_id, owner_user_id, commission_rate, commission_basis, khoảng hiệu lực, trạng thái và người tạo. Mức phí có thể khác giữa Owner/khách sạn. Chỉ dùng thỏa thuận hợp lệ cho đúng bên/phạm vi; không cho hiệu lực chồng trong cùng phạm vi. Giữ điều khoản đã dùng và tạo thỏa thuận mới khi thay đổi; snapshot trong booking không đổi theo cấu hình mới. Thời điểm chọn hợp đồng và quy trình duyệt còn cần chốt. |
| PM-17 | booking_financials.booking_id là PK: tối đa một bản tài chính gốc/booking. Lưu thỏa thuận, Owner, tiền khách trả, voucher mỗi bên chịu, căn cứ/tỷ lệ hoa hồng, phần nền tảng và khách sạn được hưởng. PENDING/FINALIZED thể hiện chốt tài chính, chưa nói đã trả Owner. Báo cáo tách khoản dự kiến/chốt, thực thu, thực hoàn, chi phí voucher, thực chi và còn phải trả; số ròng phải xét điều chỉnh, không cộng doanh thu lần nữa khi chi. |
| PM-18 | hotel_payouts lưu từng lần thực hiện chi cho một booking_financials, số tiền/tiền tệ, idempotency_key, thông tin tài khoản nhận tại lúc chi, người tạo/thực hiện, tham chiếu và kết quả. Chỉ SUCCEEDED tính đã trả; FAILED/CANCELLED không xóa lịch sử. Cần chống nhiều lần PENDING/PROCESSING/SUCCEEDED trùng nghĩa vụ và khóa/kiểm tra trước chi; UNIQUE idempotency_key một mình chưa đủ ngăn gửi lại bằng khóa khác. |
| PM-19 | Khi hoàn trước hoặc sau chi, giữ snapshot gốc và lần chi cũ; ghi điều chỉnh có nguồn, lý do và tác động đúng một lần theo kết quả hoàn thực tế. Nếu đã trả Owner thì xác định nghĩa vụ thu hồi/bù trừ theo thỏa thuận; không trì hoãn khoản khách được hoàn để chờ Owner trả lại. DBML mới chưa có cấu trúc phân bổ/lịch sử điều chỉnh đầy đủ, cần đặc tả trước triển khai. |
| PM-20 | Trước chi phải kiểm tra trạng thái chốt, điều kiện phục vụ, khoản hoàn/khiếu nại liên quan, người nhận, tiền tệ và số đủ điều kiện còn phải trả. Timeout không tự là FAILED; phải đối chiếu kết quả trước thử lại. Lịch chi và cách xử lý booking hủy/NO_SHOW có khoản được giữ còn cần chốt; không ép chúng sang CHECKED_OUT để được chi. |

**Đã chốt liên quan:** Thu đủ 100% tổng sau voucher đã lưu trong đơn để xác nhận; không hoàn phần tiền được giảm nhưng chưa thu. NO_SHOW không thu thêm, không hoàn tiền. Hoàn tiền theo PRO-08 và giới hạn PM-09.

**Còn cần chốt:** Chọn cổng thanh toán nào và dùng môi trường thử nghiệm hay thu tiền thật? VNPAY đã được trao đổi như một phương án tích hợp, chưa được xác nhận là lựa chọn chính thức. Thời gian dự kiến khách nhận được tiền hoàn cũng chưa xác định; chưa cam kết thời gian hoàn với khách.

Việc chọn cổng thu tiền chưa chứng minh có khả năng thu hộ/chia tiền/chi trả đối tác. [DBML hiện tại](../database/proposals/hotel-booking-current-fixed.dbml) đã có ba bảng tài chính và promotions.funding_source; còn thiếu snapshot bên tài trợ ở redemption và dữ liệu điều chỉnh đầy đủ. Không có khối checks; các bất biến dưới đây vẫn cần thực thi trong migration/backend, không suy ra từ Note hoặc FK.

### 8.1. Đọc ba bảng tài chính

| Bảng | Dữ liệu trong DBML | Giới hạn cần giữ rõ |
| --- | --- | --- |
| hotel_contracts | Chủ thể ký, khách sạn, mã, commission_rate, commission_basis, starts_at/ends_at, trạng thái DRAFT/ACTIVE/EXPIRED/TERMINATED | Không có bảng phiên bản riêng; tạo thỏa thuận mới và giữ bản cũ. FK owner chỉ trỏ users, chưa chứng minh người đó có quyền ký cho hotel. |
| booking_financials | PK booking_id; contract/Owner; subtotal/discount; platform_discount_amount, hotel_discount_amount; customer_paid_amount; snapshot hoa hồng và phần được hưởng; PENDING/FINALIZED | Snapshot gốc không phải sổ điều chỉnh. FINALIZED không tự bằng Owner đã nhận hoặc đồng nghĩa số phải trả chưa bị thay đổi bởi refund. |
| hotel_payouts | Nhiều lần thử/booking, amount/currency, tài khoản nhận, idempotency_key, bank_transaction_ref, người thực hiện và paid_at | PENDING/PROCESSING/SUCCEEDED/FAILED/CANCELLED là trạng thái chi, tách khỏi trạng thái khách thanh toán. Chưa có partial UNIQUE chống nhiều lần đang hiệu lực/thành công cho cùng booking. |

commission_basis hỗ trợ BEFORE_DISCOUNT (giá trước giảm), AFTER_DISCOUNT (giá sau toàn bộ giảm), AFTER_HOTEL_DISCOUNT (giá trước giảm trừ voucher HOTEL). Sự tồn tại của enum không chốt một cách tính mặc định cho mọi hợp đồng; tỷ lệ, cách làm tròn và thời điểm áp dụng phải được duyệt.

Các bất biến cho bản thu đủ 100%, chưa phân bổ phí cổng riêng:

```text
discount_amount = platform_discount_amount + hotel_discount_amount
customer_paid_amount = subtotal_amount - discount_amount
customer_paid_amount + platform_discount_amount
    = platform_fee_amount + hotel_earning_amount
```

platform_fee_amount là hoa hồng gốc trước chi phí nền tảng; phần sau tài trợ voucher là platform_fee_amount - platform_discount_amount, chưa phải lợi nhuận sau mọi chi phí. Kiểm tra tổng không âm phù hợp, tỷ lệ trong [0, 100], nguồn giá/voucher/tiền thu khớp booking, quyền truy cập và xử lý đồng thời. Tiền thu trùng phải theo dõi để hoàn, không cộng vào customer_paid_amount của snapshot xác nhận đơn.

Giữ chỗ tạm trong lúc thanh toán khác đặt cọc một phần và khác nền tảng giữ khoản đã thu. Bản hiện tại chưa thêm trạng thái thu cọc/trả phần còn lại; vẫn xác nhận khi thu đủ 100%.

## 9. Module media — Ảnh và tệp đính kèm

**Phạm vi:** Ảnh khách sạn, ảnh loại phòng, ảnh đại diện tài khoản; tải lên, liên kết, chọn ảnh đại diện hiển thị, sắp xếp và gỡ ảnh.

**Vai trò:** Khách hàng quản lý ảnh hồ sơ của mình; quản lý quản lý ảnh khách sạn được giao; quản trị can thiệp khi cần. Lễ tân không quản lý thư viện ảnh.

**Đã chốt:** Không giới hạn số lượng ảnh của khách sạn hoặc loại phòng; bỏ mức tối đa 10 ảnh. Số ảnh lưu và số ảnh được chọn làm ảnh đại diện là hai khái niệm khác nhau.

| Mã | Quy tắc nghiệp vụ |
| --- | --- |
| MD-01 | Mỗi ảnh có người tải lên và đối tượng sở hữu rõ ràng. Chỉ người có quyền đối với đối tượng đó được liên kết, đổi thứ tự hoặc gỡ ảnh. |
| MD-02 | Bản đầu chỉ nhận ảnh JPEG, PNG hoặc WebP, tối đa 5 MiB/tệp; không nhận tài liệu tùy thân, tệp thanh toán hoặc video. |
| MD-03 | Không áp đặt giới hạn số lượng ảnh theo khách sạn, loại phòng hoặc tài khoản. Mỗi tài khoản vẫn chỉ chọn một ảnh làm ảnh đại diện hiện hành; việc chọn ảnh đại diện không giới hạn tổng số ảnh được lưu. |
| MD-04 | Chỉ liên kết ảnh đã tải và được kiểm tra hợp lệ. Không cho lấy mã ảnh của khách sạn khác để gắn vào khách sạn mình. |
| MD-05 | Mỗi khách sạn hoặc loại phòng có tối đa một ảnh đại diện hiển thị. Khi gỡ ảnh này, người quản lý chọn ảnh thay thế hoặc hệ thống hiển thị ảnh mặc định. |
| MD-06 | Chỉ ảnh của khách sạn/loại phòng đang công khai mới được truy cập công khai. Ảnh tài khoản chỉ hiển thị cho chủ tài khoản và nhân sự có nhu cầu xử lý hợp lệ. |
| MD-07 | Gỡ liên kết một ảnh không được làm mất ảnh vẫn đang được sử dụng hợp lệ ở nơi khác. Tệp tải lên chưa được sử dụng được dọn sau thời hạn đề xuất 24 giờ. |

**Còn cần chốt:** Giới hạn 5 MiB cho từng tệp và quyền hiển thị ảnh đại diện khách hàng. Đề xuất giữ ảnh tài khoản riêng tư vì bản đầu chưa có đánh giá hoặc hồ sơ công khai. Quyết định không giới hạn số lượng ảnh đã chốt; không đồng nghĩa đã bỏ giới hạn kích thước từng tệp.

## 10. Nghiệp vụ mở rộng reviews — Đánh giá khách sạn

**Phạm vi đề xuất:** Khách đánh giá sau lưu trú; sửa đánh giá của mình; hiển thị điểm trung bình/số đánh giá công khai; quản trị ẩn/hiện nội dung có lý do. Ánh xạ dữ liệu chính là bảng reviews.

**Trạng thái duyệt:** Đã yêu cầu bổ sung đặc tả; việc đưa reviews vào MVP và các chính sách RV dưới đây vẫn chờ nhóm duyệt. Đây chưa phải module đã có trong source.

**Vai trò đề xuất:** Khách truy cập đọc đánh giá công khai; khách hàng viết/sửa của mình; quản lý xem/báo cáo đánh giá thuộc khách sạn mình; quản trị kiểm duyệt. Lễ tân và quản lý không sửa điểm/nội dung của khách.

| Mã | Quy tắc đề xuất |
| --- | --- |
| RV-01 | Chỉ người đặt có tài khoản hợp lệ, đang đăng nhập, được đánh giá booking của mình sau khi booking đã CHECKED_OUT. CONFIRMED, CHECKED_IN, CANCELLED, EXPIRED và NO_SHOW không được tạo đánh giá. |
| RV-02 | Một booking có tối đa một đánh giá, kể cả đơn gồm nhiều loại/phòng vật lý. Một người có nhiều lần lưu trú có thể đánh giá mỗi booking đủ điều kiện một lần. Không tạo đánh giá riêng cho từng đêm. |
| RV-03 | customer_id và hotel_id của đánh giá phải khớp người đặt/khách sạn trong booking. Không cho chọn booking người khác hoặc gán đánh giá sang khách sạn khác; đại diện lưu trú khác người đặt chưa có quyền đánh giá riêng. |
| RV-04 | Điểm rating bắt buộc là số nguyên từ 1 đến 5. Tiêu đề và nội dung có thể để trống theo thiết kế hiện tại; giới hạn độ dài và thời hạn được đăng/sửa cần chốt trước khi triển khai. |
| RV-05 | Đề xuất đánh giá hợp lệ được PUBLISHED ngay. Khách chỉ sửa điểm/tiêu đề/nội dung của chính mình; không sửa liên kết booking, người viết hoặc khách sạn. Sửa đánh giá đang HIDDEN không tự làm nó xuất hiện lại. |
| RV-06 | Chỉ quản trị được chuyển PUBLISHED sang HIDDEN hoặc khôi phục, phải ghi lý do, người và thời điểm. Quản lý khách sạn chỉ báo cáo nội dung cần xem xét; không tự ẩn đánh giá thấp. Không xóa mất dấu vết kiểm duyệt. |
| RV-07 | Danh sách và điểm tổng hợp công khai chỉ tính đánh giá PUBLISHED của khách sạn. Khách sạn đang ngừng bán vẫn giữ lịch sử đánh giá; quyền xem công khai tiếp tục theo trạng thái công khai của khách sạn ở HT-05. |
| RV-08 | Điểm trung bình = tổng rating PUBLISHED / số đánh giá PUBLISHED, đề xuất hiển thị một chữ số thập phân. Không có đánh giá thì hiển thị chưa có đánh giá, không coi là 0 sao. Sửa/ẩn/khôi phục phải cập nhật cả điểm và số lượng tương ứng. |
| RV-09 | Công khai điểm, nội dung, thời gian và tên hiển thị phù hợp; không công khai email, số điện thoại, mã booking hoặc thông tin thanh toán. Ảnh hồ sơ vẫn theo quyền của MD-06, không tự công khai vì khách viết đánh giá. |
| RV-10 | Gửi lại cùng yêu cầu đăng không sinh đánh giá thứ hai; trả đánh giá đã tạo hoặc hướng dẫn sửa rõ ràng. Thay đổi nội dung/kiểm duyệt cần có lịch sử phù hợp; không tự tạo hoàn tiền hoặc bồi thường từ điểm đánh giá. |

**Luồng đề xuất:** Booking CHECKED_OUT → khách nhập điểm/nội dung → kiểm tra sở hữu và chưa có đánh giá → lưu PUBLISHED → cập nhật tổng hợp; khi có báo cáo, quản trị xem xét và ẩn/khôi phục có lý do.

**Ví dụ:** Khách sạn có ba đánh giá công khai 5, 4, 3 điểm thì hiển thị 4,0/5 với 3 đánh giá. Nếu quản trị ẩn đánh giá 3 điểm theo lý do kiểm duyệt hợp lệ, còn 4,5/5 với 2 đánh giá.

**Còn cần chốt:** Có đưa vào MVP không; đăng công khai ngay hay chờ duyệt; thời hạn đăng/sửa; giới hạn nội dung; khách có được tự gỡ đánh giá không. Lịch sử sửa/kiểm duyệt chưa được mô tả đầy đủ trong bảng reviews hiện có và cần rà thêm khi triển khai.

## 11. Voucher cơ bản — promotions và promotion_redemptions

**Phạm vi đã yêu cầu:** Có voucher, điều kiện áp dụng, cách giảm và số tiền tối đa được giảm; làm đơn giản trước theo các cột DBML đang có. Phần này thay phương án không voucher trước đây, chưa triển khai cộng dồn mã hoặc công cụ chiến dịch phức tạp.

**Vai trò:** Quản trị tạo/quản lý mã toàn hệ thống hoặc của khách sạn; quản lý chỉ tạo/quản lý mã của khách sạn được giao; khách áp dụng mã khi đặt. Lễ tân không tự cấp/sửa mức giảm trong đơn.

| Nội dung | Cột trong promotions | Cách hiểu |
| --- | --- | --- |
| Mã và phạm vi | code, hotel_id | Mã duy nhất; hotel_id NULL là toàn hệ thống, có giá trị là riêng khách sạn |
| Điều kiện áp dụng | status, starts_at, ends_at, min_booking_amount | ACTIVE, đúng thời gian, đúng hotel và đạt giá trước giảm tối thiểu |
| Giảm phần trăm | discount_type = PERCENTAGE, percentage_off | Ví dụ 10 là giảm 10% |
| Giảm cố định | discount_type = FIXED_AMOUNT, fixed_amount | Ví dụ giảm 100.000 đồng |
| Tiền giảm tối đa | max_discount_amount | Có giá trị thì không vượt trần; NULL là không có trần riêng |
| Giới hạn lượt có trong DB | usage_limit, per_user_limit | Có cấu hình thì phải kiểm tra; mặc định mỗi user tối đa 1 lượt theo nguồn |

| Mã | Quy tắc cơ bản |
| --- | --- |
| PRO-01 | code duy nhất theo DBML; đề xuất bỏ khoảng trắng đầu/cuối và so sánh sau chuẩn hóa chữ hoa. Khách không tự nhập số tiền giảm. |
| PRO-02 | Chỉ áp mã ACTIVE, đúng hotel hoặc toàn hệ thống, starts_at <= lúc tạo đơn < ends_at và subtotal_amount >= min_booking_amount. Đề xuất xét lúc đặt; điều kiện riêng theo ngày lưu trú chưa có cột cấu hình trong DBML. |
| PRO-03 | PERCENTAGE dùng percentage_off trong (0, 100] và fixed_amount NULL; FIXED_AMOUNT dùng fixed_amount > 0 và percentage_off NULL. Trần giảm nếu có phải dương. Đây là kiểm tra nghiệp vụ; nguồn mới chưa có khối CHECK thực thi. |
| PRO-04 | Tính giảm sơ bộ theo phần trăm hoặc số tiền cố định; nếu có max_discount_amount thì lấy mức nhỏ hơn. Trần áp dụng cho cả hai cách giảm khi được cấu hình; khoản giảm không vượt giá trước giảm. |
| PRO-05 | total_amount = subtotal_amount - discount_amount. Đề xuất làm tròn phần trăm xuống đến đồng và yêu cầu tổng sau giảm > 0 cho luồng thu tiền hiện tại; mã làm tổng bằng 0 bị từ chối với lý do rõ ràng, chưa có luồng đơn miễn phí. |
| PRO-06 | Một booking dùng tối đa một mã nhờ UNIQUE promotion_redemptions.booking_id. Lưu promotion_id, booking_id, user_id đúng người đặt, discount_amount thực tế và status; bookings.discount_amount phải khớp khoản giảm đó. |
| PRO-07 | Xem báo giá chưa tạo lượt dùng. Kiểm tra lại khi tạo đơn, giữ tồn và ghi redemption nhất quán; gửi lại cùng yêu cầu không tạo thêm đơn/lượt. Giá/voucher thay đổi sau khi đơn đã được chấp nhận không tự làm đổi tổng đã lưu; thử thanh toán lại không gia hạn. |
| PRO-08 | Payment thu tổng sau giảm và hoàn theo số thực thu/điều khoản của đơn; không hoàn phần voucher chưa thu. Hủy/hết hạn trước xác nhận đề xuất chuyển lượt sang CANCELLED; hoàn tiền thực sự hoàn tất dùng REFUNDED. Không dùng REVERSED vì enum nguồn không có. |
| PRO-09 | promotions.funding_source đã có trong DBML, dùng promotion_funding_source__t: PLATFORM là nền tảng chịu, HOTEL là khách sạn chịu. hotel_id chỉ phạm vi sử dụng, không xác định bên tài trợ. Nghiệp vụ yêu cầu lưu lại funding_source cùng khoản giảm trong promotion_redemptions, nhưng nguồn hiện tại còn thiếu cột snapshot này. Quản lý khách sạn chỉ tạo mã HOTEL; quản trị được chọn PLATFORM; khách không quyết định bên chịu. Công thức hoa hồng/hoàn theo bên tài trợ còn cần chốt; chưa đồng tài trợ. |

**Ví dụ:** Giá trước giảm 2.000.000 đồng; mã giảm 10%, tối đa 150.000 đồng → giảm 150.000 đồng → khách trả 1.850.000 đồng. Mã giảm cố định 200.000 đồng nhưng trần 150.000 đồng cũng chỉ giảm 150.000 đồng. Không có mã thì discount_amount = 0.

**Giới hạn lượt:** Các cột usage_limit/per_user_limit vẫn được giữ theo DB, không bỏ điều kiện đã cấu hình. Nếu dùng hạn mức, phải tính cả lượt APPLIED đang giữ cho đơn chờ thanh toán; APPLIED kết hợp trạng thái booking để phân biệt giữ/đã dùng. Cách đếm và trả lượt sau khi đã thanh toán cần chốt trước khi triển khai, không mở thêm quy trình phức tạp trong bản mô tả cơ bản này.

**Cần chốt chi tiết:** Làm tròn, tổng bằng 0, trả lượt sau thanh toán, đổi mã trên đơn đã tạo và tính hoa hồng/hoàn khi mỗi bên tài trợ. DB đã có bên tài trợ trên promotions và các khoản giảm tách bên ở booking_financials; promotion_redemptions vẫn thiếu funding_source và snapshot mã/điều kiện. Quyền tạo/sửa phải kiểm tra backend; promotions chưa có created_by hoặc bảng lịch sử thay đổi. Không dùng cấu hình voucher mới để suy lại bên chịu tiền của lượt cũ.

**Điểm DB cần lưu ý:** ON DELETE SET NULL ở promotions.hotel_id có thể biến mã riêng thành toàn hệ thống nếu xóa hotel. Không cho thao tác xóa làm thay đổi phạm vi theo cách này; giữ đúng mô tả nguồn và đánh dấu cần sửa/chốt trong docs DB.

Đối chiếu [DBML hiện tại](../database/proposals/hotel-booking-current-fixed.dbml) và [ERD 40 bảng](../database/erd.md). Các công thức tài chính liên quan ở mục 8.1.

## 12. Ranh giới trách nhiệm giữa các module

| Câu hỏi nghiệp vụ | Module chịu trách nhiệm chính | Phối hợp |
| --- | --- | --- |
| Người này là ai, được thao tác với khách sạn nào? | identity | Mọi module kiểm tra quyền tương ứng. |
| Khách sạn có mở bán, có những phòng vật lý nào, thuộc loại nào? | hotel | inventory dùng danh mục để xác định năng lực; booking dùng loại phòng/sức chứa khi đặt. |
| Còn đủ q phòng liên tục cả kỳ, lịch có trùng hoặc phòng có bảo trì không? | inventory | booking yêu cầu giữ/xác nhận/trả tồn và kiểm tra gán phòng; không tự quản lý số còn lại. |
| Phòng đã dọn xong và có sẵn sàng bàn giao ngay không? | inventory | Lễ tân/quản lý cập nhật; booking kiểm tra tại check-in và ghi nhận chờ dọn khi check-out. |
| Ai được chặn phòng, chặn những đêm nào và khi nào mở lại? | inventory | Nhóm quy tắc BL; hotel cung cấp danh mục, booking cung cấp các đơn và phân công bị ảnh hưởng. |
| Kỳ lưu trú này giá bao nhiêu? | pricing | booking lưu báo giá được khách chấp nhận. |
| Khách có đơn nào, được gán số phòng nào, được hủy hay nhận phòng chưa? | booking | Sở hữu liên kết đơn/phòng được gán và lịch sử lưu trú; inventory kiểm tra, bảo vệ lịch khả dụng tương ứng. |
| Khách khiếu nại đơn nào, có dấu hiệu dẫn giao dịch ra ngoài không? | booking phối hợp quản trị | Gắn phản ánh với booking, lưu chứng cứ/kết quả kiểm tra; identity giới hạn người xử lý, payment theo dõi khoản hoàn nếu có. |
| Đã thực thu/hoàn bao nhiêu tiền? | payment | booking áp dụng chính sách để xác định khoản phải thu hoặc được hoàn. |
| Hoa hồng theo thỏa thuận nào, phần nền tảng và Owner là bao nhiêu? | payment | hotel cung cấp khách sạn/Owner; booking cung cấp đơn/trạng thái; pricing và voucher cung cấp giá, khoản giảm và bên tài trợ; identity kiểm tra quyền tài chính. |
| Đã chuyển Owner chưa, còn phải trả và điều chỉnh gì? | payment | Lưu hotel_payouts; đối chiếu thu/hoàn, snapshot tài chính và khiếu nại gắn booking trước chi; không tự coi FINALIZED là đã chuyển tiền. |
| Ảnh thuộc ai, có được sử dụng hoặc hiển thị không? | media | hotel và identity liên kết ảnh theo quyền. |
| Khách đã lưu trú có được đánh giá, điểm nào được công khai? | Nghiệp vụ reviews, đề xuất mở rộng | booking cung cấp điều kiện CHECKED_OUT, identity kiểm tra người đặt/quyền kiểm duyệt; chưa mặc định có module source riêng. |
| Mã giảm có hợp lệ và được giảm bao nhiêu? | Nghiệp vụ voucher cơ bản | promotions cấu hình điều kiện/cách giảm/trần; pricing tính, booking lưu redemption và khoản giảm, payment thu/hoàn theo số thực tế. Source chưa có logic triển khai. |

`shared` là phần dùng chung về kỹ thuật, không phải module sở hữu nghiệp vụ hoặc nơi đặt các chính sách còn thiếu chủ quản.

## 13. Tổng hợp nội dung đã chốt và còn cần duyệt

Các quyết định được ghi rõ dưới đây; không mặc định nội dung chưa được phản hồi là đã được duyệt. Phần giải thích IV-06 và mốc giờ trong ví dụ BK-10 vẫn dùng các giá trị đề xuất để minh họa.

| Nội dung | Đã chốt | Còn đề xuất hoặc cần quyết định |
| --- | --- | --- |
| identity | Đăng ký trước khi đặt; xác minh email một lần; reset password qua URL gửi email | Thời hạn URL xác minh/reset; dịch vụ gửi email. |
| hotel | Quyền công khai/khóa bán như trên; danh mục phòng vật lý có số phòng, tầng và loại phòng | Quyền mở bán lại sau khi quản lý tự tạm ngừng. |
| inventory | Không bán vượt số lượng; tồn khớp phòng vật lý; kiểm tra khả năng phân bổ liên tục/trùng lịch/bảo trì; gán phòng không trừ tồn thêm | Thời lượng giữ 15 phút; quy trình xử lý bảo trì khẩn cấp. Hạn thanh toán độc lập với giờ check-in. |
| room_blocks | Đã thuộc phạm vi inventory và đã yêu cầu bổ sung đặc tả BL | Quyền thao tác, khoảng ngày bao gồm đêm cuối, giải phóng/gia hạn và xử lý sự cố khẩn cấp như phần 5.1. |
| pricing | Giá phòng trọn gói trước giảm; có voucher cơ bản, thu đúng tổng sau giảm | Giá cơ bản dự phòng, cách làm tròn và luồng tổng bằng 0 cần chốt như phần 11. |
| booking — thanh toán | Trả 100% rồi xác nhận; nhân viên thực hiện check-in | Giờ nhận/trả mặc định 14:00/12:00, hạn đặt trong ngày và tối đa 30 đêm. |
| booking — phòng vật lý | Khách đặt loại phòng/số lượng; nhân viên gán đúng/đủ phòng trước hoặc khi check-in, kiểm tra sẵn sàng; check-out chuyển chờ dọn | Chuyển phòng sau check-in/nâng hạng chưa thuộc phạm vi; quy trình buồng phòng chi tiết chưa triển khai trong bản đầu. |
| booking — NO_SHOW | Nhân viên đánh dấu khi quá hạn; chưa đánh dấu thì vẫn CONFIRMED; báo đến muộn thì không đánh dấu theo mốc cũ; không hoàn/không thu thêm; trả tồn chưa dùng | Mốc 00:00 hôm sau; khách tiếp tục không đến theo giờ báo muộn hoặc đến sau NO_SHOW. |
| booking — hủy | Chưa có quyết định mới về hủy đơn | Miễn phí trước 24 giờ, phí hủy muộn 100% và khách sạn hủy hoàn 100% vẫn là đề xuất. |
| payment | Thu đúng giá trọn gói; NO_SHOW và gán phòng cùng loại không phát sinh thu thêm | Cổng thanh toán, môi trường thử nghiệm/thu thật, thời gian hoàn tiền. |
| media | Không giới hạn số lượng ảnh | Giới hạn kích thước 5 MiB/tệp; quyền hiển thị ảnh tài khoản. |
| reviews | Đã yêu cầu bổ sung đặc tả đánh giá | Chưa chốt vào MVP; điều kiện đăng/sửa, công khai, kiểm duyệt và tổng hợp điểm RV đều là đề xuất. |
| promotions | Tối đa một mã/đơn; có điều kiện/cách giảm/trần; bên tài trợ PLATFORM/HOTEL đã có trên promotions | Bổ sung funding_source ở redemption, snapshot mã/điều kiện; chốt làm tròn, miễn phí toàn bộ, trả lượt và hoa hồng/hoàn theo bên tài trợ. |
| Tài chính nền tảng/Owner | DBML có hotel_contracts, booking_financials, hotel_payouts; thu hộ/giữ khoản chưa chi, hoa hồng riêng và báo cáo hai bên đã chọn | Mức/căn cứ áp dụng theo hợp đồng, thời điểm chọn hợp đồng, lịch/điều kiện chi, phí cổng, phân bổ và lịch sử điều chỉnh hủy/hoàn/NO_SHOW; ràng buộc chống chồng hợp đồng/chi trùng. |
| Giao dịch ba bên | 5 nguyên tắc PV-12; hỗ trợ khiếu nại thủ công và kiểm tra hủy bất thường | Phạm vi liên hệ, thời hạn xử lý phản ánh/giải trình và tiêu chí kiểm tra; không tự coi mọi lần hủy là gian lận. |
| Phạm vi chung | Ưu tiên luồng đặt phòng/thanh toán chính xác và gán phòng vật lý khi vận hành; khách đặt theo loại phòng | Nhiều khách sạn; một đơn nhiều loại phòng cùng kỳ; các quyền chưa chốt riêng và các mục ngoài phạm vi khác. |

Sau khi duyệt phạm vi và quy tắc, mới đặc tả đầy đủ trạng thái, use case, database và API dựa trên các quyết định được chấp nhận. Mã quy tắc trong tài liệu này là điểm tham chiếu cho các tài liệu đó.

## 14. Căn cứ và giới hạn

- Quyết định giao dịch ba bên được giữ ở PV-12, HT-13, BK-20/BK-21 và PM-14 đến PM-20 của tài liệu này. Hợp đồng, phân quyền và chính sách ở đây là yêu cầu nghiệp vụ, chưa xác nhận đã triển khai.
- [README dự án](../../README.md): mô tả 7 module và tình trạng khung backend.
- Các thư mục module dưới `src/main/java/com/team/hotelbooking/` hiện là khung; chưa có mô hình hoặc quy tắc nghiệp vụ để xác nhận các đề xuất trên.
- Phân quyền và chính sách công khai trong tài liệu là hành vi mong muốn. Cấu hình bảo mật hiện tại dùng HTTP Basic, chưa thể hiện các vai trò hoặc quyền theo khách sạn ở trên.
- [Thuật ngữ dùng trong bản nháp](../../CONTEXT.md).
- [DBML 40 bảng đã sửa Ref](../database/proposals/hotel-booking-current-fixed.dbml) và [ERD 65 FK](../database/erd.md) là nguồn đối chiếu lần cập nhật này; đã bỏ khối checks, chưa chạy migration/PostgreSQL.
- [README database](../database/README.md) và [từ điển dữ liệu](../database/data-dictionary.md) vẫn theo bản 37 bảng, chưa cập nhật trong lần sửa ba file này; không dùng số đếm cũ để mô tả ERD mới.
