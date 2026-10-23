# Ví / MoodPoints / Voucher — Implementation Plan sửa vòng 3

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans hoặc quy trình tương đương của Antigravity. Làm theo dependency, theo dõi checkbox và cung cấp bằng chứng nghiệm thu cho từng task.

**Goal:** Sửa các finding N1–N14 của review vòng 3, khôi phục khả năng khởi động BE và bảo đảm retry/đối soát không làm lộ dữ liệu, mất voucher hoặc ghi tiền hai lần.

**Architecture:** Controller → coordinator → các service transaction ngắn; service nghiệp vụ không phụ thuộc ngược coordinator. Reservation, voucher lock và payment attempt được chuẩn bị cùng transaction; gateway chạy ngoài transaction giữ khóa. Mọi replay được ràng buộc actor/operation/payload, trạng thái tiền được chuyển có kiểm soát và client có thể phục hồi ý định sau mất response.

**Tech Stack:** Java/Spring Boot, PostgreSQL/Flyway, PayOS SDK hiện có; Next.js/TypeScript; Flutter/manual Riverpod, UseCase/Result/Failure.

**Spec:** Đọc cùng các tài liệu trong thư mục này:
- `2026-10-21-antigravity-wallet-review-round-3.md`: finding N1–N14, vị trí và bằng chứng tái hiện.
- `2026-10-20-single-moodpoints-final.md`: nghiệp vụ một ví và các policy P1–P10 chưa chốt.
- `2026-10-21-antigravity-wallet-remediation-round-2.md`: yêu cầu còn hiệu lực; plan này điều chỉnh thứ tự và cụ thể hóa lỗi phát hiện mới.

## Tóm tắt để giao Anti

Giữ các phần đã sửa đúng: Owner theo cơ sở, bank binding, webhook fail closed, polling capture cơ bản, category soft-delete, phân trang Admin, nhãn đối soát và confirm retry cùng reference.

Vòng này có ba mốc:
1. **A — BE khởi động được:** sửa constructor, vòng dependency và schema/entity; migration thật + Hibernate validate + context phải pass.
2. **B — Luồng tiền an toàn:** scope idempotency; phục hồi payment attempt; job không nhả voucher sai; QR đúng binding; đối soát payout giữ quyền claim và bằng chứng; retry sau restart không tạo lệnh mới.
3. **C — Hoàn thiện nghiệp vụ/UI:** lịch cơ sở, ảnh review xác thực, phân trang lịch sử, chống response cũ, nối UseCase Mobile và kiểm thử tích hợp.

Không đánh dấu xong một mốc chỉ vì đã tạo class, có phương thức nhưng chưa caller, hoặc test mock pass.

## Global Constraints

- Customer **một số dư MoodPoints, 1 MoodPoint = 1đ**. Đổi điểm thành voucher trước checkout; đặt bàn chỉ áp voucher và trả phần thiếu PayOS. Không thêm ví VNĐ riêng hoặc trừ điểm trực tiếp khi checkout.
- Hoàn phần PayOS thực thu đủ điều kiện thành điểm; xử lý voucher riêng, không hoàn hai lần cùng giá trị. REFUND/REWARD là metadata nội bộ, không phải hai ví UI.
- Chỉ OWNER active đúng store được ví/ngân hàng/rút tiền. Staff thao tác booking theo quyền riêng; Admin payout dùng endpoint riêng.
- Kiểm tra cả ba repo ở `main`; giữ toàn bộ thay đổi chưa commit. Không tự reset/stash/chuyển nhánh, commit/push/deploy hoặc chạy migration trên DB server.
- Chỉ sửa BE/FE/MB trong phạm vi ví/voucher/booking/review liên quan; không mở rộng Auth hoặc dựng booking thứ hai.
- Migration đã chạy không sửa checksum. Tên V40/V41 bên dưới là đề xuất, phải dùng version kế tiếp thực tế; ưu tiên align entity với schema V39 đã có thay vì đổi tên tùy ý.
- Không vô hiệu hóa Hibernate validate để che lỗi N1; không bật circular references để che N3; không bỏ test đang fail để có báo cáo xanh.
- Không hardcode chính sách P1–P10 chưa duyệt. Cấu hình, tiền nguyên, rounding và snapshot theo spec; ví dụ 100k/15% không phải quyết định sản phẩm.
- Không gateway call trong transaction đang giữ khóa tiền/voucher; timeout là chưa rõ kết quả. Không URL giả, không âm số dư, không sửa/xóa ledger để cân lại.
- Mỗi task: thêm test tái hiện → xác nhận fail → sửa đúng đường chạy → xác nhận pass → bàn giao diff. Không tự commit trong lượt sửa này.

