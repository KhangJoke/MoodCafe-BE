# Hoàn thiện ví, voucher, hoa hồng và đặt bàn — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans hoặc quy trình tương đương của Antigravity. Thực hiện từng task theo dependency và theo dõi bằng checkbox. Mỗi task phải có regression test, caller production và bằng chứng nghiệm thu.

**Goal:** Sửa lỗi hiện tại và hoàn thiện toàn bộ luồng Owner nhận/rút tiền, Customer tích/hoàn điểm và đổi/dùng voucher, cấu hình hoa hồng cùng đối soát theo task gốc.

**Architecture:** Mở rộng booking chính thức đang có; BE quyết định quyền, giá, trạng thái và ledger. Các giao dịch tài chính có idempotency theo actor/operation/payload, transaction ngắn và cơ chế phục hồi khi chưa rõ kết quả gateway. FE/MB có đầy đủ màn hình gọi API thật; Mobile tuân thủ presentation → domain ← data, UseCase/Result/Failure.

**Tech Stack:** Spring Boot/Java, PostgreSQL/Flyway, PayOS/VietQR; Next.js/TypeScript; Flutter/manual Riverpod.

**Spec:**
- `2026-10-22-wallet-task-coverage-review.md`: hiện trạng mới nhất, finding F1–F6 và chức năng còn thiếu.
- `2026-10-20-single-moodpoints-final.md`: nghiệp vụ một ví, các quyết định đã chốt và policy P1–P10 còn mở.
- `2026-10-21-antigravity-wallet-remediation-round-3.md`: chi tiết các lỗi kỹ thuật còn áp dụng. Chỉ thực hiện phần chưa đạt, không sửa lại constructor/vòng service đã được giải quyết.
- Attachment requirement gốc kết thúc tại tiêu đề 3.2; không tự coi các phần không có trong file là đã được duyệt.

Plan này là tài liệu thực thi tổng hợp mới nhất, bao gồm **sửa lỗi và hoàn thiện chức năng**, không chỉ sửa các finding trong review.

## Tóm tắt để duyệt

1. Owner có ví VNĐ theo cơ sở, xem tiền chờ phục vụ/khả dụng/chờ chuyển, quản lý ngân hàng và rút tiền. Admin xử lý bằng VietQR, có chứng từ và đường đối soát khi chưa rõ kết quả.
2. Customer có **một số dư MoodPoints, 1 điểm = 1đ**; hoàn tiền đặt bàn đủ điều kiện vào điểm, không rút/nạp/chuyển tiền. Điểm dùng đổi voucher trước, checkout áp voucher rồi trả phần còn lại PayOS.
3. Hoàn thiện “Voucher của tôi”, điều kiện/hạn dùng, chọn voucher khi đặt bàn, nhánh 0đ, lịch sử và màn hủy nhận điểm.
4. Staff/Cashier có màn booking đúng cơ sở, thấy **gross_amount** để giao đủ nước; không thấy ví/ngân hàng/rút tiền Owner. Check-in/out, giao nước trễ và no-show thực hiện đúng vòng đời.
5. Admin có màn cấu hình hoa hồng global/theo cơ sở, voucher và chính sách điểm/rút tiền; cấu hình được BE sử dụng thật và snapshot vào giao dịch.
6. Ledger/đối soát so với payment, refund, voucher funding và payout; có báo cáo lệch, không chỉ cộng tổng số dư.

**Điều kiện hoàn thành:** Người dùng thực hiện được các luồng trên qua UI thật, các lỗi F1–F6 được đóng và kiểm thử DB/concurrency/E2E đạt. Có model/service hoặc test mock pass chưa đủ.

## Global Constraints

- Phạm vi: BE + FE Admin/Owner/Staff + MB Customer/Owner của plan gốc. Tái sử dụng kiến trúc/theme hiện có; không thiết kế lại Auth hay phần không liên quan.
- Giữ Customer một số dư. Metadata REFUND/REWARD phục vụ nội bộ, không đưa ra hai ngăn tiền/điểm để khách chọn; không thanh toán trực tiếp bằng ví tại checkout.
- Quán được nhận `gross_amount - commission_amount`; voucher không làm giảm phần quán. Discount nguồn REWARD và nguồn REFUND xử lý theo chính sách tài trợ đã duyệt, không dùng cùng một cap tùy tiện.
- BE không tin giá, funding source, số dư hoặc role từ client. VNĐ/điểm nguyên, không tính tài chính bằng float/double; snapshot rate, amounts, deadline và policy version cần thiết.
- Chỉ OWNER active đúng store dùng tài chính Owner; Admin payout có endpoint/quyền riêng. Không chỉ ẩn nút FE để thay thế kiểm tra API.
- Ledger không sửa/xóa; đảo bằng bút toán riêng. Không clamp ngăn tiền thiếu về 0 rồi cộng đủ tiền sang ngăn khác; không tự cân số dư bằng cách phát sinh tiền.
- Network gateway ngoài DB transaction giữ khóa. UNKNOWN không đồng nghĩa thất bại. Retry cùng ý định không tạo booking/voucher/payout thứ hai.
- Kiểm tra main ở cả ba repo; giữ thay đổi chưa commit. Không tự reset/stash/chuyển nhánh, commit/push/deploy hoặc thao tác DB/giao dịch thật trong lượt thực hiện.
- Migration mới sau version cao nhất; không sửa checksum migration đã chạy. Không tắt Hibernate validate, bỏ assertion hoặc skip IT để báo pass.
- FE đọc hướng dẫn Next.js đi kèm repo trước sửa product code; MB dùng manual Riverpod, typed exceptions, repository Result/Failure, UseCase và DTO freezed/json_serializable đúng AGENTS.md.
- Các giá trị business chưa duyệt đặt trong bảng quyết định bên dưới. Không coi constants hiện tại hoặc số ví dụ là quyết định sản phẩm.

