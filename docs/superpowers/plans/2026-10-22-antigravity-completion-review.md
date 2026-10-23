# Review bản Antigravity mới sau plan hoàn thiện task

Ngày: **22/10/2026**, Asia/Saigon. Đối chiếu working tree BE/FE/MB đang ở `main` với `2026-10-22-wallet-voucher-commission-completion-plan.md`. Source sản phẩm chưa commit; lượt này không sửa code, không gọi DB/giao dịch thật.

**Kết luận: chưa nghiệm thu.** Đã sửa prefix API và guard số dư, thêm một số màn hình/navigation/QR, nhưng màn Staff mới gọi endpoint chưa tồn tại; các action/status và DTO hoa hồng chưa khớp BE. Rủi ro voucher và payout đã nêu trong review trước vẫn tồn tại.

Prefix file: BE = `D:/Learn/MoodCafe-BE/moodcafe-be/`; FE = `D:/Learn/MoodCafe-FE/`; MB = `D:/Learn/MoodCafe-MB/`.

## P1 — cần ưu tiên

### L1. Màn đặt bàn Staff mới không tải được danh sách

**FE:** `src/services/reservation.service.ts:18–22`; caller `src/app/merchant/reservations/page.tsx:42`.
**BE:** `src/main/java/com/moodcafe/reservation/controller/ReservationController.java:47`, `:108`.

FE gọi `GET /api/reservations?storeId=...&page=...`. BE chỉ có POST collection và GET `/api/reservations/me`, không có GET collection/store list. Đã gọi controller hiện tại qua MockMvc với phụ thuộc giả: **HTTP405, GET is not supported**. Vì vậy màn mới chưa xem hoặc phục vụ được đơn dù menu/markup đã có.

**Cần sửa:** thống nhất endpoint staff list với kiểm tra membership/role theo store, paging và DTO gross phục vụ; viết contract test FE→controller. Không đổi sang `/me` vì đó là danh sách đơn của Customer, không phải đơn của quán.

### L2. Release vẫn có thể hồi voucher USED về AVAILABLE

**BE:** `src/main/java/com/moodcafe/voucher/service/VoucherLifecycleService.java:60–90`; `reservation/scheduler/ReservationLifecycleScheduler.java:148–160`.

Capture/release vẫn lookup không khóa và không yêu cầu current status LOCKED. Voucher USED giữ reservation binding nên một release stale vẫn tìm được và ghi AVAILABLE. Đã chạy service hiện tại với voucher USED, còn hạn và binding cũ: kết quả **AVAILABLE**. Đây là tái hiện service-level với repository giả, không phải test concurrency PostgreSQL.

Job cũng vẫn nhả attempt PENDING ở vòng orphan sau hold15phút, hoặc expire sau30phút khi gateway còn PENDING mà chưa cancel. Có thể thanh toán link cũ khi voucher đã được dùng lại.

**Cần sửa:** một pipeline transition có khóa/CAS và binding, xác minh trạng thái gateway cuối trước release; không USED→AVAILABLE qua release thường. Regression stale release tuần tự và callback/job concurrent trên DB.

### L3. Payout reconciliation còn bypass người claim và bằng chứng chưa chuyển

**BE:** `src/main/java/com/moodcafe/wallet/service/WithdrawalService.java:241–252`, `:261–319`.

Mark/resolve đối soát không kiểm tra claimedBy như confirmPaid. Admin B có thể đưa lệnh A đang chuyển vào đối soát, rồi resolve REJECTED và unlock. Nhánh REJECTED chấp nhận notes/proof trống, tự điền câu đã xác nhận tiền chưa rời tài khoản. Nếu tiền đã chuyển hoặc đang chưa rõ, available vẫn có thể được hồi sai.

**Cần sửa:** enforce claim/handoff đã chốt ở service, transition hợp lệ và bằng chứng/audit của kết quả chưa chuyển; UNKNOWN giữ locked. Thêm test hai Admin và missing proof, không chỉ test happy path.

