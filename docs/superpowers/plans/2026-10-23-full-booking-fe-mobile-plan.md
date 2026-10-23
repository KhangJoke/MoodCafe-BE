# Full Booking Flow trên FE và Mobile — Implementation Plan

> **For agentic workers:** Thực hiện theo dependency, từng task có test và bằng chứng nghiệm thu. Có thể dùng superpowers:executing-plans hoặc quy trình tương đương của Antigravity. Đây là plan bàn giao, chưa triển khai code.

**Goal:** Customer dùng FE hoặc Mobile đặt bàn, áp voucher, thanh toán, theo dõi/hủy/check-in/out; Owner nhận đúng đơn và doanh thu, rút tiền; mọi thao tác đi qua một booking và một bộ sổ cái.

**Architecture:** Chọn module `booking` và `/api/bookings` hiện có làm nguồn đơn chính. Tích hợp tài chính vào vòng đời booking, tái sử dụng calculator/wallet/voucher/loyalty/PayOS; không bắt hai client tạo thêm một reservation để có tiền. FE và Mobile dùng cùng API/state machine; API trả dữ liệu theo role.

**Tech Stack:** Spring Boot/Java21/PostgreSQL/Flyway/PayOS; Next.js16/React/TypeScript; Flutter/Riverpod manual/Clean Architecture.

**Spec:** Kế hoạch nghiệp vụ user gửi ngày 23/10 (bản đính kèm hiện dừng ở tiêu đề 3.2); các yêu cầu và quyết định một ví `2026-10-20-single-moodpoints-final.md`; master `2026-10-22-wallet-voucher-commission-completion-plan.md`; findings `2026-10-22-antigravity-completion-rereview.md`; hiện trạng deploy `2026-10-23-aws-backend-deployment.md`.

## Tóm tắt để duyệt / giao Antigravity

1. FE/Mobile hiện chưa có UI Customer tạo booking thật. Không coi nút chỉ setState là tính năng đặt bàn.
2. BE có hai module booking/reservation; ví đang nối chủ yếu với reservation mới. **Bắt buộc thống nhất BE trước**, không chỉ làm UI gọi hai API song song.
3. Luồng Customer: Chi tiết quán → lịch và loại bàn → thông tin khách → voucher/báo giá → xác nhận tạo đơn → PayOS hoặc zero-pay → chi tiết/Đơn của tôi → nhận bàn/trả bàn/hủy/đánh giá.
4. Luồng Owner/Staff: cùng booking xuất hiện ở cơ sở → nhận bàn/giao nước trễ/hoàn tất → tiền Pending/Available cập nhật đúng. Owner tạo payout; Admin xử lý trên UI hiện có.
5. FE và Mobile có ví điểm, đổi voucher và kho voucher; Mobile có thêm luồng đơn cơ sở và Ví Quán theo quyền Owner. Reuse màn đã có, không dựng bản thứ hai.
6. Mất mạng, đóng app, F5, double-click, hai tab, callback trùng không được tạo thêm đơn/điểm/voucher/payout.
7. Nghiệm thu bằng một hành trình có dữ liệu/ledger thật trong môi trường test, không dùng số dư seed để thay bằng chứng tiền đến từ booking.

**Ruling kỹ thuật trong plan:** Chọn `booking` làm canonical vì đang có lịch, quota, đơn và API đã triển khai. Đây là lựa chọn của plan nhằm bảo toàn chức năng hiện tại. Không tự xóa lịch sử reservation hay thay API cũ khi chưa có migration/caller mapping.

## Global Constraints

- Một số dư Customer MoodPoints, **1 MoodPoint = 1đ**; không rút/nạp/chuyển điểm. Đổi điểm thành voucher trước checkout; không toggle dùng ví và không thanh toán trực tiếp điểm–PayOS.
- Quán thực nhận gross−commission; voucher sàn không làm giảm phần quán. Phân biệt nguồn REFUND/REWARD nội bộ theo spec; không áp trần marketing cho tiền hoàn đã có tài trợ.
- Check-in chưa giải ngân. COMPLETED/CLAIMED_LATE mới giải ngân; TABLE_EXPIRED chỉ trả slot; EXPIRED_NO_SHOW cuối ngày giải ngân nhưng0 điểm.
- Chỉ OWNER truy cập bank/wallet/payout của cơ sở; MANAGER/CASHIER chỉ nghiệp vụ phục vụ, không thấy commission/net/bank. Admin riêng và có audit.
- Tiền/điểm nguyên; tỷ lệ/làm tròn/timing theo config được duyệt. 15%,100k hoặc mức thưởng trong ví dụ không phải chính sách được tự chốt. Thiếu config phải có lỗi/hướng xử lý rõ.
- Main cả ba repo, giữ working tree hiện tại và stash bảo toàn; không reset/checkout/stash/commit/push/deploy server thật nếu chưa được yêu cầu. Không ghi secrets vào source.
- Server hiện đã migrateV44; migration mới lấy version tiếp theo thực tế. Không sửa V1–V44, không repair/tắt validate để bỏ qua lỗi.
- FE/MB base API đã có `/api`; service path không lặp prefix. Auth FE đi qua proxy hiện có. MB đọc cả dotenv và dart-define, xác nhận đúng ưu tiên AppConfig.
- Giữ SDK-compatible Map webhook và tất cả signed fields; không đổi về fail-open. Giữ test SDK thật và legacy subscription/sponsored/booking.
- FE đọc tài liệu Next bundled trước code. Mobile presentation→UseCase→domain repository Result/Failure←data; typed exceptions, DTO JSON ở data, manual Riverpod. Không để page gọi repository/Dio trực tiếp.
- Payment PENDING/UNKNOWN không đồng nghĩa thất bại. Lookup/cancel gateway ngoài transaction giữ lock; chỉ release sau kết quả authoritative và recheck dưới khóa.

## Review Focus

- Hai client/hai tab tranh slot cuối hoặc retry sau server commit: một slot/một booking/một intent — Task 1–3,6,9.
- Thanh toán xong nhưng return URL chưa về/app bị kill/webhook đến muộn: khôi phục theo BE, không suy từ URL — Task 3,7,10.
- Đổi quán/ngày/loại bàn/voucher lúc request cũ chờ: không nhận quote, đơn hoặc số dư sai context — Task 6,9,12.
- Hủy/expiry/capture/check-out cạnh tranh: ledger/balance/voucher/points nguyên tử, không mất tiền hay ghi hai lần — Task 2–4,15.
- Customer/Staff/Owner khác cơ sở mở deep link/scan QR: chặn ở API, không chỉ ẩn nút — Task 1,4,8,11,12.