## Review Focus

- Base URL đã có `/api`: request ví/voucher/config chỉ có một `/api`, API cũ không bị đổi convention — Task 1.
- Mất response rồi reload/restart: giữ cùng intent và lấy kết quả cũ, không trừ/khóa tiền thêm — Task 2–3, 5–7.
- Webhook/job/cancel đồng thời: đúng một trạng thái cuối, không hồi USED về AVAILABLE và không bỏ khoản tiền đã thu — Task 3.
- Admin khác hoặc kết quả ngân hàng không rõ: không giải phóng locked trái quyền/thiếu bằng chứng — Task 5.
- Cấu hình đổi sau khi đã tạo đơn/voucher: không tính lại lịch sử; paging/filter/store đổi nhanh không lộ hoặc ghi đè dữ liệu — Task 8–11.

## Bảng chính sách cần có nguồn duyệt

Đây là danh sách quyết định đầu vào, không yêu cầu hỏi lại lựa chọn một ví/1đ=1điểm đã chốt. Anti cần tìm quyết định nhóm đã có; nếu chưa có, ghi rõ mục chờ và tiếp tục các task độc lập. Phần phụ thuộc chưa chốt không được tự bật trên dữ liệu thật hoặc báo hoàn thành.

| Nhóm | Cần chốt/đối chiếu | Không tự dùng làm mặc định |
|---|---|---|
| P1 | Phạm vi ví/quyền và tổng hợp theo Owner | Mô hình hiện tại mỗi Store một ví là hướng đang triển khai |
| P2/P3 | Catalog mệnh giá, point_cost=face_value theo lựa chọn 1:1; expiry voucher/điểm; hoàn backing REFUND khi voucher hết hạn | Voucher60ngày, REWARD365ngày chưa có nguồn duyệt |
| P4/P5 | Earn rate/cap, bonus check-in/checkout/review, reversal; review sau CLAIMED_LATE | 1%, cap50k, bonus1k/1k/2k chưa có nguồn duyệt |
| P6 | Free cancel, late/partial/store-fault refund và reward đã phát | 2 giờ không tự coi là đã duyệt |
| P7/P10 | Commission/default/store override/rounding; REFUND/REWARD allocation và subsidy cap | 15% là ví dụ; không mặc định chặn voucher REFUND bằng commission |
| P8 | Min/max/phí/SLA/bank verification/claim handoff payout | 100k và24h trên UI chưa có nguồn duyệt |
| P9 | Giờ mở cửa, ca đêm,24/7, đóng bất thường và boundary | Không tự gán 07:00–22:00 khi thiếu lịch |

## Bản đồ file và mốc nghiệm thu

Prefix: **BE** `D:/Learn/MoodCafe-BE/moodcafe-be/`; **FE** `D:/Learn/MoodCafe-FE/`; **MB** `D:/Learn/MoodCafe-MB/`.

File/route mới dưới đây là đề xuất triển khai. Nếu có thành phần tương đương, sửa hoặc mở rộng nó; không tạo module trùng. Đường dẫn booking cụ thể phải khóa lại trong Task 0 vì checkout hiện tại chưa thấy caller flow mà nhóm nói đang có.

- **Mốc A:** API đúng URL, backend migrate/startup test đạt, invariants/idempotency được bảo vệ.
- **Mốc B:** Owner/Admin payout và Customer wallet/voucher/booking/refund/Staff phục vụ chạy qua UI.
- **Mốc C:** Cấu hình, expiry/reward, đối soát và toàn bộ acceptance tests đạt; policy chưa chốt vẫn phải nêu rõ.

## Task 0 — Xác định booking chính thức, API contract và policy

**Files:** Spec/review, branch/ref và route/controller/service booking hiện có trong ba repo; BE `pom.xml`, migrations; FE/MB router hiện tại.

**Deliverable:** Tài liệu mapping gồm canonical booking entity/status/API, caller Customer/Staff, schema/migration đã chạy, task dependencies và policy source. Không cần dựng một spec mới khác hướng một ví.

- [ ] Đọc hướng dẫn repo, kiểm tra main/diff. Tìm flow booking thật ở source/ref có sẵn bằng đọc lịch sử; không tự merge nhánh khác hoặc tạo booking thứ hai.
- [ ] Nếu nguồn flow chưa ở checkout, ghi cụ thể branch/module cần có và các điểm ghép; hoàn thành phần tài chính độc lập. Không báo đã nối booking khi chưa có caller thật.
- [ ] Map reservation mới với flow chính thức, chọn một nguồn dữ liệu/trạng thái. Ghi đường dẫn màn booking/Cashier thực tế để các Task 7–8 dùng, thay placeholder trước khi code phần đó.
- [ ] Lập bảng endpoint/DTO gồm amount nguyên, paging, error code, intent replay/query, policy snapshot và quyền từng role. Sửa contract ở một nơi và cập nhật mọi caller cùng task.
- [ ] Đối chiếu bảng P1–P10; lưu quyết định đã có hoặc mục đang chờ. Không dùng policy thiếu để trì hoãn URL/ownership/locks đã rõ.

## Task 1 — Sửa URL API và smoke test contract

