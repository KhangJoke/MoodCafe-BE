# Review lại bản sửa ví / MoodPoints / voucher

Ngày: 21/10/2026. Đối chiếu working tree BE/FE/MB với `2026-10-21-antigravity-wallet-remediation.md` và spec một ví MoodPoints. Review code và chạy kiểm thử; chưa render UI live hay thao tác tiền/DB server. Không sửa code sản phẩm trong lượt review này.

Cả BE, FE và MB hiện ở nhánh `main`; các thay đổi sản phẩm đang review chưa commit.

**Kết luận: đã sửa được một số lỗi cũ, nhưng chưa đủ điều kiện nghiệm thu luồng tài chính.** Các điểm P1 bên dưới còn có thể xác nhận thanh toán không hợp lệ, làm mất khả năng xử lý đơn đã trả tiền hoặc xử lý tiền hai lần.

Đường dẫn BE tính từ `D:/Learn/MoodCafe-BE/moodcafe-be`, FE từ `D:/Learn/MoodCafe-FE`, MB từ `D:/Learn/MoodCafe-MB`.

## Những phần đã sửa được

- Merchant financial API đã kiểm tra OWNER active theo đúng store, thay cho chỉ yêu cầu đăng nhập. Withdrawal service cũng kiểm tra quyền.
- Ngân hàng rút tiền đã được ràng buộc với store, kiểm tra soft-delete và snapshot vào payout.
- Admin khác không được reject/confirm một lệnh PROCESSING do người khác claim.
- BE đã hỗ trợ tái sử dụng payout khi gửi lại cùng idempotency key; có unique index theo store/key. Tuy nhiên FE chưa giữ key khi retry.
- Booking đã dùng hàm tạo PayOS riêng, truyền payable của booking và bỏ fallback URL simulated.
- Có scheduler thực sự gọi các mốc grace/no-show; settlement khóa dòng reservation.
- FE đã reset ngân hàng khi đổi store, bỏ qua phản hồi initial load cũ, thêm phân trang Admin và đổi bước nhập tiền thành 1đ.
- FE có giới hạn giao diện ví theo OWNER; Mobile hiển thị trạng thái lỗi thay vì biến lỗi API thành số dư 0.
- Mobile đã tách data source/repository, nhưng chưa hoàn tất UseCase/Result theo AGENTS.md.

## P1 — cần sửa trước khi nghiệm thu

### R1. Callback sai chữ ký vẫn xác nhận booking và ghi tiền vào ví quán

**File:** BE `src/main/java/com/moodcafe/payment/service/PayOSService.java:288–295`, nhánh booking mới ở `:331–338`.

Webhook endpoint được public trong `auth/config/SecurityConfig.java:46`. SDK verify ném lỗi nhưng code chỉ log rồi tiếp tục; khi gateway chưa cấu hình thì bỏ qua verify hoàn toàn. Handler cũng không kiểm tra mã thành công/currency trước khi gọi capture. Vì vậy một request chứa orderCode booking và amount do người gửi khai báo vẫn có thể chuyển đơn sang PAID_CONFIRMED và cộng pending dù chưa có thanh toán thật. Capture còn chấp nhận amount null và tự điền amount dự kiến (`ReservationPaymentCaptureService.java:59–60`).

Phần bắt lỗi chữ ký là code có sẵn, nhưng nhánh booking vừa bổ sung đang sử dụng trực tiếp đường xử lý không an toàn này. Cần fail closed trước mọi tác dụng phụ, xác nhận payload hợp lệ từ gateway và test callback sai/mất chữ ký, code thất bại, thiếu amount, sai currency.

### R2. Job quá hạn nhả voucher ngay cả khi chưa xác định được tình trạng tiền

**File:** BE `src/main/java/com/moodcafe/reservation/scheduler/ReservationLifecycleScheduler.java:92–98`, `:105–113`; `payment/service/PayOSService.java:377–380`.

`checkPaymentStatus` trả false cho cả chưa trả tiền và lỗi kết nối. Job coi false là đã thất bại, chuyển PAYMENT_EXPIRED và trả voucher; không hủy/xác nhận link PayOS không còn khả năng thu tiền. Nếu khách đã trả nhưng PayOS timeout, hoặc khách trả ngay sau lần kiểm tra, callback đến sau sẽ bị capture bỏ qua vì đơn không còn PENDING_PAYMENT (`ReservationPaymentCaptureService.java:45–47`).