## Review Focus

- Khởi động với DB migrate thật và cấu hình gateway/QR test, không chỉ tạo service bằng `new` — Task 1–2.
- Cùng key khác user/payload, hai request cùng lúc và restart sau commit — Task 3–4, 8.
- Callback/job/release chạy xen kẽ: voucher không AVAILABLE sau khi đã tài trợ đơn PAID — Task 5.
- Admin khác can thiệp và tiền ngân hàng chưa rõ: tiền phải giữ locked — Task 7.
- A→B→A, filter/page đổi giữa refresh và history hơn một trang: không ghi đè dữ liệu mới hoặc mất lịch sử — Task 11.

## Bản đồ file

Prefix **BE** = `D:/Learn/MoodCafe-BE/moodcafe-be/`; **FE** = `D:/Learn/MoodCafe-FE/`; **MB** = `D:/Learn/MoodCafe-MB/`.

Tên file mới dưới đây là thiết kế đề xuất; nếu repo đã có lớp tương đương thì sửa lớp đó, không tạo bản trùng. Cập nhật test/caller cùng lúc khi đổi signature.

## Task 0 — Chốt đầu vào và bằng chứng baseline

**Files:** Review vòng 3, spec gốc; AGENTS.md; BE `pom.xml`, `src/main/resources/db/migration/`; booking/API caller hiện có.

- [ ] Kiểm tra branch/diff và mapping booking thật. Liệt kê file sẽ sửa; không trộn thay đổi `infra/` hoặc file không liên quan vào nhiệm vụ.
- [ ] Ghi V37–V39 đã chạy ở đâu, version kế tiếp và DB test biệt lập. Không dùng DB server; không in secrets.
- [ ] Baseline đã biết: 202 test mặc định pass; 34 IT có 1 NPE test QR; FE typecheck/lint pass; MB analyze/316 test pass. `verify -o` còn lỗi cache/dependency Maven jar plugin, chưa kết luận do source.
- [ ] Lập bảng policy P1–P10 theo quyết định đã có. Các sửa correctness không phụ thuộc policy vẫn thực hiện; mục chưa có quyết định không tự chốt và phải ghi trong bàn giao.

## Task 1 — Sửa constructor và bỏ vòng service

**Finding:** N2, N3 và NPE test quyền. **Priority:** P1.

**Modify BE:** `src/main/java/com/moodcafe/payment/service/PayOSService.java`, `reservation/service/ReservationService.java`, `payment/service/BookingPaymentCoordinator.java`, `reservation/controller/ReservationController.java`; các test đang gọi constructor trực tiếp, gồm `ReservationAuthorizationIT.java`.
**Create test:** `src/test/java/com/moodcafe/payment/FinancialServiceWiringTest.java`.

**Interfaces:** Controller gọi Coordinator để tạo booking. Coordinator gọi `ReservationService.prepareReservation(...)`; ReservationService không inject Coordinator. PayOSService có một constructor production rõ ràng; dependency optional dùng cơ chế Spring hỗ trợ, không tạo overload chỉ để giữ test cũ.

- [ ] Tạo context test tái hiện `No default constructor found` và `BeanCurrentlyInCreationException`, mock hạ tầng ngoài phạm vi nhưng dùng bean thật của các service cần kiểm tra.
- [ ] Chọn constructor injection chính, cập nhật test tương ứng; bỏ field/delegate tạo phụ thuộc ngược ở ReservationService nếu không còn caller cần nó. Không chỉ chuyển `@Lazy` sang chỗ khác để giữ vòng không cần thiết.
- [ ] Sửa fixture `ReservationAuthorizationIT`: inject QR dependency, gửi token hợp lệ trong test self checkout; test token sai phải bị từ chối. Không bỏ điều kiện xác thực để test pass.
- [ ] Chạy wiring test và test quyền; context phải refresh thành công, không có hai exception trên. Rà lại nhánh gateway chưa cấu hình để optional dependency không làm crash ngoài chủ đích cấu hình.

**Verify:** `./mvnw.cmd -Dtest=FinancialServiceWiringTest,ReservationAuthorizationIT test`.

## Task 2 — Align schema redeem và thêm migration/startup gate

**Finding:** N1. **Priority:** P1. **Phụ thuộc:** Task 1.

**Modify BE:** `voucher/entity/VoucherRedemption.java`, `voucher/abstraction/repository/VoucherRedemptionRepository.java`, `voucher/service/VoucherRedemptionService.java`; `pom.xml`.
**Create đề xuất:** migration `V40__align_redemption_and_intent_schema.sql`; `src/test/java/com/moodcafe/wallet/FinancialSchemaStartupIT.java`; profile cấu hình test trong `src/test/resources/`.