**Finding:** F1. **Priority:** P1. Có thể làm ngay sau inventory.

**Modify FE:** `src/services/wallet.service.ts`, `src/services/voucher.service.ts`; đọc `src/lib/axios.ts`, `.env.example`.
**Modify MB:** `lib/features/wallet/data/datasources/wallet_remote_data_source.dart`, `lib/features/vouchers/data/datasources/voucher_remote_data_source.dart`; đọc `lib/core/config/app_config.dart`, `lib/core/network/dio_client.dart`.
**Tests:** FE financial URL contract tests theo runner hiện có; MB `test/features/wallet/data/datasources/wallet_remote_data_source_test.dart`, voucher tương ứng.

**Contract:** Base URL hiện có `/api`; module service dùng `/me/wallet`, `/merchant/stores/...`, `/admin/withdrawals`, `/voucher-catalog`, `/me/vouchers`, không thêm `/api` lần hai. Các service tài chính mới sau này theo cùng convention.

- [ ] Test URL sau khi Axios/Dio ghép base + path, gồm query/page và POST headers. Assert có đúng một `/api`, không `/api/api`; tái hiện fail trước sửa.
- [ ] Sửa module mới theo convention cũ, không đổi base URL toàn app để chữa riêng ví. Regression một API cũ Auth/Store vẫn giữ đúng URL.
- [ ] Smoke gọi API qua môi trường test với role đúng; xác minh 200/403 theo contract, không chỉ getUri hoặc typecheck.

**Nghiệm thu:** UI wallet/catalog/payout không còn lỗi route do prefix; test kiểm tra request thật hoặc adapter capture URL đã resolve.

## Task 2 — Hoàn tất nền DB, idempotency và invariants số dư

**Finding:** F5/F6 và phần còn thiếu của mốc A. **Phụ thuộc:** Task 0.

**Modify BE:** `payment/entity/PaymentAttempt.java`, `payment/abstraction/repository/PaymentAttemptRepository.java`, `payment/service/PaymentAttemptService.java`, `OrderCodeGeneratorService.java`, `BookingPaymentCoordinator.java`; `wallet/service/MerchantWalletService.java`, transaction repositories/entities; `voucher/service/VoucherRedemptionService.java` và redemption entity/repository.
**Create:** Migration kế tiếp cho constraints/business keys, backfill có kiểm chứng; `FinancialSchemaStartupIT`, `FinancialIntentConcurrencyIT`, `MerchantWalletInvariantIT` trong cây test BE.

**Interfaces:** Actor + operation/purpose + intentKey + payloadHash quyết định replay; cùng key/khác payload trả409. Financial event có unique business reference theo store/user + loại event + reference liên quan. Transaction ledger/balance/state cùng commit hoặc rollback.

- [ ] Test migrate PostgreSQL + Hibernate validate + context thật; giữ những sửa constructor/vòng dependency đã đạt, không làm lại hoặc tắt validate.
- [ ] Thêm unique DB cho payment intent theo actor/purpose/key, kiểm tra ownership trước replay; backfill đúng actor/purpose, isolate legacy record không xác định thay vì gán bừa.
- [ ] Redeem compare template/payloadHash; concurrent cùng key chỉ một debit/voucher và trả kết quả cũ. Xử lý conflict sau rollback bằng transaction mới, không tiếp tục transaction đã lỗi.
- [ ] Merchant settle/unlock/revert phải kiểm tra đủ số dư nguồn. Thiếu tiền → rollback và tạo cảnh báo đối soát ngoài transaction thất bại bằng cơ chế bền vững; không clamp rồi cộng đủ, không silently bỏ đảo pending mà vẫn refund.
- [ ] Ghi đủ delta/source/destination cho chuyển pending/available/locked để tái dựng và đối chiếu; chống event reference lặp bằng constraint, không chỉ kiểm tra tồn tại trước ghi.
- [ ] Giữ prepare reservation + voucher lock + attempt chung REQUIRED transaction; gateway ngoài transaction này. Bỏ sequence fallback AtomicLong trùng qua restart/instance; khi không cấp mã bền vững, fail trước gateway.
- [ ] Test simultaneous intents, rollback giữa state/ledger/balance, retry business event và nguồn tiền thiếu. Không mock repository cho các test constraints/locks này.

**Mốc A một phần:** chỉ đạt khi schema/context và test invariant thật pass; test tên IT nhưng dùng Mockito không thay thế.

## Task 3 — Payment/voucher/refund lifecycle có thể phục hồi

**Finding:** F2/F5. **Phụ thuộc:** Task 2.

**Modify BE:** `payment/service/BookingPaymentCoordinator.java`, `PaymentAttemptService.java`, `PayOSService.java`; `reservation/service/ReservationPaymentCaptureService.java`, `reservation/scheduler/ReservationLifecycleScheduler.java`; `voucher/service/VoucherLifecycleService.java`, voucher repository; `finance/service/ReservationRefundService.java`.
**Tests:** `PaymentAttemptRecoveryIT`, `BookingPaymentReconciliationIT`, `VoucherCaptureReleaseConcurrencyIT`, `ReservationRefundLifecycleIT`.

**Contract:** Resume theo trạng thái workflow, không chỉ trả record cũ. PAID không lùi UNKNOWN; expiry phải biết link không còn thu tiền. Release/capture chỉ động đúng voucher/attempt/reservation và transition hợp lệ dưới khóa/CAS.

