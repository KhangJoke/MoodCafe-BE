# Review implementation full booking — 23/10/2026

## Kết luận

Chưa đủ điều kiện deploy. Anti đã nối tài chính vào booking chính, thêm quote và migration V45, bổ sung màn đặt bàn/chi tiết/payment return trên FE và Mobile. Tuy nhiên còn lỗi khởi động BE, hoàn sai điểm, xác thực check-in và nhiều lỗi contract Mobile khiến luồng chưa chạy xuyên suốt.

Đối chiếu với `2026-10-23-full-booking-fe-mobile-plan.md`. Review chỉ đọc code và chạy kiểm tra; không sửa sản phẩm, không deploy, không chạy migration hay thay đổi DB server.

P1: cần sửa trước khi deploy/test giao dịch thật. P2: cần hoàn thiện trước khi nghiệm thu full flow.

## Các lỗi cần sửa

### 1. [P1] BE không tạo được hai service khi khởi động

- `moodcafe-be/src/main/java/com/moodcafe/booking/service/BookingServiceImpl.java:85–88`
- `moodcafe-be/src/main/java/com/moodcafe/loyalty/service/LoyaltyRewardService.java:26–29`

Cả hai có constructor Lombok và constructor thủ công tương thích code cũ, nhưng không chỉ định constructor injection. Spring không chọn được constructor khi có nhiều constructor và không có constructor mặc định.

Đã probe bằng Spring AnnotationConfigApplicationContext với lớp đã compile: cả hai BeanCreationException, nguyên nhân NoSuchMethodException `<init>()` / No default constructor found. Test wiring hiện tại mock hoặc không đăng ký chính các service này nên không phát hiện.

Yêu cầu: thống nhất một constructor production hoặc đánh dấu rõ constructor Spring dùng; thêm kiểm tra wiring bằng service thật.

### 2. [P1] Đơn voucher 100% được hoàn thêm MoodPoints không có tiền thực trả

- `BookingServiceImpl.java:612–637`: zero-pay đặt actualPaidAmount bằng grossAmount.
- `BookingServiceImpl.java:963–997`: hủy khôi phục voucher và creditRefundPoints theo actualPaidAmount.

Ví dụ gross 100.000đ, voucher giảm 100.000đ, không trả PayOS: booking vẫn ghi actualPaidAmount 100.000đ. Hủy hợp lệ trả voucher đồng thời cộng thêm 100.000 MoodPoints. Có thể tái sử dụng voucher để lặp lại.

Yêu cầu: actualPaidAmount chỉ là tiền thực đã thu; zero-pay phải bằng 0. Hoàn tiền và trả voucher phải atomic/idempotent. Thêm test zero-pay → cancel, cancel lặp và voucher-funded booking. Giữ quy ước 1 MoodPoint = 1đ, không hoàn gross cộng thêm voucher.

### 3. [P1] Customer tự check-in/check-out từ xa, không cần QR hợp lệ

- `BookingServiceImpl.java:1361–1387`: selfCheckIn không đọc qrToken và không giới hạn thời gian hợp lệ.
- `BookingServiceImpl.java:1392–1417`: selfCheckOut không đọc qrToken.

Customer sở hữu đơn PAID_CONFIRMED có thể gọi API ngay cho đơn tương lai, nhận CHECKED_IN rồi COMPLETED, giải ngân cho quán và nhận Spend Points. Chờ đủ 30 phút còn có thể lấy thưởng check-out. Điều kiện sở hữu đơn không chứng minh khách đang ở quán.

Yêu cầu: chốt và thực thi bằng chứng QR tại cơ sở cùng cửa sổ thời gian phía server. Không dùng chính booking code/token đã trả cho customer làm bằng chứng có mặt. Giữ điều kiện thưởng check-out >=30 phút và chống cộng lặp.

### 4. [P1] Mobile không mở đúng trang đặt bàn

- `D:/Learn/MoodCafe-MB/lib/features/store_detail/presentation/pages/store_detail_page.dart:467–470`

CTA gọi `context.push('?storeName=')`, không điều hướng tới route đặt bàn theo storeId. Cần dùng route đã khai báo và truyền đúng cơ sở; kiểm tra từ màn chi tiết quán thật.

### 5. [P1] Mobile parse lịch khác cấu trúc BE, luôn không có slot

- `D:/Learn/MoodCafe-MB/lib/features/booking/domain/models/store_schedule_entity.dart:22–26`
- BE `booking/dto/response/StoreDailyScheduleResponse.java`

Mobile mong top-level timeSlots[].availableCategories; BE trả categories[].timeSlots. Một số tên giờ mở/đóng cũng khác. Probe đưa payload đúng BE vào parser thật: 1 slot thành 0 slot. UI báo không có lịch dù quán có bàn.

