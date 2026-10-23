# Plan sửa implementation ví, MoodPoints, voucher và đặt bàn

Ngày: 21/10/2026 — Bàn giao cho Antigravity. Đây là plan sửa, chưa thay đổi code sản phẩm.

> For agentic workers: dùng quy trình executing-plans hoặc tương đương; thực hiện từng task, kiểm chứng trước khi đánh dấu hoàn thành. Không coi scaffold hoặc unit test dùng mock là bằng chứng luồng đã nối đầy đủ.

**Goal:** Sửa các lỗi đã xác nhận trong review, hoàn thiện các luồng còn thiếu và nghiệm thu BE/FE/MB theo cơ chế một ví MoodPoints.

**Architecture:** BE kiểm soát quyền, state machine, ledger và giao dịch nguyên tử; FE/MB phản ánh trạng thái do BE xác nhận. Payment gateway nằm sau adapter, có đối soát khi kết quả chưa rõ. MB tuân thủ Clean Architecture theo AGENTS.md.

**Tech Stack:** Spring/Java, PostgreSQL, PayOS; Next.js/TypeScript; Flutter/Riverpod.

**Spec:**
- `2026-10-20-single-moodpoints-final.md`: nghiệp vụ gốc đã tổng hợp.
- `2026-10-21-antigravity-wallet-review.md`: 12 lỗi và khoảng trống implementation đã review.
- Plan này là phần sửa bổ sung, không thay thế các yêu cầu còn lại của spec gốc.

## Tóm tắt để duyệt

Giữ **một số dư MoodPoints cho khách, 1 MoodPoint = 1đ**. Tiền đã trả qua PayOS được hoàn thành điểm khi đủ điều kiện. Khách dùng điểm đổi voucher trước, rồi chọn voucher khi đặt bàn; phần còn lại thanh toán PayOS. Không thêm ví VNĐ riêng hoặc thanh toán trực tiếp bằng điểm tại checkout.

Cần sửa 6 nhóm chính:
1. Chặn truy cập ví và thao tác booking trái quyền.
2. Nối đúng thanh toán booking, số tiền, callback và xử lý thanh toán không thành công.
3. Bảo đảm không mất voucher, không cộng/hoàn/giải ngân hai lần.
4. Bảo đảm rút đúng tiền, đúng ngân hàng, không trả lại số dư khi tiền đã chuyển.
5. Nối các mốc đi trễ/no-show/review; hoàn thiện cấu hình và đối soát còn thiếu.
6. Sửa UI FE/MB, kiến trúc Mobile và kiểm thử toàn luồng.

**Chưa nghiệm thu tiền thật** cho tới khi các task P1 và kiểm thử tích hợp bên dưới đạt yêu cầu. Các kiểm thử cũ đã pass không bao phủ những lỗi này.

## Global Constraints

- Chỉ sửa trong phạm vi implementation ví/điểm/voucher/booking và các màn hình liên quan của BE/FE/MB. Không sửa lan sang Auth hoặc chức năng không liên quan.
- Kiểm tra branch hiện tại ở cả ba repo; yêu cầu trước đây của người dùng là `main`. Nếu khác `main` và có thay đổi chưa commit, báo rõ, không tự reset/stash/chuyển nhánh làm mất việc.
- Giữ nguyên thay đổi khác đang có. Không tự commit/push/deploy, chạy migration server hay thực hiện chuyển tiền thật trong lượt sửa này.
- OWNER đúng cơ sở mới được xem ví quán/ngân hàng/tạo lệnh rút. MANAGER/CASHIER bị chặn API tài chính; quyền thao tác booking được xét riêng.
- Customer không rút MoodPoints. REFUND/REWARD là metadata nguồn điểm nội bộ để đối soát, không hiển thị thành hai số dư.
- Hủy hợp lệ: hoàn phần thực thu qua PayOS thành điểm; xử lý trả voucher riêng. Không vừa hoàn giá trị voucher thành điểm vừa trả lại voucher đó.
- Không hardcode chính sách chưa được duyệt; không tạo URL PayOS giả; không biến lỗi API thành số dư 0.
- Số tiền VNĐ/điểm là số nguyên, xử lý làm tròn theo chính sách đã duyệt. Không tính tài chính bằng số thực thiếu kiểm soát.

