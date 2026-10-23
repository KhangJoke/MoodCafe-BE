# Sửa bản Antigravity mới — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans hoặc quy trình tương đương của Antigravity. Làm theo dependency, theo dõi checkbox và kiểm chứng từng lỗi bằng đường chạy production.

**Goal:** Đóng các lỗi L1–L8 trong review mới nhất: Staff list405, action sai trạng thái, voucher bị hồi sai, payout thiếu bảo vệ, retry trùng, commission DTO sai, inventory voucher sai và stale responses.

**Architecture:** Contract BE/FE/MB thống nhất trước khi sửa màn hình. BE kiểm soát scope/quyền/state machine, transaction và unique constraints; client giữ intent bền vững và chỉ hiển thị hành động đúng trạng thái. Tái sử dụng module hiện có, không dựng booking hay ví thứ hai.

**Tech Stack:** Spring Boot/Java, PostgreSQL/Flyway, PayOS/VietQR; Next.js/TypeScript; Flutter/manual Riverpod, Clean Architecture.

**Spec:**
- `2026-10-22-antigravity-completion-review.md`: finding L1–L8 và bằng chứng mới nhất.
- `2026-10-22-wallet-voucher-commission-completion-plan.md`: phạm vi hoàn thiện task tổng thể vẫn còn hiệu lực.
- `2026-10-20-single-moodpoints-final.md`: nghiệp vụ một ví và policy P1–P10 chưa chốt.

Ngày lập:22/10/2026. Đây là plan sửa sau review, chưa thay đổi product code.

## Tóm tắt bàn giao

1. Bổ sung API Staff list có scope Store, DTO phục vụ và kiểm tra quyền; sửa FE dùng đúng contract, thay cho request đang405.
2. Sửa action: nhận bàn đúng giờ/giao nước trễ cho state hợp lệ; chỉ checkout CHECKED_IN, không checkout CLAIMED_LATE.
3. Voucher release/capture có khóa và kiểm tra binding; job không nhả khi payment còn PENDING/UNKNOWN.
4. Đối soát payout tôn trọng người claim, bằng chứng và audit; chưa rõ tiền đã chuyển hay chưa phải giữ locked.
5. Payout/redeem intent phục hồi qua reopen/reload/restart, cùng key không tạo lần thứ hai.
6. Commission Global/Store, ghi chú/ngày hiệu lực và My vouchers all-status dùng đúng DTO/API.
7. Fix stale responses và lint các file mới, chạy DB/contract/UI tests trước đóng lỗi.

Đóng L1–L8 không đồng nghĩa đã hoàn tất các phần Customer booking, QR/review, expiry, config và đối soát toàn hệ thống trong plan tổng thể; Task8 bên dưới yêu cầu cập nhật coverage thực tế.

## Global Constraints

- Customer giữ **một số dư MoodPoints,1điểm=1đ**, điểm đổi voucher trước checkout; không thêm ví VNĐ Customer hay trừ điểm trực tiếp lúc đặt bàn.
- Giữ các sửa đã đạt: prefix API không lặp, guard nguồn tiền thiếu, Owner/store boundary, constructor/wiring, snapshot payout, navigation và ảnh VietQR.
- Quán nhận gross−commission; Cashier phục vụ theo gross. BE tính tiền/quyền, không tin role/amount/funding từ client.
- Kiểm tra main ở ba repo, giữ thay đổi chưa commit. Không tự reset/stash/chuyển branch, commit/push/deploy hoặc chạy migration/giao dịch trên server thật.
- Migration mới sau version cao nhất; không sửa migration đã chạy. Không tắt validate hoặc skip test để báo pass.
- Network PayOS ngoài transaction giữ khóa. UNKNOWN không phải FAILED; không nhả voucher/tiền chỉ vì thiếu response hoặc quá TTL.
- Transaction bao trùm trạng thái/ledger/balance; ghi event idempotent bằng business key/constraint. Không hồi USED về AVAILABLE bằng release thường, không tự phát thêm available khi nguồn thiếu.
- Số tiền/điểm nguyên, policy và snapshot từ quyết định đã duyệt; không tự chốt15%,100k,24h hoặc thời hạn voucher. Không hỏi lại hướng một ví đã chốt.
- FE đọc tài liệu Next.js bundled trước sửa product code; MB production đi qua UseCase/Result/Failure/manual Riverpod theo AGENTS.md, không chỉ thêm class không có consumer.

