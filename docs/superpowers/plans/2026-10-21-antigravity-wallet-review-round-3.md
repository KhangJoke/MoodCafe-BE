# Review vòng 3 — bản sửa theo remediation round 2

Ngày: 21/10/2026. Phạm vi: working tree BE/FE/MB trên `main`, so với `2026-10-21-antigravity-wallet-remediation-round-2.md`. Không sửa code sản phẩm, không chạy migration/DB server/giao dịch thật.

**Kết luận: chưa nghiệm thu. Có ba lỗi mới chặn khởi động BE, cùng các lỗi còn lại về scope idempotency, voucher expiry, QR và đối soát payout.** Một số lỗi khởi tạo và luồng đã được tái hiện bằng chương trình kiểm tra tạm ngoài repo, dùng Spring context nhỏ hoặc repository/gateway giả; không khẳng định đã tái hiện trên môi trường deploy.

Prefix file: BE = `D:/Learn/MoodCafe-BE/moodcafe-be/`; FE = `D:/Learn/MoodCafe-FE/`; MB = `D:/Learn/MoodCafe-MB/`.

## P1 — lỗi chặn khởi động

### N1. V39 không khớp entity VoucherRedemption

**Files:** BE `src/main/resources/db/migration/V39__durable_payment_attempts_and_financial_constraints.sql:45–49`; `src/main/java/com/moodcafe/voucher/entity/VoucherRedemption.java:36`, `:48`; `src/main/resources/application.properties:39`.

Migration tạo `idempotency_key`, `payload_hash NOT NULL`, nhưng entity đọc/ghi `intent_key`, `points_spent` và không có payload_hash. Cấu hình hiện tại dùng `ddl-auto=validate`, nên schema migrate theo V39 không qua Hibernate validation. Nếu bỏ validate thì query/insert redeem vẫn lỗi cột hoặc NOT NULL.

**Cần sửa:** thống nhất schema/entity/repository/unique key; nếu V39 đã chạy thì dùng migration mới. Test khởi động với PostgreSQL đã migrate thật, không dùng mock repository làm bằng chứng schema hợp lệ.

### N2. PayOSService có hai constructor nhưng Spring không biết chọn constructor nào

**File:** BE `src/main/java/com/moodcafe/payment/service/PayOSService.java:52`, `:74`.

Constructor overload mới làm mất điều kiện tự chọn constructor duy nhất. Không constructor nào có `@Autowired`; annotation ở tham số không chỉ định constructor chính. Spring thử constructor không tham số và thất bại.

**Đã tái hiện:** đăng ký PayOSService trong Spring context riêng → `NoSuchMethodException: PayOSService.<init>()`, `No default constructor found`. Đây không phải lỗi kết nối PayOS hoặc DB.

**Cần sửa:** chỉ định constructor injection chính hoặc bỏ overload không cần thiết; thêm Spring context test cho wiring thật.

### N3. ReservationService và BookingPaymentCoordinator phụ thuộc vòng qua constructor

**Files:** BE `src/main/java/com/moodcafe/reservation/service/ReservationService.java:63–70`; `payment/service/BookingPaymentCoordinator.java:26`.

ReservationService inject Coordinator và Coordinator inject ReservationService. `@Lazy` đặt ở field final không được Lombok tự sao chép vào tham số constructor với cấu hình hiện tại. Reflection trên class vừa compile xác nhận tham số constructor không có annotation Lazy.

**Đã tái hiện:** context với hai service và mock các phụ thuộc còn lại → `BeanCurrentlyInCreationException` tại vòng `reservationService → bookingPaymentCoordinator → reservationService`.

**Cần sửa:** bỏ phụ thuộc ngược/delegate thừa; controller đã gọi coordinator trực tiếp. Test context phải qua được, không chỉ `new` service trong Mockito test.

## P1 — quyền và tính đúng của luồng tiền

### N4. Retry booking có thể trả đơn của user khác

**Files:** BE `src/main/java/com/moodcafe/payment/service/BookingPaymentCoordinator.java:42–51`; `payment/abstraction/repository/PaymentAttemptRepository.java:20`; V39 index intent không unique và không có actor scope.