- [ ] Test dừng sau prepare trước gateway, remote tạo link nhưng response mất, URL chưa lưu, zero-pay chưa capture: retry cùng key tiếp tục được, không sinh booking/QR mới. Nếu chưa rõ, trả VERIFYING cùng reference để client tra lại.
- [ ] Webhook verify fail closed, kiểm tra order/amount/currency/purpose; webhook/polling cùng capture idempotent, actual gateway paymentLinkId lưu đúng. PAID booking/USED voucher/pending/ledger nhất quán trong transaction.
- [ ] Bỏ vòng quét orphan nhả LOCKED chỉ theo TTL. Đơn20phút/PENDING/UNKNOWN giữ voucher; quá30phút vẫn không suy ra thất bại. Cancel gateway và xác minh cuối trước khi release; network ngoài khóa.
- [ ] Capture/release/refund restore recheck binding và state dưới khóa, thống nhất thứ tự khóa; không USED→AVAILABLE qua release thường, không job đơn cũ ảnh hưởng lock đơn mới.
- [ ] Hủy đủ điều kiện: đảo pending, hoàn phần PayOS thực thu thành điểm1:1 đúng một lần, voucher xử lý riêng. Thiếu source balance không được vẫn phát refund. Phần voucher không vừa restore vừa quy thành điểm.
- [ ] Late payment sau cancel/expire có record đối soát/compensation, không log rồi bỏ tiền; policy partial/late/store-fault chưa duyệt không tự áp tỷ lệ.
- [ ] Test PostgreSQL callback/job/cancel concurrent, retry/restart và zero-pay. Query job theo deadline/status phân trang, không findAll toàn DB.

## Task 4 — Deadline, QR và đủ nguồn thưởng có xác thực

**Phụ thuộc:** Task 0/3 và policy P4/P5/P9.

**Modify BE:** `reservation/service/ServiceDayDeadlineResolver.java`, `ReservationQrVerificationService.java`, `ReservationService.java`, reservation controller/DTO; `finance/service/ReservationSettlementService.java`; `loyalty/service/LoyaltyRewardService.java`; `store/service/VisitVerificationServiceImpl.java`, `StoreReviewServiceImpl.java`, visit/review entities và migration binding.
**Tests:** deadline/QR/reward integration tests, Clock có thể inject.

- [ ] Chặn booking ngoài ca, giờ quá khứ hoặc thiếu lịch hợp lệ trước lock/gateway. Hỗ trợ ca đêm/24h/đóng bất thường theo P9; snapshot deadline, không đổi đơn cũ khi sửa lịch.
- [ ] Giữ TABLE_EXPIRED chỉ trả bàn và giữ pending; CLAIMED_LATE trước closing giải ngân + Spend; closing no-show giải ngân,0điểm. Test job chạy bù và staff claim cạnh tranh job.
- [ ] QR có actor/booking/store/action/expiry/nonce và consume nguyên tử cùng transition. Bỏ secret mặc định/log expected HMAC; có production issuance lẫn verify. Daily token chụp lại không được coi là proof có mặt; không tin `isSelfQr=false/true` để tự đổi actor.
- [ ] Spend tính gross theo rate/cap đã duyệt khi COMPLETED hoặc CLAIMED_LATE; check-in đúng giờ mới bonus; self QR checkout29:59 không bonus,30:00 đủ điều kiện; staff checkout không self bonus.
- [ ] Verified review cần ảnh và visit verification gắn đúng user/store/booking; ảnh bất kỳ + reservationId chưa đủ. Reward tối đa một lần/booking, có caller production và xử lý event retry bền vững.
- [ ] CLAIMED_LATE review theo P5 đã duyệt; không tự gộp thành check-in/checkout thưởng. Có policy snapshot và kế hoạch reversal khi review/refund thay đổi theo rule.

## Task 5 — Hoàn thiện ví Owner, ngân hàng và payout Admin

**Finding:** F3/F4. **Phụ thuộc:** Task 1–2; policy P1/P8.

**Modify BE:** `wallet/service/WithdrawalService.java`, `StoreFinancialSecurityService.java`, merchant wallet/withdrawal controllers, `AdminWithdrawalController.java`, bank/payout entities/repositories.
**Modify FE:** `src/app/merchant/wallet/page.tsx`, `src/app/admin/withdrawals/page.tsx`, `src/services/wallet.service.ts`, `src/components/admin/shell/AdminSidebar.tsx`, OwnerSidebar.
**Create MB:** Owner wallet feature dưới `lib/features/merchant_wallet/` theo Clean Architecture; nối merchant settings/router. Không dùng Customer wallet page làm ví quán.

**Interfaces:** API list/add/update/delete/default bank; delete mềm và snapshot payout cũ bất biến. Payout intent có create/replay/query theo key và owner/store. Reconciliation tôn trọng claim hoặc chuyển giao có audit theo policy, không có override ngầm cho Admin khác.