Vòng quét voucher được gọi là “mồ côi” thực tế chỉ lọc LOCKED + holdUntil, không kiểm tra reservation/gateway. Ngay cả khi polling thấy PAID, vòng này vẫn có thể nhả voucher của đơn đó. Cần phân biệt PAID/PENDING/CANCELLED/UNKNOWN, khóa đúng aggregate và giữ để đối soát khi chưa rõ; không release chỉ dựa TTL.

### R3. Polling trả đã thanh toán nhưng không capture reservation

**File:** BE `src/main/java/com/moodcafe/payment/service/PayOSService.java:362–373`.

Khi PayOS trả PAID, hàm chỉ confirm subscription rồi return true. Không có nhánh tìm reservation và gọi `ReservationPaymentCaptureService`. Nếu webhook mất, khách đã trả tiền nhưng booking vẫn PENDING_PAYMENT, quán không có pending và voucher chưa USED. Scheduler cũng dùng chính hàm này nên không khắc phục được. Cần dùng chung capture idempotent cho webhook và polling, lấy số tiền thực từ gateway.

### R4. Tạo link gateway trong transaction DB có thể để lại link thu tiền không có đơn hợp lệ

**File:** BE `src/main/java/com/moodcafe/reservation/service/ReservationService.java:64`, `:179–192`; `payment/service/PayOSService.java:117–120`.

Reservation và voucher lock chưa commit khi gọi PayOS. Nếu gateway tạo link thành công nhưng response mất, hàm ném RuntimeException làm rollback DB; gateway vẫn có thể giữ link thật nhưng không còn reservation/orderCode bền vững để xử lý callback. Cũng không có payment attempt/reconciliation record ngoài transaction này để phục hồi.

Cần lưu ý định thanh toán bền vững trước, gọi gateway ngoài transaction giữ khóa, sau đó cập nhật kết quả; timeout phải được xem là chưa rõ kết quả và truy vấn lại cùng attempt. Test lỗi sau khi remote đã tạo thành công, không chỉ lỗi trước khi gửi.

### R5. QR có thể tự dựng bằng storeId; checkout không xác thực QR

**File:** BE `src/main/java/com/moodcafe/reservation/service/ReservationService.java:307–315`, `:298–301`; `reservation/controller/ReservationController.java:92–98`.

Check-in chỉ cần chuỗi chứa UUID store, là thông tin khách đã biết. Khách có thể gửi `STORE:<storeId>` từ xa, nhận thưởng đúng giờ. Checkout chỉ tin boolean `isSelfQr` do client gửi, mặc định true; sau 30 phút có thể tự gọi endpoint để nhận thưởng và giải ngân mà không quét gì.

Kiểm tra actor chéo đã tốt hơn, nhưng chưa giải quyết xác thực có mặt/chống replay. Cần token QR xác thực theo cơ chế dự án và suy ra self-QR từ kết quả kiểm chứng server, không từ boolean tự khai.

### R6. Admin đang claim vẫn có thể reject để hồi số dư khi tiền có thể đã chuyển

**File:** BE `src/main/java/com/moodcafe/wallet/service/WithdrawalService.java:203–215`, `:230`.

Đã chặn Admin B can thiệp lệnh của A, nhưng chính A vẫn reject PROCESSING bằng một reason bất kỳ; code unlock tiền ngay, không có trạng thái/bằng chứng đã xác nhận chưa chuyển. Ví dụ A chuyển thành công nhưng response confirm mất, sau đó thao tác “từ chối” vì tưởng lỗi: Owner vừa nhận ngân hàng vừa được hồi available.

`markForReconciliation` chưa có controller/caller production; UI cũng không có hành động đưa lệnh vào đối soát. Cần đường xử lý trạng thái chưa rõ thực sự dùng được, giữ locked; chỉ hoàn available sau xác nhận chưa chuyển.

### R7. FE tạo idempotency key mới mỗi lần gửi lại lệnh rút

**File:** FE `src/app/merchant/wallet/page.tsx:219–226`.

