# Bàn giao quản lý hoa hồng Owner/Admin

Plan: `2026-10-23-owner-commission-management-plan.md`.

## Đã triển khai

- BE: phiên bản chính sách phí, lịch hiệu lực không chồng lấn, khóa theo phạm vi cơ sở/toàn sàn, audit người thao tác; endpoint đổi phí và xem audit dành cho Admin.
- Snapshot quote/booking lưu policyId khi biết nguồn. Đơn cũ không bị tính lại.
- V48: bảng phạm vi khóa, audit policy, sổ commission và index; backfill từ giao dịch ví gắn BOOKING, không tạo thêm số dư ví. Dữ liệu chưa đủ bằng chứng hiển thị cần đối soát.
- Commission HOLD/SETTLE/REVERSE dùng chung transaction với booking/ví, unique business key và khóa booking. Nối cả các tác vụ tự động hoàn tất/no-show; hủy phiên PayOS không ghi reversal.
- API Owner cho chính sách hiện tại, lịch sử liên quan cơ sở, thống kê, bảng kê và chi tiết đơn. Kiểm tra OWNER đang hoạt động của đúng cơ sở bằng StoreFinancialSecurityService; không mở rộng DTO customer/nhân viên.
- FE Owner: tab Hoa hồng & Đối soát, bộ lọc/phân trang, chi tiết gross/phí/net và lịch sử sự kiện. Giao dịch ví liên kết vào chi tiết commission của booking.
- FE Admin: tìm/chọn cơ sở theo tên/địa chỉ, đổi rate thành phiên bản mới, đặt lịch tương lai, bật/tắt và xem audit.
- Sửa mô tả thời điểm tiền pending được giải ngân, không còn ghi giải ngân ngay lúc check-in.
- ESLint bỏ qua `.worktrees/**` vì đây là checkout phụ chứa build artifacts, không phải nguồn của checkout hiện tại. Không tắt rule lint.

## Kiểm tra đã thực hiện

- Không thêm test tự động, không thêm thư viện test, không chạy test suite theo yêu cầu người dùng.
- BE: `./mvnw.cmd -Dmaven.test.skip=true package` với JDK 21: BUILD SUCCESS trên bản cuối.
- FE: `npx tsc --noEmit`: pass.
- FE: ESLint các file thay đổi của chức năng và eslint.config.mjs: 0 lỗi, 0 cảnh báo.
- FE: `npm run build -- --webpack`: pass trên bản cuối. Build Turbopack mặc định lỗi tạo tiến trình Node con ở môi trường máy hiện tại; lần chạy trong sandbox còn bị chặn tải Google Fonts. Không đổi engine build trong package.json.
- Lint toàn repo sau khi bỏ worktree phụ: 107 lỗi và 202 cảnh báo cũ ngoài chức năng hoa hồng, gồm reports/stores/menu/sponsored và các service/type cũ. Không báo toàn repo đã sạch lint.
- PostgreSQL 18 tạm tại localhost:55432, dữ liệu riêng trong target, không dùng DB server: Flyway áp dụng thành công 61 migrations/seed, tới V48; chạy lại xác nhận checksum/schema hợp lệ.
- Bản BE chạy được với cấu hình local riêng; `/api/configs/public` trả 200; API policy Owner và API Admin commission trả 401 khi chưa đăng nhập.
- Chưa kiểm tra PayOS thật, tạo/hoàn booking thật, rút tiền thật, giao diện sau đăng nhập trên trình duyệt và các race đồng thời. Local không có Redis đang chạy để xác minh phiên đăng nhập đầy đủ. Build và migration không thay thế các bước này.

## Sau khi đưa lên môi trường kiểm tra

1. Backup DB trước khi chạy V48. Đọc số liệu backfill/đơn cần đối soát trước khi kết luận tất cả lịch sử đã khớp.
2. Deploy BE có V48 trước, sau đó FE; các API Owner commission là API mới.
3. Admin chọn một cơ sở test, đổi từ 15% sang 10%. Quote mới đơn 4.000đ phải ra phí 400đ/net 3.600đ; quote còn hạn/booking đã chốt ở 15% vẫn giữ phí 600đ/net 3.400đ.
4. Thanh toán đơn test: kiểm tra HOLD commission và net pending; sau hoàn tất kiểm tra SETTLE và net available. Refresh/nút Kiểm tra không tạo giao dịch thứ hai.
5. Đơn test riêng: hủy hợp lệ trước hạn sau thanh toán thì commission REVERSE, net pending bị đảo và hoàn actual paid thành MoodPoints. Hủy phiên PayOS đơn thuần giữ booking/voucher, không đảo tiền.
6. Kiểm tra TABLE_EXPIRED không giải ngân; CLAIMED_LATE/COMPLETED/no-show cuối ngày giải ngân đúng một lần.
7. Owner đúng cơ sở xem được; MANAGER/CASHIER/customer/Owner cơ sở khác bị chặn. Đổi cơ sở trên FE không giữ detail/dữ liệu của cơ sở trước.

Chưa commit, push hoặc deploy thay đổi này. Không thay policy trên server và không thực hiện thanh toán/chuyển khoản thật trong phiên triển khai code.