- [ ] OWNER exactStore mới dùng wallet/bank/payout; MANAGER/CASHIER/Customer/Owner store khác403. MB role switch không mở quyền tài chính nếu storeRole không OWNER.
- [ ] Bank CRUD/default atomic; unique một default active/store. Missing isDefault không tạo nhiều default. Kiểm tra bankCode/account data và verification theo P8, không chỉ lưu tên tự nhập rồi tuyên bố đã xác minh.
- [ ] FE/MB Owner có số dư, lịch sử có paging, ngân hàng và tạo/tracking payout; min/max/phí/SLA lấy config BE, không ghi cứng100k/24h. Đổi store reset đúng phạm vi và không nhận response cũ.
- [ ] Client lưu durable intent theo user/store/payload trước POST, phục hồi khi đóng/mở/reload/restart. Timeout VERIFYING; tra/replay cùng key, không tự sinh key mới. Logout/user switch không lộ intent người khác.
- [ ] Claim/reconcile/resolve kiểm tra claimedBy và transition tại BE. UNKNOWN giữ locked; resolve NOT_TRANSFERRED bắt buộc verification reference/evidence/notes theo quy trình, không default câu đã xác minh. Confirm PAID có unique bank reference; same-intent retry trả kết quả cũ.
- [ ] Thêm link payout Admin vào sidebar; hiển thị VietQR thật từ snapshot bankCode/account/amount/nội dung đúng payout. QR là hướng dẫn chuyển thủ công, không tự đánh PAID khi QR được mở. Chứng từ/reference và ledger chỉ xác nhận theo bước Admin.
- [ ] Test hai Admin, duplicate reference, bank đổi sau create, bank default concurrent, lost response và role boundaries trên API/UI. Tiền thật không được dùng để kiểm thử.

## Task 6 — Hoàn thiện Customer wallet, catalog và Voucher của tôi

**Phụ thuộc:** Task 1–3; policy P2/P3. Clean Architecture là một phần task, không để UseCase scaffold không có consumer.

**Modify MB:** wallet/voucher providers/pages, domain repositories/usecases, data sources/repositories/models hiện có; `lib/app/router/app_routes.dart`, `app_router.dart`, profile page.
**Create MB:** `lib/features/vouchers/presentation/pages/my_vouchers_page.dart`, data DTO freezed/json_serializable trong `data/models/`, widget/controller tests. Route đề xuất `myVouchers('/profile/vouchers')`; giữ routes moodPointsWallet/voucherCatalog hiện có.
**Modify BE:** `CustomerWalletController`, `CustomerVoucherController`, redemption/status query APIs để cung cấp đầy đủ DTO và paging.

- [ ] Wallet một số dư hiển thị rõ1điểm=1đ, tổng khả dụng và lịch sử; không có nút rút/nạp/chuyển hoặc toggle tiêu ví. Loading/error/empty/stale tách biệt, không giả0 khi API lỗi.
- [ ] Catalog hiển thị mệnh giá thực, point cost, hạn dùng/điều kiện áp dụng trước xác nhận. Rule do BE trả, không tính applicability từ text hardcode. Không đưa funding REFUND/REWARD thành lựa chọn cho khách.
- [ ] My vouchers có AVAILABLE/LOCKED/USED/EXPIRED theo nhãn dễ hiểu, giá trị, expiry, điều kiện và paging; LOCKED gắn đơn liên quan để mở theo dõi khi đúng quyền. Redeem xong nhìn thấy voucher mới.
- [ ] Redeem dùng durable intent như payout: restart/retry cùng key trả cùng voucherId, khác template409. Repository/usecase trả kết quả voucher/intent, không void làm mất reference. Refresh authoritative balances/history/vouchers sau xác nhận.
- [ ] Presentation chỉ gọi UseCase; repo trả Result/Failure, typed exception map tại data; DTO JSON không nằm domain. Các UseCase hiện có phải được dùng thực tế. Paging giữ total/hasNext, không chỉ list trang0.
- [ ] Test widgets/production wiring: loading/retry,21+history,10+voucher, insufficient points, response mất/restart, dispose giữa request và user switch. Regenerate models khi thay đổi, giữ manual Riverpod.

## Task 7 — Nối voucher/payment/refund vào booking Customer đang dùng

**Phụ thuộc bắt buộc:** Mapping canonical booking Task 0 và Task 1–4/6; không tạo entity/flow đặt bàn thứ hai.

**Files:** Các page/controller/usecase booking thật ghi rõ trong mapping Task 0; BE reservation coordinator/controller/DTO, pricing/refund; thêm service booking FE hoặc datasource/usecases MB trong feature booking hiện hữu nếu thiếu.

**Contract:** Quote BE trả đủ gross, voucher discount, payable, conditions, quote/policy version; tạo booking kiểm tra lại và snapshot. Customer không truyền merchant net/commission để BE tin. Intent stable cho một lần đặt; endpoint status/detail/cancel theo ownership.

- [ ] Màn đặt bàn chọn category/khung giờ và voucher phù hợp; server xác nhận điều kiện, lý do không áp được và giá cuối. Đổi input thì quote mới, không dùng payable cũ.
- [ ] Hóa đơn Customer hiển thị gross, giảm voucher, số cần trả. Payable>0 tạo/hiển thị link/QR PayOS đúng amount; return URL chỉ kích hoạt tra BE, không tự coi paid. Polling/callback cùng nguồn trạng thái.
- [ ] Payable=0 bỏ PayOS và xác nhận nội bộ, giữ capture/voucher/ledger đúng. Response mất/reload/resume không tạo đơn mới; VERIFYING có trạng thái theo dõi được.
- [ ] Booking detail/my bookings cho xem trạng thái, voucher và điều kiện hủy snapshot. Hủy đủ điều kiện báo số điểm hoàn chính xác, không tiền mặt; refresh ví và voucher sau BE xác nhận.
- [ ] Nối check-in/out QR đã xác minh và review có visit binding; không cấp điểm từ client. Có lịch sử để thấy điểm chi tiêu/bonus/refund tương ứng.
- [ ] Nếu nguồn booking vẫn chưa có trong checkout, ghi task này chưa hoàn thành với dependency cụ thể; không dùng màn demo hoặc data giả thay cho luồng yêu cầu.