## Review Focus

- API Staff được gọi đúng HTTP method/path và không lộ dữ liệu/tài chính giữa store/role — Task1.
- Job/callback/refund xen kẽ không làm voucher USED trở lại AVAILABLE — Task2.
- Admin B can thiệp claim A, hoặc thiếu proof ngân hàng: không unlock — Task3.
- Mất response rồi reopen/restart: một intent, một payout/voucher và một lần đổi số dư — Task4.
- Filter/store/page đổi giữa các request và inventory chỉ còn USED/LOCKED: không sai dữ liệu hoặc báo mất voucher — Task6–7.

## Đường dẫn và dependency

Prefix **BE** = `D:/Learn/MoodCafe-BE/moodcafe-be/`; **FE** = `D:/Learn/MoodCafe-FE/`; **MB** = `D:/Learn/MoodCafe-MB/`.

Tên file mới là đề xuất. Nếu có lớp tương đương thì sửa lớp đó, không tạo bản trùng. Thực hiện Task0 trước; Task1/2/3/5 có thể tiến triển độc lập, Task4 phụ thuộc contract payout/redeem và Task6–7 dùng API đã thống nhất. Task9 nghiệm thu sau cùng.

## Task 0 — Chốt contract, phạm vi và baseline

**Files:** Review/spec; BE controllers/DTO/security/migrations; FE services; MB data sources/routes.

- [ ] Kiểm tra main/diff, đọc hướng dẫn repo. Xác nhận bug hiện còn trên working tree, không lặp lại finding đã sửa.
- [ ] Ghi bảng API gồm path/method/request/response/error/role/state; ghi quyết định mới trong contract doc của repo và cập nhật mọi caller khi đổi.
- [ ] Giữ conventions: BE endpoint có `/api`, FE/MB service path không thêm `/api` vì base đã có. Không chữa Staff list bằng `/reservations/me`.
- [ ] Baseline: BE238 tests pass, FE tsc pass nhưng lint2 errors, MB analyze/316 tests pass; MockMvc Staff list405, service probe USED→AVAILABLE. Các test hiện có chưa bao phủ lỗi đã tái hiện.
- [ ] Xác định migration tiếp theo và PostgreSQL test biệt lập. Ghi policy còn thiếu từ plan tổng thể; tiếp tục sửa correctness không phụ thuộc policy.

## Task 1 — Staff list API và actions khớp state machine

**Finding:** L1,L5. **Priority:** P1 cho list. **Phụ thuộc:** Task0.

**Create BE đề xuất:** `src/main/java/com/moodcafe/reservation/controller/MerchantReservationController.java`, DTO `reservation/dto/ReservationStaffResponse.java` và service/query nếu chưa có.
**Modify BE:** `ReservationService.java`, `ReservationRepository.java`, staff access/booking action hiện có.
**Modify FE:** `src/services/reservation.service.ts`, `src/app/merchant/reservations/page.tsx` và OwnerSidebar nếu cần.
**Test:** BE `src/test/java/com/moodcafe/reservation/MerchantReservationControllerIT.java`; FE state/action contract tests.

**Interfaces được chọn:**
- GET `/api/merchant/stores/{storeId}/reservations?page=0&size=10&status=...&serviceDate=...`; FE gọi `/merchant/stores/${storeId}/reservations` theo base convention.
- DTO Staff gồm reservationId,storeId, khách hiển thị cần thiết,bookingTime,guestCount,category/table info,grossAmount,status,graceDeadline,serviceClosingDeadline và paging metadata. Không trả bank,merchant net,commission hoặc funding cho Cashier.
- List/action yêu cầu active OWNER/MANAGER/CASHIER đúng store. Customer/role sai/store khác403; action kiểm tra trên reservation thật, không chỉ storeId client gửi.
- Tái sử dụng check-in/out service hiện có; nếu thêm wrapper scoped action thì mọi wrapper vẫn đi cùng state/transaction, không có business logic thứ hai.