## Review Focus

Mỗi task phải có kiểm thử tái hiện lỗi trước khi sửa, kiểm thử sau sửa và bằng chứng đường gọi production. Đặc biệt kiểm tra ownership, trạng thái chuyển tiền chưa rõ, webhook lặp, request đồng thời và phản hồi về sai thứ tự khi đổi cơ sở.

Các đường dẫn BE bên dưới tính từ `D:/Learn/MoodCafe-BE/moodcafe-be`; FE từ `D:/Learn/MoodCafe-FE`; MB từ `D:/Learn/MoodCafe-MB`. File test/job mới là tên đề xuất; ưu tiên quy ước sẵn có của repo.

## Task 0 — Chốt nền booking và lập bảng chính sách

**Ưu tiên:** Làm trước mọi thay đổi schema/payment.

**Files:** `src/main/resources/db/migration/V37__single_moodpoints_wallet_voucher_reservation_schema.sql`; các package `reservation`, `payment`, `finance`; spec gốc và report review.

- [ ] Đọc hướng dẫn repo, kiểm tra branch/diff và xác định flow đặt bàn thật đang ở đâu. So sánh controller, entity, bảng và trạng thái với phần Antigravity vừa tạo; chọn một flow chính thức, không giữ hai hệ thống booking song song.
- [ ] Kiểm tra V37 đã được áp dụng ở môi trường nào. Migration đã chạy phải được sửa bằng migration mới, không sửa lịch sử. Không chạy migration trên DB server trong task này.
- [ ] Lập bảng API hiện có/cần sửa/còn thiếu và bảng trạng thái booking, voucher, payout thực tế; ghi rõ tên nào khác spec.
- [ ] Ghi lại quyết định nhóm cho P1–P10 của spec gốc: phạm vi ví quán; catalog voucher; hạn dùng/hoàn điểm nguồn REFUND; mức thưởng; review sau CLAIMED_LATE; hủy trễ/hoàn một phần; commission/làm tròn; hạn mức/phí rút; ngày dịch vụ/giờ đóng cửa; phân bổ REFUND/REWARD và giới hạn tài trợ.
- [ ] Những số đang hardcode như 15%, 100k, 2 giờ, 15 phút, +12 giờ, 60 ngày, 365 ngày không được coi là đã duyệt. Dùng cấu hình đã có nếu hợp lệ; đánh dấu quyết định còn thiếu, tiếp tục sửa các lỗi không phụ thuộc quyết định đó.

**Nghiệm thu:** Có bảng mapping booking và chính sách trong báo cáo bàn giao; không tạo thêm schema trùng chức năng.

## Task 1 — Khóa quyền ví và ngân hàng theo cơ sở

**Ưu tiên:** P1. **Phụ thuộc:** Task 0.

**Files BE:** `wallet/controller/MerchantWalletController.java`, `wallet/controller/MerchantWithdrawalController.java`, `wallet/service/WithdrawalService.java`, lớp authorization/membership hiện có.
**Files FE:** `src/components/merchant/shell/OwnerSidebar.tsx`, `src/app/merchant/wallet/page.tsx`, guard merchant hiện có.
**Test đề xuất:** `src/test/java/com/moodcafe/wallet/MerchantWalletAuthorizationIT.java`.

**Contract:** Mọi API ví/ngân hàng/rút tiền xác minh user là OWNER active của store được yêu cầu. `bankAccountId` phải thuộc chính store và còn hợp lệ. Không tin role hoặc storeId từ client.

