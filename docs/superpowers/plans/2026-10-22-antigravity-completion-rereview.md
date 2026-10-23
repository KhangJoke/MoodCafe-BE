# Review lại bản sửa Antigravity — 22/10/2026

## Kết luận

**Chưa đủ điều kiện nghiệm thu.** Bản sửa đã đóng một số lỗi hiển thị/contract và thêm guard, nhưng các luồng thanh toán, voucher, payout và retry chưa đáp ứng plan. Có thêm lỗ hổng DTO Staff trả thông tin tài chính Owner và trường hợp generation counter vẫn nhận dữ liệu sai cơ sở.

Đối chiếu `2026-10-22-antigravity-completion-fix-plan.md`, completion plan tổng thể và quy tắc một ví MoodPoints, 1 điểm = 1đ. Review working tree hiện tại của BE/FE/MB, cả ba ở `main`, gồm cả file chưa được Git track. Không sửa product code, không thao tác DB/gateway/tiền thật.

Prefix đường dẫn trong báo cáo:
- **BE:** `D:/Learn/MoodCafe-BE/moodcafe-be/`
- **FE:** `D:/Learn/MoodCafe-FE/`
- **MB:** `D:/Learn/MoodCafe-MB/`

## Findings cần sửa

### R1 — [P1] Job vẫn hết hạn đơn và nhả voucher khi PayOS còn PENDING

**File:** BE `src/main/java/com/moodcafe/reservation/scheduler/ReservationLifecycleScheduler.java:109–120`; capture `reservation/service/ReservationPaymentCaptureService.java:45–47`.

Đơn cũ 31 phút, gateway trả PENDING: chỉ nhánh dưới 30 phút được `continue`. Sau đó job gọi `handlePaymentExpiredOrFailed`, nhả voucher và ghi attempt EXPIRED mà chưa hủy link PayOS. Khách có thể dùng lại voucher trong khi link cũ còn thu tiền. Nếu link cũ được thanh toán, capture gặp PAYMENT_EXPIRED chỉ log rồi return, không có bản ghi xử lý khoản tiền đến muộn.

**Đã tái hiện:** probe gọi scheduler production bằng dependency mocks xác nhận gateway PENDING tại phút31 vẫn gọi expire và `markStatus(EXPIRED)`.

**Cần đạt:** giữ lock cho đến kết quả gateway terminal/cancel authoritative; lookup/cancel ngoài transaction rồi recheck dưới khóa; khoản thu đến muộn có bản ghi đối soát/compensation bền vững. Không dùng TTL thay bằng chứng giao dịch thất bại.

### R2 — [P1] Đối soát payout vẫn mở khóa tiền chỉ bằng ghi chú

**File:** BE `src/main/java/com/moodcafe/wallet/service/WithdrawalService.java:319–330`; payload `wallet/controller/AdminWithdrawalController.java:179–182`.

Admin đang claim gửi `action=REJECTED, notes="x"` là đủ gọi unlock. Không có verificationReference/evidence như plan yêu cầu, nên có thể trả available cho khoản đã chuyển nhưng chưa xác định kết quả. Bản sửa chỉ bỏ ghi chú tự sinh và bắt buộc notes, chưa hoàn thành proof contract.

Ngoài ra `markForReconciliation` tại `WithdrawalService.java:245–260` cho REQUESTED chưa claim đi thẳng vào RECONCILIATION_REQUIRED. Claimant null không bị guard chặn; Admin khác có thể resolve. Guard mới chỉ bảo vệ khi `claimedBy` đã tồn tại.

**Cần đạt:** transition hợp lệ từ PROCESSING với claimant; quyết định VERIFIED_NOT_TRANSFERRED có reference/evidence/notes và audit; UNKNOWN giữ locked. Test REQUESTED/unclaimed và hai Admin, không chỉ trường hợp claimedBy khác nhau.

### R3 — [P1] FE payout và Mobile redeem vẫn mất intent khi thử lại

**Files:** FE `src/app/merchant/wallet/page.tsx:57,80,308`; MB `lib/features/vouchers/presentation/pages/voucher_catalog_page.dart:48–55`, datasource `lib/features/vouchers/data/datasources/voucher_remote_data_source.dart:60–67`.

FE giữ key trong React state và sinh key mới mỗi lần mở modal. Mobile sinh key theo timestamp mỗi lần xác nhận. Nếu server commit nhưng response mất, đóng/mở lại, reload hoặc restart rồi thử lại sẽ gửi key mới: có thể tạo thêm payout/khóa thêm tiền hoặc đổi thêm voucher/trừ thêm điểm nếu số dư còn đủ. BE chống trùng theo key không bảo vệ được hai key khác nhau của cùng thao tác chưa rõ kết quả.