- [ ] Test HTTP request đúng URL FE sẽ gọi: role hợp lệ200/Page; Customer/store khác403; filter/page đúng; không còn405. Test DTO không chứa field tài chính Owner.
- [ ] Implement query theo store/status/ngày phục vụ và paging; validate params/bounds. Không dùng findAll rồi lọc ở client hoặc trả danh sách Customer `/me`.
- [ ] FE lấy selected store nhất quán với provider/router, reset dữ liệu khi đổi store và dùng request generation của Task7; không dùng tên store B cho dữ liệu A.
- [ ] Map actions: PAID_CONFIRMED → nhận bàn trong cửa sổ hợp lệ; PAID_CONFIRMED đã quá grace/TABLE_EXPIRED → giao nước trễ nếu trước closing và staff có quyền; CHECKED_IN → checkout staff; CLAIMED_LATE/COMPLETED/NO_SHOW/CANCELLED không có checkout thường.
- [ ] Không chỉ thêm action TABLE_EXPIRED mà bỏ deadline. BE kiểm tra thời gian; FE giải thích trạng thái và refresh sau action. Retry/reload không settle hoặc thưởng hai lần.
- [ ] Test tất cả state, quá giờ, action stale bị BE từ chối và reload; fixture gross100k vẫn hiện100k dù Customer paid90k/10k.

**Nghiệm thu:** Danh sách Staff gọi được API thật của controller; TABLE_EXPIRED giao nước được khi hợp lệ; CLAIMED_LATE không có nút chắc chắn400.

## Task 2 — Voucher release/capture/expiry có transition an toàn

**Finding:** L2. **Priority:** P1. **Phụ thuộc:** Task0.

**Modify BE:** `voucher/service/VoucherLifecycleService.java`, `voucher/abstraction/repository/CustomerVoucherRepository.java`, `reservation/scheduler/ReservationLifecycleScheduler.java`, `reservation/service/ReservationPaymentCaptureService.java`, `payment/service/PaymentAttemptService.java`, voucher/attempt version hoặc migration cần thiết.
**Test:** `src/test/java/com/moodcafe/voucher/VoucherTransitionTest.java`, `VoucherCaptureReleaseConcurrencyIT.java` và scheduler integration test.

**Interfaces:** Capture/release kiểm tra voucher ID + reservation/attempt binding + expected status dưới khóa/CAS. Release thường chỉ từ LOCKED và payment đã xác định không còn thu tiền; restore sau refund là hành động riêng có proof/điều kiện nghiệp vụ.

- [ ] Regression đơn giản trước: USED còn binding gọi release → phải giữ USED; stale release của đơn cũ không động lock đơn mới. Xác nhận test fail trên bản hiện tại trước sửa.
- [ ] Chuẩn hóa thứ tự khóa reservation/attempt/voucher, recheck trong transaction; không dựa vào đọc reservation ngoài khóa trước khi quyết định release. Capture thiếu voucher tài trợ phải flag invariant, không âm thầm xác nhận discount.
- [ ] Bỏ nhả “orphan” chỉ theo holdUntil. PENDING20phút/UNKNOWN không release; PENDING30phút cũng không expire nếu link còn trả được. Cancel/lookup gateway ngoài transaction rồi recheck trạng thái DB trước commit cuối.
- [ ] Webhook PAID đi chung capture với polling/job, đồng bộ attempt/booking/voucher/ledger. PAID không lùi UNKNOWN bởi response create chậm/lỗi.
- [ ] Late payment sau terminal state có record đối soát/compensation, không return/log rồi bỏ khoản thu. Nếu không rõ gateway, giữ lock và cho tra lại thay vì dùng timeout làm failure.
- [ ] Test PostgreSQL callback/job/cancel concurrent, hai worker, retry/restart và release/capture lặp. Assert một voucher không tài trợ hai đơn và không hồi USED bằng release.