- [ ] Viết test Customer, Manager, Cashier, Owner cơ sở khác bị 403 trên cả API đọc và ghi; xác nhận test tái hiện lỗi cũ.
- [ ] Dùng kiểm tra quyền dùng chung tại service boundary và bảo vệ endpoint; áp dụng cho tất cả đường đọc/ghi liên quan, không chỉ nút tạo lệnh rút.
- [ ] Tách quyền Admin quản lý payout khỏi endpoint ví Owner. Không cho Admin giả làm owner để bỏ qua kiểm tra phạm vi.
- [ ] FE chỉ hiện mục Ví và cho vào route khi người dùng có quyền Owner tại cơ sở đang chọn; xử lý đổi sang cơ sở không đủ quyền và API trả 403.
- [ ] Test bank của A không dùng được để rút ví B, kể cả gửi request trực tiếp.

**Nghiệm thu:** Không có tác dụng phụ hoặc lộ dữ liệu khi bị từ chối; Owner hợp lệ vẫn dùng được.

## Task 2 — Quyền booking, QR và category đúng cơ sở

**Ưu tiên:** P1 cho actor/QR, P2 cho category. **Phụ thuộc:** Task 0.

**Files:** BE `reservation/service/ReservationService.java`, controller/DTO/repository booking và cơ chế QR hiện có.
**Test đề xuất:** `reservation/ReservationAuthorizationIT.java`, `reservation/ReservationCategoryValidationTest.java` trong cây test BE.

**Contract:** Phân biệt self check-in/out của khách và thao tác staff đúng cơ sở; late claim chỉ cho staff có quyền. Category phải active, chưa xóa và thuộc store.

- [ ] Test tài khoản khác biết UUID không thể check-in/out/cancel hộ; staff cơ sở khác không thể giao nước.
- [ ] Xác minh QR bằng cơ chế hiện có: đúng mục đích, phạm vi, hạn dùng và chống replay; không chỉ nhận UUID booking.
- [ ] Kiểm tra cận sớm, thời gian grace và deadline đóng cửa theo chính sách Task 0; không cho check-in trước ngày hẹn chỉ vì chưa quá grace.
- [ ] Truyền actor context rõ ràng để checkout hộ không được nhận thưởng tự quét QR. Lặp request không lặp cộng điểm/giải ngân.
- [ ] Tìm category theo ID + store + trạng thái; bỏ fallback giá 100k không có rule. Giữ validation bàn/khung giờ của booking chính thức.
- [ ] Chạy test truy cập chéo, QR hết hạn/replay, category sai cơ sở/inactive/deleted và ranh giới thời gian.

## Task 3 — Sửa payment booking và capture nguyên tử

**Ưu tiên:** P1. **Phụ thuộc:** Task 0, 2.

**Files:** BE `reservation/service/ReservationService.java`, `payment/service/PayOSService.java`, webhook/polling controllers/services; model/repository payment attempt và migration bổ sung nếu cần.
**Test đề xuất:** `payment/ReservationPaymentIntegrationTest.java`, `payment/ReservationPaymentConcurrencyIT.java`.

**Contract:** Tạo payment có purpose rõ BOOKING/SUBSCRIPTION; lưu đúng amount snapshot, gateway orderCode và paymentLinkId thực. Webhook/polling dùng chung xử lý capture idempotent.

