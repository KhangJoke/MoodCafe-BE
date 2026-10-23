# Ví / MoodPoints / Voucher — Implementation Plan sửa vòng 2

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans hoặc quy trình tương đương của Antigravity để thực hiện từng task. Theo dõi bằng checkbox; hoàn thành kiểm thử và review trước khi chuyển task phụ thuộc.

**Goal:** Sửa 14 finding R1–R14 của lượt review mới nhất và hoàn thiện các khoảng trống được chỉ ra, giữ đúng cơ chế Customer một ví MoodPoints.

**Architecture:** BE quyết định quyền, giá và trạng thái; ledger và thay đổi số dư chạy nguyên tử. Ý định thanh toán được lưu bền vững trước khi gọi gateway; timeout có trạng thái chưa rõ kết quả để đối soát. FE/MB giữ idempotency key của cùng ý định và hiển thị trạng thái BE xác nhận.

**Tech Stack:** Java/Spring Boot, PostgreSQL/Flyway, PayOS SDK hiện có; Next.js/TypeScript; Flutter/manual Riverpod, Full Clean Architecture.

**Spec:** Đọc cả ba tài liệu cùng thư mục:
- `2026-10-21-antigravity-wallet-rereview.md`: nguồn finding R1–R14 và bằng chứng file/dòng.
- `2026-10-20-single-moodpoints-final.md`: nghiệp vụ gốc, bao gồm chính sách P1–P10 còn mở.
- `2026-10-21-antigravity-wallet-remediation.md`: plan vòng 1; các yêu cầu chưa hoàn tất vẫn còn hiệu lực.

Khi thứ tự sửa khác nhau, ưu tiên dependency của plan vòng 2. Không coi các con số minh họa là chính sách đã duyệt.

## Tóm tắt giao nhóm

Đã có quyền Owner theo cơ sở, kiểm tra ngân hàng, hàm PayOS booking riêng, scheduler, phân trang và error state Mobile. Vòng này tập trung sửa các trường hợp **callback giả, mất response, gateway timeout, retry và chạy đồng thời** còn làm sai trạng thái tài chính.

Triển khai ba đợt:
1. **Đợt A — Payment và QR:** xác minh callback; lưu payment attempt; polling phải xác nhận booking; hết hạn chỉ nhả voucher khi an toàn; QR được kiểm chứng server.
2. **Đợt B — Payout và redeem:** giữ tiền khi chuyển khoản chưa rõ; mã ngân hàng không dùng hai lần; retry không tạo payout/voucher mới; UI đối soát có đường thao tác thật.
3. **Đợt C — Nghiệp vụ/UI/kiến trúc:** deadline theo cơ sở, category chưa xóa, review reward có caller, sửa phân trang/error cũ; hoàn tất UseCase/Result Mobile và các mục còn thiếu của plan vòng 1.

**Kết quả mong muốn:** Mỗi finding có test tái hiện, thay đổi đúng đường chạy production và bằng chứng pass. Không nghiệm thu chỉ dựa vào số test tăng hoặc tên file chứa chữ Integration/IT.

## Global Constraints

- Customer chỉ có **một số dư MoodPoints; 1 MoodPoint = 1đ**. Điểm đổi voucher trước checkout; checkout áp voucher, trả phần thiếu qua PayOS. Không trừ điểm trực tiếp, không thêm ví hoàn tiền VNĐ riêng.
- Hoàn phần thực thu đủ điều kiện qua PayOS thành MoodPoints; phục hồi voucher riêng. Không hoàn trùng giá trị voucher thành điểm và trả lại cùng voucher.
- Ví quán vẫn pending/available/locked bằng VNĐ. Chỉ OWNER active đúng store được dùng API ví/ngân hàng/rút; MANAGER/CASHIER không được vào tài chính.
- Giữ các sửa đúng ở vòng 1. Không viết lại Auth, UI ngoài phạm vi hoặc tạo booking thứ hai.
- Kiểm tra BE/FE/MB ở `main` trước khi sửa. Nếu checkout khác main hoặc có thay đổi chưa commit, giữ nguyên dữ liệu và báo rõ; không tự reset/stash/chuyển branch làm mất việc.
- Không tự commit/push/deploy, chạy migration server hoặc chuyển tiền thật. Mỗi task kết thúc bằng diff có thể review; commit chỉ khi người dùng yêu cầu.
- Migration đã chạy không sửa nội dung/checksum; dùng migration mới sau version cao nhất thực tế. Tên V39 bên dưới là dự kiến nếu V38 vẫn là bản cuối.
- Không hardcode quyết định P1–P10 chưa duyệt. Chuẩn hóa số tiền/điểm nguyên; rate/rounding/bonus/min phải lấy chính sách đã được xác nhận và snapshot.
- Giữ ledger bất biến, chống ghi lặp bằng business key và constraint DB. Network call không nằm trong transaction đang giữ khóa tiền/voucher.
- Không đưa số dư giả 0, link thanh toán giả hoặc toast thành công trước khi BE xác nhận.

