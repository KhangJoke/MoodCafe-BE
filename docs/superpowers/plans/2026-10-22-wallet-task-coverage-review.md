# Review đối chiếu task: ví Owner, MoodPoints, voucher và hoa hồng

Ngày: **22/10/2026**, timezone Asia/Saigon. Review working tree BE/FE/MB trên `main`; thay đổi sản phẩm đang kiểm tra chưa commit. Lượt này chỉ review và viết báo cáo, không sửa code sản phẩm hoặc thao tác DB/giao dịch thật.

## Chuẩn dùng để đối chiếu

- Attachment mới: `C:/Users/nhatn/.codex/attachments/a7292609-04cc-4ed6-90fd-59a95e5eed94/Pasted text.txt`. File kết thúc ở **tiêu đề 3.2**, chưa có ma trận biến động hoặc các phần tiếp theo; không coi đó là requirement đầy đủ.
- Phần bổ sung từ người dùng: Owner nhận/rút tiền; Customer tích điểm, đổi voucher, tiền hoàn vào điểm và không rút tiền mặt; hệ thống voucher và cấu hình hoa hồng theo đơn.
- Quyết định đã chốt trước đây: **Customer một số dư MoodPoints, 1 điểm = 1đ**; điểm dùng đổi voucher trước checkout, không tiêu trực tiếp tại checkout. Hai loại ví của hệ thống vẫn là ví quán VNĐ và ví khách MoodPoints, không phải hai số dư trong ví khách.
- Các luồng bổ sung và phạm vi đã tổng hợp trong `2026-10-20-single-moodpoints-final.md`; nghiệm thu sửa theo `2026-10-21-antigravity-wallet-remediation-round-3.md`.

Prefix file: **BE** = `D:/Learn/MoodCafe-BE/moodcafe-be/`; **FE** = `D:/Learn/MoodCafe-FE/`; **MB** = `D:/Learn/MoodCafe-MB/`.

## Kết luận

**Đã có phần lớn nền tảng BE và một phần màn hình, nhưng chưa hoàn tất task ở mức luồng người dùng có thể sử dụng trọn vẹn.** Chưa đủ bằng chứng để nghiệm thu hệ thống tài chính.

Khoảng trống lớn nhất là kết nối với booking thật: checkout local chưa có màn hình/caller FE hoặc MB gọi `/api/reservations`, áp voucher, hủy nhận điểm và thu ngân xem gross để phục vụ. Điều này không chứng minh flow mà nhóm nói đang có không tồn tại; cần xác định đúng branch/module và nối vào flow đó. Không dựng thêm booking song song.

Ngoài thiếu chức năng, có lỗi đường dẫn API mới ở cả FE và MB: theo base URL mặc định/tài liệu cấu hình hiện tại, các request ví/voucher bị lặp `/api/api/...`.

## Các finding cần ưu tiên

### F1 — P1: FE/MB gọi URL tài chính bị lặp /api

**FE:** `src/lib/axios.ts:7` và `.env.example:5` cấu hình base URL `http://localhost:8080/api`; `src/services/wallet.service.ts:76`, `:92`, `:112` và `voucher.service.ts:30` lại thêm `/api/...` vào path. Axios ghép thành ví dụ:

`http://localhost:8080/api/api/merchant/stores/demo/wallet`

**MB:** `lib/core/config/app_config.dart:40–42` dùng base URL có `/api`; `lib/features/wallet/data/datasources/wallet_remote_data_source.dart:22`, `:34` và `vouchers/data/datasources/voucher_remote_data_source.dart:20`, `:30`, `:41` cũng thêm `/api`. Dio ghép baseUrl + path nên có cùng lỗi với cấu hình mặc định.

**Ảnh hưởng:** màn ví quán, payout Admin, ví điểm và catalog/redeem có UI/service nhưng không gọi đúng endpoint BE dưới cấu hình được hướng dẫn. Typecheck/lint/repository mock không kiểm tra được URL thực này. Không nên đổi base URL chung để chữa riêng module mới vì các API cũ đang dùng path tương đối đúng convention.