## Task 3 — Payout reconciliation không bypass claim/proof

**Finding:** L3. **Priority:** P1. **Phụ thuộc:** Task0.

**Modify BE:** `wallet/service/WithdrawalService.java`, `wallet/controller/AdminWithdrawalController.java`, `wallet/entity/WithdrawalRequest.java`, DTO/audit và migration nếu cần.
**Modify FE:** `src/services/wallet.service.ts`, `src/app/admin/withdrawals/page.tsx`.
**Test:** `src/test/java/com/moodcafe/wallet/WithdrawalResolutionIT.java`, concurrency/security tests.

**Contract:** PROCESSING→RECONCILIATION_REQUIRED chỉ người claim hoặc handoff đã được ghi nhận theo policy; chưa có policy handoff thì không mở override ngầm. UNKNOWN giữ locked. Resolve PAID cần unique bank reference; resolve VERIFIED_NOT_TRANSFERRED cần verificationReference,evidence,notes và audit người/thời điểm kiểm tra.

- [ ] Test A claim, B mark/resolve →403 và không đổi tiền. Test thiếu/blank proof hoặc notes → validation error, locked giữ nguyên; tái hiện fail trước sửa.
- [ ] Enforce quyền và transition trong service cho mọi endpoint mark/resolve/confirm/reject. Không kiểm tra mỗi FE, không để REQUESTED/terminal tùy ý nhảy vào resolution.
- [ ] Bỏ câu xác nhận ngân hàng tự sinh khi thiếu dữ liệu. Manual verification do Admin ghi phải hiển thị/audit đúng tính chất, không mô tả là ngân hàng tự động xác nhận.
- [ ] Snapshot claim,decision/evidence/reference/resolver/time; ledger/balance/state cùng transaction. Same-intent retry trả kết quả cuối cũ, khác decision/reference409; không debit/unlock lần hai.
- [ ] FE hành động theo claim/state và lỗi403/409 có refresh đúng. Nếu confirm response mất, tra kết quả, không hướng Admin chuyển lại hoặc reject ngay.
- [ ] Test hai Admin, same reference hai payout, confirm/resolve cạnh tranh và lost response trên PostgreSQL; không chuyển tiền thật.

## Task 4 — Durable payout/redeem intent qua reopen/reload/restart

**Finding:** L4 và redeem payload còn thiếu. **Priority:** P1. **Phụ thuộc:** Task3 và BE idempotency contracts.

**Modify BE:** `voucher/service/VoucherRedemptionService.java`, `voucher/controller/CustomerVoucherController.java`, redemption/withdrawal repositories/controllers; thêm owned intent lookup DTO/endpoint nếu chưa có.
**Modify FE:** `src/app/merchant/wallet/page.tsx`, `src/services/wallet.service.ts` và hook/storage intent tách riêng.
**Modify MB:** `lib/features/vouchers/presentation/pages/voucher_catalog_page.dart`, redeem UseCase/repository/datasource và abstraction storage hiện có.
**Tests:** BE redeem/withdrawal idempotency/concurrency; FE/MB lost-response/restart tests.

**Interfaces:** Intent scope user+operation+store/template, key,payloadHash,submittedAt,state và result ID; persist trước gửi. Cùng key/payload trả cùng payout/voucher, khác payload409. Lookup chỉ actor/store có quyền; key không thay ownership.