## Review Focus

- Callback sai chữ ký/sai purpose/thiếu amount không được cập nhật booking hoặc tiền — Task 1.
- Gateway đã thực hiện nhưng response mất: giữ attempt và tra lại, không tạo giao dịch thứ hai — Task 2–3.
- Job, callback và retry cùng chạy: trạng thái không lùi, voucher không dùng lại, tiền chỉ ghi một lần — Task 3, 5, 7.
- Client đổi store/filter khi request đang chạy: không hiển thị dữ liệu sai phạm vi hoặc kẹt trang — Task 6, 10.
- Process restart sau khi gửi request: ý định tài chính còn phục hồi được, không sinh key mới vô tình — Task 2, 6, 7.

## Bản đồ file và quy ước

Các đường dẫn bên dưới có prefix:
- **BE:** `D:/Learn/MoodCafe-BE/moodcafe-be/`.
- **FE:** `D:/Learn/MoodCafe-FE/`.
- **MB:** `D:/Learn/MoodCafe-MB/`.

Tên class mới là thiết kế đề xuất của người lập plan, không phải API đang có. Dùng convention hiện tại nếu có lớp tương đương; không tạo bản trùng. Các contract giữa task phải được cập nhật cùng nhau nếu đổi tên.

## Task 0 — Chốt mapping và baseline trước khi sửa

**Files:** Spec/review nói trên; BE `pom.xml`, V37/V38 và booking/payment hiện có; AGENTS.md của từng repo.

**Deliverable:** Một bảng ngắn trong báo cáo bàn giao: finding → file thực tế → test → trạng thái; bảng policy lấy từ quyết định nào.

- [ ] Đọc spec/review, kiểm tra branch/diff. Xác định booking chính thức và mapping với reservation đang review; ghi rõ nếu flow thật ở nhánh khác, không tiếp tục dựng hệ thống song song.
- [ ] Xác định migration nào đã chạy, môi trường test PostgreSQL biệt lập và số version tiếp theo. Không đọc/in secrets hoặc dùng DB server để thử.
- [ ] Ghi policy còn thiếu P1–P10. Các task không phụ thuộc policy vẫn làm; task phụ thuộc ghi rõ phần còn chờ, không tự chọn 15%, 100k, 23:00 hoặc số điểm thưởng làm mặc định sản phẩm.
- [ ] Lưu baseline vòng review: BE 200 test + 12 test quyền chạy riêng; FE typecheck/lint pass; MB analyze và 309 test pass. Không chạy lại baseline vô ích nếu chưa sửa code.

## Task 1 — Chặn callback không hợp lệ trước mọi cập nhật tiền

**Finding:** R1. **Priority:** P1. **Phụ thuộc:** Task 0.

**Modify:** BE `src/main/java/com/moodcafe/payment/service/PayOSService.java`, `payment/controller/PaymentController.java`, `payment/dto/request/PayOSWebhookRequest.java`, `reservation/service/ReservationPaymentCaptureService.java`.
**Test:** BE `src/test/java/com/moodcafe/payment/PayOSWebhookSecurityTest.java` mới; cập nhật `PayOSServiceTest.java`.

**Interfaces:** Handler chỉ chuyển sự kiện đã verify sang capture. `capturePaymentSuccess(reservationId, orderCode, paidAmountVnd)` phải kiểm tra binding với attempt/order đã lưu; không tự gán amount thiếu thành amount dự kiến hoặc ghi đè orderCode để hợp thức hóa callback.

- [ ] Viết regression test: chữ ký sai/mất; gateway chưa cấu hình; code thanh toán thất bại; thiếu amount; sai currency; order không khớp. Assert booking, voucher, ledger và pending không thay đổi. Chạy test xác nhận fail trên code hiện tại.
- [ ] Dùng kết quả verify của SDK hiện có, fail closed khi xác minh không thành công. Ping cấu hình không có giao dịch có thể trả ACK theo contract nhưng không capture tiền.
- [ ] Validate success code, currency và amount theo hợp đồng gateway; đúng số tiền snapshot mới capture. Nếu gateway có overpayment hợp lệ, đưa phần lệch vào đối soát, không âm thầm ghi nhận/hoàn thiếu.
- [ ] Test callback hợp lệ chạy được; callback trùng sau PAID hoặc sau các trạng thái phục vụ không lặp tiền/điểm. Regression subscription vẫn pass.