**Contract quyết định kỹ thuật:** Giữ cột V39 `idempotency_key` và `payload_hash` làm canonical. Java có thể giữ property intentKey nhưng phải map về idempotency_key; bổ sung payloadHash. Nếu cần pointsSpent, thêm cột thật và backfill từ snapshot pointCost của voucher kết quả, không lấy giá template hiện tại làm lịch sử.

- [ ] Test PostgreSQL mới: Flyway chạy toàn bộ migrations → Hibernate validate → context; phải tái hiện mismatch trên bản cũ.
- [ ] Đồng bộ entity, query, constraint và logic insert/replay theo contract; không chỉ sửa tên một cột rồi bỏ sót NOT NULL còn lại.
- [ ] Test nâng cấp DB fixture đã có V39 và redemption hợp lệ; bảo toàn voucher/ledger. Dữ liệu thiếu binding/snapshot phải được báo kiểm tra, không tự tạo số dư hoặc xóa để vượt migration.
- [ ] Cấu hình Failsafe `integration-test` + `verify` cho `*IT`; Surefire cho unit tests. Các IT mock có thể đổi tên thành unit test để phản ánh đúng phạm vi, không coi tên IT là bằng chứng DB.
- [ ] Chạy startup trên DB test biệt lập với gateway giả/test credentials, scheduler không tự chạy tác vụ trong lúc setup. Xác nhận validate và wiring thật pass, không chỉ mock context nhỏ.

**Gate A:** Chỉ đánh dấu A đạt khi Task 1–2 pass, có log migration/context và không tắt validate/circular-reference guard. Nếu DB test không chạy được, ghi gate chưa xác minh; không báo đã nghiệm thu startup.

## Task 3 — Payment intent có scope, unique và cùng transaction prepare

**Finding:** N4 + REQUIRES_NEW/sequence fallback. **Priority:** P1. **Phụ thuộc:** Gate A.

**Modify BE:** `payment/entity/PaymentAttempt.java`, `payment/abstraction/repository/PaymentAttemptRepository.java`, `payment/service/PaymentAttemptService.java`, `payment/service/BookingPaymentCoordinator.java`, `payment/service/OrderCodeGeneratorService.java`, `reservation/service/ReservationService.java`.
**Create:** migration kế tiếp cho intent scope/unique; `src/test/java/com/moodcafe/payment/BookingIntentAuthorizationIT.java`, `BookingIntentConcurrencyIT.java`.

**Interfaces:** Lookup theo `(actorId, purpose, intentKey)`, không key toàn hệ thống. Lưu payloadHash từ request có ý nghĩa: store, category, bookingTime, guestCount, voucher, notes sau normalization nhất quán. Cùng key/cùng payload trả cùng businessId; khác payload 409. Trước trả dữ liệu luôn xác nhận ownership/purpose.

- [ ] Test A tạo key K; B dùng K không thấy đơn/payment URL của A. Cùng A/K đổi payload trả 409. Hai request cùng A/K đồng thời chỉ một reservation/attempt.
- [ ] Bổ sung actor scope/hash/unique DB và map caller. Financial write phải có key ổn định từ client; cập nhật các caller thực tế trước khi yêu cầu bắt buộc, không âm thầm sinh timestamp server rồi tuyên bố retry-safe.
- [ ] Chuyển createAttempt tham gia transaction prepare (`REQUIRED` hoặc lớp transaction hiện có), không REQUIRES_NEW. Reservation + voucher lock + attempt cùng commit/rollback; network vẫn ở coordinator ngoài transaction đó.
- [ ] Migration backfill actor/hash từ business record đúng purpose. Attempt mồ côi/ambiguous phải tách xử lý legacy/reconciliation, không gán cho user đầu tiên hoặc tự tạo booking. Không replay record chưa xác minh ownership.
- [ ] OrderCode chỉ dùng nguồn bền vững chung booking/subscription. Khi sequence lỗi, fail rõ trước gateway, bỏ fallback AtomicLong trùng qua restart/instance. Khởi tạo sequence tránh mã đã dùng, không mặc định một hằng số thấp hơn dữ liệu.
- [ ] Test rollback outer transaction không để attempt mồ côi; test nhiều instance/concurrent intent và cấp mã lỗi không tạo link.

## Task 4 — Resume payment workflow và giữ trạng thái không lùi

**Finding:** N7 + attempt state chưa monotonic. **Priority:** P1. **Phụ thuộc:** Task 3.