- [ ] Test gross 100k, voucher 90k chỉ tạo QR 10k; tái hiện lỗi hiện gọi nhánh subscription/default 50k.
- [ ] Tách adapter/entry point booking khỏi subscription; không dùng UUID booking để gọi hàm chỉ hiểu subscription. Thêm regression test subscription hiện có.
- [ ] Bỏ URL `/simulated/`. Thất bại xác định phải trả lỗi thật; timeout chưa rõ kết quả phải giữ trạng thái chờ đối soát, không tự tạo payment lần hai.
- [ ] Persist payment attempt trước, gọi gateway ngoài transaction giữ khóa DB, sau đó lưu kết quả và có đường phục hồi nếu tiến trình dừng giữa chừng.
- [ ] Xác minh chữ ký callback, amount, currency, orderCode/purpose và trạng thái. Sai chữ ký/amount không làm thay đổi tiền hoặc booking.
- [ ] Capture trong transaction: booking PAID_CONFIRMED, voucher USED, ledger và pending quán; không để chỉ một phần thành công. Webhook trùng/polling đồng thời chỉ capture một lần.
- [ ] Nếu payable = 0: không gọi PayOS, xác nhận bằng transaction nội bộ với cùng invariant; không tạo khoản tiền gateway giả.
- [ ] Test webhook đến trước response tạo link, callback lặp/sai amount, timeout và khởi động lại. Giữ snapshot commission/voucher/funding để không đổi kết quả khi config thay đổi.

## Task 4 — Nhả voucher đúng lúc, refund không nhân đôi

**Ưu tiên:** P1. **Phụ thuộc:** Task 3.

**Files:** BE `voucher/service/VoucherLifecycleService.java`, reservation cancel/payment failure handlers, voucher/payment repositories; worker mới theo scheduler convention dự án.
**Test đề xuất:** `voucher/VoucherReservationLifecycleIT.java`.

**Contract:** AVAILABLE → LOCKED theo reservation/payment attempt → USED khi thanh toán thành công. Release phải kiểm tra đúng lock owner và trạng thái payment; không được nhả lock mới của đơn khác.

- [ ] Test hai request cùng voucher chỉ một request giữ được; thêm xử lý failed/expired và job quét lock quá hạn.
- [ ] Trước khi nhả do timeout, xác minh/cancel gateway theo contract. Nếu kết quả chưa rõ, giữ để đối soát; không tự suy ra chưa thanh toán từ việc thiếu webhook.
- [ ] Payment thất bại/hết hạn xác nhận: đưa voucher còn hạn về AVAILABLE; voucher hết hạn xử lý theo chính sách đã duyệt, không tự gia hạn.
- [ ] Cancel hợp lệ: hoàn số tiền thực thu PayOS thành MoodPoints đúng một lần, trả voucher theo rule; không cộng lại phần voucher thành tiền hoàn.
- [ ] Xử lý callback trả tiền trễ bằng state machine có đường đối soát; không âm thầm xác nhận đơn đã giải phóng tài nguyên hoặc dùng lại voucher đang thuộc đơn khác.
- [ ] Test retry cancel, job và webhook chạy đồng thời, voucher được lock lại bởi đơn mới, lỗi giữa ledger/refund/voucher rollback toàn bộ.

## Task 5 — Payout an toàn, chống retry và sai người nhận

**Ưu tiên:** P1; idempotency là điều kiện nghiệm thu. **Phụ thuộc:** Task 1.

**Files:** BE `wallet/service/WithdrawalService.java`, payout controller/DTO/entity/repository; migration bổ sung; FE `src/services/wallet.service.ts`, `src/app/merchant/wallet/page.tsx`, `src/app/admin/withdrawals/page.tsx`.
**Test đề xuất:** `wallet/WithdrawalConcurrencyIT.java`, `wallet/WithdrawalIdempotencyIT.java`.

**Contract:** REQUESTED → PROCESSING có người claim → PAID khi xác nhận. Kết quả chuyển tiền không rõ phải giữ locked trong trạng thái đối soát tương đương RECONCILIATION_REQUIRED. Không reject PROCESSING theo cách tự động hoàn available.