**Verify:** `./mvnw.cmd -Dtest=PayOSWebhookSecurityTest,PayOSServiceTest test`; không gọi PayOS thật.

## Task 2 — Lưu payment attempt bền vững và cấp orderCode dùng chung

**Finding:** R4 + khoảng trống trùng orderCode. **Priority:** P1. **Phụ thuộc:** Task 1.

**Modify:** BE `reservation/service/ReservationService.java`, `payment/service/PayOSService.java`, entity/repository reservation và subscription payment liên quan.
**Create đề xuất:** BE `payment/entity/PaymentAttempt.java`, `payment/abstraction/repository/PaymentAttemptRepository.java`, `payment/service/PaymentAttemptService.java`, `payment/service/BookingPaymentCoordinator.java`, `payment/dto/GatewayPaymentObservation.java`, migration `V39__durable_payment_attempts.sql`.
**Test:** BE `src/test/java/com/moodcafe/payment/PaymentAttemptRecoveryIT.java`.

**Interfaces:**
- `BookingPaymentCoordinator.createReservation(request, actorId, intentKey) -> ReservationResponse`: điều phối ngoài transaction bao trùm network.
- Attempt lưu `attemptId`, `purpose`, `businessId`, `orderCode`, amount/currency snapshot, trạng thái, gateway link ID thật, intent key, timestamps/version.
- `GatewayPaymentObservation` phân biệt `PAID`, `PENDING`, `CANCELLED`, `EXPIRED`, `UNKNOWN`; chứa amount/currency/linkId đã lấy từ gateway khi có. UNKNOWN là kết quả chưa xác định, không tương đương thất bại.

- [ ] Test gateway đã tạo link nhưng response timeout; DB vẫn có reservation/attempt/orderCode bền vững, không tự unlock voucher. Test restart tra lại đúng attempt, không gửi create mới với mã mới.
- [ ] Tách transaction chuẩn bị reservation + snapshot + lock voucher + attempt; commit trước khi gọi SDK. Gọi network ở coordinator không có transaction giữ khóa; transaction ngắn khác ghi kết quả/capture.
- [ ] Dùng nguồn orderCode duy nhất chung booking/subscription, an toàn nhiều instance, unique DB. Migration khởi tạo trên dữ liệu đã có và tránh trùng các mã gateway đã dùng; bỏ hai generator chỉ tra bảng riêng.
- [ ] Lưu gateway paymentLinkId thật, không lấy orderCode thay thế. Phân biệt lỗi tạo xác định và timeout/không rõ; có đường lookup/retry cùng attempt.
- [ ] Test hai booking đồng thời và booking + subscription cùng thời điểm không trùng orderCode; callback tới trước response tạo link vẫn tìm được attempt.
- [ ] Giữ nhánh payable=0 xác nhận nội bộ nguyên tử, không gọi gateway hoặc tạo tiền mặt giả.

**Verify:** Chạy `PaymentAttemptRecoveryIT` với PostgreSQL test; assert rollback/network timeout không làm mất business reference.

## Task 3 — Capture chung cho polling và nhả voucher an toàn

**Finding:** R2, R3. **Priority:** P1. **Phụ thuộc:** Task 1–2.

**Modify:** BE `payment/service/PayOSService.java`, `reservation/service/ReservationPaymentCaptureService.java`, `reservation/scheduler/ReservationLifecycleScheduler.java`, `reservation/abstraction/repository/ReservationRepository.java`, `voucher/service/VoucherLifecycleService.java`, voucher repository.
**Test:** BE `src/test/java/com/moodcafe/payment/BookingPaymentReconciliationIT.java`, `src/test/java/com/moodcafe/voucher/VoucherPaymentRaceIT.java`.

**Interfaces:** `PaymentAttemptService.reconcile(attemptId) -> GatewayPaymentObservation` dùng kết quả Task 2. Polling và webhook PAID đi chung capture. Nếu cần giữ endpoint check boolean cho client cũ, chỉ trả true sau khi đã capture thành công; scheduler dùng typed observation, không dùng boolean.