Mobile còn gọi repository trực tiếp, bypass RedeemVoucherUseCase; datasource trả void và bỏ voucherId thực tế từ server.

**Cần đạt:** persist intent trước request, khôi phục và tra/replay cùng key; chỉ tạo ý định mới khi người dùng chủ động làm thao tác mới sau khi ý định cũ được xử lý. Có test mất response + reload/restart, kiểm tra số row và delta số dư.

### R4 — [P1] Booking retry trả workflow dang dở mà không tiếp tục

**File:** BE `src/main/java/com/moodcafe/payment/service/BookingPaymentCoordinator.java:54–58`.

Crash sau transaction prepare commit, trước tạo/lưu link PayOS hoặc trước capture đơn zero-pay: cùng intent tìm thấy reservation thì trả ngay. Đơn vẫn PENDING_PAYMENT, paymentUrl có thể null; retry không tạo/khôi phục link, cũng không capture lại zero-pay. Có record durable nhưng chưa có resume workflow.

**Cần đạt:** đọc giai đoạn thật của attempt, resume cùng orderCode/businessId; zero-pay capture idempotent. Test crash tại từng ranh giới commit/network/capture.

### R5 — [P1] Database chưa bảo đảm unique booking intent

**File:** BE `src/main/resources/db/migration/V40__payment_attempt_actor_and_hash.sql:7–9`; prepare `src/main/java/com/moodcafe/reservation/service/ReservationService.java:176–193`.

Index `(actor_id,purpose,intent_key)` không UNIQUE. Hai request cùng actor/key chạy đồng thời có thể cùng không thấy record, rồi tạo hai reservation/attempt/link khác nhau. Lookup đầu coordinator không thay thế DB constraint; orderCode unique không chống hai orderCode khác nhau cho cùng intent.

**Cần đạt:** migration mới thêm scoped unique constraint theo contract, xử lý transaction thua bằng rollback rồi read/replay transaction khác; PostgreSQL test hai request cùng key. Không sửa migration đã chạy.

### R6 — [P1] Redeem cùng key nhưng khác template vẫn báo thành công

**File:** BE `src/main/java/com/moodcafe/voucher/service/VoucherRedemptionService.java:61–66`.

Đổi template A với K, sau đó gửi template B cùng K: service trả voucher A, không so template/payloadHash đã lưu. UI có thể báo đổi B thành công trong khi nhận A. Có cột hash nhưng chưa dùng để kiểm tra replay.

**Cần đạt:** same key/same payload trả cùng voucher; same key/different template/hash trả409. Test cả tuần tự và đồng thời, không chỉ đếm số voucher cùng key.

### R7 — [P1] Attempt PAID có thể bị ghi lùi UNKNOWN

**File:** BE `src/main/java/com/moodcafe/payment/service/PaymentAttemptService.java:94–98`; caller `BookingPaymentCoordinator.java:114–120`.

`markStatus` ghi đè bất kỳ status. Polling/job đã capture và ghi PAID, sau đó lỗi trả về chậm của create link chạy catch ghi UNKNOWN. Nếu giao dịch này đọc record sau PAID, optimistic version không ngăn việc ghi lùi. Các worker dựa trên attempt sẽ hiểu sai kết quả tiền đã thu.

**Cần đạt:** transition đơn điệu bằng lock/CAS, PAID không xuống UNKNOWN/PENDING/EXPIRED; kiểm tra cả callback/job/create response đến sai thứ tự.

### R8 — [P1] API thao tác Staff trả commission và merchant net cho Cashier

**File:** BE `src/main/java/com/moodcafe/reservation/controller/MerchantReservationController.java:74–92,98–116`; mapper `reservation/service/ReservationService.java:354–358`.

GET list dùng ReservationStaffResponse đã giới hạn field đúng, nhưng POST check-in/out cho phép CASHIER và trả ReservationResponse đầy đủ. Mapper điền cả commissionAmount, merchantNetAmount, customerPaidAmount và voucherDiscount. Cashier gọi thao tác hợp lệ là đọc được dữ liệu mà contract chỉ dành cho Owner.

**Đã tái hiện:** probe controller với service response thực hiện contract hiện tại trả `commission=15000, merchantNet=85000`. Đây là kiểm tra payload controller; không thay thế full HTTP security test. Production mapper cũng điền các field đó.