## Task 8 — Hoàn thiện FE Staff/Cashier phục vụ booking

**Phụ thuộc:** Task 0/3/4/7; sử dụng booking chính thức.

**Create/Modify FE:** Route đề xuất `src/app/merchant/reservations/page.tsx`, components dưới `src/components/merchant/reservations/`, service booking canonical, OwnerSidebar/menu theo storeRole.
**Modify BE:** Booking list/detail/actions theo staff store access, DTO riêng cho staff; không trả toàn entity tài chính vào cashier endpoint.

- [ ] Owner/Manager/Cashier đúng store xem danh sách ngày/phân trang/filter và chi tiết booking; mỗi action có actor permission tại BE. Staff store khác403.
- [ ] Thu ngân thấy gross_amount/hạn mức nước phải phục vụ, guest/category/time/trạng thái; không cần thấy voucher funding/hoa hồng/merchant net/bank/withdrawal. Dùng DTO đúng mục đích, không chỉ CSS ẩn field.
- [ ] Thao tác nhận bàn, staff checkout và giao nước trễ trước closing; xác nhận thành công dựa BE, retry không giải ngân/thưởng hai lần. TABLE_EXPIRED không tự hiểu đã hủy quyền lấy nước.
- [ ] Cashier không nhận bonus self-QR thay Customer; hết closing không claim trễ, job no-show0điểm đúng rule.
- [ ] Test thao tác chéo store/role, mốc thời gian, job/action concurrent và hiển thị gross100k dù Customer trả90k hoặc10k do voucher.

## Task 9 — Config hoa hồng và snapshot từng đơn có UI sử dụng

**Phụ thuộc:** Task 0/2; policy P7/P10. Có thể hoàn thiện API/UI độc lập booking UI sau khi contract rõ.

**Modify BE:** `finance/controller/AdminCommissionPolicyController.java`, `finance/entity/CommissionPolicy.java`, `CommissionPolicyRepository.java`, `finance/service/ReservationPricingService.java`; thêm service validation/audit nếu controller đang làm trực tiếp repository.
**Create FE:** `src/services/commission.service.ts`, `src/app/admin/commission-policies/page.tsx`; link AdminSidebar và form theo theme Admin hiện có.

- [ ] Admin list/create/enable chính sách global/store, rate/effective period; kiểm tra0≤rate≤1, valid dates, store tồn tại và quy tắc policy chồng lấn/ưu tiên rõ. Non-Admin403.
- [ ] FE nhập % dễ hiểu nhưng gửi rate đúng đơn vị; chọn store, xem chính sách đang có hiệu lực và lịch sử/audit. Không gọi generic config editor mà BE quote vẫn đọc constants khác.
- [ ] Quote ưu tiên store override→global theo effective time đã chốt; thiếu default hợp lệ phải báo rõ, không âm thầm lấy15% ví dụ. Snapshot policy ID/version/rate/rounding cùng năm amount vào booking.
- [ ] Thay config không đổi đơn đã snapshot. Test global/store, ngày hiệu lực, disable, overlap, invalid rate và fixture100k/15% → commission15k/merchant85k, không mặc định15% sản phẩm.
- [ ] Cashier vẫn dùng gross; Customer payable và merchant net không bị tính lại ở FE/MB. Giá trị platform revenue/funding dùng cho nội bộ đúng chính sách nguồn điểm.

## Task 10 — Admin voucher/loyalty/payout policies và expiry thực sự chạy

**Phụ thuộc:** Task 0/2/3/9; P2–P8/P10 đã có quyết định cho nhánh tương ứng.

**Modify BE:** `voucher/entity/VoucherTemplate.java`, template repository/catalog; `loyalty/service/LoyaltyRewardService.java`, `wallet/service/CustomerWalletService.java`, `WithdrawalService.java`, `finance/service/ReservationRefundService.java`; tái sử dụng `configuration/abstraction/service/SystemConfigurationService.java` cho config runtime phù hợp.
**Create BE nếu thiếu:** Admin voucher template controller/service và typed financial/loyalty policy service, expiry worker; migration config version/audit cần thiết.
**Create FE:** `src/app/admin/vouchers/page.tsx`, `src/app/admin/loyalty/page.tsx`, services/hooks tương ứng; payout policy có thể đặt tab cùng khu vực tài chính, không thêm một editor rời không được dùng.

- [ ] Voucher template CRUD/activate/deactivate có validation mệnh giá/point cost/eligibility/validity/budget theo policy. Không sửa điều kiện voucher cá nhân đã đổi; lưu snapshot khi redeem.
- [ ] Loyalty policy cấu hình earn rate/cap, bonuses, expiry/reversal; payout policy min/max/phí/SLA và refund free-cancel theo quyết định đã duyệt. Service đọc policy typed thật; bỏ các constants dùng làm quyết định ngầm.
- [ ] Config có version/audit/effective time, validation server, UI phân biệt draft/chưa có policy với giá trị0. Không tạo thêm approval workflow kinh doanh ngoài yêu cầu; quyền Admin và nguồn quyết định cần rõ.
- [ ] Worker xử lý AVAILABLE voucher hết hạn và expired reward lots theo P3/P4, không chỉ expiry khi booking release/refund. Nếu hoàn REFUND backing thì một lần duy nhất theo voucher/allocation, không cả USED hoặc đã restore.
- [ ] Đồng bộ pointsBalance/spendablePoints/lot khi expiry; không để UI tổng điểm khác giá trị dùng được mà không giải thích. REFUND không bị hết hạn nếu policy bảo toàn này được duyệt.
- [ ] Test policy đổi sau redeem, expiry/booking lock cùng lúc, cron chạy bù/retry, lot mix REFUND/REWARD và ngưỡng commission. Không tự mở marketing vượt ngân sách khi chưa được duyệt.

