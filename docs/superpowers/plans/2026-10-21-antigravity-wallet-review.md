# Review Antigravity: ví, MoodPoints, voucher và đặt bàn

Ngày: 21/10/2026. Review thay đổi chưa commit của BE/FE/MB so với `2026-10-20-single-moodpoints-final.md`.

**Kết luận: chưa đủ điều kiện nghiệm thu hoặc triển khai tiền thật.** Có lỗi quyền rút tiền, payment integration, quyền chuyển trạng thái booking và chọn nhầm ngân hàng. Báo cáo này không sửa code sản phẩm.

Phạm vi UI: review source/luồng thao tác và contract API; chưa kiểm tra ảnh render hoặc phiên đăng nhập thật trên trình duyệt/thiết bị.

## P1 — cần sửa trước

### 1. API ví/ngân hàng/rút tiền chỉ yêu cầu đăng nhập, không yêu cầu OWNER của Store

- BE `moodcafe-be/src/main/java/com/moodcafe/wallet/controller/MerchantWithdrawalController.java:51`, `:85`, `:93`; `MerchantWalletController.java:42`.
- FE `src/components/merchant/shell/OwnerSidebar.tsx:77`, `src/app/merchant/wallet/page.tsx:35` không có owner guard theo cơ sở.
- Người dùng đăng nhập có thể đọc tài khoản ngân hàng cơ sở khác, thêm ngân hàng của mình rồi tạo lệnh rút từ available của cơ sở đó. Đây là tạo yêu cầu/khóa tiền trái phép; việc tiền thực rời ngân hàng vẫn cần Admin chuyển.
- `WithdrawalService` không có kiểm tra sở hữu bổ sung; SecurityConfig không bù kiểm tra này. Manager cũng được OwnerShell cho vào merchant, nên ẩn ở UI không đủ.
- Sửa: kiểm tra OWNER active đúng Store trên mọi API đọc/ghi; validate bank belongs to Store; FE chặn route/menu. Test Customer/Manager/Cashier/Owner cơ sở khác đều403.

### 2. Booking gọi nhầm PayOS subscription, sai số tiền và không nhận thanh toán booking

- BE `reservation/service/ReservationService.java:164` gọi `PayOSService.createPaymentLink(reservationUUID)`.
- PayOSService hiện tìm SubscriptionPayment; không tìm thấy thì amount mặc định50.000 (`payment/service/PayOSService.java:81-96`), sinh orderCode riêng và lưu anchor subscription. Webhook/polling không xác nhận Reservation.
- Ví dụ gross100k, voucher90k cần trả10k nhưng link có thể thu50k; đơn vẫn PENDING_PAYMENT, quán không nhận pending và voucher không capture. Order code của reservation cũng không được cập nhật thành mã gateway thực trả.
- Catch `ReservationService.java:170-174` còn trả URL `.../simulated/...` khi lỗi thay vì báo thất bại/đối soát, khiến UI báo có link nhưng không thanh toán được.
- Sửa: booking-specific create/capture, snapshot amount, persist gateway code, verify signature/amount/purpose, cập nhật PAID_CONFIRMED+pending+USED nguyên tử. Không fallback URL giả. Test gateway integration riêng cho booking, không chỉ mock refund.

### 3. Check-in/out không xác minh người thao tác hoặc QR

- BE `reservation/service/ReservationService.java:180`, `:211`: nhận userId nhưng không dùng; controller chỉ isAuthenticated.
- Tài khoản khác có reservation UUID có thể check-in/check-out, làm mất quyền hủy và giải ngân đơn. Ngay chính chủ cũng có thể gọi endpoint từ xa vì không cần QR xác thực.
- Check-in trước ngày hẹn cũng có thể thành CHECKED_IN vì chỉ so now<graceDeadline, không có cận sớm. Nhánh nhận nước trễ không kiểm tra closing deadline hoặc quyền thu ngân.
- Sửa: phân biệt Customer self-QR và staff đúng Store, kiểm tra ownership/token QR/thời gian; checkout hộ không được self bonus; late claim chỉ cho staff có quyền và trong deadline. Test truy cập chéo và replay.