Key dùng `Date.now()` ngay trong submit. Nếu BE đã tạo lệnh 100k nhưng response mất, người dùng bấm lại sẽ có key mới: với số dư đủ, BE tạo thêm lệnh và khóa thêm 100k. BE đã hỗ trợ key nhưng UI hiện làm mất tác dụng chống retry. Giữ key cho cùng ý định rút xuyên qua lỗi/mất response; chỉ đổi khi bắt đầu yêu cầu mới hoặc thay payload có chủ ý.

### R8. Hai payout có thể xác nhận bằng cùng một mã giao dịch ngân hàng khi chạy đồng thời

**File:** BE `src/main/java/com/moodcafe/wallet/service/WithdrawalService.java:162–176`; schema `src/main/resources/db/migration/V37__single_moodpoints_wallet_voucher_reservation_schema.sql:235`, V38.

`existsByTransactionReference` là kiểm tra trước khi ghi; mỗi transaction chỉ khóa payout của nó. Hai Admin confirm hai store khác nhau cùng một reference có thể cùng thấy chưa tồn tại rồi cùng ghi PAID/debit locked. Không có unique constraint cho reference trong V37/V38. Cần constraint theo phạm vi reference ngân hàng đã quy định và xử lý conflict nguyên tử; test PostgreSQL đồng thời.

## P2 — nghiệp vụ và UI còn lỗi

### R9. Mất response đổi voucher rồi thử lại có thể trừ điểm lần hai

**File:** BE `src/main/java/com/moodcafe/voucher/controller/CustomerVoucherController.java:48`, `voucher/service/VoucherRedemptionService.java:57`; MB `lib/features/vouchers/presentation/pages/voucher_catalog_page.dart:63–72`.

Controller luôn truyền null businessKey, service sinh key timestamp mới. Mobile báo thất bại và mở lại nút khi timeout, không xác minh kết quả đã commit. Nếu ví đủ điểm, lần thử lại đổi thêm voucher và trừ thêm điểm dù người dùng chỉ định đổi một lần. API/client cần intent key bền vững và tra lại voucher gốc; không chỉ disable nút trong lúc đang gửi.

### R10. Giờ đóng cửa vẫn sai theo cơ sở

**File:** BE `src/main/java/com/moodcafe/reservation/service/ReservationService.java:319–327`.

Đã bỏ `booking + 12 giờ` nhưng thay bằng 23:00 cố định cho mọi quán. Store có `closingTime` riêng. Quán đóng 21:00 vẫn cho staff claim tới 23:00; quán mở qua nửa đêm có thể bị no-show/giải ngân sớm. Đặt sau 23:00 còn bị đẩy deadline sang 23:00 hôm sau. Phải tính ngày dịch vụ từ lịch cơ sở và snapshot theo chính sách đã chốt.

### R11. Category đã xóa mềm vẫn đặt được

**File:** BE `src/main/java/com/moodcafe/reservation/service/ReservationService.java:74–78`.

Đã check store và isActive nhưng thiếu isDeleted. Category `isDeleted=true`, `isActive=true` vẫn được dùng lấy minSpend và tạo booking. Query/validation cần đầy đủ điều kiện chưa xóa; bổ sung regression test.

### R12. Thưởng review xác thực vẫn chưa nối vào luồng tạo review

**File:** BE `src/main/java/com/moodcafe/loyalty/service/LoyaltyRewardService.java:131`; `store/service/StoreReviewServiceImpl.java:214–254`.

Tìm toàn production source chỉ có định nghĩa `awardVerifiedReview`, không có caller. Review hợp lệ có ảnh được tạo xong không nhận nguồn thưởng này. Cần nối vào review/visit-verification thật, chứng minh user/booking hợp lệ và dedup theo booking; không gọi thẳng hàm nhận boolean do client khai.

### R13. Admin bị kẹt trang rỗng sau khi xử lý dòng cuối trang cuối

**File:** FE `src/app/admin/withdrawals/page.tsx:34–39`, `:74–78`, `:287`.

Ví dụ có 16 REQUESTED, mỗi trang 15. Đang trang 2, tiếp nhận dòng duy nhất làm nó rời filter REQUESTED. Reload vẫn gửi page=1 dù chỉ còn một trang, nhận content rỗng; totalPages=1 khiến thanh phân trang biến mất. Người dùng không còn nút về trang trước dù 15 lệnh vẫn tồn tại. Clamp/reset page khi tổng trang giảm và fetch lại.