## Task 11 — Ledger reconciliation có nguồn và màn Admin xử lý lệch

**Phụ thuộc:** Task 2–5/9–10.

**Modify BE:** `finance/service/ReconciliationService.java`, transaction repositories, payment/refund/voucher/payout records; bổ sung job/report/audit entities nếu cần.
**Create FE:** `src/app/admin/finance/page.tsx`, `src/services/finance.service.ts`, AdminSidebar navigation.

**Contract:** Report có cutoff/time range, currency/unit, business references, expected/actual/difference và status. Phân biệt việc Admin đối soát một payout với việc đối soát toàn hệ thống; tổng số dư không phải bằng chứng cân tiền.

- [ ] So booking financial snapshots + gateway captures/refunds + payout với merchant ledger và pending/available/locked; so customer point ledger/remaining lots/voucher allocations và obligations đúng đơn vị. Không cộng VNĐ với điểm rồi coi đã cân chỉ vì tỷ lệ1:1.
- [ ] Kiểm tra invariant chuyển ngăn tổng tiền không tự tăng, unique event, nghĩa vụ nguồn REFUND không biến mất hoặc ghi hai lần; reward subsidy được tách khỏi khoản tiền hoàn đã có backing.
- [ ] Gateway network/extract ngoài transaction giữ khóa; report dùng cutoff/snapshot nhất quán, ghi dữ liệu chưa xác minh thay vì báo mismatch giả do query các thời điểm khác nhau.
- [ ] FE Admin xem summary và drill-down mismatch tới payment/booking/payout, filter/page/export nếu cần cho review nhóm; quyền Admin server. Không đưa thao tác “sửa số dư” tùy ý, adjustment nếu có phải bút toán/audit được xác nhận riêng.
- [ ] Test dữ liệu cân, thiếu capture, duplicate ledger, voucher backing thất lạc, bank reference trùng và gateway UNKNOWN. Report phải tìm đúng reference, không chỉ thay đổi tổng.

## Task 12 — UI nhất quán, paging và chống stale response

**Phụ thuộc:** Các task có màn hình/API tương ứng.

**Modify:** FE merchant wallet/admin withdrawals và tất cả màn tài chính mới; MB wallet/voucher/booking/merchant wallet controllers/pages.

- [ ] Dùng request generation/abort cho effect lẫn reload/action; A→B→A hoặc refresh A cũ không ghi đè A mới, filter/page cũ không ghi rows/totalPages/loading vào selection mới.
- [ ] Owner transactions/withdrawals, point history/My vouchers, Staff bookings và Admin reports đều xem được trang tiếp; giữ fix clamp trang cuối sau action. Không chỉ fetch page0/20 rồi bỏ metadata.
- [ ] Loading/error/empty/permission/VERIFYING tách biệt. Quy trình không hiển thị chi tiết kỹ thuật ledger/funding/stacktrace nếu không giúp người dùng ra quyết định.
- [ ] Owner dùng theme Owner, Admin dùng theme Admin hiện có; Customer/MB giữ theme ứng dụng. Không thay đổi theme toàn hệ thống trong đợt này.
- [ ] Kiểm tra FE360/390/768/1280px, Mobile chữ lớn và bàn phím: modal không tràn, số tiền/tên quán/ngân hàng dài đọc được, hành động truy cập bằng bàn phím, status dễ hiểu.
- [ ] Lưu screenshot thật cho mỗi role và state quan trọng. Nếu chưa render trực tiếp, báo chưa verify UI; build pass không thay thế kiểm tra này.

## Task 13 — Gate kiểm thử, tài liệu vận hành và bàn giao

**Phụ thuộc:** Task 0–12.

**Modify BE:** `pom.xml`, profile/test fixtures; dùng PostgreSQL test biệt lập và gateway giả hoặc sandbox không tiền thật.
**Test locations:** BE `src/test/java/com/moodcafe/...`; MB `test/features/...`; FE test/E2E setup theo runner hiện có, bổ sung browser E2E tối thiểu nếu chưa có. Không gọi test repo pass-through là test full flow.

- [ ] Gate migration/startup: Flyway toàn bộ + Hibernate validate + context thật; configure Surefire unit/Failsafe IT để verify không bỏ qua *IT. Test tên IT nhưng Mockito ghi rõ là unit-level.
- [ ] Gate quyền: Customer/Owner/Manager/Cashier/Admin, inactive staff, store khác, direct API và UI route đều đúng; chưa có quyền phải không có side effects.
- [ ] Gate concurrency PostgreSQL: same intent, capture/release, cancel/refund, expiry, same bank reference, default bank và payout resolve. Khẳng định số record/balance/ledger, không chỉ HTTP200.
- [ ] Gate E2E Customer: điểm→voucher→booking→PayOS/0đ→check-in/out→thưởng; cancel→điểm+voucher theo rule; UNKNOWN/resume; no-show/late claim/review.
- [ ] Gate E2E Owner/Admin: chọn store/bank→payout→mất response/restart→claim→QR/confirm hoặc reconciliation; verify không tăng available khi chưa chứng minh chưa chuyển.
- [ ] Gate config: thay commission/template/loyalty sau giao dịch không đổi snapshot cũ; report reconciliation cân và tìm đúng mismatch giả lập.
- [ ] BE `./mvnw.cmd verify`; FE `npx tsc --noEmit`, lint file thay đổi và `npm run build`; MB `flutter analyze`, `flutter test`, regenerate DTO nếu cần. Full FE lint có lỗi có sẵn/generated phải phân loại, không báo full pass khi chưa pass hoặc sửa lan không liên quan.
- [ ] Xác minh Maven dependency/cache khi packaging lỗi, không downgrade thư viện tùy ý để né lỗi cache; ghi lệnh, exit code, test count và skip chính xác.
- [ ] Bàn giao API contract, migration/config cần bổ sung, cách chạy worker, xử lý UNKNOWN/mismatch, screenshots và bảng coverage requirement. Không tự deploy/push/seed server hoặc gọi chuyển tiền thật.