- [ ] BE compare saved template/hash trước replay; thêm unique constraint actor/operation/key còn thiếu và xử lý concurrent loser sau rollback bằng read/replay transaction mới, không tiếp tục transaction đã lỗi.
- [ ] Giữ key qua modal reopen, reload, A→B→A và Mobile restart; không tạo Date.now key cho một lần thử lại của ý định cũ. Persist tối thiểu IDs/amount/key/state, không credentials/full bank data không cần thiết.
- [ ] Timeout →VERIFYING, đọc kết quả hoặc replay cùng key. Key mới chỉ cho ý định mới có chủ đích sau xử lý ý định cũ; đổi payload không âm thầm bỏ unresolved request.
- [ ] Logout/user switch không lộ/tiếp tục intent user khác; khi cùng user quay lại có thể lấy trạng thái authoritative từ BE. Retry sau input validation thất bại không được hiểu nhầm là đã tạo giao dịch.
- [ ] Redeem result trả voucherId/intent outcome thay vì void; refresh wallet/history/inventory sau xác nhận. Presentation gọi UseCase, repository map Result/Failure theo AGENTS.md.
- [ ] Test server commit rồi mất response → đóng/mở/reload/restart chỉ một payout hoặc voucher/debit. Assert số row và delta tiền/điểm, không chỉ nút disabled.

## Task 5 — Align commission contract, form và rule hiệu lực

**Finding:** L6 và commission fallback. **Priority:** P2. **Phụ thuộc:** Task0, policy P7/P10 cho giá trị sản phẩm.

**Modify FE:** `src/services/commission.service.ts`, `src/app/admin/commission-policies/page.tsx`.
**Modify BE:** `finance/entity/CommissionPolicy.java`, `finance/controller/AdminCommissionPolicyController.java`, repository, `finance/service/ReservationPricingService.java`; thêm service/audit/DTO nếu controller đang thao tác repository trực tiếp.
**Create:** Migration description/audit nếu contract cần; `src/test/java/com/moodcafe/finance/CommissionPolicyContractIT.java`.

**Contract lựa chọn:** Global là storeId=null, Store là storeId hợp lệ; bỏ field FE bắt buộc `policyType` chưa có. Persist description mà form đã cho nhập bằng request/response/entity và migration nullable, không giả UI đã lưu. effectiveFrom/effectiveTo cùng timezone chuẩn; rate truyền dạng0..1, UI nhập0..100%.

- [ ] Test response BE thật có global null/store ID; UI hiện đúng scope, không “Cơ sở (... )” rỗng. Test create/load lại giữ description, rate và dates.
- [ ] Form dùng chọn cơ sở từ API có quyền thay raw UUID, có tùy chọn toàn sàn và ngày hiệu lực; hiển thị đúng active vs currently effective. Audit thay đổi cần persist, không chỉ textarea không có field BE.
- [ ] BE validate store tồn tại, rate, interval và overlap/precedence theo policy. Khi không còn active default/store policy, quote báo cấu hình thiếu; không âm thầm lấy15% ví dụ.
- [ ] Snapshot policy ID/version/rate/rounding vào đơn cần thiết; policy mới không sửa giá booking/voucher cũ. Test global/store, interval boundaries, disable-all và fixture100k/15% chỉ để kiểm công thức.
- [ ] Sửa effect tải dữ liệu theo pattern hợp lệ, không disable eslint để che lỗi mới. UI loading/error/empty tách biệt và không báo lưu khi API thất bại.

## Task 6 — My vouchers dùng inventory all-status và paging

**Finding:** L7. **Priority:** P2. **Phụ thuộc:** Task4 cho result/refetch; inventory read có thể làm độc lập.

**Modify MB:** `lib/features/vouchers/presentation/pages/my_vouchers_page.dart`, providers, domain repository, datasource/repository, `domain/models/customer_voucher.dart` và data DTO theo kiến trúc.
**Create:** `lib/features/vouchers/domain/usecases/get_my_vouchers_usecase.dart`, controller/notifier paging và widget tests nếu chưa có tương đương.
**BE:** Tái sử dụng GET `/api/me/vouchers`, không thay đổi ý nghĩa `/usable` đang dành chọn voucher checkout.

**Interfaces:** `getMyVouchers(status,page,size)` trả page gồm content,totalPages/hasNext. Inventory mặc định all-status; filter AVAILABLE/LOCKED/USED/EXPIRED/REVOKED theo status API. Usable provider chỉ dùng cho chọn voucher áp booking.