- [ ] Test mất webhook, polling thấy PAID → reservation PAID_CONFIRMED, voucher USED, merchant pending đúng một lần. Test polling và webhook đồng thời chỉ có một ledger capture.
- [ ] Test gateway timeout/unconfigured/UNKNOWN → giữ attempt và voucher để tra lại, không PAYMENT_EXPIRED. PENDING quá TTL cần yêu cầu cancel rồi xác minh trạng thái cuối trước khi release.
- [ ] Bỏ quét mọi LOCKED chỉ theo holdUntil. Job truy vấn attempt cần xử lý, phân trang; không tải toàn DB bằng findAll. Voucher thật sự mồ côi cần kiểm tra binding/payment trước khi sửa.
- [ ] Release/capture dùng khóa và kiểm tra voucher còn LOCKED đúng reservation/attempt. Không được hồi USED về AVAILABLE, hoặc động tới lock thuộc đơn mới.
- [ ] Late payment sau hủy xác nhận phải tạo bản ghi đối soát/compensation có thể xử lý; không chỉ log rồi bỏ qua khoản tiền đã vào. Không tự phục hồi booking đã giải phóng bàn/voucher.
- [ ] Test job release cạnh tranh callback, khách trả ngay lúc cancel, hai worker và process restart. Invariant: không có một voucher tài trợ hai booking; tiền đã trả luôn có trạng thái xử lý được.

**Verify:** Chạy hai lớp IT với PostgreSQL + gateway adapter giả có kịch bản timeout; kiểm tra ledger/state sau mỗi race.

## Task 4 — Xác thực QR bằng server, không tin storeId hoặc isSelfQr

**Finding:** R5. **Priority:** P1. **Phụ thuộc:** Task 0.

**Modify:** BE `reservation/controller/ReservationController.java`, `reservation/service/ReservationService.java`; tái sử dụng `store/service/VisitVerificationServiceImpl.java` nếu phù hợp cơ chế QR/visit thật.
**Create nếu chưa có tương đương:** BE `reservation/service/ReservationQrVerificationService.java`, DTO check-in/out; FE/MB caller booking thực tế xác định ở Task 0.
**Test:** BE `src/test/java/com/moodcafe/reservation/ReservationQrVerificationIT.java`.

**Interfaces:** `verifyAndConsume(actorId, reservationId, action, token)` trả kết quả xác minh server; action tách CHECK_IN/CHECK_OUT. Token phải có binding và hạn dùng theo cơ chế dự án, chống replay; secret nằm server. Không nhận `isSelfQr=true` làm bằng chứng.

- [ ] Test `STORE:<uuid>`, chuỗi bất kỳ chứa uuid, token giả/hết hạn/sai action/sai store/sai booking và replay đều không chuyển trạng thái hoặc thưởng.
- [ ] Map cơ chế QR hiện tại; dùng challenge/token xác thực có expiry/nonce và consumption nguyên tử. Nếu QR đang là mã tĩnh công khai thì nó không chứng minh có mặt; không báo hoàn tất chống gian lận chỉ bằng ký một mã tĩnh có thể sao chép.
- [ ] Khách đúng booking cần verification hợp lệ. Staff đúng store dùng nhánh actor riêng, không nhận self-checkout bonus. Không cấp thưởng dựa vào role/boolean do client tự gửi.
- [ ] Test cận thời gian nhận bàn và checkout 29:59 không bonus, 30:00 đủ điều kiện; retry sau thành công không thưởng lần hai. Cập nhật caller cùng contract, không để màn hình cũ gửi boolean rồi tự coi thành công.

## Task 5 — Payout: đối soát dùng được, confirm/reject có trạng thái an toàn

**Finding:** R6, R8, R14 + UI reconciliation. **Priority:** P1. **Phụ thuộc:** Task 0.

**Modify:** BE `wallet/service/WithdrawalService.java`, `wallet/controller/AdminWithdrawalController.java`, `wallet/entity/WithdrawalRequest.java`, `wallet/abstraction/repository/WithdrawalRequestRepository.java`; FE `src/services/wallet.service.ts`, `src/app/admin/withdrawals/page.tsx`.
**Create:** Migration kế tiếp để unique reference và audit resolution; BE `src/test/java/com/moodcafe/wallet/WithdrawalResolutionIT.java`, `WithdrawalReferenceConcurrencyIT.java`.