## Hiện trạng và đường dẫn

Prefix **BE**=`D:/Learn/MoodCafe-BE/moodcafe-be/`; **FE**=`D:/Learn/MoodCafe-FE/`; **MB**=`D:/Learn/MoodCafe-MB/`.

- FE `src/components/store-detail/StoreDetailView.tsx` gọi StoreSidebar không bật booking. `StoreSidebar.tsx` nút ẩn và chỉ set bookingRequested; chưa gọi create API.
- FE đã có `src/app/merchant/reservations/page.tsx`, `merchant/wallet/page.tsx`, `admin/withdrawals/page.tsx`, `admin/commission-policies/page.tsx`.
- Mobile chưa có feature booking/route booking; store detail ở `lib/features/store_detail/presentation/pages/store_detail_page.dart`; router ở `lib/app/router/`; ví/voucher mới ở `features/wallet`, `features/vouchers`.
- BE `booking/controller/BookingController.java` đã có schedule, availability, create/detail/my/cancel/payment-link và staff operations. DTO create hiện thiếu voucher/intent; DTO response hiện chủ yếu minSpend/payment, chưa đủ finance snapshot.
- BE `booking/service/BookingServiceImpl.java` confirm payment chỉ đổi status; cancel còn refundProcessed=false để hoàn thủ công. `finance`/`wallet`/`loyalty` hiện dùng `Reservation` là chính.
- Baseline gần nhất: BE clean package275 tests0fail/error1skip; MB analyze/316 tests pass trước thay đổi; FE tsc/scoped lint pass. Đây không phải bằng chứng booking UI/E2E đã tồn tại.

## Contract đích dùng chung

Các endpoint ghi **NEW** chưa có; Anti phải implement BE trước caller. Giữ endpoint hiện có tương thích, không mô tả proposal như API đã chạy.

| API BE | Trạng thái | Ý nghĩa / quyền |
|---|---|---|
| GET `/api/stores/{storeId}/schedule?date=YYYY-MM-DD` | Có | Lịch/quota/time slots/config ngày theo múi giờ quán |
| GET `/api/stores/{storeId}/table-categories/active` | Có | Loại bàn hiện hữu |
| GET `/api/stores/{storeId}/booking-availability?categoryId=&startTime=` | Có | Kiểm tra slot, không cam kết giữ chỗ |
| POST `/api/bookings/quote` | NEW | Customer nhận giá/voucher eligibility/config version trước xác nhận |
| POST `/api/bookings` + `Idempotency-Key` | Bổ sung | Tạo một booking/giữ slot/lock voucher atomically |
| GET `/api/bookings/my?page=&size=&status=` | Có, thêm filter | Danh sách của actor, paging authoritative |
| GET `/api/bookings/{bookingId}` | Có, sửa guard/DTO | Customer sở hữu đơn; không lộ owner finance |
| POST `/api/bookings/{bookingId}/payment-link` | Có, sửa resume | Trả/reconcile cùng attempt, không tạo orderCode mới vì retry |
| POST `/api/bookings/{bookingId}/cancel` | Có, sửa nghiệp vụ | Hủy có quyền, hoàn điểm/voucher/slot/ledger atomic |
| POST `/api/bookings/{bookingId}/self-check-in` | NEW | Physical QR hợp lệ, đúng booking/store/time |
| POST `/api/bookings/{bookingId}/self-check-out` | NEW | QR trả bàn hợp lệ; >= 30 phút mới có thưởng checkout |
| GET `/api/stores/{storeId}/bookings` và `/summary` | Có | Owner/Manager/Cashier đúng cơ sở; DTO Staff |
| POST `/api/stores/{storeId}/bookings/check-in` | Có | Staff nhận bàn bằng code/token theo contract hiện có |
| POST `/api/stores/{storeId}/bookings/{bookingId}/claim-late` | Có | Giao nước trễ trước closing |
| POST `/api/stores/{storeId}/bookings/{bookingId}/complete` | Có | Staff hoàn tất CHECKED_IN |
| GET `/api/me/wallet`, `/transactions` | Có | Một ví Customer và lịch sử |
| GET `/api/voucher-catalog`; POST `/api/me/vouchers/redeem/{templateId}` | Có, sửa retry | Đổi điểm trước checkout, key bền vững, trả voucherId |
| GET `/api/me/vouchers`, `/usable` | Có | Inventory all-status vs voucher khả dụng, không dùng nhầm |
| `/api/merchant/stores/{storeId}/wallet`, `/bank-accounts`, `/withdrawals` | Có | Tái sử dụng đúng method từng controller; Owner only |

**Request/response phải chốt trong Task 1:**

- `BookingSelection`: storeId,categoryId,startTime ISO instant,guestCount,customerName,customerPhone,customerEmail?,note?,voucherId?. Capacity kiểm tra BE; guestCount là field bổ sung, không truyền amount/rate/funding từ client.
- `BookingQuote`: quoteId,expiresAt,selectionFingerprint,pricingVersion,grossAmount,voucherDiscount,customerPaidAmount,currency,eligible/reasons,freeCancelDeadline,graceDeadline,serviceClosingDeadline. Không trả commission/net cho Customer. Chọn lưu quote tại bảng `booking_quotes`, có actor/selection/snapshot/version/expiry; quote không giữ slot hay khóa voucher. Create xác minh ownership và revalidate giá/quota/quyền/voucher. Giá đổi→409 `QUOTE_CHANGED`, không âm thầm thu giá mới. TTL lấy từ config được duyệt, không hardcode một giá trị như chính sách đã chốt.
- Create thêm voucherId,guestCount,quoteId; idempotency actor+operation+key+payloadHash. Response gồm bookingId/code/status, Customer money snapshot và payment state, checkoutUrl/QR nếu có; tách WAITING_GATEWAY/UNKNOWN/PENDING/PAID và zero-pay. Không đánh đồng response200 với đã trả tiền.
- `BookingPage`: items,page,size,totalElements,totalPages,last; hiện PageResponse có alias content. Client normalize đúng một lần trong data/service, không mỗi màn đoán shape khác nhau.
- `BookingCustomerDetail`: thông tin quán/bàn/khách, tiền Customer, trạng thái/giờ/cancel eligibility, payment state, refund outcome, allowedActions do BE tính. QR/token nhạy cảm chỉ trả cho actor hợp lệ; không để public code endpoint trả đầy đủ PII/token.
- `BookingStaffDetail`: mã đơn, khách tối thiểu, lịch/bàn/guestCount,grossAmount,status,deadlines,allowedActions; không commission/net/discount/funding/bank. Owner xem tài chính ở API ví có guard riêng.
- `RefundOutcome`: actualPaidRefundPoints,voucherRestorationStatus,processedAt,eventId. Chỉ hiển thị “đã hoàn” sau transaction ledger hoàn thành.