**Modify BE:** `payment/service/BookingPaymentCoordinator.java`, `PaymentAttemptService.java`, `PayOSService.java`, `reservation/service/ReservationPaymentCaptureService.java`; entity/repository attempt nếu cần.
**Test:** `src/test/java/com/moodcafe/payment/PaymentAttemptRecoveryIT.java`, thêm test PostgreSQL về interleaving/restart.

**Interfaces:** `resume(actorId, purpose, intentKey)` hoặc entry point create/replay thống nhất dùng Task 3; kết quả phân biệt READY/paymentUrl, PAID, VERIFYING, FAILED đã xác định. `UNKNOWN` là quan sát chưa rõ, không được ghi đè PAID. Capture đồng bộ booking/attempt/voucher/ledger theo transition hợp lệ.

- [ ] Test dừng sau prepare trước gateway: retry cùng key tra lại đúng orderCode rồi tiếp tục tạo link chỉ khi gateway xác nhận chưa có link. Không trả success PENDING với URL null như kết quả cuối cùng.
- [ ] Test remote tạo link nhưng response mất: lookup cùng attempt và lưu gateway link ID/URL thật, không tạo payment mới. Nếu lookup chưa rõ, trả VERIFYING và cho tra lại; không yêu cầu đổi key.
- [ ] Test zero-pay dừng sau prepare: resume capture nội bộ đúng một lần, không gọi gateway, voucher USED và pending đúng.
- [ ] Thay `markStatus(orderCode, arbitraryString)` bằng transition có expected state/version. Callback đã PAID rồi response create lỗi không được hạ về UNKNOWN. Callback/polling ghi attempt cùng trạng thái capture; complete/claim/cancel không bị callback lặp đảo ngược.
- [ ] Test webhook tới trước response, hai resume đồng thời, process restart và DB ghi response thất bại. Mọi khoản gateway đã thu đều có record xử lý/đối soát, không chỉ log rồi bỏ qua.

## Task 5 — Một đường expiry và voucher transition có khóa

**Finding:** N5, N6. **Priority:** P1. **Phụ thuộc:** Task 3–4.

**Modify BE:** `reservation/scheduler/ReservationLifecycleScheduler.java`, `reservation/service/ReservationPaymentCaptureService.java`, `voucher/service/VoucherLifecycleService.java`, `voucher/abstraction/repository/CustomerVoucherRepository.java`, repository attempt/reservation.
**Test:** `src/test/java/com/moodcafe/reservation/BookingPaymentReconciliationIT.java`, `src/test/java/com/moodcafe/voucher/VoucherCaptureReleaseConcurrencyIT.java` mới.

**Interfaces:** Expiry chỉ qua service transition kiểm tra reservation + attempt + voucher binding dưới khóa/CAS. Job không trực tiếp release mọi LOCKED theo holdUntil. Capture phải thấy voucher thuộc đúng booking/attempt và trạng thái hợp lệ; thiếu voucher tài trợ là lỗi invariant cần đối soát, không âm thầm confirm discount.

- [ ] Tái hiện đơn 20 phút, gateway/attempt PENDING, hold quá 15 phút: voucher vẫn LOCKED. Reconcile UNKNOWN trong khi DB còn PENDING cũng không được nhả ở vòng quét tiếp theo.
- [ ] Bỏ vòng “orphan” bypass hoặc đưa vào cùng pipeline xác minh. PENDING dù quá 30 phút phải cancel link và xác nhận trạng thái cuối; thiếu cấu hình/lỗi network không được suy ra chưa trả tiền.
- [ ] Network reconcile/cancel ngoài khóa DB; khi có kết quả cuối mở transaction ngắn recheck trạng thái mới nhất. Gateway PAID phải capture; UNKNOWN giữ pending/lock; CANCELLED/EXPIRED xác nhận mới có thể release đúng attempt.
- [ ] Khóa/CAS voucher theo ID + LOCKED + reservation/attempt. Thống nhất thứ tự khóa giữa capture/cancel/release để tránh deadlock. Không USED→AVAILABLE qua release thường; restore khi refund dùng transition riêng có điều kiện.
- [ ] Test PostgreSQL callback commit USED giữa job read và release; release không hồi AVAILABLE. Test voucher đã re-lock đơn mới không bị job đơn cũ động tới, hai worker và retry chỉ một ledger.
- [ ] Thêm pagination/query theo trạng thái/deadline, bỏ findAll quét toàn bảng. Late-paid sau đóng attempt cần record compensation/reconciliation, không return bỏ qua tiền.

## Task 6 — QR có binding và consume, bỏ secret mặc định

**Finding:** N8. **Priority:** P1. **Phụ thuộc:** Task 1.