### L4. Client vẫn mất khóa chống trùng sau reopen/restart

**FE:** `src/app/merchant/wallet/page.tsx:224`, `:308`.
**MB:** `lib/features/vouchers/presentation/pages/voucher_catalog_page.dart:48–55`.

FE giữ key trong state cho retry ở cùng modal, nhưng mở lại/reload vẫn tạo key khác. Mobile mỗi lần xác nhận redeem lại tạo timestamp key mới. Server đã commit nhưng response mất → thử lại có thể thêm payout/khóa tiền hoặc thêm voucher/trừ điểm. Chưa có durable intent và truy vấn authoritative result sau restart.

**Cần sửa:** key gắn với ý định/payload, lưu/khôi phục theo user/store/operation; trạng thái chưa rõ phải xác minh hoặc replay cùng key. Không dùng double-click disable làm bảo đảm idempotency.

## P2 — lỗi UI/logic mới

### L5. Không giao nước trễ được cho TABLE_EXPIRED; lại cho checkout CLAIMED_LATE

**FE:** `src/app/merchant/reservations/page.tsx:184–185`, `:216–234`.
**BE:** `src/main/java/com/moodcafe/reservation/service/ReservationService.java:221`, `:287`.

UI chỉ hiện Nhận bàn cho PAID_CONFIRMED. Khi job đổi TABLE_EXPIRED, khách vẫn được lấy nước trước closing nhưng hàng không còn action để staff nhận/giao nước trễ. Ngược lại, UI cho checkout cả CLAIMED_LATE trong khi BE checkout chỉ nhận CHECKED_IN; bấm nút đó chắc chắn bị BAD_REQUEST/400. CLAIMED_LATE đã được settle ở nhánh nhận nước trễ.

**Cần sửa:** map action theo state machine, có action giao nước trễ đúng quyền/deadline; không gộp CLAIMED_LATE vào CHECKED_IN. Test từng trạng thái bằng fixture/contract thật.

### L6. Chính sách toàn sàn bị hiển thị thành chính sách cơ sở

**FE:** `src/app/admin/commission-policies/page.tsx:161–174`; `src/services/commission.service.ts:7`.
**BE:** `src/main/java/com/moodcafe/finance/entity/CommissionPolicy.java:41–47`.

FE dựa vào `policyType === GLOBAL`, nhưng BE không trả policyType; global được biểu diễn bằng storeId=null. Mọi global row từ API hiện tại đi vào nhánh Cơ sở và hiện ID rỗng. Đây là lỗi dễ khiến Admin hiểu sai phạm vi áp dụng hoa hồng.

Ngoài ra form gửi/hiển thị description nhưng request DTO/entity BE không có trường này; chưa có contract để lưu ghi chú. Form cũng chưa có effectiveFrom/effectiveTo và nhập raw Store UUID thay vì chọn cơ sở theo plan. Không coi typecheck pass là DTO runtime đã khớp.

**Cần sửa:** derive scope từ storeId hoặc thêm DTO scope do BE trả thống nhất; align description/effective period/store selector/audit với API thật.

### L7. Voucher của tôi chỉ lấy voucher khả dụng

**MB:** `lib/features/vouchers/presentation/pages/my_vouchers_page.dart:11`; `vouchers/data/datasources/voucher_remote_data_source.dart:30`.

Page mới watch usableVouchersProvider, gọi `/me/vouchers/usable` chỉ trả AVAILABLE/chưa hết hạn. LOCKED/USED/EXPIRED sẽ biến mất, dù page có nhánh vẽ status tương ứng. Nếu không còn AVAILABLE, UI báo “Bạn chưa có voucher nào” dù tài khoản có voucher đang giữ chỗ hoặc đã dùng. Endpoint GET `/me/vouchers` phân trang/all-status đã có nhưng chưa được dùng.