## Routes và khung màn hình

| Mục | FE đề xuất | Mobile enum/path đề xuất |
|---|---|---|
| Bắt đầu đặt bàn | `/stores/[storeId]/book` | bookingCreate `/store/:storeId/book` |
| Thanh toán đơn | `/bookings/[bookingId]/payment` | bookingPayment `/bookings/:bookingId/payment` |
| Đơn của tôi | `/bookings` | myBookings `/profile/bookings` |
| Chi tiết đơn | `/bookings/[bookingId]` | bookingDetail `/bookings/:bookingId` |
| Return thanh toán | `/payment/return` | HTTPS return landing + app resume/deep link được allowlist |
| Ví Customer | `/profile/wallet` | moodPointsWallet đã có |
| Đổi voucher / tồn kho | `/profile/voucher-catalog`, `/profile/my-vouchers` | voucherCatalog/myVouchers đã có |
| Đơn cơ sở | Giữ `/merchant/reservations` làm URL UI tương thích, đổi datasource sang canonical bookings | merchantBookings `/merchant/bookings` |
| Ví Owner | `/merchant/wallet` hiện có | merchantWallet `/merchant/wallet` |

- Theme FE Customer theo Coffee/Cream của StoreDetail, Mobile theo AppColors/AppTheme; Owner/Admin giữ theme đang phân vai. Không redesign toàn bộ app.
- Form đặt bàn desktop hai cột: lựa chọn + tóm tắt chi phí; mobile một cột, nút xác nhận trong safe area, không che field/lỗi/keyboard. Lịch/slot có label và trạng thái, không chỉ màu.
- Điểm vào: CTA rõ trên chi tiết quán khi bookingEnabled; entry Đơn của tôi trong menu/profile cả hai client. Guest chọn lịch được, xác nhận mới yêu cầu login và khôi phục draft sau login.
- Loading/error/no category/closed/full đều có UI riêng. Không bật nút tạo đơn nếu chưa có quote hợp lệ; không hiển thị slot/món/đơn mock như dữ liệu thật.

## Task 0 — Contract audit và bảo toàn dữ liệu hiện có

**Files:** BE controllers/BookingServiceImpl/PayOSServiceImpl/Reservation services; FE StoreSidebar/StoreDetailView; MB router/StoreDetail; tạo `docs/contracts/booking-flow.md` và cập nhật coverage.

- [ ] Kiểm tra branch/diff/server version, đọc AGENTS. Map toàn bộ caller `/bookings` và `/reservations`, bảng/event/orderCode/QR/status/paging. Ghi contract đích ở trên vào file theo response thực tế.
- [ ] Ghi design canonical Booking + các service tài chính dùng chung; xác định chỗ scheduled jobs/webhook cũ sẽ đổi. Không chọn cách FE tạo booking xong tạo thêm reservation.
- [ ] Audit số đơn/tiền thực có ở hai bảng, attempt và ledger; phân loại legacy PAID cần đối soát. Không tự coi pending/paid cũ đã được thu/ghi ví chỉ từ status.
- [ ] Thiết lập fixture/test DB biệt lập; ghi baseline. Mỗi task sau có test đỏ trước sửa và test xanh sau, không dùng mocks làm bằng chứng DB race.

**Gate:** có API/state/role matrix và chiến lược cutover được ghi; không mất API cũ hoặc history.

## Task 1 — Canonical Booking, snapshot và migration tương thích

**Files BE:** `booking/entity/Booking.java`, `dto/request/CreateBookingRequest.java`, `dto/response/BookingResponse.java`, `mapper/BookingMapper.java`, `controller/BookingController.java`, repositories; Create quote DTO/controller/service, Customer/Staff DTO, `booking/service/BookingFinanceService.java`; migration mới sau version cao nhất.
**Tests:** `booking/BookingFinancialSchemaIT.java`, `BookingContractIT.java`, HTTP RBAC tests.

- [ ] Test snapshot 100k / discount 10k / commission 15%: Customer90k,Owner 85k; Customer/Staff JSON không lộ commission/net; quote/create chặn store/category sai hoặc guestCount vượt capacity.
- [ ] Bổ sung snapshot bất biến vào booking: gross/discount/paid/commission/net, funding split, rate/policy ID+version/rounding, voucherId, deadlines. Tái sử dụng ReservationPricingService logic hoặc tách calculator trung lập; không copy công thức sang client.
- [ ] Chuyển binding/dedup bằng migration additive: voucher thêm locked_booking_id, giữ locked_reservation_id cho history và cấm cùng lúc hai binding; rewards thêm booking_id FK/unique theo reward_type, cho reservation_id nullable với constraint đúng một nguồn; review thêm booking binding; attempt có business_type phân biệt Booking và Legacy Reservation. Audit/backfill từ record có bằng chứng, không gán bookingUUID vào FK reservations hoặc tự đổi ID lịch sử.
- [ ] Dữ liệu cũ không có snapshot/proof: đánh dấu legacy reconciliation, không backfill tiền ví theo rate hiện tại. Chốt cách phục vụ/hủy đơn legacy và đối soát trước cutover, không tự mint tiền khi callback cũ replay.
- [ ] Bảo toàn public code compatibility nhưng bỏ lộ PII/QR bearer; Customer detail yêu cầu ownership. Enum/allowedActions thống nhất, không cho client tự sửa status.
- [ ] Quote eligibility theo voucher/cấu hình; khi không có policy hiệu lực báo cấu hình thiếu, không fallback15%. Kiểm tra dates/overlap/precedence và không đổi giá đơn cũ.
- [ ] PostgreSQL fresh + upgrade cloneV44, Hibernate validate, JSON contract/RBAC tests pass.