- [ ] Test hai Admin: A claim/chuyển, B reject; không được vừa có tiền đã chuyển vừa có available được phục hồi.
- [ ] Enforce claim owner và phiên bản trạng thái bằng khóa/CAS. Chỉ cho từ chối sau khi có bằng chứng chưa chuyển; trường hợp không rõ đưa đối soát. Admin khác không tự hoàn tiền đang xử lý.
- [ ] Confirm yêu cầu reference/chứng từ theo chính sách; kiểm tra reference không được dùng cho hai payout và chặn tự duyệt lệnh của mình theo quy tắc nhóm. Confirm lặp trả kết quả cũ.
- [ ] Snapshot thông tin ngân hàng đã validate vào payout; thay đổi ngân hàng sau đó không đổi người nhận của lệnh đang xử lý.
- [ ] Thêm idempotency key theo actor + operation + store + payload hash, unique constraint và transaction. Cùng key/cùng payload trả lệnh cũ; khác payload trả 409.
- [ ] FE giữ key cho một lần submit và các retry do mất response; chỉ tạo key mới cho ý định rút mới. Disable double-click là hỗ trợ UI, không thay thế BE.
- [ ] Kiểm tra min config, amount nguyên dương và available; concurrent requests không làm âm số dư. Không gọi gateway/ngân hàng thật trong test.

## Task 6 — Nối deadline, settlement và đủ bốn nguồn điểm

**Ưu tiên:** P2 nhưng bắt buộc để hoàn thành nghiệp vụ. **Phụ thuộc:** Task 2–4.

**Files:** BE `finance/service/ReservationSettlementService.java`, `loyalty/service/LoyaltyRewardService.java`, reservation scheduling/repository, review/visit-verification production flow.
**Test đề xuất:** `finance/ReservationDeadlineIT.java`, `loyalty/LoyaltyRewardLifecycleIT.java`.

- [ ] Thay `booking + 12 giờ` bằng deadline theo lịch cơ sở/ngày dịch vụ/timezone đã chốt; snapshot deadline cần thiết cho đơn. Có test ca qua nửa đêm và worker chạy bù.
- [ ] Nối worker thật: quá grace → TABLE_EXPIRED chỉ trả bàn, giữ pending, không cộng điểm.
- [ ] Staff giao nước trước closing → CLAIMED_LATE: giải ngân một lần và cộng Spend Points từ gross; không thưởng check-in/checkout.
- [ ] Đóng cửa khách không tới → EXPIRED_NO_SHOW: giải ngân một lần, 0 điểm. Test cạnh tranh late claim với job đóng cửa.
- [ ] COMPLETED cộng Spend Points từ gross theo earn rate cấu hình. Tỷ lệ đổi 1 điểm = 1đ không có nghĩa chi 1đ được thưởng 1 điểm.
- [ ] Check-in hợp lệ đúng giờ mới nhận bonus. Self QR checkout cần đủ 30 phút: 29:59 không thưởng, 30:00 được thưởng; staff checkout không có self bonus.
- [ ] Nối review có ảnh và visit verification thuộc đúng user/booking vào thưởng review, tối đa một lần mỗi đơn. Review sau CLAIMED_LATE xử lý theo quyết định P5, không tự suy diễn.
- [ ] Dùng ledger business key/unique constraint và event bền vững nếu xử lý async; kiểm thử retry và restart không mất/lặp thưởng.

## Task 7 — Sửa UI FE

**Phụ thuộc:** Task 1, 5; các API đã ổn định.

**Files:** `src/app/merchant/wallet/page.tsx`, `src/app/admin/withdrawals/page.tsx`, `src/components/merchant/shell/OwnerSidebar.tsx`, services và component liên quan.

- [ ] Khi đổi A → B: reset bank selection, amount, lỗi và dữ liệu theo store; chặn submit lúc tải. Chỉ chọn bank nằm trong danh sách B sau khi tải xong.
- [ ] Hủy hoặc bỏ qua response cũ; test response A đến sau B và thao tác đổi A → B → A. Không dùng effect phụ thuộc selectedBankId gây tải lại lặp không cần thiết.
- [ ] Tiền VNĐ dùng bước 1 và min config. “Rút tối đa” với 125.500đ phải hợp lệ khi đạt min; kiểm tra min−1/min, vượt available và số thập phân.
- [ ] Admin thêm pagination/load more dùng totalPages; ít nhất 16 lệnh đều truy cập được. Reset khi đổi filter, xử lý trang cuối rỗng sau action.
- [ ] UI claim/confirm/reject phản ánh state machine, có trạng thái đang đối soát. Không hiển thị thành công khi API thất bại.
- [ ] Sửa các lỗi lint mới đã review; loading/error/empty tách biệt. Số dư có lỗi phải báo không tải được, không hiển thị 0 giả.
- [ ] Kiểm tra render ở 360/390/768/1280px: số tiền, tên quán, ngân hàng dài, bảng và modal không tràn; nút hành động truy cập được bằng bàn phím.