Lookup chỉ theo intentKey toàn hệ thống, không theo user/purpose và không kiểm tra reservation.userId trước khi trả response. User B gửi key đã được A dùng sẽ nhận reservation của A, gồm thông tin đơn và payment URL nếu có. Cùng user/key nhưng thay store/category/thời gian cũng trả kết quả cũ như thành công thay vì conflict. Hai request đồng thời còn có thể cùng tạo attempt vì chỉ có index thường trên key.

**Đã tái hiện:** coordinator với attempt của A, caller B cùng key → response.userId vẫn là A.

**Cần sửa:** actor + purpose/operation + key có unique constraint, payload hash và ownership check; cùng key/khác payload trả 409; xử lý concurrent replay đúng kết quả cũ.

### N5. Voucher vẫn được nhả khi payment còn PENDING, kể cả sau UNKNOWN

**File:** BE `src/main/java/com/moodcafe/reservation/scheduler/ReservationLifecycleScheduler.java:109–120`, `:148–160`.

Vòng đầu thấy PENDING dưới 30 phút thì bỏ qua. Vòng “orphan” sau đó chỉ giữ voucher nếu attempt.status là UNKNOWN hoặc PAID; attempt PENDING bình thường vẫn bị release sau hold 15 phút. Ngoài ra reconcile trả UNKNOWN không ghi UNKNOWN vào attempt, nên attempt trước đó PENDING vẫn có thể bị vòng sau nhả. Quá 30 phút, vòng đầu expire dù gateway vẫn PENDING và chưa cancel link.

**Đã tái hiện:** đơn 20 phút tuổi, gateway/attempt PENDING, voucher quá hold → scheduler gọi releaseVoucher. Khách vẫn có thể trả link cũ; đơn có thể không còn giữ voucher để capture đúng discount.

**Cần sửa:** không release theo TTL khi còn khả năng thu tiền; xác minh/cancel link và khóa aggregate đúng cách, dùng trạng thái observation chứ không chỉ status cũ trong DB.

### N6. Capture/release voucher chưa khóa và vẫn có thể hồi USED về AVAILABLE

**Files:** BE `src/main/java/com/moodcafe/voucher/service/VoucherLifecycleService.java:60–66`, `:76–92`; scheduler `:135–160`.

Cả capture và release dùng lookup không khóa, không bắt buộc trạng thái hiện tại LOCKED. Scheduler đọc reservation PENDING, callback có thể capture thành USED trước khi scheduler release. Vì voucher USED vẫn giữ lockedReservationId, release vẫn tìm thấy và ghi AVAILABLE. Voucher có thể được tái sử dụng dù đơn đã hưởng discount.

**Cần sửa:** khóa/CAS voucher cùng binding reservation/attempt và transition hợp lệ; job phải recheck dưới khóa. Test PostgreSQL interleaving callback/job thực sự, không chỉ Mockito thứ tự tuần tự.

### N7. Có attempt nhưng retry không phục hồi bước tạo link hoặc zero-pay bị dừng

**File:** BE `src/main/java/com/moodcafe/payment/service/BookingPaymentCoordinator.java:43–51`, `:59–68`.

Replay thấy attempt rồi trả reservation ngay, không xét bước nào đã hoàn tất. Nếu process dừng sau prepare nhưng trước gọi gateway, hoặc create timeout trước khi có link lưu, lần retry trả PENDING_PAYMENT với URL null và không tạo/tra lại link. Zero-pay dừng sau prepare cũng bị đường replay bỏ qua capture nội bộ. Chỉ có “trả record cũ”, chưa có resume workflow.

**Đã tái hiện:** replay attempt PENDING có reservation URL null → trả URL null, không có tương tác gateway.

**Cần sửa:** resume theo trạng thái attempt, tra cùng orderCode và capture zero-pay idempotent; có đường xử lý gateway xác nhận chưa từng tạo link. Không yêu cầu người dùng tạo ý định mới để thoát kẹt.

### N8. QR vẫn sao chép/replay được; secret mặc định nằm trong source

**Files:** BE `src/main/java/com/moodcafe/reservation/service/ReservationQrVerificationService.java:29–30`, `:39–47`, `:98–109`; `ReservationService.java:298–305`.