**Cần sửa:** all-vouchers provider/usecase/data source có filter/page, render đầy đủ trạng thái và booking liên quan theo quyền; không dùng inventory bằng endpoint chọn voucher checkout.

### L8. Response cũ vẫn có thể ghi đè cơ sở/filter mới

**FE:** `src/app/merchant/reservations/page.tsx:42–46`; `merchant/wallet/page.tsx:96`, `:121`, `:140`; `admin/withdrawals/page.tsx:39–54`.

Màn Staff set rows/page vô điều kiện sau fetch; chuyển A→B trong khi A đang tải có thể hiển thị A dưới tên B. Wallet guard chỉ so storeId nên A→B→A chấp nhận response A cũ. Admin manual/action reload không dùng guard của effect, có thể ghi đè filter/page mới. Đây là lỗi state nguồn dữ liệu, không chỉ spinner.

**Cần sửa:** generation/abort cho mọi đường tải và action reload, không chỉ useEffect; test response sai thứ tự, đổi store/filter và paging.

## Lỗi kỹ thuật/logic còn tồn tại từ plan

- **Resume:** `BookingPaymentCoordinator.java:43–58` vẫn trả record khi attempt có sẵn, không tiếp tục bước tạo/khôi phục link hoặc zero-pay chưa capture. V40 chỉ index actor/purpose/key thường, không unique, nên chưa đóng concurrent duplicate.
- **Redeem payload:** `VoucherRedemptionService.java:60–66` vẫn trả voucher cũ mà không so template/hash; cùng key khác template không409. Concurrent loser chưa rollback/replay đúng kết quả.
- **Commission fallback:** `ReservationPricingService.java:99–112` vẫn lấy15% khi không có active policy. Admin disable hết vẫn có hoa hồng15% ngầm; API validate rate nhưng thiếu dates/overlap. Policy ID/version/rounding audit chưa đủ.
- **QR/deadline/review:** vẫn daily token/default secret, chưa action/booking nonce consume; deadline chưa chặn ngoài ca/thiếu lịch; verified reward chưa binding visit đúng booking. Không có thay đổi đủ đóng các task này.
- **Ledger:** guard nguồn thiếu đã sửa, nhưng chuyển ngăn vẫn ghi transaction destination đơn lẻ, thiếu delta source và unique business event/reference để tái dựng/chống lặp đầy đủ. Persistent reconciliation alert chưa có, hiện chỉ log.

## Những phần đã sửa hoặc thêm được

1. **F1 prefix API:** wallet/voucher service FE và datasource MB đã bỏ `/api` lặp, khớp convention baseURL hiện có.
2. **F6 thiếu source money:** settle/revert/complete/unlock trong MerchantWalletService hiện throw trước khi sửa số dư, không còn clamp về0 rồi cộng đủ tiền. Không lặp lại finding clamp cũ.
3. AdminSidebar có link withdrawals và commission policies; OwnerSidebar có link reservations.
4. Payout confirmation đã thêm ảnh VietQR từ snapshot account/amount và nội dung; đã có markup gọi ảnh, chưa kiểm thử render/độ hợp lệ bankCode trên live UI.
5. Có commission FE page/service gọi endpoint đúng base path và MyVouchers route/page/navigation. Các lỗi contract/data của những màn này nằm ở L1/L5–L7.
6. Các sửa trước về constructor/dependency vòng, entity redemption mapping, owner/store boundary và confirm retry cùng reference vẫn được giữ.

## Coverage so với plan tổng hợp