- [ ] Fixture chỉ có LOCKED/USED/EXPIRED vẫn hiển thị trong all-status; không “Bạn chưa có voucher nào”. Filter không có kết quả dùng thông báo phù hợp filter.
- [ ] Dữ liệu đủ giá trị,code,status,expiry,điều kiện và binding booking nếu API/route có. Không chế link booking chưa có caller thật hoặc phơi thông tin đơn người khác.
- [ ] Pagination/load more/refetch không mất filter, không duplicate row; error tải thêm không xóa danh sách đã có. Empty/error/loading đều kéo refresh/retry được khi cần.
- [ ] Consumer gọi UseCase→repository Result→datasource; JSON ở data DTO, không thêm một UseCase rồi page vẫn gọi repository.
- [ ] Widget test0/21+voucher, từng state, hết trang, retry và redeemed voucher xuất hiện sau invalidate; analyze/test pass.

## Task 7 — Một cơ chế chống stale response cho mọi loader

**Finding:** L8; lint2 errors. **Phụ thuộc:** Task1/5/6 và contract đã ổn định.

**Modify FE:** `src/app/merchant/reservations/page.tsx`, `merchant/wallet/page.tsx`, `admin/withdrawals/page.tsx`, `admin/commission-policies/page.tsx`; hooks nếu tách.
**Tests:** Component/E2E deferred-response fixtures, dùng runner hiện có; nếu chưa có test browser thì thiết lập cùng task này.

- [ ] Request generation/abort phải bao phủ effect, refresh và action reload; response cũ không ghi rows/balance/bank/error/totalPages/loading của selection mới. So storeId đơn thuần không đủ.
- [ ] Test A request chậm →B→A request mới, A cũ về cuối không ghi đè. Admin đổi filter/page khi action hoặc manual refresh pending, chỉ selection mới được cập nhật.
- [ ] Reset state và store selection nhất quán, ngăn thao tác trên row stale; nếu user rời trang khi action đang chạy, xử lý kết quả ở intent/backend chứ không mutation UI sai context.
- [ ] Giữ fix clamp trang cuối; Owner histories/transactions và Mobile history còn page0 phải đưa vào coverage chưa xong hoặc hoàn thiện pagination trong task tương ứng của master plan, không báo task tổng thể đạt khi record21 không xem được.
- [ ] Sửa `react-hooks/set-state-in-effect` ở commission/reservations bằng lifecycle/fetch hợp lệ, bỏ unused CalendarDays; không tắt rule toàn repo hoặc thêm suppression để né lỗi.
- [ ] Chạy typecheck/lint file mới, test deferred responses và UI360/390/768/1280px. Lưu ảnh render thực tế; build pass không thay kiểm tra UI.

## Task 8 — Đóng các phần kỹ thuật còn mở và cập nhật coverage tổng thể

**Phụ thuộc:** Các task sửa liên quan. Task này không cho phép thay coverage bằng câu “đã có UI”.

**Files:** `payment/service/BookingPaymentCoordinator.java`, `PaymentAttemptService.java`, intent migrations; ledger/reconciliation/config/QR/booking modules được chỉ trong completion plan; docs coverage.

- [ ] Resume same actor/intent theo bước thực tế: URL null tiếp tục tra/tạo đúng attempt, zero-pay chưa capture tiếp tục capture idempotent. Không return record cũ như kết quả hoàn tất khi workflow còn dang dở.
- [ ] Unique payment intent actor/purpose/key và payload hash tại DB; state transition không PAID→UNKNOWN. Test hai retry/restart đồng thời; sequence bền vững không fallback bộ nhớ trùng.
- [ ] Ghi đủ delta source/destination và unique event ledger, persistent alert khi invariant hỏng; giữ guard nguồn thiếu đã sửa. Reconciliation còn sum-only phải ghi chưa xong cho tới khi có source comparison/mismatch report.
- [ ] Đối chiếu từng Task0–13 trong `2026-10-22-wallet-voucher-commission-completion-plan.md`: Customer booking/payment/cancel, QR/visit binding, lịch giờ hợp lệ, config voucher/loyalty/expiry, MB Owner wallet, bank CRUD, histories và đối soát. Hoàn thiện theo task tương ứng hoặc ghi dependency cụ thể; không tự bỏ khỏi phạm vi gốc.
- [ ] Các nhánh phụ thuộc P1–P10 chưa duyệt phải nêu chính sách nào thiếu và phần nào chưa nghiệm thu, không tự lấy constants để đánh dấu xong. Các sửa URL/quyền/locks/retry không được trì hoãn vì policy khác còn mở.