## Task 2 — Tạo đơn, quota và intent bền vững

**Files BE:** `booking/service/BookingServiceImpl.java`, `booking/abstraction/repository/BookingRepository.java`, `payment/service/PaymentAttemptService.java`, `OrderCodeGeneratorService.java`, repositories/migration intent.
**Tests:** `booking/BookingCreationConcurrencyIT.java`, `BookingIntentRecoveryIT.java`.

**Interface:** create(selection,quoteId,actor,key) → một booking và payment attempt; khác payload cùng key409; cùng payload trả/resume cùng booking.

- [ ] Test hai request tranh slot cuối: một thành công, request còn lại SLOT_UNAVAILABLE; hai request cùng intent chỉ một booking/lock voucher/slot.
- [ ] Recheck config/capacity/time/store active/quote/voucher dưới khóa hoặc cơ chế DB bảo đảm quota. Availability GET chỉ để tham khảo; không findAll rồi đếm không khóa.
- [ ] Transaction prepare gồm booking snapshot, giữ slot, voucher LOCKED, attempt bền vững. Global orderCode không trùng subscription/sponsored/legacy booking.
- [ ] Unique DB actor+operation+intent và hash; xử lý loser sau rollback bằng replay transaction mới. Giữ lock order thống nhất để tránh deadlock.
- [ ] UNKNOWN/retry/restart resume workflow thật, không trả booking paymentUrl=null như thành công hoàn chỉnh. Hết thời gian giữ chỗ phải đồng bộ gateway/voucher/slot; pending gateway chưa cancel không tự release vì TTL.
- [ ] Không tạo booking/reservation thứ hai hoặc tiền ledger chỉ để làm UI chạy. Chặn request thiếu intent theo contract client mới, giữ compatibility cần thiết cho caller cũ có lộ trình.

## Task 3 — PayOS capture, zero-pay và khôi phục thanh toán

**Files BE:** `payment/service/PayOSServiceImpl.java`, `BookingPaymentCoordinator.java` (refactor sang canonical hoặc thay coordinator tương đương), `PaymentAttemptService.java`, `booking/service/BookingFinanceService.java`, legacy scheduler và reservation scheduler.
**Tests:** `booking/BookingPaymentLifecycleIT.java`, real SDK webhook tests hiện có, legacy subscription/sponsored regressions.

- [ ] Tạo link ngoài transaction giữ khóa; same orderCode retry tra/create đúng gateway attempt. Business type phân biệt lịch sử reservation với booking, không dispatch theo client description tùy ý.
- [ ] Webhook và polling cùng capture: verify chữ ký/amount/currency/order binding; snapshot→PAID_CONFIRMED + voucher USED + Merchant Pending một transaction. PAID không lùi UNKNOWN/EXPIRED khi response đến sai thứ tự.
- [ ] Zero-pay hợp lệ capture nội bộ một lần, không gọi PayOS. Nếu gateway mất cấu hình thì báo không khả dụng trước/đúng giai đoạn, không âm thầm tạo tiền.
- [ ] Payment create/callback/poll/server restart xen kẽ không mất link/tiền; duplicate callback không nhân Pending. Late-paid sau terminal phải ghi reconciliation/compensation, không chỉ log return.
- [ ] Nếu UI rời trang, gateway vẫn có thể thu: hỗ trợ resume từ booking detail, trạng thái UNKNOWN và tra lại. Không coi cancel return URL là hủy ngân hàng đã xác nhận.
- [ ] Test mock gateway cho các nhánh, real SDK HMAC cho parser/signature, PostgreSQL cho transaction/race. Không giả định PayOS có sandbox; giao dịch tiền thật là nghiệm thu riêng có người dùng cho phép.

## Task 4 — Vòng đời phục vụ, hoàn điểm, thưởng và QR

**Files BE:** `booking/service/BookingServiceImpl.java`, `booking/scheduler/BookingScheduledJobs.java`, `finance/service/ReservationRefundService.java`, `ReservationSettlementService.java`, `loyalty/service/LoyaltyRewardService.java`, QR service, `store/service/StoreReviewServiceImpl.java`; đổi các tên/refactor nếu cần nhưng không copy service song song.
**Tests:** `booking/BookingMoneyLifecycleIT.java`, `BookingQrAuthorizationIT.java`, `BookingCancellationRaceIT.java`.

- [ ] Staff/self check-in hợp lệ→CHECKED_IN, thưởng đúng giờ một lần, tiền vẫn Pending. Staff complete hoặc self-checkout hợp lệ→COMPLETED, Pending→Available và Spend Points một lần.
- [ ] TABLE_EXPIRED chỉ nhả slot, không giải ngân/thưởng. CLAIMED_LATE trước closing giải ngân + Spend Points, không thưởng đúng giờ/checkout. EXPIRED_NO_SHOW cuối ngày giải ngân0 điểm.
- [ ] Free cancel deadline từ snapshot/config được duyệt. Hủy paid đủ điều kiện: đảo Pending, hoàn actualPaid thành MoodPoints1:1, restore voucher, trả slot cùng transaction; replay không hoàn hai lần. Hủy pending chưa thu tiền không tạo điểm hoàn.
- [ ] Không dùng refundProcessed=false để mô tả đã hoàn. Customer API trả refund outcome thật sau ledger commit.
- [ ] QR store/table và booking/customer QR phân biệt rõ, signed binding store+booking+action+actor+nonce+expiry; chống replay/cross-store/đơn người khác. Không dùng QR token của đơn như chứng minh khách đang ở quán.
- [ ] Self-checkout bonus cần checkOut−checkIn >= 30 phút; dưới30phút không thưởng checkout (trả bàn có thể hợp lệ theo policy). Review có ảnh/visit binding đúng booking, tối đa1 lần; không chỉ kiểm tra cùng user/store.
- [ ] Chuyển hook cả hai scheduler cùng callback sang canonical, không chạy hai worker quyết định cùng đơn bằng logic khác nhau. Test concurrency cancel/capture/complete/no-show + ledger invariant.

## Task 5 — FE nền tảng booking API, state và test runner