### F2 — P1: Expiry/capture voucher vẫn chưa an toàn

**BE:** `reservation/scheduler/ReservationLifecycleScheduler.java:109–120`, `:148–160`; `voucher/service/VoucherLifecycleService.java:60–92`.

Đơn còn PENDING sau 20 phút có thể được vòng đầu giữ lại, nhưng vòng “orphan” vẫn release voucher nếu attempt trong DB là PENDING. Reconcile UNKNOWN không tự cập nhật status của attempt trước vòng sau. Quá 30 phút vẫn expire khi gateway còn PENDING, chưa cancel link.

Capture/release không khóa voucher và không yêu cầu trạng thái LOCKED. Callback và job có thể xen kẽ để USED bị ghi lại AVAILABLE. Khách có thể trả link cũ trong khi voucher đã được dùng lại. Các lỗi N5/N6 của review vòng 3 còn tồn tại trong source mới.

### F3 — P1: Đường đối soát payout có thể hồi tiền khi chưa chứng minh chưa chuyển

**BE:** `wallet/service/WithdrawalService.java:241–252`, `:271–310`.

Mark/resolve reconciliation không enforce claimedBy như confirmPaid. Admin B có thể đưa lệnh A đang chuyển vào đối soát rồi resolve REJECTED. Nhánh REJECTED còn nhận notes null và tự điền câu đã xác nhận tiền chưa rời ngân hàng, sau đó unlock ngay. Như vậy việc thêm màn hình đối soát chưa đóng rủi ro tiền đã chuyển nhưng available được hồi.

### F4 — P1: Retry sau đóng/mở hoặc restart vẫn tạo ý định tài chính mới

**FE:** `src/app/merchant/wallet/page.tsx:57`, `:80`, `:308` chỉ giữ payout key trong state, sinh key mới mỗi lần mở modal. Nếu BE đã tạo payout nhưng response mất, mở lại/reload rồi gửi có thể khóa thêm tiền.

**MB:** `lib/features/vouchers/presentation/pages/voucher_catalog_page.dart:46–52` sinh timestamp key mỗi lần xác nhận; lỗi timeout hiển thị thất bại và lần xác nhận sau dùng key mới. BE có thể trừ thêm điểm và tạo voucher thứ hai. Chưa có durable intent và đọc lại kết quả đã commit.

**BE:** `voucher/service/VoucherRedemptionService.java:59–69` có replay user/key nhưng không so template/payloadHash; đổi template khác với cùng key vẫn trả voucher cũ như thành công, không 409.

### F5 — P1/P2: Booking attempt có lưu nhưng chưa phục hồi workflow đầy đủ

**BE:** `payment/service/BookingPaymentCoordinator.java:43–58`; V40 `payment_attempt_actor_and_hash`.

Scope actor/purpose và hash đã được bổ sung, khắc phục lookup key toàn hệ thống ở lượt trước. Tuy nhiên replay vẫn trả reservation ngay, kể cả URL null hoặc zero-pay chưa capture. Process dừng sau prepare thì retry không tiếp tục bước còn thiếu. V40 chỉ tạo index thường, chưa unique actor/purpose/key nên hai request đồng thời vẫn có thể tạo hai intent/booking.

`PaymentAttemptService.createAttempt` đã chuyển về REQUIRED, là cải thiện đúng. `markStatus` vẫn nhận trạng thái tùy ý; sequence generator vẫn có fallback bộ nhớ, chưa đủ bảo đảm trạng thái/mã dùng nhiều instance.

### F6 — P2: Có thể che sai lệch số dư bằng cách cộng đủ tiền khi ngăn nguồn thiếu

**BE:** `wallet/service/MerchantWalletService.java:88–96`, `:203–209`, `:120–134`.