Yêu cầu: DTO data map đúng contract, domain không phụ thuộc JSON. Test fixture lấy đúng response BE, bao gồm nhiều loại bàn và ngày hết slot.

### 6. [P1] Mobile đọc số tiền thành 0 và bỏ qua thanh toán

- `D:/Learn/MoodCafe-MB/lib/features/booking/domain/models/booking_quote_entity.dart:22–29`
- `D:/Learn/MoodCafe-MB/lib/features/booking/domain/models/booking_entity.dart:83–93`
- `D:/Learn/MoodCafe-MB/lib/features/booking/presentation/pages/store_booking_page.dart:995–1001`

Mobile đọc basePrice/discountAmount/finalAmount/quoteExpiresAt, trong khi BE dùng grossAmount/voucherDiscount/customerPaidAmount/expiresAt; booking cũng đọc bookingTime thay vì startTime. Giá trị thiếu bị fallback 0 hoặc thời gian hiện tại. Probe quote và booking 90.000đ đều parse thành 0, requiresPayment=false; UI đi nhánh zero-pay dù booking còn PENDING_PAYMENT.

Yêu cầu: map đúng field và dùng trạng thái server quyết định bước tiếp theo; không coi field tiền bị thiếu là thanh toán miễn phí.

### 7. [P1] Mobile payment check sai envelope, return page tin trạng thái trên URL

- `D:/Learn/MoodCafe-MB/lib/features/booking/data/datasources/booking_remote_data_source.dart:206–209`: yêu cầu Map nhưng BE payment check trả Boolean trực tiếp.
- `D:/Learn/MoodCafe-MB/lib/features/booking/presentation/pages/payment_return_page.dart:40–46,81–83`: cancel bỏ qua lookup; status=PAID trên URL vẫn đủ báo thành công khi API lỗi hoặc chưa xác nhận.

Yêu cầu: parse đúng API, lấy booking/payment phía server làm nguồn xác nhận. URL chỉ là tín hiệu bắt đầu reconcile. API lỗi phải hiển thị đang xác minh/thử lại; không tự báo paid hoặc cancelled.

### 8. [P1] FE và Mobile tạo intent mới mỗi lần thử lại

- `D:/Learn/MoodCafe-FE/src/components/store-detail/StoreBookingModal.tsx:173`
- `D:/Learn/MoodCafe-MB/lib/features/booking/presentation/controllers/booking_form_controller.dart:180–182`

Mỗi submit tạo key mới. Khi server đã tạo booking nhưng client mất response, thử lại có thể tạo thêm booking/giữ thêm bàn và voucher thay vì lấy lại đơn cũ.

Yêu cầu: giữ intent theo draft/payload đã xác nhận, lưu bookingId khi biết, phục hồi sau reload/restart; chỉ đổi intent khi người dùng chủ động tạo đơn mới hoặc thay đổi draft cần intent mới.

### 9. [P1] Quote hiển thị không ràng buộc với create

- FE `StoreBookingModal.tsx:176,496`; `src/types/booking.ts` CreateBookingRequest thiếu quoteId.
- Mobile `booking_remote_data_source.dart:108–120`; quote entity không giữ quoteId, nút submit chỉ chặn lúc submitting.
- BE `BookingServiceImpl.java:552–605`: chỉ validate quote khi client có gửi quoteId.

Người dùng có thể tạo đơn không có quote hợp lệ hoặc trả giá khác quote đã xem. Quote lỗi/hết hạn vẫn có đường submit.

Yêu cầu: FE/Mobile giữ và gửi quoteId; chặn submit khi quote chưa hợp lệ. BE bắt buộc quote cho luồng mới, kiểm tra actor, payload, thời hạn và giá; thay đổi draft phải lấy quote mới.

### 10. [P1] Hết hạn/hủy pending giải phóng tài nguyên khi PayOS link còn trả được

- BE `booking/scheduler/BookingScheduledJobs.java:60–80`
- `BookingServiceImpl.java:963` trở đi
- `payment/service/PayOSServiceImpl.java:579–589`

Job timeout hoặc hủy pending trả voucher/bàn mà chưa reconcile/cancel gateway. Webhook tới muộn chỉ confirm nếu booking còn PENDING_PAYMENT, nhưng vẫn đánh dấu attempt PAID. Khách đã trả tiền sau khi đơn bị đóng sẽ không được xác nhận hoặc bù hoàn tương ứng.

Yêu cầu: reconcile gateway trước khi kết thúc pending; xử lý race webhook/timeout/cancel và late-paid có bù hoàn idempotent. UNKNOWN phải được giữ để xử lý, không coi là thất bại chắc chắn.

### 11. [P1] Gọi tạo link PayOS trong transaction tạo booking