## Task 8 — Mobile: dữ liệu lỗi và Clean Architecture

**Files:** `lib/features/wallet/presentation/pages/moodpoints_wallet_page.dart`, `lib/features/wallet/presentation/providers/wallet_providers.dart`, `lib/features/vouchers/presentation/pages/voucher_catalog_page.dart`, providers vouchers và các tầng mới của hai feature.

**Contract:** presentation → UseCase → Repository interface trong domain; implementation/data source trong data. Data source ném typed exception, repository trả Result/Failure; manual Riverpod. Domain không phụ thuộc Dio/Flutter/storage.

- [ ] Test wallet/catalog timeout không hiển thị “0 điểm” như số dư thật; có error + retry. Nếu dùng cache, ghi rõ dữ liệu chưa cập nhật.
- [ ] Tách Dio khỏi presentation; chuyển DTO parsing sang data, dùng freezed/json_serializable theo AGENTS.md và regenerate khi đổi model.
- [ ] Bổ sung UseCase cho đọc ví/lịch sử/catalog/voucher cá nhân/đổi voucher theo API thực tế; controller xử lý Result.
- [ ] Khi đổi điểm chưa rõ kết quả, không trừ lạc quan vĩnh viễn hoặc gửi lại tạo voucher mới. Đọc lại authoritative state; BE redemption cần cùng nguyên tắc idempotency.
- [ ] Widget test loading/error/retry/empty/success và redeem thất bại; kiểm tra màn nhỏ, chữ lớn, không tràn số dư.

## Task 9 — Hoàn thiện config, đối soát và UI còn thiếu

**Phụ thuộc:** Task 0, 3–6. Đây là phần hoàn thiện spec, tách khỏi việc sửa 12 lỗi để không báo hoàn tất nhầm.

**Files:** BE `finance` (đặc biệt `ReconciliationService`), config/voucher admin API; FE admin finance/voucher và customer booking flow chính thức; MB booking/voucher flow nếu nằm trong spec.

- [ ] Inventory trước khi tạo file: xác định API/màn hình đã có, thiếu hay chưa nối. Không thêm màn giả dùng số hardcode.
- [ ] Admin config commission/default/override, min payout và quy tắc điểm/voucher theo chính sách đã duyệt; validate server, audit thay đổi và snapshot tại thời điểm tạo đơn.
- [ ] Hoàn thiện quản lý voucher và customer chọn voucher ở booking. Chỉ hiển thị voucher phù hợp, giải thích lý do không áp dụng; backend xác nhận giá cuối cùng.
- [ ] Đối soát ledger với payment/refund/voucher funding/payout theo từng business reference, không chỉ cộng các số dư ví. Báo mismatch có chi tiết để xử lý, không tự sửa số dư để cân.
- [ ] Test nguồn REFUND/REWARD theo P10 đã duyệt; ví dụ gross 100k/commission 15k/merchant 85k: reward 10k → PayOS 90k/platform 5k; refund 90k → PayOS 10k/platform 15k; refund 80k + reward 10k → PayOS 10k/platform 5k. Đây là fixture kiểm thử, không đặt commission 15% thành mặc định sản phẩm.
- [ ] Các policy chưa duyệt phải liệt kê rõ trong bàn giao; không tuyên bố tính năng phụ thuộc policy đó đã nghiệm thu.