### 4. Đổi cơ sở nhưng FE giữ ngân hàng cũ, BE vẫn nhận

- FE `src/app/merchant/wallet/page.tsx:75-76` chỉ set bank nếu selectedBankId rỗng; submit ở`:137`.
- BE `wallet/service/WithdrawalService.java:46-48` lấy bank bằng ID mà không ràng buộc Store.
- Mở ví A rồi chọn B: bankId A vẫn giữ, options select đã là của B. Lệnh rút ví B có thể snapshot ngân hàng A, gây chuyển sai người nhận.
- Sửa cả hai đầu: reset/validate bank khi đổi Store, chặn stale response; BE require bank.storeId==storeId. Test A→B→rút và request giả bank khác Store.

### 5. Admin khác có thể reject khi một Admin đang chuyển tiền

- BE `wallet/service/WithdrawalService.java:96-110`; confirm ở`:125`.
- PROCESSING được reject và hoàn locked về available mà không kiểm tra Admin đã claim hoặc tình trạng chuyển ngân hàng. Admin A chuyển tiền, B reject đồng thời → Owner có tiền ngân hàng và available được hồi, confirm của A bị lỗi vì REJECTED.
- Sửa: enforce claim, chuyển chưa rõ giữ locked/RECONCILIATION_REQUIRED, chỉ trả available khi xác minh chưa chuyển. Confirm bắt buộc reference/chứng từ, kiểm tra unique reference và tự duyệt. Test hai Admin đối đầu.

### 6. Voucher chờ thanh toán không có đường tự nhả khi khách bỏ thanh toán

- BE `voucher/service/VoucherLifecycleService.java:76`, `reservation/service/ReservationService.java:135`.
- Đã lưu holdUntil15phút nhưng tìm call sites production chỉ thấy định nghĩa releaseVoucher, chưa có caller/job xử lý failed/expired payment. Reservation cancel hiện chỉ nhận PAID_CONFIRMED.
- Khách đổi điểm, áp voucher, bỏ QR: voucher có thể LOCKED vô thời hạn, không dùng được phần giá trị đã đổi.
- Sửa: job/payment failure transition xác minh gateway rồi nhả voucher đúng reservation; không nhả khi chưa rõ có thu tiền hay chưa. Test hết TTL, late webhook, retry và re-lock ở đơn khác.

## P2 — lỗi logic và UI khác

### 7. Category đặt bàn không ràng buộc cơ sở

- BE `reservation/service/ReservationService.java:65-67`: findById rồi dùng minSpend, không check category.storeId/isActive/isDeleted.
- Có thể chọn Store A nhưng lấy category rẻ hơn của B để tính tiền cho A. Sửa query theo Store và trạng thái; không tự dùng giá100k nếu thiếu category mà chưa có rule.

### 8. Retry tạo lệnh rút sinh lệnh mới

- BE `wallet/service/WithdrawalService.java:54-57`: UUID.randomUUID mỗi request, payload/schema không có idempotency key.
- Mất response sau khi tạo100k thành công, retry với available còn đủ → hai lệnh, khóa200k, có thể chi cả hai.
- Sửa key theo actor+operation+payload, trả lại lệnh gốc; cùng key khác payload trả conflict. Test retry/concurrent.

### 9. Các mốc tự động và Review reward mới chỉ là hàm chưa được nối

- BE `finance/service/ReservationSettlementService.java:30`, `loyalty/service/LoyaltyRewardService.java:131` chỉ có definition, chưa có caller production khi tìm toàn src/main/java; không thấy @Scheduled.
- No-show không tự chuyển available, TABLE_EXPIRED không tự chạy; tạo Review không gọi thưởng này. `ReservationService.java:110` đặt closing=booking+12h, không phải giờ đóng cửa cơ sở.
- Sửa scheduler/job theo deadline thật, nối review sau xác thực; test đóng cửa/ca qua nửa đêm/job chạy bù và review đúng booking. Không coi service tồn tại là feature đã hoàn tất.