HMAC đã chặn chuỗi STORE:uuid cũ, nhưng token chỉ gắn store + ngày, dùng chung cho mọi booking/action, không nonce và không consume. Một token chụp được có thể dùng từ xa cho check-in rồi checkout. Có secret mặc định trong source nếu môi trường chưa cấu hình, cho phép tự ký token khi deployment dùng mặc định. Khoảng ngày còn chấp nhận cả ngày mai; log mismatch in expected HMAC.

**Đã tái hiện:** cùng token gọi validate hai lần đều thành công. Đây không phải proof đã quét tại thời điểm thao tác.

**Cần sửa:** secret bắt buộc từ cấu hình an toàn, không log expected signature; challenge/binding/action/expiry/consume theo plan. Checkout vẫn có đường `isSelfQr=false` cho Customer hoàn tất mà không xác minh QR; cần phân biệt actor/action server quyết định, không coi boolean tự khai là bằng chứng.

### N9. Endpoint đối soát mới bỏ qua claim và cho unlock không có bằng chứng

**File:** BE `src/main/java/com/moodcafe/wallet/service/WithdrawalService.java:241–252`, `:271–279`, `:305–318`.

Direct reject PROCESSING đã bị chặn, nhưng markForReconciliation/resolve không kiểm tra claimedBy. Admin B có thể chuyển lệnh của A đang xử lý sang đối soát rồi resolve REJECTED. Nhánh REJECTED nhận notes/reference null vẫn unlock và tự đặt CONFIRMED_FAILED kèm câu “xác nhận tiền chưa rời tài khoản” mặc định. Hai endpoint này mở lại rủi ro đã chuyển tiền nhưng available được hồi.

**Cần sửa:** quyền claim/transition và quy trình chuyển giao rõ; kết quả chưa rõ phải giữ locked. Resolve chưa chuyển cần lưu kiểm tra/chứng từ theo policy, không tự chế bằng chứng khi thiếu dữ liệu. Test hai Admin cạnh tranh và kết quả ngân hàng không rõ.

### N10. FE chỉ giữ payout key trong một lần mở modal

**File:** FE `src/app/merchant/wallet/page.tsx:57`, `:80`, `:308`.

Key hiện được giữ khi catch trong modal, nhưng mở modal lại luôn sinh key mới; đổi store xóa key và reload mất React state. BE đã commit nhưng response mất → đóng/mở hoặc reload rồi gửi lại vẫn có thể tạo payout thứ hai và khóa thêm tiền.

**Cần sửa:** lưu/khôi phục intent theo user/store/payload và tra trạng thái chưa rõ trước khi tạo intent mới; test mất response → đóng/mở/restart/đổi A→B→A.

## P2 — nghiệp vụ và UI còn thiếu

### N11. Mobile vẫn tạo key redeem mới cho mỗi lần xác nhận

**File:** MB `lib/features/vouchers/presentation/pages/voucher_catalog_page.dart:46`, `:74–79`.

Đã truyền header nhưng key timestamp được tạo lại khi người dùng xác nhận lần sau. Timeout vẫn hiển thị thất bại chắc chắn; lần thử lại dùng key khác nên có thể trừ điểm/đổi thêm voucher. Chưa có durable intent hoặc xác minh kết quả lần trước. R9 chưa đóng.

### N12. Cùng redeem key nhưng template khác vẫn trả success với voucher cũ

**File:** BE `src/main/java/com/moodcafe/voucher/service/VoucherRedemptionService.java:59–69`.

Replay không so templateId/payload. Đã đổi A, gửi cùng key để đổi B → HTTP success trả A thay vì 409. Ngoài lỗi schema N1, còn thiếu xử lý loser của hai concurrent request để trả lại voucher gốc thay vì lỗi DB. Cần test khác payload và concurrent replay thật sau khi sửa schema.

### N13. Deadline tính được ca đêm nhưng vẫn nhận booking ngoài giờ

**File:** BE `src/main/java/com/moodcafe/reservation/service/ServiceDayDeadlineResolver.java:29–51`.