**Modify BE:** `reservation/service/ReservationQrVerificationService.java`, `ReservationService.java`, `reservation/controller/ReservationController.java`, DTOs; cấu hình QR test/runtime. Cập nhật caller QR thực tế ở FE/MB.
**Create nếu cần:** entity/repository challenge và migration; `src/test/java/com/moodcafe/reservation/ReservationQrVerificationIT.java` cập nhật thành kiểm chứng đầy đủ.

**Contract:** Verification nhận actor, reservation, action CHECK_IN/CHECK_OUT, token/challenge. Có store/booking/action, expiry và nonce; consume nguyên tử cùng booking transition. Secret bắt buộc từ cấu hình, không có giá trị bí mật mặc định trong source; log không in token/expected HMAC.

- [ ] Test token giả, sai user/store/booking/action, hết hạn/ngày tương lai và replay bị chặn. Token check-in không dùng để checkout; retry action đã thành công trả trạng thái cũ nhưng không thưởng lần hai.
- [ ] Thay daily token dùng chung bằng cơ chế proof hiện diện thực tế của dự án có challenge ngắn hạn; map cả issuance và verification, có caller production. Không chỉ thêm chữ ký vào mã tĩnh rồi tuyên bố chống sao chép. TTL lấy cấu hình đã chốt, không tự chọn số phút thành chính sách.
- [ ] Nhánh Customer hoàn tất cần verification; `isSelfQr=false` không biến customer thành staff. Staff đúng store dùng hành động riêng và không nhận self bonus; actor/action do BE xác định.
- [ ] Test checkout 29:59/30:00, hai request consume cùng nonce, hợp lệ đúng một lần. Inject Clock để test ổn định; update startup test với QR secret test và kiểm tra thiếu secret fail rõ theo cấu hình.

## Task 7 — Payout reconciliation giữ claim và ghi nhận xác minh

**Finding:** N9. **Priority:** P1. **Phụ thuộc:** Gate A.

**Modify BE:** `wallet/service/WithdrawalService.java`, `wallet/controller/AdminWithdrawalController.java`, `wallet/entity/WithdrawalRequest.java`; migration audit nếu cần.
**Modify FE:** `src/services/wallet.service.ts`, `src/app/admin/withdrawals/page.tsx`.
**Test:** BE `src/test/java/com/moodcafe/wallet/WithdrawalResolutionIT.java` và test concurrency DB.

**Interfaces:** PROCESSING→RECONCILIATION_REQUIRED và resolve chỉ do claimedBy hợp lệ; chưa có policy chuyển giao thì chặn Admin khác, không tự mở quyền override. Resolve PAID có reference; resolve VERIFIED_NOT_TRANSFERRED có verificationReference, evidence và notes không rỗng theo quy trình nhóm. Kết quả chưa rõ không có transition unlock.

- [ ] Test A claim, B mark/resolve → 403 không đổi tiền. A mark với kết quả chưa rõ → locked giữ nguyên. Test claim/confirm/resolve chạy đối đầu.
- [ ] Bỏ default “xác nhận tiền chưa rời tài khoản” khi notes null. Reject resolution thiếu kết quả kiểm tra/chứng từ; không để action REJECTED đơn thuần tự tạo bằng chứng. Manual evidence là bằng chứng do Admin ghi, không mô tả thành xác minh ngân hàng tự động.
- [ ] Persist người kiểm tra, thời điểm, reference/evidence và kết luận. PAID/reference unique dùng constraint hiện có, conflict rõ, rollback cả ledger/balance khi thua race. Giữ chặn tự duyệt.
- [ ] Retry resolve/confirm cùng intent trả trạng thái cuối cũ; khác quyết định/reference conflict. UI chỉ hiện hành động theo claim/state, hiển thị rõ số tiền vẫn locked khi chờ đối soát.
- [ ] Test hai payout dùng cùng bank reference đồng thời trên PostgreSQL, hai Admin và response confirm mất. Không chuyển tiền thật trong kiểm thử.

## Task 8 — Intent rút tiền/redeem phục hồi được trên FE/MB

**Finding:** N10, N11, N12. **Priority:** P1 cho payout; bắt buộc cho redeem. **Phụ thuộc:** Task 2, 7; contract Mobile thống nhất cùng Task 12.