**Create FE:** `src/types/booking.ts`, `src/services/booking.service.ts`, `src/hooks/booking/useBookingDraft.ts`, `useBookingQuote.ts`, `useBookingDetail.ts`, `src/lib/booking-intent.ts` (hoặc module tương đương có sẵn).
**Modify:** auth redirect/session integration hiện có; package/test config nếu cần.
**Tests:** service contract, draft/intent/session unit tests; setup component + Playwright runner có thật.

**Interface:** getSchedule/checkAvailability/quote/create/getDetail/getMyBookings/resumePayment/cancel/selfCheckIn/selfCheckOut; service relative paths theo bảng API; money không xử lý bằng floating-point client.

Chốt tên hàm FE để các task dùng thống nhất: `getBookingSchedule(storeId,date): Promise<StoreDailySchedule>`, `getBookingQuote(selection): Promise<BookingQuote>`, `createBooking(command,intentKey): Promise<BookingCustomerDetail>`, `getBookingDetail(bookingId): Promise<BookingCustomerDetail>`, `getMyBookings({page,size,status}): Promise<BookingPage>`, `resumeBookingPayment(bookingId): Promise<BookingCustomerDetail>`, `cancelBooking(bookingId,reason): Promise<BookingCustomerDetail>`. Self actions nhận `{bookingId,qrToken}` và trả detail mới. Tên type khớp contract Task 1.

- [ ] Normalize PageResponse/error codes một nơi; lỗi400/401/403/409/5xx có semantic handling. Không thêm axios client bypass auth proxy/token refresh hiện tại.
- [ ] Draft theo actor/store, giữ sau login/reload nhưng không credentials/QR secrets/full PII không cần thiết. Session thay đổi hủy request/context cũ; safe internal redirect, không open redirect.
- [ ] Persist intent trước submit; UNKNOWN giữ key/selection hash/bookingId để replay/lookup. Đổi payload sau unresolved intent phải giải quyết trạng thái cũ, không tự đẻ key mới.
- [ ] Request cancellation + context epoch cho effect/manual/action refresh, gồm A→B→A và action A xong sau khi đã chuyển B.
- [ ] TDD tests lost response/F5/two tabs/actor switch và service contract mới; tsc/lint mọi file mới pass.

## Task 6 — FE chọn bàn, thông tin khách và checkout quote

**Modify FE:** `src/components/store-detail/StoreSidebar.tsx`, `StoreDetailView.tsx`.
**Create:** `src/app/stores/[storeId]/book/page.tsx`, `src/components/booking/BookingForm.tsx`, `SchedulePicker.tsx`, `CategoryPicker.tsx`, `VoucherPicker.tsx`, `BookingPriceSummary.tsx`.
**Tests:** component/E2E entry→selection→quote→create.

- [ ] Bỏ giả thao tác set bookingRequested. CTA dẫn trang thật; quán không bật booking có lý do rõ, không khởi tạo config bật quán chỉ vì Customer xem lịch.
- [ ] Lịch/giờ/loại bàn từ schedule API, dùng timezone quán; chọn ngày VN gửi Instant đúng, xử lý qua nửa đêm/ngày đóng cửa/minAdvance/quota0.
- [ ] GuestCount/contact validation, prefill profile không khóa sửa dữ liệu hợp lệ; guest đăng nhập giữ lựa chọn và revalidate sau quay lại.
- [ ] Voucher picker lấy eligible theo quote/selection, giải thích min spend/funding/expiry; invalid voucher không biến thành discount bằng0 âm thầm. Chuyển ngày/bàn phải quote lại.
- [ ] Summary chỉ gross/discount/customerPaid, thời hạn hủy và điều kiện phục vụ; tiền và policy từ BE. Trước confirm yêu cầu quote current, giá đổi 409 yêu cầu user xem/xác nhận lại.
- [ ] Create commit rồi response mất→khôi phục đúng booking. SLOT_UNAVAILABLE giữ input và refresh lịch, không báo thành công. Redirect payment hoặc detail nếu zero-pay đã confirmed.
- [ ] Responsive 360/390/768/1440, keyboard/focus/labels và loading/error/empty; không che CTA hoặc mất nội dung bằng overflow hidden.

## Task 7 — FE payment, return và resume

**Create FE:** `src/app/bookings/[bookingId]/payment/page.tsx`, `src/app/payment/return/page.tsx`, `src/components/booking/BookingPaymentPanel.tsx`, `src/hooks/booking/useBookingPayment.ts`.
**BE phụ thuộc:** allowlist return/cancel destinations theo WEB/MOBILE, không nhận arbitrary external URL từ client.
**Tests:** Playwright fake gateway return/cancel/timeout/F5/duplicate callbacks.

- [ ] Hiển thị QR/link PayOS đúng format response, tiền cần trả và deadline server; không giả QR image từ raw text hoặc ép qrCode thành URL khi không phải URL.
- [ ] Return params chỉ để tìm đơn/hướng điều hướng; không dùng status=PAID từ URL để cộng tiền/confirm. Poll BE có backoff/giới hạn và cleanup; tab hidden không polling vô hạn.
- [ ] Reload/deep link lấy booking ownership và payment authoritative; UNKNOWN có “Kiểm tra lại”, PENDING có tiếp tục thanh toán, terminal có detail/hướng tiếp theo.
- [ ] Hủy trên gateway không tự hoàn điểm nếu chưa có xác nhận và nghiệp vụ hủy. Zero-pay không render QR/đợi PayOS.
- [ ] Return landing đọc bookingId/orderCode do server binding; không cho user thay ID để xem đơn khác. Auth hết hạn→login→resume đúng đơn.

## Task 8 — FE Đơn của tôi, chi tiết, ví Customer và QR

**Create FE:** `src/app/bookings/page.tsx`, `src/app/bookings/[bookingId]/page.tsx`, booking list/card/status timeline/cancel dialog/QR widgets; `src/app/profile/wallet/page.tsx`, `profile/voucher-catalog/page.tsx`, `profile/my-vouchers/page.tsx`.
**Modify:** `src/components/shell/UserNavDropdown.tsx`, `AppNavbar.tsx`, `src/app/profile/page.tsx`, wallet/voucher services hiện có.
**Tests:** list paging/filter/detail/auth/refresh/refund/voucher redemption and scanner fallback.