**Cần đạt:** Staff action trả Staff DTO hoặc response tối thiểu, áp dụng giới hạn cho các endpoint khác mà Cashier còn có thể gọi. Test JSON response mọi role, không chỉ DTO list.

### R9 — [P1] Action hoàn tất muộn vẫn ghi danh sách cơ sở cũ lên cơ sở mới

**File:** FE `src/app/merchant/reservations/page.tsx:44,54,92–110`.

Check-in/out tại A đang chờ → chuyển B và B load xong → action A trả về → handler cũ gọi fetch closure của A. Closure này tăng generation sau request B, nên response A trở thành generation mới nhất và vượt guard. Dữ liệu A hiển thị trong context B; filter/page cũ cũng có thể ghi đè tương tự.

Ví Quán tại `src/app/merchant/wallet/page.tsx:96,121,140` chỉ so storeId, vẫn lỗi A→B→A. Admin withdrawals tại `src/app/admin/withdrawals/page.tsx:39` có manual/action loader không guard, dù effect có ignore riêng.

**Cần đạt:** selection/context epoch áp dụng effect/manual/action reload, không chỉ thứ tự request. Action cũ không được khởi tạo fetch ghi vào context mới. Test deferred responses cho cả ba đường gọi.

### R10 — [P2] My Vouchers chỉ hiển thị 20 voucher đầu

**File:** MB `lib/features/vouchers/presentation/providers/voucher_providers.dart:53–55`; datasource `lib/features/vouchers/data/datasources/voucher_remote_data_source.dart:39–55`.

Đã dùng đúng inventory all-status, nhưng provider luôn lấy page0/size20; datasource bỏ totalPages/hasNext, UI không tải thêm. Khách có21+ voucher không truy cập được phần còn lại. Bộ lọc cũng chưa có REVOKED như contract đề xuất.

**Cần đạt:** page model/controller/usecase giữ metadata, load more và refresh theo filter; test record21, lỗi trang sau, hết trang. Không trả List rồi mất pagination.

### R11 — [P2] Tắt hết commission vẫn âm thầm áp 15%; UI chưa phản ánh hiệu lực

**File:** BE `src/main/java/com/moodcafe/finance/service/ReservationPricingService.java:99–112`; FE `src/app/admin/commission-policies/page.tsx:265`.

Không có policy store/global hiệu lực vẫn trả DEFAULT_COMMISSION_RATE15%, trái yêu cầu báo thiếu cấu hình trong plan. Admin tưởng tắt policy đã dừng áp dụng nhưng booking mới vẫn tính15%. UI chỉ dựa isActive để ghi “Đang áp dụng”, kể cả policy tương lai/đã hết thời gian hiệu lực.

Form tại FE `page.tsx:89–92` chỉ gửi scope/rate; phần ghi chú/ngày hiệu lực đã bị bỏ thay vì hoàn thiện contract, và `page.tsx:330–336` vẫn bắt nhập UUID quán. BE chưa thêm description/audit và interval/overlap validation theo Task5.

**Cần đạt:** missing config là lỗi rõ ràng; tách enabled/effective, form có ngày/ghi chú/store selector và BE persist/validate đúng contract. Snapshot policy ID/version/rounding còn phải đối chiếu master plan.

### R12 — [P2] Voucher capture thiếu binding vẫn cho xác nhận đơn giảm giá

**File:** BE `src/main/java/com/moodcafe/voucher/service/VoucherLifecycleService.java:61–70`; caller `reservation/service/ReservationPaymentCaptureService.java:83–95`.

Không tìm thấy voucher theo binding hoặc voucher không LOCKED thì capture chỉ return. Caller vẫn giữ đủ merchant net và commit PAID_CONFIRMED. Khi invariant binding bị hỏng, đơn được xác nhận giảm giá mà không chứng minh đã tiêu voucher đúng. Không có persistent alert/đối soát cho trường hợp này.

**Cần đạt:** capture nhận expected voucherId/binding từ reservation, xác nhận đúng trạng thái tài trợ trước commit hoặc đi luồng đối soát rõ; silent return chỉ dành replay hợp lệ đã chứng minh cùng booking.

## UI/luồng còn thiếu cần nghiệm thu