## Task 9 — Gate nghiệm thu và bàn giao

**Phụ thuộc:** Task0–8.

- [ ] BE test unit/contract trước, PostgreSQL migration+validate/context+HTTP security/concurrency sau. Configure Surefire/Failsafe để verify chạy đúng `*IT`, không đặt tên IT cho Mockito rồi coi đã kiểm tra DB.
- [ ] Chạy `./mvnw.cmd verify`; khi thiếu dependency/cache phải ghi rõ giới hạn hoặc sửa môi trường phù hợp, không báo verify pass khi chỉ test phase pass. Không sửa lan framework để né lỗi cache.
- [ ] FE `npx tsc --noEmit`, lint mọi file sửa và build theo scripts repo; MB regenerate DTO nếu đổi model, `flutter analyze`, `flutter test`. Báo full lint có lỗi cũ riêng, không đổ lỗi cũ cho module mới hay báo full pass sai.
- [ ] E2E test URL Staff list→actions state→ledger, commission create/global/store, My vouchers all-status, durable payout/redeem, voucher stale release và Admin cross-claim. Không dùng DB/gateway/tiền thật.
- [ ] Bàn giao bảng L1–L8→file/API/UI→test→kết quả, migration/config cần thêm, screenshot và coverage master còn thiếu. Không tự commit/push/deploy.

## Ma trận nghiệm thu tối thiểu

| Finding/case | Task | Bằng chứng đóng lỗi |
|---|---|---|
| L1 Staff GET405 | 1 | URL FE gọi trả200 đúng role/store,403 sai scope; DTO không lộ tài chính |
| L2 stale release USED | 2 | USED giữ USED cả tuần tự và callback/job concurrent |
| L2 PENDING20/30phút | 2 | Không release/expire khi link còn có thể trả; UNKNOWN giữ trạng thái xử lý |
| L3 B can thiệp A/proof thiếu | 3 |403/validation error, locked không đổi; valid resolution một lần |
| L4 response mất/restart | 4 | Cùng payout/voucherId, một delta tiền/điểm |
| L5 state/actions | 1 | TABLE_EXPIRED giao nước hợp lệ; CLAIMED_LATE không có checkout400 |
| L6 Global/Store/description | 5 | API thật render đúng scope, lưu/đọc roundtrip field form |
| L7 inventory all-status | 6 | LOCKED/USED/EXPIRED vẫn có, filter/page đúng |
| L8 response sai thứ tự | 7 | A→B→A/filter/page không bị response cũ ghi đè |
| Lint2 errors | 5,7 | Không còn lỗi/warning mới trong các file sửa |
| Resume/unique/ledger và master coverage | 8–9 | Có production flow + DB test và coverage rõ, không chỉ scaffold |

## Prompt gửi Antigravity

Thực hiện `2026-10-22-antigravity-completion-fix-plan.md` cùng review `2026-10-22-antigravity-completion-review.md`. Ưu tiên Staff list API/DTO và state actions, voucher transition, claim/proof payout và durable retry. Sửa commission contract và inventory all-status, stale responses/lint bằng test thực tế. Giữ một ví MoodPoints1điểm=1đ, prefix API và guard số dư đã sửa; không tự chốt policy còn thiếu. Mỗi finding phải có caller production và regression test đúng lỗi đã tái hiện; dùng PostgreSQL cho schema/locks/concurrency. Tiếp tục đối chiếu master completion plan, không đánh đồng đóng L1–L8 với full task hoàn thành. Giữ main/thay đổi hiện có, không tự commit/push/deploy hoặc thao tác DB/tiền thật. Bàn giao bảng finding→thay đổi→test→bằng chứng và phần còn thiếu.