- [ ] Đơn của tôi có phân trang/filter/current-empty/error; detail deep link luôn GET từ BE, không dựa route state. Hiện hạn mức nước và tiền thực trả riêng, trạng thái quá grace rõ không ghi chung “đã hủy”.
- [ ] Cancellation theo allowedActions; confirm hiển thị số điểm dự kiến từ BE, sau success hiển thị refund outcome thật và refresh ví/voucher/lịch.
- [ ] QR tiếp nhận cho Staff chỉ hiện booking hợp lệ; self-checkin/out là quét mã vật lý riêng. Web camera cần permission/secure context; fallback Staff tại quầy, không tự bypass QR khi camera không dùng được.
- [ ] Thêm wallet/history/catalog/inventory frontend dùng API có sẵn; redeem dùng durable intent, trả voucherId, refresh đúng mọi query. Inventory all-status/paging, không chỉ usable.
- [ ] Sau hoàn tất có link Review đúng booking và ảnh; UI nêu chưa đủ điều kiện nếu BE từ chối, không hứa điểm chưa được server ghi.
- [ ] Không thêm bottom nav/tab quá tải; menu Đơn của tôi dễ tìm, desktop/mobile đều truy cập được.

## Task 9 — Mobile booking domain/data/controller và chọn bàn

**Create MB feature:** `lib/features/booking/domain/entities/` (Booking,BookingQuote,BookingPage,Schedule,Selection), `domain/repositories/booking_repository.dart`, `domain/usecases/` cho từng hành vi API; `data/dto/`, `data/datasources/booking_remote_data_source.dart`, `data/repositories/booking_repository_impl.dart`; `presentation/providers/booking_providers.dart`, `presentation/controllers/booking_draft_controller.dart`.
**Create pages/widgets:** `booking_create_page.dart`, schedule/category/voucher/price summary widgets.
**Modify:** `lib/features/store_detail/presentation/pages/store_detail_page.dart`, `lib/app/router/app_routes.dart`, `app_router.dart`.
**Tests:** `test/features/booking/{domain,data,presentation}/`.

UseCase tương ứng có `execute(command): Future<Result<Booking>>` cho create/action, `execute(query): Future<Result<BookingPage>>` cho list và `Future<Result<BookingQuote>>` cho quote; repository interface trả cùng Result. DataSource trả DTO và ném typed exception; repository chuyển sang Failure, UseCase không bắt lại một lỗi chung làm mất loại lỗi.

- [ ] Domain không import Flutter/Dio/storage; data DTO dùng freezed/json_serializable; repositories trả Result/Failure, UseCases thực sự có consumer. Manual Riverpod, không codegen provider.
- [ ] BookingSelection/Quote tương đương FE; normalize money nguyên/timezone/page ở data. Map typed API errors, không catch tất cả thành “không đủ điểm”.
- [ ] Provider scope actor/store/selection, cancel/epoch khi đổi context/logout; persist intent qua abstraction storage, draft restore phù hợp bảo mật.
- [ ] CTA StoreDetail đi route create thật, booking-disabled/closed/noquota states. Auth redirect giữ draft và revalidate. Android back/iOS swipe không tạo lại đơn.
- [ ] Form ngày/slot/category/guest/contact/voucher/summary parity FE; loading/error/refresh/keyboard-safe CTA; tests hết chỗ/quotechanged/response đảo thứ tự.
- [ ] Regenerate DTO bằng build_runner, không viết JSON trong domain/page; analyze và focused tests pass.

## Task 10 — Mobile PayOS, app lifecycle và deep link

**Create MB:** `presentation/pages/booking_payment_page.dart`, `controllers/booking_payment_controller.dart`, UseCases resume/getDetail; intent storage adapter; route/deep-link handlers theo router hiện có.
**Reuse:** cơ chế mở URL và lifecycle từ subscription payment nếu phù hợp, không copy nghiệp vụ confirm sang client.
**Tests:** fake gateway, widget/lifecycle/router tests và smoke test trên thiết bị.

- [ ] Mở checkout bằng cơ chế được hỗ trợ, giữ bookingId/key trước khi rời app. Khi resume/restart, đọc trạng thái BE; không phụ thuộc return link để nhận kết quả.
- [ ] HTTPS return landing mở app qua scheme/universal link cấu hình; máy chưa cài app có fallback web. Chỉ nhận destination được allowlist; không thay return URL toàn cục làm hỏng FE/subscription/sponsored.
- [ ] UNKNOWN/offline/cancel return/auth hết hạn có retry/resume. Polling có giới hạn, chỉ khi foreground; cleanup khi dispose và không điều hướng hai lần khi callback/lifecycle cùng đến.
- [ ] QR cho trường hợp thanh toán bằng thiết bị khác; mở checkout khi có URL. Zero-pay không mở gateway.
- [ ] Test kill app sau server commit, chuyển app ngân hàng rồi quay lại, callback trùng, logout/login user khác. Assert một booking và một lần ghi tiền.

## Task 11 — Mobile Đơn của tôi, detail, hủy và QR/Review

**Create MB:** `presentation/pages/my_bookings_page.dart`, `booking_detail_page.dart`, controllers paging/detail/action; QR scanner adapter qua abstraction theo package hiện có hoặc thêm dependency có lý do.
**Modify:** `features/profile/presentation/profile_page.dart`, router, wallet/voucher providers/pages, review form binding booking.
**Tests:** widget/integration booking journey, permission, deep link và lỗi quyền.

- [ ] Đơn của tôi có entry trong Profile, route detail ổn định, server allowedActions và timeline tương đương FE. Không điều hướng shell branch bằng push sai convention router.
- [ ] Hủy có refund summary/outcome rõ; invalidate ví/lịch sử/voucher đúng filter. Thất bại không đóng detail hoặc báo đã hoàn.
- [ ] Self check-in/out dùng mã vật lý có nonce/action binding. Permission bị từ chối có hướng liên hệ Staff; QR của đơn chỉ để đưa Staff xác nhận, không dùng tự điểm danh.
- [ ] My Vouchers giữ page metadata/load more, filter REVOKED, lỗi trang sau giữ dữ liệu cũ. Redeem thực sự đi qua UseCase, trả voucherId và refresh inventory.
- [ ] Review từ booking đủ điều kiện có ảnh/visit binding; chỉ refresh điểm sau award thật. Test mốc30phút và một review reward/đơn ở BE.