- `BookingServiceImpl.java:474,527,693–709`

Giữ transaction và pessimistic lock qua network call. Khi gateway nhận đơn nhưng response timeout, exception của service transactional có thể đánh dấu rollback-only; catch rồi lưu UNKNOWN trong cùng transaction không đảm bảo giữ được booking/attempt. Có nguy cơ link trả được nhưng local record rollback.

Yêu cầu: persist intent/attempt trước, gọi gateway ngoài transaction giữ bàn, sau đó finalize trạng thái bằng transaction riêng; có reconcile cho kết quả không xác định. Test accepted-but-timeout bằng gateway stub.

### 12. [P1] GET có thể chuyển no-show nhưng không giải ngân

- `BookingServiceImpl.java:1491–1504`: reconcileBookingStatus lưu EXPIRED_NO_SHOW, không settlement.
- `BookingScheduledJobs.java:166`: job chỉ lấy TABLE_EXPIRED.

Nếu customer/staff GET sau giờ đóng cửa trước cron, đơn đã thành EXPIRED_NO_SHOW nên cron bỏ qua; tiền quán nằm pending vô thời hạn.

Yêu cầu: dùng chung transition nghiệp vụ atomic/idempotent có settlement, hoặc không đổi trạng thái trong GET. Test GET và cron chạy đua; no-show phải giải ngân đúng một lần, 0 điểm.

### 13. [P1] Webhook/poll production không truyền số tiền thực nhận

- `PayOSServiceImpl.java:580,653–654`
- `BookingServiceImpl.java:761`

Webhook/poll gọi overload confirmBookingPayment(orderCode) thay vì truyền paidAmountVnd đã có. Overload tự dùng số tiền kỳ vọng nên guard sai số tiền trong overload mới không bảo vệ đường production.

Yêu cầu: truyền số tiền gateway đã xác minh từ cả webhook và polling; phân biệt thiếu/thừa/đúng tiền và lưu thực nhận. Test qua entry point webhook/poll, không chỉ gọi trực tiếp service overload mới.

### 14. [P2] Voucher hoàn tiền hết hạn bị trả về AVAILABLE nhưng không sử dụng được

- `BookingServiceImpl.java:968`

Cancel đặt AVAILABLE trực tiếp kể cả voucher hết hạn, bỏ qua policy khôi phục giá trị voucher nguồn REFUND trong lifecycle service. Checkout/inventory từ chối voucher hết hạn nên giá trị hoàn tiền có thể bị kẹt.

Yêu cầu: dùng lifecycle chung theo nguồn voucher và policy đã duyệt; bảo toàn giá trị hoàn tiền, không cộng lại điểm/voucher nhiều lần.

### 15. [P2] Một số lỗi FE còn làm sai trải nghiệm booking

- `StoreBookingModal.tsx:75–82,288`: đổi ngày reset slot nhưng vẫn cho chọn slot cũ khi lịch mới loading; response mới không xóa lựa chọn phát sinh giữa chừng. Có thể hiển thị ngày B nhưng gửi startTime ngày A. Chặn lựa chọn lịch stale, ràng buộc selection với ngày/category response.
- `src/app/bookings/[bookingId]/page.tsx:252`: gửi qrCodeToken sang api.qrserver.com trong URL ảnh. Token booking bị truyền cho bên thứ ba. Sinh QR tại client bằng thư viện cục bộ.
- Cùng file `:295–300`: đọc refundOutcome.refundProcessed và refundAmountPoints; contract thực là refundProcessed top-level và refundOutcome.actualPaidRefundPoints. Màn hoàn tiền không phản ánh kết quả thật.
- `src/app/payment/return/page.tsx:31`: URL cancel bỏ qua reconcile; `:53` đọc infoRes.data.data.referenceCode dù endpoint trả object trực tiếp. Sửa theo response thật và trạng thái server.
- `src/app/merchant/reservations/page.tsx:98,111,124`: action đang chờ của cơ sở A có thể gọi fetch cũ sau khi chuyển B và ghi danh sách A dưới context B. Cần selection epoch/storeId guard cho cả action và fetch.

### 16. [P2] Mobile thiếu phục hồi trạng thái và lịch sử đầy đủ

- `booking_form_controller.dart:128–145`: quote response không có guard; response cũ có thể ghi đè draft mới.
- `booking_detail_page.dart:416–420`: chỉ tiếp tục thanh toán khi đã có URL; chưa có resume payment-link/reconcile cho UNKNOWN hoặc URL mất/hết hạn.
- `booking_providers.dart:83–86`: lịch sử chỉ page 0 mặc định 20, datasource bỏ metadata.
- `booking_detail_page.dart:145–151`: thông báo hoàn dựa trên dữ liệu trước hủy, bỏ kết quả refundOutcome; chưa invalidate ví/voucher liên quan.
- Chưa thấy native deep link/restart recovery hoàn chỉnh; chưa có kiểm tra foreground return từ ứng dụng ngân hàng.