## Task 10 — Kiểm thử tích hợp và bàn giao

**Phụ thuộc:** Các task trên.

- [ ] Chạy PostgreSQL integration tests có transaction/unique constraints thật cho concurrent redeem, lock voucher, capture, cancel/refund, settle và payout. Mock gateway được phép; không mock thay phần khóa DB cần kiểm chứng.
- [ ] E2E: đổi điểm → voucher → đặt bàn → PayOS capture → check-in/out → thưởng/settlement; hủy hợp lệ → điểm hoàn + voucher phục hồi; bỏ QR → nhả voucher; late claim và no-show.
- [ ] E2E Owner: đổi cơ sở → chọn đúng bank → tạo payout → retry cùng key → Admin claim → confirm; thêm nhánh không rõ kết quả chuyển và đối soát.
- [ ] Đối chiếu ledger/số dư sau từng flow, không chỉ kiểm tra toast hoặc HTTP 200.
- [ ] BE: `./mvnw.cmd test`; FE: typecheck, lint file thay đổi và build theo scripts repo; MB: `flutter analyze`, `flutter test`. Ghi rõ lỗi có sẵn nếu xuất hiện, không che lỗi mới.
- [ ] Chụp UI thật FE/MB bằng môi trường test và lưu bằng chứng cho viewport/state đã kiểm tra. Không báo đã kiểm tra UI trực quan chỉ vì build pass.
- [ ] Bàn giao danh sách file thay đổi, migration, config cần bổ sung, test đã chạy/kết quả, task còn thiếu và policy đang chờ. Không tự push/deploy.

## Ma trận bao phủ review

| Lỗi trong review | Task sửa | Bằng chứng bắt buộc |
|---|---|---|
| 1. API tài chính thiếu OWNER/store scope | 1 | Test quyền đọc/ghi và cross-store 403 |
| 2. PayOS nhầm subscription/sai amount | 3 | Test amount/orderCode/capture + subscription regression |
| 3. Check-in/out thiếu actor/QR | 2, 6 | Cross-user, QR replay, time boundaries |
| 4. Bank cũ khi đổi cơ sở | 1, 5, 7 | A→B + stale response + request bank chéo |
| 5. Reject khi Admin khác đang chuyển | 5 | Hai Admin cạnh tranh, locked không được nhả sai |
| 6. Voucher LOCKED vô hạn | 4 | Timeout xác nhận, late webhook, re-lock |
| 7. Category sai cơ sở | 2 | Sai store/inactive/deleted bị từ chối |
| 8. Retry payout sinh lệnh mới | 5 | Cùng key chỉ một payout, khác payload 409 |
| 9. Jobs/review reward chưa nối | 6 | Caller production + job chạy bù + reward once |
| 10. Queue không có trang 2 | 7 | 16+ lệnh, filter và trang cuối |
| 11. Rút tối đa bị step chặn | 7 | 125.500đ và ranh giới min/available |
| 12. API lỗi Mobile hiện 0 điểm | 8 | Widget error/retry không giả số dư |
| Booking trùng/config/đối soát/UI thiếu | 0, 9, 10 | Mapping, policy, API thật, ledger reconciliation |

## Prompt giao Antigravity

Đọc plan này cùng `2026-10-20-single-moodpoints-final.md` và `2026-10-21-antigravity-wallet-review.md`. Sửa lần lượt theo dependency, ưu tiên P1; kiểm tra main và giữ nguyên thay đổi đang có. Giữ đúng một ví MoodPoints, 1 điểm = 1đ, điểm dùng đổi voucher trước checkout. Mỗi lỗi cần regression test và bằng chứng đã nối production flow. Không tự chốt chính sách còn thiếu, không tạo payment URL giả, không gọi chuyển tiền thật hoặc sửa DB server. Sau khi hoàn thành, bàn giao bảng task/test/file/migration và các mục chưa xong; không tự commit/push/deploy.