- FE `merchant/reservations/page.tsx:286–288` xét status mà chưa xét bookingTime/graceDeadline/closingDeadline: hiện nút nhận bàn quá sớm, nhãn nhận bàn cho đơn quá grace, hoặc nút lấy nước sau đóng cửa. BE chặn thời gian nên chưa phải bypass nghiệp vụ, nhưng UI hướng người dùng vào thao tác chắc chắn lỗi.
- Staff list chưa có serviceDate filter như contract plan; DTO chỉ có userId/categoryId/tableId, chưa có thông tin khách/bàn dễ nhận diện. Cần đối chiếu màn hình phục vụ thực tế.
- Mobile redeem mới invalidate usableVouchersProvider, chưa invalidate inventory myVouchersProvider. Cần test quay lại tồn kho sau redeem/filter trước khi coi refresh hoàn chỉnh.
- Các feature tổng thể Task8 vẫn cần coverage riêng: Customer booking/payment/cancel, QR/visit binding, policy/expiry, MB Owner wallet, bank CRUD, histories và reconciliation. Không thể coi các phần này hoàn thành chỉ vì L1–L8 đã có thay đổi code.
- Chưa chạy render/browser responsive và chưa có ảnh thực tế360/390/768/1280px trong lượt review này. Các kết luận UI ở trên dựa trên code và contract; chưa kết luận chất lượng bố cục thị giác.

## Những sửa đã đạt so với review trước

| Hạng mục | Kết quả hiện tại |
|---|---|
| L1 Staff list405 | Đã thêm GET `/api/merchant/stores/{storeId}/reservations`; FE gọi cùng path, list dùng Staff DTO. Còn thiếu full HTTP/security/DB verification và hạn chế DTO action ở R8. |
| L2 USED bị release | Đã thêm voucher lock và expected LOCKED/USED guard, không còn release thường USED→AVAILABLE. Orphan loop bảo vệ PENDING20phút. R1/R12 vẫn mở. |
| L3 Cross-claim | Đã chặn Admin khác khi claimedBy tồn tại, bắt nonblank notes; chưa đủ proof và unclaimed transition ở R2. |
| L4 Durable intents | Chưa đạt, R3. |
| L5 Action state | Đã thêm TABLE_EXPIRED lấy nước, bỏ checkout CLAIMED_LATE; chưa dùng service windows. |
| L6 Global label | Đã derive scope theo storeId, label Global đúng. Form/contract/rule chưa hoàn chỉnh, R11. |
| L7 Voucher source | Đã đổi sang all-status inventory và dùng GetMyVouchersUseCase. Pagination còn R10; redeem/catalog/usable vẫn có đường gọi thẳng repository. |
| L8 Stale data | Đã thêm counter cho Staff list; action closure và loader khác vẫn lỗi, R9. |
| Hai lỗi lint FE | Đã hết trong nhóm file đã kiểm tra; không cần lặp finding lint cũ. |

Các guard nguồn tiền thiếu, prefix API và navigation từ lượt trước vẫn hiện diện; không đưa lại các finding clamp/api-prefix đã đóng.

## Kiểm chứng đã thực hiện

- **BE:** `./mvnw.cmd -o -Dtest=*Test,*IT test -q` exit0; XML Surefire ghi245 tests,0failures,0errors,1skipped. Gọi rõ `*IT` vì pom chưa cấu hình Failsafe. Đây là test phase, không phải tuyên bố `mvn verify`/PostgreSQL integration đã pass.
- **FE:** `npx tsc --noEmit` pass. Scoped ESLint pass cho bốn page commission/withdrawals/reservations/wallet và bốn service commission/reservation/wallet/voucher. Chưa chạy full build/full lint.
- **MB:** `flutter analyze` không có issue; `flutter test --no-pub`316 tests pass.
- **Probe local:** scheduler PENDING31phút vẫn expire; controller Staff action vẫn trả commission/net. Probe nằm trong thư mục tạm Windows, không thêm test/product file vào repo.
- Các test MerchantReservationControllerIT/WithdrawalResolutionIT/VoucherRedemptionIdempotencyIT hiện dùng Mockito; tên IT chưa chứng minh chạy PostgreSQL, transaction locks, constraint race hoặc full security filter.
- Không chạy gateway thật, migration server hay tiền thật. Những finding concurrency còn lại được xác định từ production flow/schema, chưa tái hiện bằng PostgreSQL cạnh tranh.

## Thứ tự ưu tiên

1. R1/R2/R3/R8: giữ an toàn voucher/tiền, retry và giới hạn dữ liệu theo role.
2. R4–R7/R9: resume/idempotency/transition và context UI.
3. R10–R12 cùng UI/service windows/commission contract.
4. Test DB/security/concurrency và browser responsive, cập nhật coverage master plan trước nghiệm thu.