### R14. Confirm payout thành công nhưng response mất thì retry báo lỗi

**File:** BE `src/main/java/com/moodcafe/wallet/service/WithdrawalService.java:143–145`.

Confirm đã lưu PAID nhưng response tới Admin bị mất; retry cùng reference bị BAD_REQUEST vì code chỉ chấp nhận PROCESSING. Trạng thái tiền không bị debit lần hai, nhưng UI nhận thông báo thất bại cho việc đã hoàn tất, dễ dẫn tới xử lý thủ công sai. Cùng intent/reference cần trả kết quả đã PAID; khác thông tin mới báo conflict.

## Khoảng trống cần ghi nhận trong bàn giao

- **Mobile chưa đạt AGENTS.md:** providers gọi repository trực tiếp, page đổi voucher gọi repository; interface trả Future trực tiếp, chưa có UseCase/Result/Failure; model tự fromJson trong domain, chưa có DTO/freezed/json_serializable ở data. Có tách tầng sơ bộ nhưng Task 8 chưa hoàn tất.
- **UI đối soát chưa hoàn thiện:** Admin filter có RECONCILIATION_REQUIRED nhưng dòng lại hiển thị “Chờ Admin duyệt” (`admin/withdrawals/page.tsx:221–227`), không có hành động xử lý. Cần hoàn thiện cùng R6, không chỉ thêm tên trạng thái.
- Banner lỗi ví FE không được clear khi đổi cơ sở rồi initial load cơ sở mới thành công (`merchant/wallet/page.tsx:71–80`, `:162–173`, `:310`). Có thể hiển thị lỗi cũ cạnh số dư mới; nên reset lỗi theo store/load.
- OrderCode booking và subscription đều sinh từ giây hiện tại nhưng chỉ tra uniqueness trong bảng riêng (`ReservationService.java:330–335`, `PayOSService.generateOrderCode`). Hai flow cùng giây có thể chọn cùng mã tại gateway; cần nguồn cấp mã chung và xử lý concurrent requests/multiple instances.
- Chưa có bằng chứng hoàn tất mapping flow booking thật, chính sách P1–P10, config/admin voucher/customer checkout trong Task 0/9. Những số cũ vẫn hardcode. Không coi các mục này hoàn thành chỉ vì wallet UI đã chạy.
- `ReconciliationService.getSummary` vẫn chỉ cộng các số dư, chưa so ledger với payment/refund/funding/payout để tìm mismatch.
- `ReservationPaymentIntegrationTest` thực tế dùng Mockito và mock repository/wallet/voucher. Hai lớp tên AuthorizationIT cũng là Mockito. Chưa chứng minh DB locking/unique constraints, HTTP security/filter chain và payment callback chạy cùng nhau; cần integration/concurrency tests thật.

## Kiểm tra trong lượt review

- BE: `mvnw.cmd -o test -q` pass **200 tests**, 0 failure/error.
- BE: chạy riêng `MerchantWalletAuthorizationIT,ReservationAuthorizationIT` pass **12 tests**. Hai lớp này không được lệnh test mặc định chọn theo naming convention hiện tại; chưa cấu hình Failsafe riêng trong pom.
- FE: `npx tsc --noEmit` pass; lint 5 file thay đổi pass, không còn lỗi/warning của lượt trước.
- MB: `flutter test --no-pub` pass **309 tests**; `flutter analyze --no-pub` pass, **No issues found**.
- Chưa render UI live, chưa test gateway/ngân hàng thật, chưa chạy migration hoặc thay đổi dữ liệu DB server. Các finding race/timeout nêu trên dựa vào đường gọi và transaction trong source, không tuyên bố đã tái hiện trên production.

## Thứ tự xử lý đề nghị

1. R1–R4: xác thực payment, capture chung, trạng thái chưa rõ và lưu attempt bền vững.
2. R5–R8: QR, payout/reconciliation và chống xử lý rút trùng.
3. R9–R14: redeem retry, deadline, category, review và UI pagination/confirm retry.
4. Hoàn tất các khoảng trống theo plan; thêm test đúng các tình huống này rồi mới nghiệm thu lại.