Quán 07:00–21:00, request đặt 22:00 vẫn nhận deadline 21:00 cùng ngày. Sau payment đơn có thể bị no-show/giải ngân ngay dù chưa đến bookingTime. Quán 18:00–02:00 đặt buổi trưa cũng nhận deadline trong quá khứ. Khi thiếu giờ, code tự tạo 07:00–22:00, trái yêu cầu không tự chốt policy.

**Cần sửa:** validate booking nằm trong lịch phục vụ, xử lý schedule thiếu theo policy đã duyệt; thêm ranh giới giờ và ca/ngày dịch vụ. Category soft-delete đã được chặn đúng, không cần sửa lại phần đó.

### N14. Reward review có caller nhưng không ràng buộc VISIT_VERIFICATIONS với booking

**Files:** BE `src/main/java/com/moodcafe/store/service/StoreReviewServiceImpl.java:249–253`; `loyalty/service/LoyaltyRewardService.java:146–175`.

Có reservation đúng user/store và bất kỳ ảnh upload là đủ gọi thưởng. VisitVerification không có binding booking, được kiểm tra độc lập hoặc có thể không có. Như vậy chưa thực hiện yêu cầu ảnh xác thực gắn đúng lần đến/booking. Code còn cho CLAIMED_LATE dù P5 chưa có quyết định trong tài liệu hiện tại.

**Cần sửa:** xác minh evidence từ visit đúng user/store/booking, không chỉ images nonempty; giữ chính sách CLAIMED_LATE theo quyết định thực tế, không tự suy ra đã duyệt.

## Các khoảng trống khác của plan

- **Mobile UseCases mới là scaffold chưa nối:** wallet providers `:31–38`, voucher providers `:37–44` và catalog `:50` vẫn gọi repository; repository trả Future và pass exception, domain vẫn tự fromJson. Task 11 chưa đạt UseCase/Result/Failure/DTO mapping theo AGENTS.md dù có test UseCase pass.
- **Admin reload còn race:** FE `src/app/admin/withdrawals/page.tsx:40–55` không guard generation của manual/action loader. Đổi filter/page khi refresh/action pending, response cũ có thể ghi đè filter mới. Guard của effect không bao phủ hàm này.
- **Ví quán guard chỉ so storeId:** FE `src/app/merchant/wallet/page.tsx:96`, `:121`, `:140`; A→B→A hoặc hai refresh A cho phép response A cũ ghi đè A mới. Cần request generation cho imperative loaders.
- **Lịch sử bị cắt:** FE wallet `:120`, `:139`, `:162–163` chỉ lấy page 0/20, không UI phân trang, Owner không xem được giao dịch/lệnh cũ hơn. Mobile lịch sử cũng chỉ lấy trang đầu.
- **Attempt chưa nguyên tử với reservation:** `PaymentAttemptService.java:27` dùng REQUIRES_NEW bên trong prepareReservation. Attempt có thể commit trong khi outer reservation/voucher transaction rollback, để lại businessId không tồn tại. Payment attempt cần cùng transaction prepare, sau đó network ngoài transaction; không cần transaction lồng tách riêng để bền vững.
- **Attempt state chưa monotonic:** `markStatus` nhận status bất kỳ, webhook booking không đồng bộ attempt; catch coordinator có thể đánh UNKNOWN dù callback đã capture. Cần transition/CAS và kiểm tra trạng thái cuối nhất quán, không chỉ ghi log.
- **Sequence fallback:** `OrderCodeGeneratorService` có fallback AtomicLong bắt đầu lại từ cùng số cho mỗi process. Nếu sequence lỗi, fallback làm mất bảo đảm unique đa instance/restart. Không fallback bằng mã có thể trùng khi không cấp được sequence bền vững.
- **Đối soát và config chưa đủ:** ReconciliationService vẫn chỉ sum số dư; không so ledger/gateway/funding/payout. Chưa có bằng chứng đóng các policy/config/checkout còn thiếu của Task 12.
- **Các IT mới phần lớn vẫn mock repository:** chưa chứng minh schema/migration/context/security filter/concurrency PostgreSQL. pom chưa có Failsafe để verify tự chạy các lớp *IT.