Khi pending thiếu, settle đặt pending=0 nhưng vẫn cộng toàn bộ amount vào available. Unlock có logic tương tự với locked; revertPending thiếu tiền thì bỏ qua, trong khi caller có thể tiếp tục refund điểm. Nếu dữ liệu/luồng đã lệch, service có thể làm lệch lớn hơn thay vì chặn/đưa đối soát. Cần kiểm tra invariant và business reference đúng một lần; chưa có đối soát đủ để phát hiện/tách xử lý các trường hợp này.

## Ma trận chức năng so với task

“Có core” nghĩa là có model/service/API cơ bản đã đọc; không đồng nghĩa đã nghiệm thu E2E hoặc giao dịch thật.

| Hạng mục cần làm | Đã có | Còn thiếu / chưa đạt | Kết luận |
|---|---|---|---|
| Ví quán theo cơ sở, đơn vị VNĐ | MerchantWallet pending/available/locked, version/khóa, API số dư/lịch sử, Owner FE | API client F1; lịch sử chỉ trang đầu; invariant F6 | Có core, chưa nghiệm thu |
| Quyền Owner đúng cơ sở | StoreFinancialSecurityService kiểm tra active membership + OWNER, bank/store binding, FE menu/route guard | Cần test HTTP security + DB thật; đối soát Admin còn F3 | Phần kiểm tra Owner đã có |
| Ngân hàng nhận tiền | API/FE thêm và xem, snapshot vào payout | Chưa đủ sửa/xóa/chọn mặc định/xác minh; có thể nhiều bank default khi bỏ isDefault; không transaction/constraint active-default | Một phần |
| Owner tạo lệnh rút | Available→locked, min hiện hardcode 100k, idempotency BE, history | Intent qua reload/restart F4, config min/max/phí/SLA chưa chốt; không coi 100k là đã duyệt | Một phần |
| Admin chuyển VietQR/duyệt lệnh | Route withdrawals, queue, claim, confirm/reject, reference/evidence và reconciliation form | Không có QR generation/image thật, không có link trong AdminSidebar; F1/F3; chưa có kiểm chứng ngân hàng | Một phần |
| Ví Customer một số dư, không rút | pointsBalance/spendablePoints, REFUND/REWARD lots nội bộ, wallet/history API, Mobile page | F1, paging; chưa có widget/E2E; Owner wallet trên MB chưa có nếu giữ scope MB Owner của plan | Đúng hướng một ví; UI chưa trọn |
| Hoàn đơn thành MoodPoints | cancelAndRefund kiểm tra owner/free-cancel; revert pending; PayOS-paid amount→điểm 1:1, restore voucher riêng | Chưa nối màn hủy booking hiện dùng; free cancel hardcode; policy partial/late refund/reward reversal còn mở | Có BE cơ bản |
| Tích điểm chi tiêu | gross×earn rate, grant khi COMPLETED/CLAIMED_LATE, business key/reward type | Rate/cap/hạn dùng hardcode; chưa policy/version và actual booking UI | Có BE cơ bản |
| Bonus check-in/checkout/review | Service và caller tồn tại; checkout điều kiện 30 phút | QR daily còn replay/default secret; review chưa binding VISIT_VERIFICATIONS đúng booking; P5 late review chưa chốt | Một phần, chưa đạt anti-fraud |
| Catalog và đổi voucher | Template seed, read catalog, redeem/allocation, user/key record, Mobile catalog | F1/F4, payload mismatch, UI thiếu điều kiện/giá trị/hạn dùng; không có màn Voucher của tôi | Một phần |
| Áp voucher vào booking | BE createReservation có voucher, snapshot, PayOS phần thiếu, nhánh zero-pay | Chưa có FE/MB booking checkout caller; lock/expiry còn F2; resume còn F5 | BE một phần, chưa đủ luồng |
| Hạn voucher/điểm và bảo toàn tiền hoàn | Release/restore khi booking có xử lý REFUND-backed expiry | Không có expiry path thường cho voucher AVAILABLE bỏ không; P3/earn expiry chưa chốt; balance tổng có thể còn expired reward lots | Chưa hoàn thiện, phụ thuộc policy |
| Năm con số tài chính bất biến | gross, discount, customer_paid, commission, merchant_net và rate/funding snapshots trên reservation; quote không giảm phần quán vì voucher | Chưa có UI Cashier nhìn gross và khách nhìn invoice; chưa E2E gateway/booking; funding policy P10 chưa duyệt | Có model/công thức cơ bản |
| Config hoa hồng | AdminCommissionPolicyController list/create/enable, global/store/effective period; quote ưu tiên store→global | Không có FE service/page gọi endpoint này; 15% fallback hardcode; thiếu audit/decision mapping/validation policy đầy đủ | Có BE API, thiếu phần sử dụng |
| Config voucher/điểm/khuyến mãi | Seed template và constants trong service | Không thấy Admin API/UI quản lý voucher template và loyalty policy; generic settings không nối các rule đang hardcode | Chưa làm đầy đủ |
| Vòng đời đi trễ/no-show | Service/jobs TABLE_EXPIRED giữ pending; COMPLETED/CLAIMED_LATE giải ngân; no-show 0 điểm | Deadline vẫn nhận ngoài giờ và thiếu lịch tự fallback; FE staff/Cashier chưa có thao tác phục vụ, QR chưa đạt | Một phần |
| Ledger/đối soát | PointTransaction, MerchantWalletTransaction, lot/allocation và reference | ReconciliationService chỉ sum balances, chưa so gateway/capture/refund/funding/payout, không mismatch report/caller/UI; chưa kiểm chứng ledger cân/không lặp ở mọi nhánh | Chưa đạt đối soát requirement |
| Nối booking đang có | BE reservation/controller/coordinator mới có | Checkout local không tìm thấy FE/MB booking module/caller; cần map branch/module thật, không dựng thêm | Chưa chứng minh hoàn tất |