### 10. Admin không xem được trang thứ hai của queue rút tiền

- FE `src/app/admin/withdrawals/page.tsx:44-46` lấy15 dòng và totalPages nhưng không có nút next/previous; setPage chỉ reset0 tại`:145`.
- Có16+ lệnh thì lệnh cũ không truy cập được qua trang; lịch sử all cũng bị cắt.
- Sửa pagination/load more; test ít nhất16 lệnh và đổi filter.

### 11. Nút rút tối đa bị validation HTML chặn khi số dư lẻ10.000

- FE `src/app/merchant/wallet/page.tsx:491`: step10000 trong khi BE chỉ yêu cầu min và available.
- Balance125.500, bấm Rút tối đa → amount125500 không hợp lệ theo step, browser chặn submit.
- Dùng step1 cho VNĐ nguyên, đồng bộ min theo config. Test125.500 và ranh giới min.

### 12. Mobile hiển thị số dư0 khi API lỗi

- MB `lib/features/wallet/presentation/pages/moodpoints_wallet_page.dart:101`, `lib/features/vouchers/presentation/pages/voucher_catalog_page.dart:119`.
- Timeout/server lỗi bị hiển thị như khách mất hết điểm. Sửa error/unavailable + retry, không biến lỗi tài chính thành dữ liệu0. Test wallet lỗi trong khi tài khoản có điểm.

## Sai lệch phạm vi/kiến trúc cần hoàn thiện

- Antigravity tạo mới cả reservation/table schema trong V37 ở checkout chưa có flow booking. Cần xác định nhánh booking thật trước khi merge để tránh hai implementation không đồng bộ.
- P1–P10 chưa có bằng chứng được duyệt trong cuộc trao đổi này; code đã tự chốt15%, min100k, cancel2h, grace15phút, closing+12h, voucher60ngày, reward365ngày. Phải đối chiếu quyết định nhóm, config/snapshot đúng chính sách.
- MB `wallet/presentation/providers/wallet_providers.dart:7` và voucher providers gọi Dio trực tiếp; thiếu data source/repository/UseCase/Result theo AGENTS.md. Analyze pass không kiểm tra được quy tắc kiến trúc này.
- ReconciliationService hiện chỉ cộng số dư, chưa thực hiện đối soát ledger/payment/nguồn voucher; thiếu màn hình/config Admin voucher/finance và flow customer booking áp voucher trong thay đổi đang review. Không đánh dấu Task0–8 hoàn thành chỉ dựa vào các file scaffold.
- Chưa có integration tests PostgreSQL/concurrency/controller quyền cho wallet/booking. Test mới chủ yếu Mockito service tests.

## Thứ tự sửa đề nghị

1. Chặn quyền ví/ngân hàng/rút và check-in/out; thêm regression tests truy cập chéo.
2. Xác định booking thật, nối PayOS đúng purpose/amount/orderCode/webhook, bỏ URL simulated.
3. Khóa voucher/expiry/payment failure, settlement jobs, verified Review và nguồn tiền đối soát.
4. Sửa payout claim/reject/idempotency/bank binding và UI chọn cơ sở.
5. Sửa pagination/step/error state, hoàn thiện UI còn thiếu và Clean Architecture Mobile.
6. Chạy integration tests/database concurrency và E2E các ví dụ plan trước khi nghiệm thu.

## Kiểm tra đã chạy

- BE `mvnw.cmd test -q`:185 tests,0 failures,0 errors. Không chứng minh payment integration hoặc RBAC đúng vì chưa có test tương ứng.
- FE `npx tsc --noEmit`:pass. Lint5 file thay đổi:3 errors,9 warnings (setState-in-effect ở Admin withdrawals và merchant wallet).
- Mobile `flutter analyze --no-pub`:pass; `flutter test --no-pub`:304 tests pass. Chưa có test riêng wallet/voucher mới trong cây test được kiểm tra.
- Chưa test bằng tài khoản/ngân hàng thật, chưa render UI live; không thao tác dữ liệu server.