**Interfaces/state:**
- REQUESTED → PROCESSING khi Admin claim.
- PROCESSING chưa rõ chuyển tiền → RECONCILIATION_REQUIRED, **giữ locked**.
- Xác nhận đã chuyển → PAID, complete locked đúng một lần.
- REQUESTED có thể reject theo quyền; PROCESSING/RECONCILIATION_REQUIRED không dùng endpoint reject thường để unlock. Resolution NOT_TRANSFERRED cần kết quả kiểm tra có reference/chứng từ/audit theo quy trình nhóm.
- Thêm endpoint Admin đưa vào đối soát và endpoint resolution có quyết định rõ PAID hoặc VERIFIED_NOT_TRANSFERRED; kiểm tra actor, claim và transition tại service, không chỉ UI.

- [ ] Test A đã chuyển hoặc chưa rõ kết quả rồi gọi reject PROCESSING → không unlock. B không được can thiệp claim của A. Test request/confirm/reject/resolve chạy cạnh tranh.
- [ ] Thực thi state machine, mở API đối soát và UI hành động thật. Không để service `markForReconciliation` chỉ tồn tại mà không có caller. Không mô tả manual proof thành xác minh tự động ngân hàng.
- [ ] Unique transaction reference tại DB đúng contract hiện tại; normalize trim trước lưu. Nếu nhóm có nhiều tài khoản nguồn và reference chỉ unique trong mỗi nguồn thì bổ sung sourceAccountId và constraint composite trước khi cho dùng nhiều nguồn; không dùng mã ngân hàng người nhận làm scope thay thế.
- [ ] Test hai payout của hai store confirm cùng reference đồng thời: chỉ một PAID, lệnh kia giữ trạng thái/số dư trước confirm và có conflict rõ. Kiểm tra dữ liệu trùng cũ trước migration; không xóa ledger để vượt constraint.
- [ ] Confirm retry cùng reference khi đã PAID trả kết quả cũ; khác reference trả conflict. Không debit lần hai; giữ chặn tự duyệt và snapshot người nhận.
- [ ] FE hiện đúng “Cần đối soát”, có giải thích locked và hành động theo quyền/trạng thái. Khi mất response confirm, tải lại trạng thái; không hướng người dùng chuyển tiền lại.

## Task 6 — FE giữ ý định rút tiền qua retry và đổi cơ sở

**Finding:** R7 + race UI store. **Priority:** P1. **Phụ thuộc:** BE idempotency đã có, xác minh lại cùng Task 5.

**Modify:** FE `src/app/merchant/wallet/page.tsx`, `src/services/wallet.service.ts`; tách hook quản lý intent nếu giúp page nhỏ hơn.
**Test đề xuất:** FE `src/app/merchant/wallet/withdrawal-intent.test.tsx` hoặc vị trí tương đương của test runner hiện có. Nếu chưa có runner UI, dùng E2E fixture có kiểm chứng số request/key thay vì thêm test giả.

**Interfaces:** Ý định chứa storeId, bankAccountId, amount và idempotencyKey. Key tạo một lần cho payload đã xác nhận; lỗi response không tạo key khác. BE cùng key/cùng payload trả lệnh cũ, khác payload 409.

- [ ] Tái hiện tạo 100k đã commit nhưng response mất; gửi lại cùng ý định phải chỉ có một payout/100k locked. Không dùng Date.now ngay mỗi submit.
- [ ] Giữ intent/key qua retry; khi reload giữa kết quả chưa rõ, phục hồi intent theo user/store từ storage phù hợp hoặc truy vấn lệnh theo key. Không persist credential/secrets cùng intent.
- [ ] Chỉ kết thúc intent khi kết quả đã được xác nhận; sửa payload/đổi store phải tách ý định và cảnh báo yêu cầu cũ đang xác minh nếu còn. Không xóa dấu vết request đã gửi chỉ vì đóng modal.
- [ ] Reset bank/amount/error theo store; bỏ response cũ bằng request generation, gồm A→B→A. Disable submit trong lúc tải và khi selectedBank không thuộc danh sách hiện tại.
- [ ] Test lỗi A rồi đổi B tải thành công: không còn banner lỗi A. Giữ step=1 và các kiểm tra số nguyên/min/available hiện có.

## Task 7 — Redeem idempotent từ API tới Mobile

**Finding:** R9. **Priority:** P2 nhưng bắt buộc trước nghiệm thu redeem. **Phụ thuộc:** Task 0.

**Modify:** BE `voucher/controller/CustomerVoucherController.java`, `voucher/service/VoucherRedemptionService.java`, `voucher/entity/CustomerVoucher.java`, repository và ledger allocation; MB `lib/features/vouchers/presentation/pages/voucher_catalog_page.dart`, data source/repository và usecase ở Task 11; FE `src/services/voucher.service.ts` nếu dùng API redeem.
**Create:** Migration lưu redemption intent/binding; BE `src/test/java/com/moodcafe/voucher/VoucherRedemptionIdempotencyIT.java`; MB widget test catalog.