| Task trong completion plan | Đánh giá hiện tại |
|---|---|
| 0 Mapping booking/policy | Có Staff page mới nhưng canonical Customer booking vẫn chưa được chứng minh; policy chưa đủ nguồn duyệt |
| 1 URL contract | Prefix đã sửa; chưa thấy test URL contract/HTTP smoke đi kèm |
| 2 DB/idempotency/invariants | Guard số dư đã sửa; thiếu unique event/intent, ledger delta, PostgreSQL migration/concurrency gate |
| 3 Payment/voucher/refund recovery | Còn L2 và resume; chưa nghiệm thu |
| 4 Deadline/QR/rewards | Chưa đóng các điều kiện chống gian lận/binding/thời gian |
| 5 Owner/bank/payout | Thêm navigation/ảnh QR; claim/proof/durable retry, bank CRUD/default/verification và MB Owner wallet chưa hoàn tất |
| 6 Customer wallet/My vouchers | Prefix sửa và page mới có; inventory sai endpoint, durable redeem và Clean Architecture còn thiếu |
| 7 Booking Customer | Chưa có UI checkout/payment/cancel/refund dùng flow này; Staff page không thay thế task này |
| 8 Staff/Cashier | Có UI nhưng list405 và action/state không khớp BE, chưa sử dụng được trọn luồng |
| 9 Commission UI/config | Có page/service nhưng scope DTO sai, thiếu dates/store selection/audit và fallback còn ngầm |
| 10 Admin voucher/loyalty/expiry | Chưa có đầy đủ pages/controllers/runtime policies và expiry workers yêu cầu |
| 11 Reconciliation | ReconciliationService vẫn chỉ sum balances, chưa so gateway/refund/funding/payout và mismatch report |
| 12 Paging/responsive/state | Owner/MB histories vẫn trang0; stale responses còn; chưa kiểm tra ảnh render trực tiếp |
| 13 Verification/handoff | Test có chạy nhưng chủ yếu mock; chưa PostgreSQL/concurrency/contract/UI E2E gate |

Mobile vẫn có UseCase/provider nhưng production wallet/voucher providers và redeem page gọi repository trực tiếp. Result/Failure, DTO/freezed và paging metadata chưa hoàn tất theo AGENTS.md. Các test delegate không chứng minh UI đang dùng UseCase.

## Kiểm thử đã thực hiện

- BE `mvnw.cmd -o -Dtest=*Test,*IT test -q`: **238 tests, 0 failures/errors**. Đã đọc surefire XML xác nhận; không gọi đây là PostgreSQL integration pass vì các test tài chính vẫn mock repositories.
- Probe MockMvc ngoài repo: GET staff-list URL hiện tại → **405**. Đây là kiểm chứng route controller, không chạy server/DB thật.
- Probe VoucherLifecycleService ngoài repo với repository giả: USED + reservation binding cũ + release → **AVAILABLE**. Không sửa test/source sản phẩm để tạo bằng chứng.
- FE `npx tsc --noEmit`: pass.
- Lint các file thay đổi: **fail, 2 errors** `react-hooks/set-state-in-effect` tại `admin/commission-policies/page.tsx:47` và `merchant/reservations/page.tsx:56`; warning unused CalendarDays trong reservations. Không có bằng chứng full FE build trong lượt này.
- MB `flutter analyze`: pass, no issues; `flutter test`: **316 tests pass**. Chưa có widget/E2E cho inventory/status, durable retry hoặc booking.
- Chưa render browser/device, chưa test gateway/ngân hàng thật, không chạy migration hay sửa DB server. Report đánh giá source/contract và các probe nói trên, không khẳng định mọi race đã được tái hiện trên production.

## Thứ tự sửa đề nghị

1. Thống nhất Staff API/DTO/action state và commission DTO; sửa lint file mới, thêm contract tests để không tiếp tục thêm màn gọi API chưa tồn tại.
2. Đóng L2–L4 bằng transition/locks/claim/proof/durable intents và kiểm thử DB thật.
3. Dùng API inventory đúng cho My vouchers, nối actual Customer booking, hoàn thiện config/expiry/reconciliation còn thiếu.
4. Kiểm thử E2E/UI theo completion plan trước khi tuyên bố full task hoàn tất.
