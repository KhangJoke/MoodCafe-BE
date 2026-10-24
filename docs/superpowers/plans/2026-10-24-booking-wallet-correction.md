# Sửa tích hợp ví trên booking — 24/10/2026

## Phạm vi
- Booking của Khang là nguồn đơn mới duy nhất; gỡ module reservation trùng và các service điều phối/thanh toán/quyết toán/hoàn tiền trùng.
- Khôi phục lịch LIMITED_TIME, thời gian dọn bàn và quyền OWNER/MANAGER/CASHIER từ baseline a5852b4.
- Giữ webhook, nút kiểm tra PayOS và hủy phiên thanh toán không hủy booking trong hạn giữ chỗ.
- Client cũ được tạo booking không quoteId: server tự tính snapshot. Client mới có thể dùng quote để ràng buộc giá/voucher.
- Giữ quyết định người dùng đã chốt: hoàn thực trả vào MoodPoints, 1 điểm = 1đ, không rút tiền; đổi voucher trước checkout, không thanh toán trực tiếp bằng điểm.
- Giải ngân CHECKED_IN / CLAIMED_LATE / EXPIRED_NO_SHOW; TABLE_EXPIRED giữ pending. SETTLE chống lặp khi đơn hoàn tất.
- Điểm chi tiêu theo 1% gross (100.000đ => 1.000 điểm); đây là lựa chọn theo công thức vì ví dụ 100 điểm trong tài liệu mâu thuẫn.
- Đúng giờ 20 khi hoàn tất, tự checkout >=30 phút thưởng30, review có ảnh sau COMPLETED/CLAIMED_LATE thưởng50, mỗi loại tối đa một lần mỗi booking.

## Bổ sung
- Admin quản lý mẫu voucher trên BE/FE, có minGross, hạn dùng và snapshot khi khách đổi. Voucher theo quy đổi1:1.
- Rút tiền có bảng kê bất biến theo giao dịch nguồn, hỗ trợ cả tiền BOOKING và tiền RESERVATION lịch sử. Không tái mở module reservation.
- Cộng total_withdrawn khi xác nhận chuyển, thông báo Owner sau commit khi PAID/REJECTED. Giữ hạn mức100k và trạng thái API hiện có để tương thích client.
- QR trả bàn do nhân viên phát, gắn booking, hết hạn2phút; khách quét trên Mobile hoặc xác nhận trên FE. Không dùng QR vé do khách sở hữu để chứng minh có mặt.
- Mobile feature loyalty có domain/usecase/Result, Freezed DTO; ví/đổi thưởng/voucher/lịch sử và chọn voucher trong booking cũ.

## Migration và xác minh
- V49 chỉ bổ sung schema, không sửa V41–V48 hoặc xóa bảng/cột/dữ liệu cũ.
- Guard dừng migration nếu còn reservation đang hoạt động/voucher đang khóa vào reservation: phải đối soát trước triển khai.
- Không viết/chạy test suite. Chỉ build, lint/typecheck, analyze và áp migration trên DB local cách ly.
- Chưa push/deploy AWS. Các đơn CHECKED_IN từ trước đợt sửa chưa có SETTLE vẫn được quyết toán khi hoàn tất; không tự dịch chuyển hàng loạt số dư cũ.

## Kết quả xác minh cuối
- BE Java21 package với maven.test.skip=true: đạt. JAR không còn class của package reservation (chỉ có entry thư mục rỗng).
- BE khởi động hoàn chỉnh trên localhost:18082, DB local wallet_realign_check_v2, cron bị tắt bằng launcher tạm trong target.
- HTTP public config200, OpenAPI200; số route /reservations =0; có admin voucher-templates và checkout booking.
- Flyway trên DB local mới:62 migration gồm repeatable seed, schemaV49 thành công. Không kết nối DB AWS.
- FE TypeScript đạt; ESLint toàn dự án0 lỗi/75 cảnh báo; production webpack build đạt. Thay đổi cuối bảng kê đã typecheck và lint riêng lại.
- Mobile Dart analyze toàn dự án: No issues found; Flutter build web đạt. Chưa build Android/iOS: Windows chưa bật symlink cho plugin; cần kiểm tra camera trên thiết bị.
- Không viết hay chạy test suite. Các test cũ tham chiếu module reservation không được chạy/cập nhật trong lượt này theo yêu cầu không viết test; cần xử lý trước khi bật lại pipeline test cũ.
- Không thực hiện giao dịch PayOS, rút tiền hay hoàn tiền thật. Không push hoặc deploy.