## Phần đã có và phạm vi còn thiếu

Đã có cải thiện: canonical booking có hook quote, tiền/voucher/settlement; FE và Mobile đều có mã nguồn màn đặt/chi tiết/return. MerchantReservationController đã adapter canonical booking vào API merchant cũ, vì vậy không kết luận sai rằng dùng URL reservation là hoàn toàn không thấy booking mới.

Chưa thể nghiệm thu full plan:

- FE chưa đủ màn ví điểm, catalog/đổi voucher và inventory; link `/vouchers` trong modal chưa có trang tương ứng. Thiếu entry lịch sử booking dễ tìm và guest login phục hồi draft.
- FE/Mobile chưa hoàn chỉnh physical QR/self-service, payment resume và return theo server authority.
- Mobile chưa có đầy đủ merchant booking/wallet cho phạm vi Owner/Staff của plan.
- Mobile còn JSON parsing trong domain, catch-all failure/raw exception paths; cần giữ Clean Architecture, UseCase + Result/Failure, manual Riverpod theo AGENTS.md.
- Chưa có bằng chứng chạy V45 trên PostgreSQL với dữ liệu booking cũ, kiểm tra backfill số tiền, binding/unique constraints và rollback. Không được suy ra migration an toàn chỉ từ unit test.
- Chưa có browser/device E2E customer → PayOS → staff → settlement → wallet/refund; chưa có ảnh xác minh UI responsive. Review này không xác nhận giao diện đã đẹp hoặc không vỡ layout trên thiết bị thật.

## Kết quả kiểm tra

| Kiểm tra | Kết quả |
|---|---|
| BE `./mvnw.cmd -Dtest=*Test,*IT test -q` | 297 tests; 1 failure, 2 errors, 1 skipped |
| BE Spring constructor probe | Cả BookingServiceImpl và LoyaltyRewardService không tạo bean được |
| FE `npx tsc --noEmit` | Pass |
| FE scoped ESLint booking/payment/store-detail | 0 errors, 3 warnings |
| MB `flutter analyze` | 11 issues: 1 warning + 10 info |
| MB `flutter test --no-pub` | 327 tests pass |
| MB actual-parser contract probe | 1 slot → 0; 90.000đ → 0; nhánh zero-pay sai |

BE failure/error hiện tại nằm trong MerchantReservationControllerIT do chưa bổ sung mock BookingRepository cho dependency mới; cần sửa test setup và bổ sung integration thật. Không nhầm lỗi test này với lỗi constructor production đã probe riêng.

Log/probe nằm trong thư mục ignored `D:/Learn/MoodCafe-BE/infra/aws/.deploy-work/`: `fullflow-review-tests.log`, `fullflow-mobile-tests.log`, `FullFlowStartupProbe.java`, `FullFlowMobileContractProbe.dart`.

Read-only OpenAPI server hiện vẫn 170 paths, chưa có `/api/bookings/quote`, `/{bookingId}/customer`, `/{bookingId}/self-check-in`. FE local đang trỏ server này nên code mới chưa thể test đầy đủ với backend deploy hiện tại. Đây là chênh lệch phiên bản môi trường; review không deploy V45.

## Thứ tự sửa và điều kiện bàn giao

1. Sửa BE startup, chạy context wiring bằng service thật.
2. Sửa tiền thực nhận, zero-pay refund, voucher restoration, lifecycle no-show và xác thực QR/time.
3. Hoàn thiện payment coordinator/reconcile/late-paid cùng quote + idempotency xuyên FE/MB/BE.
4. Sửa CTA/DTO/envelope Mobile, UI return/resume và các race FE/MB.
5. Hoàn thiện màn còn thiếu; kiểm tra migration trên DB staging có dữ liệu cũ trước khi cập nhật môi trường dùng chung.
6. Chạy lại BE tests, FE type/lint/build, Flutter analyze/test; bổ sung contract/lifecycle tests, rồi E2E trên staging. Chỉ nghiệm thu khi đi được customer đặt bàn → PayOS → check-in/out hoặc late/no-show → ví Owner/điểm Customer, kèm cancel/refund/voucher và retry sau mất mạng.

Giữ quyết định nghiệp vụ: Customer chỉ có MoodPoints, 1 điểm = 1đ; đổi voucher trước checkout, không thanh toán trực tiếp bằng ví, không trộn ví–PayOS. Refund chỉ phần tiền thực trả; không đổi lại thành mô hình ví VNĐ tách riêng.