## Những phần sửa có hiệu quả

| Finding cũ | Tình trạng sau sửa |
|---|---|
| R1 chữ ký webhook | Đã fail closed khi verify lỗi/gateway chưa cấu hình; thiếu amount bị chặn. Chưa có full payment integration test thật. |
| R3 polling | Đã có nhánh capture reservation khi PAID; cần xử lý pending/late/attempt theo N5–N7. |
| R4 network trong transaction chuẩn bị | Coordinator đã tách network khỏi prepare; durability/resume còn các vấn đề N3/N7 và transaction attempt. |
| R5 QR | Đã thêm HMAC và kiểm tra QR khi cấp checkout bonus; chưa đạt chống replay, xem N8. |
| R6 reject khi đang chuyển | Direct reject PROCESSING đã chặn; đường đối soát còn N9. |
| R7 retry payout | Giữ key trong modal đã có; reopen/reload còn N10. |
| R8 bank reference | V39 có unique index normalized cho PAID, cải thiện bảo đảm DB. Chưa có test PostgreSQL và conflict mapping rõ. |
| R9 redeem | BE nhận key/replay tuần tự; schema, payload và Mobile retry còn N1/N11/N12. |
| R10 deadline | Đã dùng giờ quán và ca qua đêm, nhưng ngoài giờ/missing schedule còn N13. |
| R11 category xóa mềm | Đã kiểm tra isDeleted. |
| R12 review | Đã có caller production, nhưng evidence còn N14. |
| R13 trang cuối rỗng | Đã clamp về trang hợp lệ trong hai đường tải, case tuần tự cũ đã sửa. |
| R14 confirm retry | PAID cùng reference trả lại payout cũ, không debit lần hai. |
| UI đối soát/error | Đã có nhãn và action/API đối soát; error theo store đã reset. Chưa render live. |

## Kiểm chứng trong lượt review này

1. **BE unit/default tests:** 202 tests, 0 failure/error trong pha test của `mvnw.cmd -o verify -q`. Lệnh verify cuối cùng exit 1 ở packaging do Maven jar plugin/cache thiếu `org.codehaus.plexus.archiver.FileSet`; không quy lỗi này cho code sản phẩm khi chưa kiểm chứng cache/dependencies.
2. **BE các lớp IT chạy riêng:** `mvnw.cmd -o -Dtest=*IT test -q` chạy **34 tests, 1 error**. `ReservationAuthorizationIT.customerSelfCheckOutAwardsBonus` NPE vì test chưa inject mock `ReservationQrVerificationService`; đây là test wiring lỗi sau đổi dependency, không phải bằng chứng bean QR production luôn null. Cần cập nhật test và kiểm tra QR thật, không xóa assertion để pass.
3. **Spring wiring probes ngoài repo:** tái hiện N2 `No default constructor found` và N3 `BeanCurrentlyInCreationException` với class vừa compile, không cần DB/gateway.
4. **Flow probes ngoài repo, dùng mock:** tái hiện cross-user replay, retry trả paymentUrl null không chạm gateway, cùng QR được validate hai lần và voucher release khi payment PENDING 20 phút. Các probe là kiểm chứng đường gọi, không phải integration/concurrency PostgreSQL.
5. **FE:** typecheck và lint 5 file thay đổi pass.
6. **MB:** analyze pass, no issues; test pass **316 tests**. Chưa có widget/E2E cho restart/lost response/store switch.
7. Chưa test UI render live, chưa khởi động full app với DB migrated vì đã có blocker schema/wiring. Không thao tác dữ liệu server/gateway thật.

## Ưu tiên tiếp theo

1. Sửa N1–N3 và thêm startup test dùng schema migrate thật trước khi tiếp tục tính năng.
2. Scope/unique/recovery payment intent; sửa N5–N9 bằng state machine + transaction/locks đúng.
3. Đóng lost-response/restart ở FE/MB và sửa deadline/review evidence.
4. Chạy đúng IT gate, bổ sung PostgreSQL/concurrency và UI E2E; chỉ đánh dấu phần nào hoàn tất khi có bằng chứng tương ứng.