**Interfaces:** Redeem nhận `Idempotency-Key` hoặc field intentKey nhất quán. Unique theo actor + operation + key; lưu payload hash và voucher kết quả cùng transaction trừ điểm. Cùng key/template trả voucher gốc; khác payload 409. Client truyền key, không để controller luôn truyền null.

- [ ] Test response mất sau commit, retry cùng key với đủ điểm chỉ trừ một lần và trả cùng voucherId. Test đồng thời cùng key và rollback giữa allocation/save voucher.
- [ ] Lưu intent và kết quả voucher nguyên tử; không chỉ bắt lỗi unique point transaction rồi tạo voucher khác. Tiếp tục dùng allocation nguồn nội bộ, không tạo hai ví UI.
- [ ] Mobile giữ key của một lần xác nhận đổi; timeout hiển thị đang xác minh, đọc lại kết quả/điểm/voucher. Không báo chắc chắn thất bại khi server có thể đã commit.
- [ ] Test retry sau restart và same key/different template; sau thành công refresh authoritative wallet, transactions và vouchers. Intent mới có key mới chỉ khi người dùng thực sự muốn đổi thêm.

## Task 8 — Deadline theo lịch cơ sở và category chưa xóa

**Finding:** R10, R11. **Priority:** P2. **Phụ thuộc:** Task 0 và quyết định P9.

**Modify:** BE `reservation/service/ReservationService.java`, `reservation/abstraction/repository/TableCategoryRepository.java`, `reservation/entity/Reservation.java`, `reservation/scheduler/ReservationLifecycleScheduler.java`; đọc `store/entity/Store.java`.
**Create đề xuất:** BE `reservation/service/ServiceDayDeadlineResolver.java`; `src/test/java/com/moodcafe/reservation/ServiceDayDeadlineResolverTest.java`, `ReservationCategoryValidationTest.java`.

**Interfaces:** `resolve(bookingTime, storeSchedule, zoneId) -> serviceClosingDeadline`; kết quả snapshot khi tạo đơn. Clock inject được để test, không phụ thuộc giờ thật của máy.

- [ ] Test fixture quán 07:00–21:00 đặt 20:00 deadline 21:00 cùng ngày; quán 18:00–02:00 đặt 23:30 deadline 02:00 ngày sau. Các giờ này là fixture, không phải default sản phẩm.
- [ ] Bỏ 23:00 cố định; xác minh booking nằm trong lịch phục vụ. Không tự suy ra opening=closing là 24/7 hoặc tự đặt giờ khi schedule thiếu; dùng rule P9 đã chốt, nếu thiếu báo cấu hình chưa đủ.
- [ ] Test timezone, booking sau nửa đêm trong ca trước, đóng bất thường và job chạy bù theo policy; deadline booking cũ không tự đổi khi sửa giờ quán. Chỉ backfill dữ liệu cũ bằng migration/routine được review riêng.
- [ ] Query category thuộc store, isActive=true, isDeleted=false; validate guestCount/capacity theo booking thật. Test category sai store, inactive, deleted và thiếu ID đều bị từ chối trước khi khóa voucher/gọi gateway.
- [ ] Giữ TABLE_EXPIRED chỉ trả bàn; closing no-show giải ngân một lần, 0 điểm; late claim trước deadline chỉ Spend, không bonus đúng giờ/checkout.

## Task 9 — Nối review có ảnh tới booking và thưởng đúng một lần

**Finding:** R12. **Priority:** P2. **Phụ thuộc:** Task 0, 4 và chính sách P4/P5.

**Modify:** BE `store/service/StoreReviewServiceImpl.java`, `store/service/VisitVerificationServiceImpl.java`, `store/entity/VisitVerification.java`, `loyalty/service/LoyaltyRewardService.java`, reservation/review DTO và quan hệ cần thiết.
**Test:** BE `src/test/java/com/moodcafe/loyalty/VerifiedReviewRewardIT.java`.

**Interfaces:** Award nhận reviewId hoặc event có review/visit/reservation đã lưu; service tự tải bằng chứng, user và trạng thái. Không dùng `hasValidPhoto=true` do caller/client tùy ý làm toàn bộ điều kiện.