**Modify BE:** `voucher/service/VoucherRedemptionService.java`, `voucher/controller/CustomerVoucherController.java`, redemption repository/entity; thêm endpoint đọc trạng thái theo key nếu API hiện tại không đủ.
**Modify FE:** `src/app/merchant/wallet/page.tsx`, `src/services/wallet.service.ts`, `src/services/voucher.service.ts` khi có caller.
**Modify MB:** `lib/features/vouchers/presentation/pages/voucher_catalog_page.dart`, data source/repository/usecase redeem; module storage intent theo abstraction hiện có.
**Tests:** BE `VoucherRedemptionIdempotencyIT.java`; FE/MB lost-response/restart tests.

**Interfaces:** Intent lưu user, operation, store hoặc template, payloadHash, key và trạng thái SUBMITTED/VERIFYING/CONFIRMED. Key tạo một lần trước gửi và được giữ qua đóng modal/reload/restart. Endpoint query/replay phải kiểm tra owner, không trả kết quả theo key toàn cục.

- [ ] BE redeem dùng contract canonical Task 2: cùng actor/key/template trả voucher cũ; cùng key template khác 409. Concurrent loser tra lại kết quả sau rollback/transaction mới thay vì tiếp tục trong transaction đã lỗi. Chỉ một debit/voucher.
- [ ] FE không sinh key mỗi lần mở modal; khôi phục unresolved intent khi reopen/reload/A→B→A. Mobile không sinh timestamp key cho mỗi lần xác nhận lại sau timeout.
- [ ] Timeout không thông báo chắc chắn thất bại. Cho “Đang xác minh”, tra trạng thái hoặc replay cùng key; chỉ mở ý định mới khi kết quả cũ đã rõ và người dùng chủ ý tạo thêm. Không xóa unresolved intent chỉ vì đóng modal.
- [ ] Lưu intent qua storage phù hợp không chứa token/secrets; scope theo user/operation/store. Logout/user switch không cho user mới xem/tiếp tục intent của user cũ; không đánh mất dấu vết giao dịch đã gửi phía BE.
- [ ] Test server commit nhưng response mất → modal reopen/reload/app restart vẫn một payout hoặc một voucher, số dư chỉ đổi một lần. Test payload khác conflict, hết số dư và request song song; refresh authoritative balances/history sau xác nhận.

## Task 9 — Chặn booking ngoài giờ và lịch thiếu cấu hình

**Finding:** N13. **Priority:** P2. **Phụ thuộc:** Policy ngày phục vụ P9.

**Modify BE:** `reservation/service/ServiceDayDeadlineResolver.java`, `ReservationService.java`; test `src/test/java/com/moodcafe/reservation/ServiceDayDeadlineResolverTest.java` và booking integration test.

**Interface:** Resolver xác minh booking thuộc ca hợp lệ trước trả closing deadline; deadline snapshot. Không fallback 07:00–22:00 khi thiếu dữ liệu.

- [ ] Test fixture 07:00–21:00 đặt 22:00 bị từ chối; 18:00–02:00 đặt 12:00 bị từ chối; cùng ca đặt 23:30/01:00 có deadline 02:00 đúng ngày phục vụ. Giờ fixture không phải default sản phẩm.
- [ ] Xử lý opening=closing, 24/7, đóng bất thường và thiếu giờ theo P9 đã chốt. Nếu chưa có schedule hợp lệ, trả lỗi cấu hình rõ trước khóa voucher/gọi gateway, không tự suy diễn ngày sau.
- [ ] Kiểm tra booking trong quá khứ, ranh giới closing/grace theo policy; không để paid mới lập tức no-show vì deadline trước bookingTime. Không tự tính lại deadline đơn cũ khi giờ quán thay đổi.
- [ ] Test late claim/job close chạy bù: TABLE_EXPIRED chỉ nhả bàn, CLAIMED_LATE chỉ Spend, no-show 0 điểm; giữ các behavior đã đúng.

## Task 10 — Thưởng review dựa trên visit đúng booking

**Finding:** N14. **Priority:** P2. **Phụ thuộc:** Task 6 và P4/P5.

**Modify BE:** `store/entity/VisitVerification.java`, `store/service/VisitVerificationServiceImpl.java`, `store/service/StoreReviewServiceImpl.java`, `loyalty/service/LoyaltyRewardService.java`, DTO và migration binding.
**Test:** `src/test/java/com/moodcafe/loyalty/VerifiedReviewRewardIT.java`.

**Interface:** Reward nhận reviewId đã lưu; service tải visit evidence được liên kết reservationId đúng user/store và trạng thái hợp lệ. Ảnh nonempty chỉ là một điều kiện, không thay visit verification.