## Đối chiếu plan sửa vòng 3

| Task | Trạng thái hiện tại |
|---|---|
| 0 — Mapping/baseline | Đã có các tài liệu; chưa xác định được caller/branch booking thật hoặc quyết định đủ P1–P10 |
| 1 — Constructor/vòng service | Đã bỏ overload gây ambiguity, thêm @Autowired constructor; bỏ dependency ngược Coordinator trong ReservationService; wiring test pass |
| 2 — Schema redeem/startup gate | Entity đã khớp idempotency_key/payload_hash ở V39; chưa chạy PostgreSQL migrate+Hibernate validate/full startup; pom chưa có Failsafe |
| 3 — Scope/atomic prepare | Đã scope actor/purpose/hash và dùng REQUIRED; thiếu unique DB, legacy/backfill/retry concurrency và vẫn fallback sequence bộ nhớ |
| 4 — Resume/state monotonic | Chưa xong: replay trả record không tiếp tục payment/zero-pay; markStatus còn tùy ý |
| 5 — Expiry/lock voucher | Chưa đạt: F2 còn hiện hữu |
| 6 — QR binding/consume | Chưa đạt: vẫn daily store token/default secret, không booking/action/nonce consume |
| 7 — Payout claim/evidence | Chưa đạt: F3 còn hiện hữu |
| 8 — Durable financial intent | Chưa đạt: FE reopen/MB confirmation tạo key mới, BE redeem chưa compare payload |
| 9 — Lịch phục vụ hợp lệ | Chưa đạt: resolver có ca đêm nhưng chưa chặn ngoài giờ/thiếu lịch |
| 10 — Review visit binding | Chưa đạt: có caller nhưng ảnh thường vẫn đủ điều kiện, VisitVerification chưa gắn booking |
| 11 — Response generation/history | Chưa đạt: manual reload race, histories chỉ page 0; Admin clamp trang cuối đã có |
| 12 — Mobile Clean Architecture | Có UseCase class/provider nhưng production vẫn gọi repository; Result/Failure/DTO chưa đầy đủ |
| 13 — Reconciliation/test gate/bàn giao | Chưa đạt: summary-only, IT đa số Mockito, thiếu PostgreSQL/concurrency và UI E2E |