- [ ] Test review thật qua production service, có ảnh xác thực và reservation thuộc cùng user/store sau lần check-in hợp lệ → thưởng theo policy một lần.
- [ ] Liên kết visit verification tới booking xác định; không chọn booking bất kỳ gần nhất chỉ theo store. Không tự gán review cũ vào đơn để nhận thưởng.
- [ ] Nối reward trong transaction hoặc durable event/outbox có retry; việc gửi review thành công không làm mất sự kiện thưởng nếu process dừng.
- [ ] Test review không ảnh, chưa check-in/no-show, sai owner/store, reuse visit, sửa review và event lặp đều không thưởng sai/lặp. CLAIMED_LATE xử lý theo P5 đã duyệt; nếu chưa duyệt ghi rõ phần này còn chờ.

## Task 10 — Pagination, error và trạng thái UI

**Finding:** R13 + banner lỗi/sai nhãn đối soát. **Priority:** P2. **Phụ thuộc:** Task 5–6.

**Modify:** FE `src/app/admin/withdrawals/page.tsx`, `src/app/merchant/wallet/page.tsx`, components/hook tách ra nếu cần.
**Test:** Component/E2E theo runner có sẵn, sử dụng dataset và response fixture thật của API.

- [ ] Test 16 REQUESTED, page size=15, xử lý dòng duy nhất trang 2 → tự clamp về trang hợp lệ, tải lại và vẫn truy cập được 15 dòng còn lại.
- [ ] Khi totalPages giảm, không để page vượt giới hạn; filter mới reset page. Cancel/bỏ response cũ khi đổi filter/page nhanh, không để action reload cũ ghi đè filter mới.
- [ ] Test A lỗi → B thành công không giữ banner lỗi; retry không trộn số dư/lịch sử của hai store. Dữ liệu stale nếu hiển thị phải gắn trạng thái rõ.
- [ ] Render đúng RECONCILIATION_REQUIRED, chỉ hành động hợp lệ theo state/claim và quyền. Không dùng fallback “Chờ Admin duyệt” cho trạng thái chưa nhận diện.
- [ ] Kiểm tra UI thật 360/390/768/1280px, tên ngân hàng/quán dài, số dư lớn, modal và bàn phím. Lưu ảnh cho loading/error/empty/success/đối soát; không chỉ kiểm tra build.

## Task 11 — Hoàn tất Clean Architecture Mobile

**Finding:** Khoảng trống Task 8 vòng 1. **Phụ thuộc:** Task 7 chốt contract redeem.

**Modify:** MB `lib/features/wallet/presentation/providers/wallet_providers.dart`, `lib/features/vouchers/presentation/providers/voucher_providers.dart`, hai page wallet/catalog; domain repository và data implementations hiện có.
**Create:** UseCases trong `lib/features/wallet/domain/usecases/`, `lib/features/vouchers/domain/usecases/`; DTOs trong từng `data/models/`; widget tests trong `test/features/wallet/` và `test/features/vouchers/`.

**Interfaces:** Presentation gọi UseCase, UseCase gọi repository interface; repository trả `Result<T>` hiện có. Data source ném typed exceptions, data repository map sang Failure; domain không import Flutter/Dio/storage hoặc chứa DTO JSON hạ tầng.

- [ ] UseCases đọc ví/lịch sử/catalog/voucher và redeem với intentKey; providers/controllers xử lý Ok/Err, không gọi repository trực tiếp từ page.
- [ ] Dùng freezed/json_serializable cho data DTO; mapper sang domain entity. Giữ manual Riverpod, không thêm Riverpod generator.
- [ ] Nếu đổi model, chạy `dart run build_runner build --delete-conflicting-outputs` và review file generate.
- [ ] Widget tests loading/error/retry/empty/success và redeem timeout sau commit; assert không giả 0, không trừ điểm lặp, không crash sau dispose. Giữ các test repository cũ nhưng không coi pass-through test thay thế test nghiệp vụ/UI.

## Task 12 — Đóng các phần còn thiếu của spec và test gate

**Phụ thuộc:** Task 0–11. Không đánh dấu toàn hệ thống hoàn thành nếu chỉ sửa R1–R14.

**Files:** BE `finance/service/ReconciliationService.java`, `finance/controller/AdminCommissionPolicyController.java`, module configuration/voucher admin thực tế; FE/MB booking/admin screens theo mapping Task 0; BE `pom.xml` và cấu hình integration test.