- [ ] Test ảnh upload thường + booking nhưng không visit hợp lệ → không thưởng verified review. Visit của đơn khác/cùng store, user khác, evidence bị dùng lại đều không hợp lệ.
- [ ] Gắn visit tới booking khi tạo/xác minh visit bằng dữ liệu server; không chọn đơn gần nhất tùy ý hoặc tin reservationId client mà không ownership check.
- [ ] Nối production review→reward trong transaction hoặc durable event. Thưởng tối đa một lần/booking, retry/event lặp/sửa review không cộng thêm.
- [ ] P5 chưa được duyệt thì không tự cho CLAIMED_LATE nhận verified bonus; ghi rõ phần policy chờ, không đổi bonus/cap theo suy đoán. Tránh mất review hợp lệ chỉ vì chưa đủ điều kiện thưởng.

## Task 11 — Chống response cũ và xem đủ lịch sử

**Finding:** Khoảng trống UI round 3. **Phụ thuộc:** Task 7–8.

**Modify FE:** `src/app/admin/withdrawals/page.tsx`, `src/app/merchant/wallet/page.tsx`, `src/services/wallet.service.ts`.
**Modify MB:** wallet history provider/controller/page và repository paging contract theo Task 12.
**Tests:** Component hoặc E2E fixture của runner hiện có; không thêm test chỉ lặp lại hàm implementation.

- [ ] Dùng request generation/abort cho cả effect và imperative reload; old response không được set rows, balances, bank selection, loading hoặc totalPages. So storeId đơn thuần không đủ cho A→B→A.
- [ ] Test refresh A chậm → B → A mới; response A cũ tới cuối không ghi đè. Test Admin action reload cũ + đổi filter/page, chỉ response của selection mới có hiệu lực.
- [ ] Giữ fix clamp trang cuối; thêm pagination/load more giao dịch và payout Owner, không chỉ fetch page 0/20. Mobile lịch sử cũng tải thêm được, hiển thị metadata hết trang/lỗi tải thêm.
- [ ] Test 21+ record xem được record 21, không duplicate/mất khi chuyển trang; 16 REQUESTED xử lý dòng cuối trang 2 về trang hợp lệ. API lỗi không giả danh sách rỗng/số dư 0.
- [ ] Kiểm tra render thật 360/390/768/1280px và Mobile chữ lớn: số tiền, tên ngân hàng dài, modal, hàng trạng thái VERIFYING/đối soát; lưu ảnh làm bằng chứng. Chưa render thì ghi rõ chưa kiểm tra trực quan.

## Task 12 — Nối UseCase Mobile vào đường chạy thực tế

**Finding:** UseCase scaffold chưa có consumer, Result/DTO chưa làm. **Phụ thuộc:** Contract redeem Task 8.

**Modify MB:**
- `lib/features/wallet/presentation/providers/wallet_providers.dart`, wallet page, `domain/repositories/wallet_repository.dart`, data repository/data source và các model hiện có.
- `lib/features/vouchers/presentation/providers/voucher_providers.dart`, catalog page, `domain/repositories/voucher_repository.dart`, data repository/data source/model.
- Tái sử dụng `domain/usecases/get_my_wallet_usecase.dart`, `get_wallet_transactions_usecase.dart`, `get_voucher_catalog_usecase.dart`, `get_usable_vouchers_usecase.dart`, `redeem_voucher_usecase.dart` trong feature tương ứng.
**Create:** DTOs trong `data/models/`; widget/controller tests trong `test/features/wallet/`, `test/features/vouchers/`.

**Interfaces:** Repository domain trả `Future<Result<T>>` theo Result hiện có; data source ném typed exceptions, repository map Failure. Provider/controller gọi UseCase thật; redeem trả CustomerVoucher hoặc kết quả intent rõ, không `Future<void>` làm mất định danh kết quả. Domain không JSON parsing hạ tầng/Flutter/Dio/storage.

- [ ] Thay production consumers: bỏ page/provider gọi repository trực tiếp; wiring test xác nhận thao tác UI đi qua UseCase và xử lý Ok/Err.
- [ ] Freezed/json_serializable DTO ở data, mapper sang domain entity; giữ manual Riverpod. Regenerate model bằng `dart run build_runner build --delete-conflicting-outputs` khi cần.
- [ ] Kết nối paging Task 11 và durable intent Task 8 qua abstraction, không đưa storage/Dio vào domain hoặc page.
- [ ] Widget/controller test loading/error/retry/empty/success, dispose giữa request, lost response và restart; không chỉ test UseCase delegate. Analyze/test pass không thay thế bằng chứng caller thật.

## Task 13 — Đối soát, gate tích hợp và bàn giao

**Phụ thuộc:** Các task trên.

**Files:** BE `finance/service/ReconciliationService.java`, config/admin modules hiện có, `pom.xml`, profile/test fixtures; tài liệu mapping Task 0.