**Không giữ nguyên kết luận “3 lỗi chặn khởi động” từ review cũ:** N1/N2/N3 đã được chỉnh trong source hiện tại và constructor/context wiring test đã pass. Full startup với schema migrate thật vẫn chưa được xác minh; không suy ra pass hoặc fail từ test mock.

## Các quyết định còn thiếu bằng chứng duyệt

Những việc sau cần đối chiếu quyết định thực tế của nhóm, không dùng constants hiện có làm bằng chứng đã chốt:

- Earn rate/cap, check-in/checkout/review bonus và expiry/reversal; code đang 1%, cap50k, bonus1k/1k/2k và reward365ngày.
- Catalog mệnh giá/point cost, voucher60ngày và rule bảo toàn REFUND-backed khi hết hạn; quy tắc trợ giá REWARD vượt commission/marketing.
- Free-cancel2giờ, late/partial/store-fault refund, điểm thưởng đã phát và trường hợp tiền quán đã giải ngân.
- Default/override commission, rounding/effective period/audit; 15% là fallback trong code, requirement chỉ dùng làm ví dụ.
- Min/max/phí/xác minh bank/SLA rút tiền; FE ghi100k và24h nhưng chưa có nguồn policy đã duyệt.
- Lịch24/7, ca qua nửa đêm, ngoài giờ/đóng bất thường và review sau CLAIMED_LATE.

Quyết định một ví và 1đ=1điểm đã rõ, không cần hỏi lại. Không dùng policy chưa rõ để trì hoãn sửa URL, quyền, retry, locks và invariants tiền đã xác định.

## Bằng chứng kiểm thử lần này

- BE `mvnw.cmd -o test -q`: **203 tests, 0 failures/errors**, gồm FinancialServiceWiringTest mới.
- BE chạy riêng `mvnw.cmd -o -Dtest=*IT test -q`: **35 tests, 0 failures/errors**. NPE test QR lượt trước đã được sửa. Các IT vẫn chủ yếu Mockito; chưa có bằng chứng migration/locking/concurrency PostgreSQL.
- FE `npx tsc --noEmit --incremental false`: pass; lint 5 file wallet/voucher liên quan: pass.
- Full FE `npm run lint`: fail, 824 errors và13.055 warnings, gồm `.worktrees`/generated và lỗi source ngoài phạm vi. Không quy toàn bộ các lỗi này cho module mới; cũng không báo full lint pass.
- Không chạy Flutter analyze/test mới trong lượt này. Kết quả gần nhất ở review trước là analyze pass và316 tests pass; đó không phải kiểm chứng UI/API/DB E2E của lần này.
- URL Axios đã được ghép thử bằng runtime; Dio ghép URL được đối chiếu source thư viện. Chưa gọi HTTP/live server để quan sát 404, chưa render UI trên browser/thiết bị và chưa thao tác dữ liệu server.

## Thứ tự hoàn thiện còn lại để duyệt

1. Sửa convention URL F1 và thêm test contract URL thực. Xác định flow booking thật và map tất cả điểm tích hợp Customer/Cashier.
2. Đóng các lỗi tiền F2–F6: voucher job/race, claim/evidence, retry qua restart, resume, unique và invariant số dư.
3. Nối các màn thiếu: áp voucher/zero-pay/cancel→điểm, Voucher của tôi, Cashier gross/claim/check-out; menu/QR payout Admin và config hoa hồng/voucher/điểm.
4. Hoàn thiện ledger reconciliation có nguồn/bất thường, expiry theo policy đã duyệt, Clean Architecture và lịch sử/paging.
5. Nghiệm thu bằng PostgreSQL migration/security/concurrency và E2E/UI. Không đánh dấu xong task gốc chỉ vì bảng/model/API đã tồn tại.