- [ ] Đối chiếu Task 9 vòng 1 với API/UI hiện có. Hoàn thiện phần thiếu config/voucher/checkout theo spec đã duyệt; tái sử dụng AdminCommissionPolicyController đang tồn tại, không tạo controller commission trùng.
- [ ] Bỏ fallback hardcode chính sách không có nguồn duyệt; config thay đổi có audit/effective time và snapshot, không tính lại đơn đã chốt. Liệt kê P1–P10 nào chưa có quyết định nên chưa triển khai, không báo giả hoàn tất.
- [ ] Đối soát theo business reference giữa gateway amount/capture/refund, voucher funding, ledger và payout. Report mismatch có chi tiết; không chỉ sum wallet, không tự sửa số dư để khớp.
- [ ] Thêm PostgreSQL integration/concurrency tests thật bằng DB test biệt lập. Cấu hình Surefire/Failsafe để `*Test` và `*IT` được chạy đúng gate; các Mockito class tên IT không được dùng làm bằng chứng DB transaction/HTTP security đã kiểm thử.
- [ ] Chạy các task test cụ thể trước; cuối cùng BE `./mvnw.cmd verify` sau khi cấu hình gate IT; FE typecheck/lint/build theo package scripts; MB `flutter analyze`, `flutter test`. Testcontainers hay PostgreSQL riêng đều được, nhưng phải ghi rõ phương án và không dùng DB server.
- [ ] E2E qua gateway adapter/test environment: create link response mất → reconcile; polling không webhook; timeout expiry; cancel/refund; voucher re-lock; QR; review; late claim/no-show; payout retry/confirm/reconcile.
- [ ] Đối chiếu số dư/ledger sau mỗi luồng. Fixture gross100k/commission15k/merchant85k chỉ để kiểm thử: voucher REFUND90k → PayOS10k/platform15k; REWARD10k → PayOS90k/platform5k; REFUND80k+REWARD10k → PayOS10k/platform5k, sau khi P10 được duyệt.
- [ ] Bàn giao file diff, migration, config, test command/kết quả, ảnh UI và mục còn thiếu. Không tự commit/push/deploy hoặc đánh dấu xong khi test bị skip/không chạy được.

## Ma trận nghiệm thu

| Finding | Task | Điều kiện đóng lỗi |
|---|---|---|
| R1 callback giả/thiếu dữ liệu | 1 | Không có tác dụng phụ khi verify/amount/purpose sai |
| R2 expiry nhả sai voucher | 3 | UNKNOWN giữ lock, late payment có đường xử lý |
| R3 polling không capture | 3 | Mất webhook vẫn PAID + USED + pending đúng một lần |
| R4 gateway trong DB transaction | 2 | Remote thành công/response mất không mất attempt |
| R5 QR tự dựng/boolean | 4 | Token giả/replay bị chặn, bonus dựa verification server |
| R6 reject PROCESSING hồi tiền sai | 5 | Chưa rõ kết quả giữ locked, có API/UI đối soát |
| R7 FE retry key mới | 6 | Retry cùng intent chỉ một payout |
| R8 trùng reference concurrent | 5 | Constraint DB chặn, không debit hai payout |
| R9 redeem trừ điểm lặp | 7 | Lost response/restart vẫn cùng voucher và một debit |
| R10 closing 23:00 cố định | 8 | Lịch từng cơ sở/ca đêm đúng snapshot |
| R11 category soft-delete | 8 | Category đã xóa không đặt được |
| R12 review reward chưa nối | 9 | Review production flow thưởng đúng một lần |
| R13 trang cuối rỗng | 10 | Xử lý dòng 16 tự về trang hợp lệ |
| R14 confirm retry lỗi | 5 | Same reference trả PAID cũ, không debit thêm |
| OrderCode, kiến trúc, UI/config/đối soát, test gate | 2, 10–12 | Có implementation + bằng chứng riêng, không chỉ scaffold |

## Prompt gửi Antigravity

Thực hiện plan `2026-10-21-antigravity-wallet-remediation-round-2.md` theo thứ tự dependency. Đọc review mới nhất `2026-10-21-antigravity-wallet-rereview.md` và spec `2026-10-20-single-moodpoints-final.md`. Giữ các sửa đúng của vòng 1, ưu tiên P1 về payment/QR/payout; mỗi finding cần regression test và caller production. Kiểm tra đang ở main và giữ nguyên thay đổi chưa commit. Không tự chốt P1–P10, không dùng DB/giao dịch thật, không tự commit/push/deploy. Bàn giao bảng R1–R14 với file, test, kết quả và phần chưa hoàn tất; không coi test mock hoặc build pass là đã nghiệm thu tài chính/UI toàn luồng.