## Task 12 — Owner/Staff nhận cùng canonical booking trên FE/Mobile

**Modify FE:** `src/services/reservation.service.ts` (chuyển sang booking adapter, không duplicate), `src/app/merchant/reservations/page.tsx`, OwnerSidebar.
**Create MB:** feature `merchant_bookings` gồm domain/data/UseCases/controller/pages; entry MerchantManagementHub/router.
**BE:** list/summary/actions hiện có, DTO và role guard theo Task 1.
**Tests:** Customer create → Staff list/action cùng bookingId, cross-store/cross-role, response sai thứ tự.

- [ ] Đơn tạo FE hiện ở Mobile Staff và ngược lại. Giữ URL UI cũ cho bookmark nhưng chỉ đọc canonical bookings, không hai danh sách hai bảng.
- [ ] Hiện khách/giờ/loại bàn/gross; filter ngày/trạng thái/phân trang; nhận bàn bằng code/QR, giao nước trễ và hoàn tất theo allowedActions/deadline.
- [ ] Action ở A trả về sau khi đã chuyển B không được tải và ghi dữ liệu A lên B. Test A→B→A và manual refresh với context epoch.
- [ ] Chỉ CHECKED_IN được complete; CLAIMED_LATE không checkout thường. BE chặn thao tác lặp/stale, client refresh authoritative state.
- [ ] OWNER/MANAGER/CASHIER active đúng cơ sở; cả GET và POST response không lộ tài chính Owner. Chặn actor/cơ sở sai ở API, không chỉ UI.

## Task 13 — Owner wallet/payout và điểm đến đối soát

**Modify FE:** `merchant/wallet/page.tsx`, `admin/withdrawals/page.tsx`, `services/wallet.service.ts`, `admin/commission-policies/page.tsx`.
**Create MB:** feature `merchant_wallet` gồm domain/data/UseCases/controllers/pages, bank form, payout intent, route merchantWallet và entry chỉ Owner.
**Modify BE:** WithdrawalService/AdminWithdrawalController, bank account service, commission config theo findings còn mở.
**Tests:** booking paid → Pending → complete → Available → payout → Admin outcome, lost response/restart/security.

- [ ] Ví/lịch sử hiển thị tiền đến từ chính booking fixture và có bookingId/code truy vết. Làm pagination; không seed Available để coi đã tích hợp booking.
- [ ] Ngân hàng có validation, ownership/default; amount theo config. Durable intent cho FE/MB: hash/ownership/replay đúng; timeout → VERIFYING. Không gửi key mới khi chưa rõ kết quả cũ.
- [ ] Admin phải claim trước PROCESSING/reconciliation; không bypass khi claimedBy=null. Quyết định chưa chuyển tiền có verification reference/evidence/notes và audit mới unlock. UNKNOWN giữ Locked, bank reference unique; mất response confirm không hướng chuyển lại.
- [ ] Owner-only guard ở API/sidebar. Staff chỉ xem gross; form nhỏ vẫn thấy lỗi/nút; không trả ngân hàng trong Staff DTO.
- [ ] Commission: Global/Store, dates/description lưu thật, enabled khác effective, chọn cơ sở từ API, snapshot policy/version/rounding. Không mặc định15% khi đã tắt hết policy.

## Task 14 — Legacy reservation cutover và đóng lỗi tài chính

**Files:** compatibility adapters, PaymentAttemptService, VoucherLifecycleService, reconciliation/ledger services, schedulers, migrations và coverage doc.

- [ ] History reservation cũ có mapping/read-only endpoint khi cần. Attempt cũ còn sống được route/capture/compensate đúng; không bỏ webhook vì client đã đổi path.
- [ ] Không tạo hai đơn cho một intent. Ngừng public create reservation mới sau khi audit caller và có compatibility plan; không drop bảng/history hoặc tùy ý sửa businessId lịch sử.
- [ ] Đóng R1–R12 liên quan flow: expiry khi gateway còn pending; payout proof/claim; intent bền vững; resume; unique DB; redeem khác hash409; PAID không lùi; DTO Staff; stale context; voucher paging; commission fallback; capture thiếu binding.
- [ ] Ledger đủ cả nguồn/đích và unique event. Mismatch có record/audit bền vững, không chỉ log hoặc tổng số dư; reconciliation so nguồn gateway/booking/withdrawal.
- [ ] Policy chưa duyệt phải được ghi rõ dependency/giá trị hiện dùng. Không tự chốt điểm/thời hạn/hoa hồng; service/scaffold không có production caller chưa được coi hoàn thành.

## Task 15 — Dữ liệu test, E2E hai client và bàn giao

**Create:** fixture generator chỉ chạy test DB, `docs/testing/booking-e2e-checklist.md`, BE PostgreSQL suite, FE Playwright suite, MB integration tests và screenshot artifacts.

- [ ] Fixture có Customer/Owner/Manager/Cashier/Admin, quán A/B, ngân hàng test, hai loại bàn/quota, giờ quán và policy test. Ví bắt đầu 0 để kiểm chứng nhận doanh thu. Scenario voucher riêng cấp reward/refund lots có audit; generator rerun không nhân bản và không mặc định chạy DB server thật.
- [ ] Cross-client: FE tạo → Mobile xem, Mobile tạo → FE Staff phục vụ. Dùng chính bookingId/orderCode để truy vết, không ghép theo tên/quán/giờ gần đúng.
- [ ] BE package/verify với runner thực tế; PostgreSQL upgrade/schema/HTTP security/concurrency. Giữ real SDK signature và các regression cũ; tên IT chưa chứng minh có DB integration.
- [ ] FE tsc, scoped lint, build, component/browser E2E; MB build_runner khi đổi DTO, flutter analyze, flutter test, integration trên thiết bị. Mocks không thay nghiệm thu redirect/app resume/camera.
- [ ] Web360/390/768/1440; Flutter màn nhỏ/lớn, font scale, keyboard/permission/offline. Lưu screenshot và bằng chứng Network đã bỏ token/PII.
- [ ] Máy dev không host nổi BE: dùng backend test trên server/CI với DB riêng; FE local và Mobile trỏ cùng API test. Không tự seed/thu tiền thật trên server đang dùng. Camera web ngoài localhost và return app cần HTTPS phù hợp.
- [ ] Bàn giao task → file → API → caller FE/MB → kết quả test, policy blockers, migration/rollback/return/deep-link config và screenshots. Không gọi deploy/API200 là hoàn tất nghiệp vụ.