- [ ] Đối chiếu ledger với payment captures/refunds, voucher funding và payout theo business reference; báo mismatch cụ thể, không chỉ sum ví, không tự sửa số dư. Hoàn tất config/checkout/voucher admin còn thiếu theo Task 12 round 2, tránh tạo controller trùng.
- [ ] Bổ sung test PostgreSQL thật cho unique intent/reference, capture/release và payout races; mock gateway được phép, không mock repository thay kiểm chứng khóa/constraint.
- [ ] Chạy unit/IT theo gate đã cấu hình và kiểm tra report không skip. Xử lý NPE test QR bằng fixture đúng; xử lý Maven cache/plugin bằng tải đúng dependency khi được phép. Không pin/downgrade framework tùy ý chỉ để né lỗi cache; không báo `verify` pass khi mới pass pha test.
- [ ] BE `./mvnw.cmd verify`; FE typecheck/lint/build theo scripts repo; MB `flutter analyze`, `flutter test`. Chạy test tập trung sau từng task, cuối cùng mới full gate; ghi rõ môi trường/lệnh/exit/result.
- [ ] E2E theo các invariant: một ý định một booking/payout/voucher; PENDING/UNKNOWN giữ quyền voucher; PAID/cancel/settlement chỉ ghi một lần; đúng owner/store/actor; khoản tiền chưa rõ còn đường đối soát.
- [ ] Bàn giao bảng finding→file→test→bằng chứng→trạng thái, migration/config cần thiết, ảnh UI và mục còn chờ policy. Tách lỗi source với hạn chế môi trường test; không tuyên bố hoàn thành phần chưa chạy được.

## Ma trận đóng finding

| Finding | Task | Bằng chứng bắt buộc |
|---|---|---|
| N1 schema/entity redeem | 2 | Flyway + Hibernate validate trên PostgreSQL |
| N2 constructor PayOS | 1 | Spring chọn constructor production và context pass |
| N3 dependency vòng | 1–2 | Context thật không circular dependency |
| N4 replay khác user/payload | 3 | Không lộ đơn A, conflict payload, một record khi concurrent |
| N5 expiry PENDING/UNKNOWN | 5 | Case 20 phút không release, 30 phút không tự suy ra thất bại |
| N6 USED hồi AVAILABLE | 5 | Capture/job xen kẽ trên DB không hồi voucher |
| N7 resume thiếu link/zero-pay | 4 | Restart tiếp tục cùng attempt, kết quả dùng được |
| N8 QR replay/default secret | 6 | Binding/action/nonce/expiry và consume atomic |
| N9 đối soát bỏ claim/bằng chứng | 7 | B không can thiệp A, không unlock khi thiếu xác minh |
| N10 FE mất payout intent | 8 | Reopen/reload/store switch vẫn một payout |
| N11 MB key redeem mới | 8, 12 | Restart/retry vẫn một voucher/debit |
| N12 redeem khác template | 2, 8 | 409 khác payload, concurrent replay cùng kết quả |
| N13 booking ngoài giờ | 9 | Ngoài ca/thiếu lịch bị chặn trước tác dụng phụ |
| N14 review thiếu visit binding | 10 | Không thưởng ảnh thường/visit đơn khác, thưởng một lần hợp lệ |
| Transaction/sequence/state | 3–5 | Rollback không orphan, mã bền vững, PAID không lùi UNKNOWN |
| UI history/races | 11 | Record 21 và response cũ không ghi đè selection mới |
| Mobile architecture | 12 | Production gọi UseCase/Result, DTO đúng tầng |
| Config/reconciliation/test gate | 0, 13 | Policy mapping, mismatch report, verify/IT không skip |

## Prompt gửi Antigravity

Đọc `2026-10-21-antigravity-wallet-remediation-round-3.md` cùng review `2026-10-21-antigravity-wallet-review-round-3.md` và spec một ví MoodPoints. Thực hiện Gate A trước: sửa constructor, dependency vòng và schema/entity; chứng minh startup với DB migrate thật. Sau đó sửa intent scope/resume, voucher race/expiry, QR và payout đối soát; hoàn tất retry sau restart ở FE/MB cùng các task UI/nghiệp vụ/kiến trúc. Mỗi finding phải có regression test đúng đường production, không chỉ thêm class/test mock. Giữ main và thay đổi hiện có, không tự chốt policy chưa duyệt, không dùng DB/tiền thật, không tự commit/push/deploy. Bàn giao bảng N1–N14 với bằng chứng và những mục còn thiếu; không báo toàn bộ hoàn thành nếu gate bị skip hoặc chưa chạy được.