## Bộ acceptance bắt buộc

| Case | Kết quả cần chứng minh | Task |
|---|---|---|
| URL base đã có /api | FE/MB chỉ gửi một /api; API cũ không hỏng | 1 |
| Hạn mức100k, rate15%, voucher REWARD10k | PayOS90k, merchant85k, platform5k; Cashier thấy100k | 3,7–9 |
| Voucher REFUND90k, đơn100k, rate15% | PayOS10k, merchant85k; backing/refund không bị cap sai theo commission | 3,7,9–11 |
| Voucher đủ100% payable | Không gọi PayOS; booking/voucher/pending đúng một lần | 3,7 |
| Hủy hợp lệ đã trả90k | +90k điểm, đảo pending, voucher xử lý riêng, không tiền mặt/hoàn trùng | 3,7 |
| PayOS pending20phút/UNKNOWN | Không nhả voucher sai; resume cùng attempt | 3 |
| Callback/job/expiry đồng thời | Không USED→AVAILABLE hoặc tài trợ hai booking | 2–3,10 |
| Owner rút100k, response mất, reopen/reload | Một payout, locked tăng100k một lần;100k chỉ fixture | 5 |
| Redeem response mất/restart | Cùng voucherId, một debit; khác template cùng key409 | 2,6 |
| Admin B can thiệp claim A | B bị chặn; UNKNOWN giữ locked, resolution cần bằng chứng | 5 |
| Checkout29:59/30:00 | Không/có self bonus khi QR hợp lệ, staff không self bonus | 4,8 |
| TABLE_EXPIRED/CLAIMED_LATE/NO_SHOW | Lần lượt giữ pending/settle+Spend/settle+0điểm | 4,8 |
| Policy đổi sau tạo đơn/voucher | Snapshot cũ giữ nguyên, giao dịch mới dùng policy mới | 9–10 |
| 21+history/A→B→A/đổi filter | Xem đủ trang và không bị response cũ ghi đè | 12 |
| Mismatch tài chính giả lập | Admin tìm được nguồn/reference lệch, không tự sửa số dư | 11 |

Các giá trị trong case là test fixture để xác minh công thức, không tự cấu hình production theo fixture. Case phụ thuộc P3/P5/P10 dùng quyết định đã duyệt; chưa có thì ghi rõ chưa nghiệm thu nhánh đó.

## Ma trận bao phủ task gốc và lỗi review

| Yêu cầu/finding | Task thực hiện |
|---|---|
| F1 URL /api/api | 1 |
| F2 expiry/voucher races | 2–3 |
| F3 payout reconciliation | 5 |
| F4 durable retry/payload | 2,5–6 |
| F5 payment resume/unique/state | 2–3 |
| F6 clamp/missing source money | 2,11 |
| Ví Owner/bank/rút/nhận/3 ngăn | 2–5 |
| Ví Customer/không rút/hoàn điểm1:1 | 3,6–7 |
| Voucher catalog/redeem/My vouchers/checkout | 3,6–7,10 |
| Năm amount/commission config/snapshot | 7–9 |
| Cashier gross/serving/late/no-show | 4,8 |
| Bốn nguồn điểm/QR/review/expiry | 4,10 |
| Admin navigation/VietQR/financial config | 5,9–11 |
| Ledger/reconciliation/reports | 2,11 |
| Mobile Clean Architecture/caller thật | 5–7 |
| Booking canonical và policy chưa duyệt | 0 và task phụ thuộc |
| Paging/responsive/stale responses/verification | 12–13 |

## Prompt gửi Antigravity

Thực hiện `2026-10-22-wallet-voucher-commission-completion-plan.md` để vừa sửa F1–F6 vừa hoàn thiện task gốc, không chỉ vá lỗi review. Đọc coverage review22/10 và spec một ví MoodPoints. Bắt đầu mapping booking thật và policy; sửa /api/api ngay, rồi đóng invariants/idempotency/payment/voucher/payout trước khi mở rộng UI phụ thuộc. Giữ Customer một số dư1điểm=1đ, hoàn tiền vào điểm, điểm đổi voucher trước checkout. Hoàn thiện Owner/Admin/Mobile/Cashier, config hoa hồng/voucher/điểm, expiry và đối soát có nguồn theo plan. Mỗi task cần caller production, test phù hợp và bằng chứng UI/DB; không dùng scaffold hoặc mock pass để báo full completion. Giữ main và thay đổi hiện có, không tự chốt policy chưa duyệt, không tự commit/push/deploy hoặc thao tác tiền/DB server. Bàn giao bảng requirement/finding→file/API/UI→test→trạng thái và các mục còn thiếu.