## Ma trận nghiệm thu bắt buộc

| Case | Kết quả phải chứng minh |
|---|---|
| Customer từ StoreDetail đến tạo đơn | CTA thật; lịch/quota/contact/quote hợp lệ; có bookingId thật |
| Hai client tranh slot cuối | Một đơn giữ slot; bên còn lại SLOT_UNAVAILABLE, không overbook |
| Gross100k, rate15%, voucher REWARD10k (fixture) | PayOS90k, Owner Pending85k; policy mới không đổi snapshot cũ |
| Voucher REFUND90k trên gross100k (fixture) | PayOS10k, Owner85k, nguồn tài trợ đầy đủ; không bị trần marketing chặn sai |
| Voucher phủ100% hợp lệ | Không gọi PayOS; capture/slot/ledger chỉ một lần |
| Gateway PENDING/UNKNOWN quá TTL | Không nhả voucher/slot như FAILED khi link còn có thể thu tiền; có reconcile/cancel proof |
| Webhook trùng + polling + retry | Một Pending credit, một USED; đúng binding; chữ ký giả400 |
| Mất response/F5/kill app sau server commit | Resume cùng booking/attempt/key, không nhân đơn hoặc treo link rỗng |
| Check-in rồi checkout sau ít nhất 30 phút | Giải ngân khi hoàn tất; Spend theo gross và thưởng đúng điều kiện, mỗi loại một lần |
| Checkout trước 30 phút | Không thưởng self-checkout; không lách bằng QR/đơn/actor khác |
| TABLE_EXPIRED → CLAIMED_LATE | Grace chỉ trả slot; nhận nước trễ giải ngân + Spend, không thưởng đúng giờ/checkout |
| No-show khi đóng cửa | Owner Available tăng net, Customer 0 điểm |
| Hủy paid đúng hạn | Hoàn actualPaid thành MoodPoints1:1, đảo Pending, restore voucher; retry không nhân điểm |
| Hủy pending chưa thu tiền | Không tạo điểm hoàn; release sau kết quả payment authoritative |
| Review xác thực | Đúng booking/actor/ảnh, một reward; request giả hoặc đơn người khác bị chặn |
| Payout mất response/cross-claim | Một lock; key bền vững; không unlock UNKNOWN hoặc thiếu proof |
| Đổi quán/ngày/user giữa request | Không dữ liệu A đè B, không lộ QR/PII/tài chính sai role |
| History/voucher trên 20 bản ghi | Tải trang tiếp/filter/retry đúng, không mất bản ghi |
| API lỗi/offline/quán đóng | Báo đúng trạng thái, giữ draft; không báo đã trả tiền/đã hoàn giả |

## Dependency / Definition of Done

Task 0→1→2→3→4 là nền BE. Task 5→6→7→8 và Task 9→10→11 dùng cùng contract sau Task 1; được làm UI với contract fixtures nhưng chỉ nghiệm thu khi nối BE thật. Task 12→13 chứng minh Customer→Owner; Task 14 là migration/correctness gate; Task 15 cuối cùng.

Hoàn thành khi Customer thao tác trên FE hoặc Mobile và Owner nhận đúng đơn/tiền; điểm/voucher/hoàn/rút chạy qua UI và có ledger chứng minh, không cần sửa DB giữa luồng. Chỉ có screens/API riêng lẻ hoặc seed Available rồi rút được **không đạt**.

## Lệnh và bằng chứng bàn giao

- BE tại `D:/Learn/MoodCafe-BE/moodcafe-be`: `./mvnw.cmd -Dtest=*Test,*IT verify`. Cấu hình runner/profile để test PostgreSQL thật được chạy; ghi tổng test/fail/error/skip từ report. Thiếu DB/dependency phải báo giới hạn, không skip rồi gọi verify pass.
- FE tại `D:/Learn/MoodCafe-FE`: `npx tsc --noEmit`, `npx eslint <các file sửa>`, `npm run build`. Task 5 thêm scripts `test:unit` và `test:e2e` nếu chưa có; chạy cả hai và ghi case/report, không chỉ thêm test file không được runner tìm thấy.
- MB tại `D:/Learn/MoodCafe-MB`: `dart run build_runner build --delete-conflicting-outputs` khi đổi DTO, `flutter analyze`, `flutter test`; integration test chạy với device/emulator được chỉ định và API test tương ứng.
- Thêm checklist thủ công bằng tên màn/nút/role/fixture, kết quả mong đợi và Network/DB event. Lưu screenshot thật cho mỗi màn mới, không dùng mockup thay ảnh chạy ứng dụng.

## Prompt gửi Antigravity

Thực hiện `2026-10-23-full-booking-fe-mobile-plan.md`. Hoàn thiện hành trình Customer FE/Mobile đến Owner, không chỉ thêm màn. Dùng canonical `/api/bookings`; tích hợp ví/điểm/voucher/commission vào vòng đời booking hiện có, không tạo thêm reservation song song. Giữ lịch/quota/PayOS/subscription/sponsored/auth đã deploy, migrations V1–V44 và strict SDK signature tests. Làm BE contract/idempotency/capture/refund/lifecycle trước, rồi StoreDetail → đặt bàn → voucher → PayOS/zero-pay → Đơn của tôi/detail → QR/hủy/review. Owner/Staff FE/Mobile phải thấy cùng đơn và ví tăng từ chính đơn đó. Một ví Customer, 1 điểm = 1đ; đổi điểm thành voucher trước checkout, không dùng trực tiếp điểm–PayOS. Mobile theo Clean Architecture/UseCases/Result/manual Riverpod; FE theo tài liệu Next trong repo, giữ theme hiện có. Đóng findings tài chính còn mở, test PostgreSQL/security/races/lost response và browser/device E2E. Giữ main/working tree cả ba repo; không tự commit/push/deploy/seed DB hoặc giao dịch tiền thật. Bàn giao API/caller/test/screenshots và phần chưa đạt; build pass hoặc số dư seed không chứng minh nghiệp vụ hoàn thành.
