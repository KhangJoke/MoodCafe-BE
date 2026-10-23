# [ĐÃ THAY THẾ — KHÔNG TRIỂN KHAI] Ví đối soát, điểm thưởng, voucher và hoa hồng

> Người dùng đã chốt **Cách 1: một số dư MoodPoints, 1 MoodPoint = 1đ**. Dùng [plan mới](./2026-10-20-single-moodpoints-final.md). Toàn bộ thiết kế hai số dư Customer, credit và thanh toán ví trực tiếp bên dưới chỉ lưu lịch sử, không còn là hướng triển khai.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Hoàn thiện dòng tiền và khuyến mãi gắn với đơn đặt bàn: thu tiền, ghi nhận công nợ quán, hoàn vào ví khách, tích điểm, đổi voucher, rút tiền và đối soát.

**Architecture:** BE là nguồn tính toán và quyết định trạng thái duy nhất; dùng sổ giao dịch bất biến, transaction DB, khóa số dư và chống xử lý trùng. FE phục vụ Admin/Owner/Cashier; Mobile phục vụ Customer và Owner theo các tính năng hiện có. Tích hợp vào vòng đời đặt bàn đã có trên nhánh được nhóm xác nhận.

**Tech Stack:** Spring Boot/Java, PostgreSQL/Flyway, PayOS; Next.js/React/TypeScript; Flutter/Riverpod, Clean Architecture.

**Spec:** `2026-10-20-wallet-requirement-original.txt` cùng thư mục, kết hợp yêu cầu chat ngày 20/10/2026: tiền hoàn vào ví khách, không rút tiền mặt.

**Trạng thái:** BẢN ĐỀ XUẤT ĐỂ DUYỆT — đã cập nhật review do người dùng chuyển ngày 20/10/2026. Chưa cho phép triển khai các quyết định nghiệp vụ còn mở. Các endpoint, schema, trạng thái mới dưới đây là đề xuất, không phải API đã tồn tại.

## 1. Tóm tắt gửi người duyệt

Hệ thống đặt bàn đã có theo thông tin của nhóm. Phần bổ sung gồm:

1. **Ví quán:** theo dõi tiền tạm giữ, tiền được rút, tiền đang chờ chuyển; có lịch sử giao dịch và truy ngược tới đơn đặt bàn. Đề xuất mỗi cơ sở một ví, Owner xem tổng hợp và chọn cơ sở.
2. **Rút tiền:** Owner chọn tài khoản ngân hàng, tạo yêu cầu; hệ thống khóa số tiền đó. Admin kiểm tra, dùng VietQR để chuyển khoản và xác nhận bằng chứng giao dịch. Từ chối yêu cầu sẽ trả tiền về số dư khả dụng.
3. **Ví khách:** cùng màn hình, tách số dư hoàn tiền VNĐ và MoodPoints; không rút tiền mặt. Khách bật dùng số dư: đủ tiền thì trừ ví và xác nhận đơn không qua PayOS; thiếu tiền thì khóa phần ví và trả phần thiếu qua PayOS; thất bại thì nhả khóa.
4. **Voucher:** Admin tạo chương trình, Customer đổi điểm lấy voucher cá nhân; tạo đơn chờ thanh toán thì LOCKED, thanh toán thành công thì USED, thất bại hoặc hủy đúng hạn thì khôi phục AVAILABLE nếu còn hạn. Voucher đổi từ MoodPoints do MoodCafe tài trợ, không giảm tiền quán thực nhận.
5. **Hoa hồng:** Admin cấu hình tỷ lệ; BE chốt số tiền và phiên bản chính sách trên từng đơn. Chỉnh cấu hình chỉ tác động đơn mới.
6. **Vòng đời và điểm:** TABLE_EXPIRED chỉ nhả bàn, giữ tiền pending; CLAIMED_LATE giải phóng tiền quán và cộng Spend Points; EXPIRED_NO_SHOW cuối ngày giải phóng tiền quán nhưng không cộng điểm. Spend Points tính trên gross_amount; điểm check-in đúng giờ, self check-out đủ 30 phút và review xác thực có ảnh được kiểm tra riêng.
7. **Màn hình và báo cáo:** chỉ OWNER có quyền ví quán/ngân hàng/rút tiền; MANAGER và CASHIER bị chặn cả API lẫn giao diện. Admin dùng API đối soát riêng. Có ví Owner, xử lý rút tiền Admin, cấu hình hoa hồng/voucher/điểm; ví/voucher Customer; thu ngân thấy hạn mức đồ uống gốc của đơn.

**Không thực hiện ở bước hiện tại:** sửa code sản phẩm, migration DB thật, chuyển tiền thật hoặc triển khai server. Tài liệu này để nhóm duyệt rồi giao Antigravity.

## 2. Requirement đã nhận và phần còn thiếu

- File gốc có Phần 1, Phần 2 và Phần 3.1; kết thúc ngay tại tiêu đề **3.2. Ma trận Biến động Số dư**, không có nội dung sau đó.
- Đã xác định: ba ngăn ví quán; năm số tài chính bắt buộc trên đơn; voucher do sàn tài trợ; MoodPoints dùng đổi voucher; thu PayOS và chuyển tiền thủ công qua VietQR.
- Chat bổ sung: hoàn tiền đặt bàn vào ví Customer, không rút ra tiền mặt.
- Review bổ sung đã làm rõ thanh toán bằng credit, ba mốc đi trễ, cơ sở gross cho Spend Points, điều kiện 30 phút, voucher LOCKED và quyền OWNER. Còn cần duyệt: scope ví, tỷ lệ/hạn điểm, hủy muộn, phí/hạn mức rút, policy ưu tiên và ngoại lệ điểm Review cho CLAIMED_LATE (D15).
- Giá trị 15%, 10.000đ, 100.000đ trong file là **ví dụ**, không mặc nhiên là cấu hình sản phẩm.

### Kết quả đối chiếu code local ngày 20/10/2026

- BE tại `D:/Learn/MoodCafe-BE/moodcafe-be`: có `payment/service/PayOSService.java`, `payment/controller/PaymentController.java`; PayOS đang có liên kết `SubscriptionPayment`.
- Có module `configuration` và `AdminSystemConfigurationController.java` để tham khảo cơ chế phân quyền/config hiện tại.
- FE tại `D:/Learn/MoodCafe-FE`: có layout Admin/merchant, trang settings/profile; chưa tìm thấy module booking/reservation/wallet/voucher trong cây `src` đang đọc.
- Mobile tại `D:/Learn/MoodCafe-MB`: có profile/router, subscription và sponsored payment; chưa tìm thấy module booking/reservation/wallet/voucher trong cây `lib` đang đọc.
- Đây là nhận xét về checkout local, **không kết luận nhóm chưa làm đặt bàn**. Bước 0 phải xác định commit/branch có flow đặt bàn. Không tự tạo module đặt bàn thay thế.

## 3. Các quyết định cần duyệt trước khi code

| Mã | Câu hỏi phải chốt | Phương án đề xuất để duyệt |
|---|---|---|
| D1 | Ví quán theo Owner hay Store? | Mỗi Store một ví; Owner có trang tổng hợp, rút theo từng Store. Không chuyển số dư giữa cơ sở trong đợt này. |
| D2 | Hoàn tiền vào điểm hay tiền trong ví? | Review dùng hai ngăn: refund_credit_balance VNĐ và reward_points MoodPoints. Cả hai không rút/chuyển sang người khác. |
| D3 | Số dư hoàn tiền được dùng thế nào? | Bật dùng ví: credit đủ thì trả toàn bộ, PAID_CONFIRMED, không PayOS; thiếu thì khóa phần khả dụng và PayOS phần còn lại; PAYMENT_FAILED nhả khóa. Xem 5.2. |
| D4 | Có quy đổi tiền hoàn thành MoodPoints không? | Theo hướng review: không; giữ VNĐ riêng. Phương án quy đổi thành điểm cũ được bỏ khỏi phạm vi hiện tại. |
| D5 | Khi nào pending được giải phóng? | COMPLETED hoặc CLAIMED_LATE: chuyển available; TABLE_EXPIRED chỉ nhả bàn; EXPIRED_NO_SHOW khi hết hạn nhận nước cuối ngày: chuyển available, không cộng điểm. |
| D6 | Hủy và hoàn bao nhiêu? | Hủy trước free_cancel_hours: CANCELLED_REFUNDED, hoàn giá trị đã thanh toán vào credit. Mức hoàn khi lỗi quán/hủy muộn và phí cần duyệt; EXPIRED_NO_SHOW giải ngân cho quán theo review. |
| D7 | Điểm thưởng và hạn dùng? | Bốn nguồn tại mục 8: spend theo gross, check-in đúng giờ, self check-out >=30 phút, review xác thực có ảnh. Còn chốt mức điểm/trần/hạn dùng/thu hồi. |
| D8 | Khôi phục voucher? | PAYMENT_FAILED: LOCKED → AVAILABLE; CANCELLED_REFUNDED đúng hạn: USED → AVAILABLE, chỉ nếu còn hạn; hết hạn → EXPIRED. Không hoàn thêm điểm đổi voucher. Ngoại lệ lỗi quán cần duyệt. |
| D9 | Hoa hồng cấu hình theo cấp nào? | Global mặc định + override từng Store, có thời gian hiệu lực. Không cho sửa tùy ý hoa hồng của đơn đã chốt. |
| D10 | Voucher có được giảm vượt hoa hồng? | Đợt 1 không cho vượt hoa hồng; nếu muốn hỗ trợ quỹ marketing phải bổ sung ngân sách và bút toán tài trợ. |
| D11 | Quyền và hạn mức rút? | OWNER đúng cơ sở được đọc ví/ngân hàng/tạo lệnh; MANAGER và CASHIER bị cấm toàn bộ API ví quán/rút/ngân hàng. min_payout_amount <= amount <= available_balance; 100.000đ là ví dụ, số chính thức/phí/hạn tối đa/xác minh ngân hàng cần chốt. |
| D12 | Phạm vi UI? | FE Admin/Owner/Cashier; Mobile Customer + Owner wallet. Customer web chỉ bổ sung nếu web có đặt bàn. |
| D13 | Hoàn tiền có hiệu lực/điểm hết hạn không? | Đề xuất số dư hoàn tiền không hết hạn; điểm thưởng và voucher có hạn dùng được chốt riêng. |
| D14 | Cộng Spend Points trên tiền nào? | Theo review: gross_amount tại COMPLETED hoặc CLAIMED_LATE, kể cả đơn dùng voucher/credit. Thay thế đề xuất cũ tính trên PayOS; chống farm bằng trạng thái phục vụ và key theo đơn, xử lý đảo điểm khi hoàn. |
| D15 | CLAIMED_LATE có được thưởng Review không? | Hai câu review chưa thống nhất: “chỉ Spend Points” và “Review có ảnh sau CLAIMED_LATE”. Đề xuất: tại giao nước trễ chỉ cộng Spend Points, không thưởng check-in/check-out; sau đó review xác thực có ảnh vẫn được thưởng một lần. Cần nhóm xác nhận trước khi code nhánh Review này. |

Các nội dung từ review là đầu vào của bản sửa, chưa phải phê duyệt triển khai. Người duyệt xác nhận các hàng đã làm rõ và trả lời phần còn mở; D15 phải chốt để tránh hai cách triển khai trái nhau.

## 4. Global Constraints

- Quán nhận doanh thu dựa trên `gross_amount`, không bị gánh voucher của sàn.
- `gross_amount = table_category.min_spend_amount` theo requirement; phải kiểm tra flow hiện có có số lượng bàn/phụ phí hay không trước khi mở rộng công thức.
- BE tính và snapshot giá, tỷ lệ, chính sách; không nhận số tiền do client quyết định.
- Customer không rút tiền mặt; không chuyển điểm/số dư sang tài khoản khác.
- Tất cả tiền VNĐ là số nguyên; Java dùng BigDecimal và quy tắc làm tròn được duyệt, đề xuất HALF_UP tới đồng; không dùng float/double cho tiền.
- Mọi số dư không âm. Ledger không sửa/xóa, sửa sai bằng giao dịch đảo có liên kết.
- API áp dụng đúng quyền Customer/Owner/Store/Admin; không tin storeId/userId do client gửi.
- Mobile tuân thủ `presentation → domain ← data`, UseCase, Result/Failure, Riverpod manual và freezed/json_serializable theo AGENTS.md.
- Chỉ triển khai migration mới; không sửa checksum migration đã chạy. Không cập nhật server thật trong quá trình phát triển.
- Không ghép các khoản thu subscription/sponsored với tiền đặt bàn; chung PayOS nhưng có loại mục đích giao dịch riêng.

## 5. Công thức đơn và ví khách

### 5.1 Trường bắt buộc và snapshot

`gross_amount`, `voucher_discount`, `customer_paid_amount`, `commission_amount`, `merchant_net_amount`.
Bổ sung `commission_rate_snapshot`, `commission_policy_id/version`, `voucher_instance_id`, `voucher_policy_snapshot`, `cancellation_policy_snapshot`, tiền tệ VND và thời điểm báo giá hết hạn.

Ví dụ gốc: gross 100.000, voucher 10.000, rate 15% → customer_paid 90.000, commission 15.000, merchant_net 85.000, platform_net 5.000.

`commission_amount = round(gross_amount × rate)`; `merchant_net_amount = gross_amount − commission_amount`.
`customer_paid_amount = gross_amount − voucher_discount` là tổng giá trị khách phải trả.
`platform_net_revenue = commission_amount − voucher_discount` trước phí cổng thanh toán/chi phí khác.

### 5.2 Dùng số dư hoàn tiền trả đơn — ba trường hợp bắt buộc

UI có toggle **Dùng số dư hoàn tiền trong Ví**. Tắt: credit_used = 0; bật: BE lấy min(số dư khả dụng, customer_paid_amount), không tin số dư client.
Quy ước refund_credit_balance là phần credit đang khả dụng; reserved_credit là phần khóa riêng. Reserve X: khả dụng −X, reserved +X; capture: reserved −X, ghi khoản đã chi; release: reserved −X, khả dụng +X. Không trừ khả dụng hai lần.

| Trường hợp | Ví khách | Đơn / PayOS |
|---|---|---|
| Đơn 90.000, ví 100.000 | Capture nguyên tử 90.000, còn khả dụng 10.000 | PAID_CONFIRMED, payos_amount=0, không tạo payment link; ghi pending quán và voucher USED cùng transaction |
| Đơn 90.000, ví 30.000 | Lock 30.000: khả dụng 0, reserved 30.000 | PENDING_PAYMENT, tạo PayOS 60.000; xác minh thành công mới capture khóa, PAID_CONFIRMED và cộng pending quán |
| Đơn kết hợp trên PAYMENT_FAILED/hết hạn được xác nhận | Release 30.000: khả dụng 30.000, reserved 0 | Không ghi nhận tiền quán; nhả voucher theo mục 8 |

Khóa ví/voucher và tạo reservation nguyên tử. Lỗi tạo link chắc chắn chưa thu tiền thì nhả cả hai; timeout chưa rõ thì đối soát trước, không nhả chỉ vì client đóng màn hình. Webhook đến sau khi đã nhả khóa phải theo luồng late payment, không trừ lại ví đã được khách dùng cho đơn khác. Khóa theo reservationId, replay release/capture không thay đổi số dư lần hai.
Tên trường chuẩn API/DB trong plan là payos_charge_amount; payos_amount trong review cùng nghĩa, không tạo hai trường độc lập.

Định nghĩa thêm `refund_credit_used` và `payos_charge_amount`:
`payos_charge_amount = customer_paid_amount − refund_credit_used`.
PayOS nhận `payos_charge_amount`; đây là thay đổi so với công thức file gốc khi chưa có ví hoàn tiền.
Ví dụ trên dùng thêm 20.000 credit → PayOS 70.000, credit 20.000, quán vẫn nhận 85.000.
Không đánh giá khả năng thanh toán cho quán chỉ bằng tiền PayOS của riêng đơn này; credit là nghĩa vụ đã được tài trợ từ khoản thu trước.
Nếu PayOS amount bằng 0 thì không tạo payment link; hoàn tất thanh toán nội bộ nguyên tử, vẫn ghi payment allocation và ledger.
Đợt 1 không cộng dồn nhiều voucher, không trả đơn bằng reward points trực tiếp; points chỉ dùng đổi voucher.

### 5.3 Hoàn đúng giá trị đã thanh toán

Ví dụ đơn 100.000 giảm 10.000, trả PayOS 70.000 + credit 20.000: hoàn toàn phần là 90.000 vào credit, không phải 100.000. Voucher xử lý riêng theo D8.
Tổng các lần hoàn không vượt PayOS đã xác nhận + credit đã tiêu, trừ phần đã hoàn; không hoàn tiền khi đơn chưa có thanh toán thành công.
Nếu đã giải phóng/rút tiền quán: không làm âm ví tự động; ghi khoản phải thu/điều chỉnh và đưa Admin xử lý theo chính sách được duyệt.

## 6. Ma trận sự kiện tài chính đề xuất

Tên sự kiện dưới đây là ngữ nghĩa; map sang enum đặt bàn thật ở Bước 0.

| Sự kiện | Ví quán | Ví khách / voucher | Chốt chặn |
|---|---|---|---|
| Tạo báo giá | Chưa cộng tiền | Kiểm tra voucher/credit | Quote có TTL và snapshot |
| Bắt đầu trả đơn | Chưa cộng tiền | Giữ voucher, reserve credit | Một reservation/payment attempt có khóa chống trùng |
| Thanh toán thành công được xác minh | pending + merchant_net | Capture credit, voucher USED | Webhook và polling cùng gọi một nghiệp vụ |
| Thanh toán thất bại/hết TTL trước khi thu | Không đổi | Nhả credit và voucher giữ | Phải tra lại gateway khi trạng thái chưa rõ |
| Hoàn tất phục vụ | pending − net; available + net | Cộng điểm theo D7/D14 | Chỉ một settlement cho mỗi đơn |
| CHECKED_IN hợp lệ, đúng giờ | Giữ pending | Thưởng check-in đúng giờ một lần | Kiểm tra thời gian và quyền xác nhận trên server |
| TABLE_EXPIRED sau grace_period_mins | Giữ pending, chỉ nhả bàn | Không cộng điểm tại sự kiện này; giữ quyền lấy nước tới giờ đóng cửa | Không hủy thanh toán, không tự hoàn, không giải ngân |
| CLAIMED_LATE do thu ngân xác nhận giao nước | pending − net; available + net | Spend Points theo gross; không thưởng check-in/check-out | Đơn đã trả, từng TABLE_EXPIRED, chưa hết hạn nhận nước; Review sau đó theo D15 |
| EXPIRED_NO_SHOW cuối ngày cơ sở | pending − net; available + net | 0 MoodPoints | Chỉ đơn đã trả, hết hạn nhận nước, chưa nhận nước/chưa settle |
| Hủy đúng hạn đã trả tiền | Đảo pending tương ứng | Cộng refund credit, xử lý voucher | Reservation/refund/ledger trong cùng transaction |
| Tạo yêu cầu rút | available − X; locked + X | Không đổi | Khóa hàng ví; không vượt khả dụng |
| Admin từ chối / hủy trước chuyển | locked − X; available + X | Không đổi | Có lý do, chưa có chuyển khoản đang xử lý |
| Admin xác nhận chuyển thành công | locked − X; ghi paid out X | Không đổi | Reference ngân hàng + chứng từ + người thao tác |
| Chuyển tiền chưa rõ kết quả | Giữ locked | Không đổi | Đưa đối soát; không tự nhả tiền hoặc chuyển lần hai |

### 6.1 Scheduler và giao nước trễ

Job quá grace_period_mins chỉ nhả tài nguyên bàn. Job no-show chạy theo giờ đóng cửa/ngày phục vụ của cơ sở, timezone Asia/Ho_Chi_Minh; quán đóng sau nửa đêm phải gắn với đúng ngày phục vụ. Đề xuất snapshot hạn nhận nước lúc tạo đơn; cần chốt cách xử lý quán 24/7 hoặc đóng cửa ngoại lệ. Chuyển CLAIMED_LATE và EXPIRED_NO_SHOW phải cạnh tranh trên cùng khóa/version của đơn: chỉ một trạng thái thắng, một settlement, một cơ chế thưởng. Đơn CHECKED_IN chưa checkout không tự coi là no-show. Test job chạy lại sau downtime. 

Thanh toán đến muộn sau hủy/hết hạn thanh toán (khác TABLE_EXPIRED): xác minh khoản thu, ghi nhận một lần, hoàn credit hoặc đưa đối soát theo chính sách; không hồi sinh đơn/bàn đã cấp khách khác.

## 7. Rút tiền và đối soát

**RBAC:** API phía merchant về số dư, lịch sử ví, ngân hàng và rút tiền chỉ cho OWNER có quyền sở hữu Store đó. MANAGER/CASHIER không được đọc hay ghi; trả 403 kể cả gọi URL/API trực tiếp. FE ẩn menu và chặn route; BE là nơi cưỡng chế. Admin vẫn xử lý payout bằng endpoint Admin riêng. Cashier chỉ nhận gross_amount trong dữ liệu tài chính phục vụ giao nước, không nhận balances/net/commission/bank details qua DTO đặt bàn.

**Hạn mức:** min_payout_amount <= amount <= available_balance, kiểm tra server trong cùng transaction khóa ví. min_payout_amount do Admin cấu hình; 100.000đ là ví dụ cần duyệt. Với cấu hình test 100.000: 99.999 bị từ chối, 100.000 được tạo nếu đủ, cao hơn available bị từ chối; một lệnh concurrent phải kiểm tra số dư sau lệnh trước.

Luồng đề xuất: REQUESTED → PROCESSING → PAID; REQUESTED → REJECTED/CANCELLED; PROCESSING → RECONCILIATION_REQUIRED nếu chưa biết tiền đã đi chưa.

- Khi tạo lệnh: snapshot ngân hàng, số tài khoản, tên chủ, amount, fee, net payout; sửa ngân hàng sau đó không đổi lệnh cũ.
- Admin nhận xử lý bằng thao tác nguyên tử; hai Admin không cùng nhận một lệnh. VietQR chỉ hỗ trợ nhập lệnh chuyển, không là bằng chứng đã trả.
- Chỉ đánh dấu PAID khi có bằng chứng chuyển và reference không trùng; audit người duyệt, thời điểm, tài khoản nhận.
- Không tự retry chuyển tiền thực khi timeout. Sai thông tin/không chuyển được có thao tác trả lại khả dụng được ghi ledger, sau khi xác nhận chưa trả tiền.
- Báo cáo: tiền đã thu PayOS, credit đã cấp/tiêu, pending/available/locked, tiền đã chuyển quán, commission, tài trợ voucher; lọc cơ sở/thời gian/đơn.
- Job đối soát so sánh ledger với balance projection, payment events với đơn, payout với chứng từ. Có danh sách sai lệch, không âm thầm sửa/xóa lịch sử.

## 8. Điểm thưởng và voucher

### 8.1 Bốn nguồn thưởng

| Nguồn | Điều kiện | Thời điểm / chống trùng |
|---|---|---|
| Spend Points | Tính từ gross_amount theo tỷ lệ/làm tròn được duyệt, không từ PayOS thực trả | COMPLETED hoặc CLAIMED_LATE; mỗi reservation một lần |
| Check-in đúng giờ | CHECKED_IN hợp lệ và thỏa khung giờ đúng giờ của booking | Server xác nhận; mỗi đơn một lần; định nghĩa khung đúng giờ cần map ở Task 0 |
| Self QR check-out | Khách tự quét QR hợp lệ, đã CHECKED_IN, check_out_time − check_in_time >= 30 phút | Server timestamps; 29:59 không thưởng, 30:00 được thưởng; mỗi đơn một lần |
| Review xác thực | Review có ảnh thực tế upload thành công, VISIT_VERIFICATIONS chứng minh đúng khách/đơn từng CHECKED_IN hoặc CLAIMED_LATE | Một thưởng Review/đơn; sửa/xóa/đăng lại không thưởng thêm; quyền CLAIMED_LATE chờ D15 |

Điều kiện 30 phút chỉ quyết định **điểm thưởng self check-out**, không tự cấm khách trả bàn sớm hoặc mất Spend Points. Thu ngân checkout hộ không được thưởng self QR. QR phải xác minh đơn/người dùng/hiệu lực và chống replay.
Review cần reservation_id, verification nguồn lần ghé và ảnh hợp lệ; client tự khai đã đến quán không đủ. Không mặc nhiên thêm AI nhận diện ảnh; rule xác minh ảnh/kiểm duyệt cần phù hợp chức năng hiện có.
EXPIRED_NO_SHOW không có điểm; TABLE_EXPIRED chưa phát sinh điểm. CLAIMED_LATE không có check-in bonus và checkout bonus, dù đợi thêm 30 phút.

### 8.2 Đổi điểm và vòng đời voucher
- Điểm check-in đúng giờ, self check-out và review cần tín hiệu server, khung giờ và giới hạn theo đơn; không thưởng lặp khi sửa review/quét lại.
- Review phải liên kết được với trải nghiệm đặt bàn; nếu code hiện tại chỉ có review theo Store thì cần quan hệ hoặc rule xác minh trước khi thưởng.
- Đổi điểm: trừ điểm + cấp voucher cá nhân trong cùng transaction; hết quota hoặc thiếu điểm không được trừ tiền/điểm dở dang.
- Template voucher: loại FIXED/PERCENT, giá trị, mức giảm tối đa, đơn tối thiểu, phạm vi Store, lịch hiệu lực, điểm đổi, quota tổng/người, trạng thái.
- Voucher instance: AVAILABLE/LOCKED/USED/EXPIRED/REVOKED; lưu reservationId đang giữ và holdUntil.
- PENDING_PAYMENT: AVAILABLE → LOCKED trong transaction tạo đơn; một voucher chỉ lock cho một reservation. Hai tab chỉ một tab thành công. PAID_CONFIRMED: LOCKED → USED; trả hoàn toàn bằng credit cũng phải capture voucher.
- PAYMENT_FAILED/hết hạn thanh toán đã xác minh: LOCKED → AVAILABLE nếu now < expiresAt, ngược lại EXPIRED. CANCELLED_REFUNDED trước free_cancel_hours: USED → AVAILABLE nếu còn hạn, ngược lại EXPIRED. Không gia hạn tự động hoặc trả thêm điểm đổi voucher.
- Mọi unlock phải kiểm tra reservationId hiện đang giữ voucher, tránh webhook/job cũ mở khóa voucher đã được dùng lại cho đơn mới; lưu usage history theo từng đơn.
- Chặn dùng mã người khác; check phạm vi/ngưỡng/giờ/giới hạn hoa hồng cả lúc quote và lúc chốt.
- Theo D10: discount ≤ commission_amount và ≤ gross_amount; không dùng ngưỡng 10–20k cố định làm bằng chứng không lỗ (ví dụ 20k > commission 15k).
- Nếu đơn đã thưởng rồi hoàn: ledger đảo phần thưởng theo chính sách; trường hợp điểm đã tiêu phải có reward debt/khóa đổi tiếp được duyệt, không trừ vào credit hoàn tiền.

## 9. Thiết kế dữ liệu và API đề xuất

### Dữ liệu

- `merchant_wallets(store_id unique, pending, available, locked, version)`.
- `customer_wallets(user_id unique, refund_credit_balance, reserved_credit, reward_points, reserved_points, version)`; số dư khác đơn vị không cộng với nhau.
- `ledger_transactions(id, business_key unique, event_type, source_type, source_id, reversal_of, created_at)` + `ledger_entries(transaction_id, account_id, unit, amount)`; tổng debit/credit cân bằng theo từng unit.
- Tài khoản ledger gồm payment clearing, công nợ quán, nghĩa vụ refund credit, doanh thu commission, tài trợ voucher và tài khoản reward riêng.
- `withdrawal_requests`, `bank_accounts`, `commission_policies`, `voucher_templates`, `customer_vouchers`, `voucher_redemptions`, `reward_events`, `refunds`, `payment_allocations`, `payment_events`, `outbox_events`, quan hệ `visit_verifications` tới reservation/user/review; reservation lưu check_in_time, check_out_time, late_claimed_at, claim_deadline và reward policy snapshot.
- Unique business key cho payment capture, refund operation, settlement, reward, voucher redeem và payout confirmation. HTTP idempotency key phải ràng buộc actor + endpoint + payload hash; cùng key khác payload trả conflict.
- DB transaction bao trùm state transition + ledger + balances + outbox. Network PayOS ngoài DB lock; reconcile khi timeout, không coi timeout là thất bại chắc chắn.

### API contract (chốt OpenAPI trước khi FE/Mobile tích hợp)

| Nhóm | Endpoint đề xuất | Nội dung |
|---|---|---|
| Owner | GET `/api/merchant/stores/{storeId}/wallet` | Ba số dư VNĐ, updatedAt |
| Owner | GET `.../wallet/transactions`; GET/POST `.../withdrawals` | Lịch sử phân trang; tạo lệnh amount/bankAccountId/idempotencyKey |
| Owner | GET/POST/PATCH `/api/merchant/bank-accounts` | Quản lý tài khoản nhận, trạng thái xác minh |
| Admin | GET `/api/admin/withdrawals`; POST `/{id}/process`, `/{id}/reject`, `/{id}/confirm-paid` | State machine, lý do/reference/chứng từ |
| Customer | GET `/api/me/wallet`; GET `/api/me/wallet/transactions` | Credit VNĐ + MoodPoints, lịch sử theo loại |
| Customer | GET `/api/voucher-catalog`; POST `/api/me/vouchers/redeem`; GET `/api/me/vouchers` | Xem/đổi/ví voucher |
| Booking | POST `/api/reservations/quote` | Phân tích giá, voucher, credit, PayOS; endpoint có thể đã tồn tại, phải mở rộng đúng nơi |
| Admin | GET/POST/PATCH `/api/admin/commission-policies`, `/api/admin/vouchers`, `/api/admin/loyalty-policies` | Cấu hình và audit |
| Admin | GET `/api/admin/finance/reconciliation` | Tổng hợp và sai lệch đối soát |

Mọi danh sách hỗ trợ pagination; dùng response envelope theo repo. Viết OpenAPI cho error thiếu tiền, voucher hết hạn/đã dùng, sai scope, stale quote và idempotency conflict. Không trả thông tin ngân hàng đầy đủ cho role không cần xem.

## 10. Review Focus

1. Webhook trùng, polling cùng webhook, mạng timeout: chỉ một lần ghi tiền — test Task 3.
2. Hai thao tác đồng thời rút/đổi voucher/tiêu credit: không chi vượt số dư/quota — test Tasks 2, 4, 5.
3. Thanh toán đến sau khi bàn đã hết hạn hoặc hủy: không mất khoản thu, không phục hồi đơn sai — test Task 3.
4. Đổi tỷ lệ/giá/voucher sau khi khách tạo đơn: snapshot giữ nguyên, quote cũ hết hạn rõ ràng — test Tasks 1, 3.
5. Refund sau khi reward đã dùng hoặc quán đã rút: không số dư âm/không cấp tiền hai lần — test Tasks 3, 4, 8.

## 11. Các task bàn giao Antigravity

Các đường dẫn `reservation` là điểm tích hợp cần tìm ở Task 0; các module wallet/loyalty/voucher bên dưới là đường dẫn mới đề xuất. Không tự tạo migration với số V cố định: chọn số tiếp theo sau khi đồng bộ nhánh thực tế.

### Thứ tự theo phụ thuộc

Thực hiện **0 → 1 → 2 → 4 → 3 → 5 → 6/7 → 8**. Task 1 chỉ kiểm thử phép tính bằng voucher benefit đã validate qua interface giả lập, chưa xác nhận voucher ngoài thực tế. Task 4 xây module điểm/voucher và kiểm thử bằng event nghiệp vụ giả lập, không phụ thuộc payment đã nối. Task 3 mới nối các module thật và chạy integration tests payment + voucher + credit. Tasks 6 và 7 chỉ bắt đầu khi contract BE tương ứng ổn định. Số task là mã tham chiếu, không phải thứ tự chạy tuyến tính.

### Task 0 — Chốt phạm vi và xác định code đặt bàn

- [ ] Người duyệt trả lời D1–D15 và bổ sung phần sau 3.2 nếu có; lưu quyết định ngay đầu tài liệu.
- [ ] Xác định branch/commit của BE, FE, MB đang có đặt bàn; lập bảng file entity/service/controller/DTO/page/provider/test thật.
- [ ] Ghi state machine: tạo đơn, giữ bàn, trả tiền, xác nhận, check-in, check-out, hoàn tất, hủy, TABLE_EXPIRED, CLAIMED_LATE, EXPIRED_NO_SHOW; map đúng trạng thái, quyền thu ngân, closing deadline và grace/free_cancel boundary.
- [ ] Đọc quyền của StoreStaff/Owner/Cashier và test hiện tại. Chạy baseline; ghi lỗi có sẵn.
- [ ] Chốt OpenAPI/ERD và sơ đồ bút toán dựa trên D1–D15. Review mới cho phép Task 1 chạy.

Deliverable: bảng mapping code + spec đã duyệt; không dựng flow đặt bàn thứ hai.

### Task 1 — Tính giá và hoa hồng snapshot

Files mới BE: `moodcafe-be/src/main/java/com/moodcafe/finance/service/ReservationPricingService.java`, `finance/dto/ReservationFinancialSnapshot.java`, `finance/entity/CommissionPolicy.java`, `finance/controller/AdminCommissionPolicyController.java`.
Modify: module `configuration` nếu dùng để lưu default; entity/DTO/service đặt bàn thật ở Task 0; thêm Flyway migration mới.
Test: `moodcafe-be/src/test/java/com/moodcafe/finance/ReservationPricingServiceTest.java`.
Interface đề xuất: `quote(ReservationPricingInput input) -> ReservationFinancialSnapshot`; input chứa store/category/user/voucher/credit, service tự đọc giá và policy.

- [ ] Viết test ví dụ 100k/10k/15% → 90k/15k/85k; override hết hiệu lực dùng default; discount 20k vượt 15k bị chặn; rounding và quote hết hạn.
- [ ] Chạy test thấy fail trước implementation.
- [ ] Implement snapshot và Admin policy; validate rate 0–100%, ngày hiệu lực không mâu thuẫn, audit ai đổi.
- [ ] Chạy test pass; chứng minh sửa config không đổi đơn cũ; commit task.

### Task 2 — Ledger và ví

Files mới BE dưới `com/moodcafe/wallet/`: entity wallet/ledger, `service/LedgerService.java`, `service/MerchantWalletService.java`, `service/CustomerWalletService.java`, repository và migration.
Test: `wallet/LedgerIntegrationTest.java` trên PostgreSQL.
Interfaces: `post(LedgerCommand command) -> LedgerTransaction`; command chứa businessKey, nguồn, unit, entries; `getStoreBalance(storeId, actor)` và `getCustomerBalance(actor)`.

- [ ] Test debit/credit cân bằng, unique businessKey, rollback khi lỗi giữa chừng, hai lệnh đồng thời không âm, Store khác bị từ chối.
- [ ] Xác nhận test fail; implement transaction/locking/ledger và read API.
- [ ] Test lại và kiểm tra ledger khớp balances; commit task.

### Task 3 — Nối vòng đời đặt bàn, payment và refund

Modify: `payment/service/PayOSService.java`, `payment/controller/PaymentController.java`, DTO webhook và booking service/job thật đã map.
Create dưới `finance/service/`: `ReservationSettlementService.java`, `ReservationRefundService.java`; thêm payment-purpose/events/allocation và outbox theo contract.
Interfaces: `captureVerifiedPayment(VerifiedPayment event)`, `settleCompletedReservation(UUID reservationId)`, `refundReservation(RefundCommand command)`; gọi LedgerService Task 2 và snapshot Task 1.
Test: `finance/ReservationMoneyLifecycleIntegrationTest.java`.

- [ ] Test webhook/polling trùng chỉ cộng pending một lần; sai chữ ký/số tiền/currency/reference không ghi tiền; callback giao diện không chứng minh đã thanh toán.
- [ ] Test 90k/100k credit → còn10k, PAID_CONFIRMED, không gọi PayOS; 90k/30k → reserved30k, PayOS60k, capture khi thành công; PAYMENT_FAILED trả đủ30k, chạy release lại không nhân đôi; toggle tắt không tiêu ví; pending → available đúng event; hủy đúng hạn hoàn 90k theo ví dụ; hoàn một phần cộng dồn có giới hạn nếu D6 cho phép.
- [ ] Test TABLE_EXPIRED chỉ nhả bàn giữ pending/không điểm; CLAIMED_LATE giải ngân + spend một lần; EXPIRED_NO_SHOW cuối ngày giải ngân/0 điểm; job đối đầu thao tác thu ngân chỉ một bên thành công; quán qua nửa đêm, job chạy bù.
- [ ] Test late payment, refund lặp, refund đồng thời settlement, timeout gateway và retry; đơn subscription không bị xử lý như booking.
- [ ] Implement adapter sự kiện và jobs có idempotency; chạy integration tests pass, commit task.

### Task 4 — Điểm và voucher

Create BE modules `com/moodcafe/loyalty/`, `com/moodcafe/voucher/` theo pattern entity/dto/abstraction/service/controller hiện có.
Interfaces: `awardReward(RewardEvent event)`, `reverseReward(sourceId, reason)`, `redeemVoucher(userId, templateId, idempotencyKey)`, `reserveVoucher(userId, voucherId, reservationId, expiresAt)`, `captureVoucher(reservationId)`, `releaseVoucher(reservationId)`.
Test: `loyalty/RewardLifecycleTest.java`, `voucher/VoucherRedemptionIntegrationTest.java`.

- [ ] Test đổi điểm concurrent không vượt balance/quota; điểm chỉ trừ khi voucher tạo thành công; không dùng voucher người khác/cơ sở khác.
- [ ] Test Spend Points theo gross100k dù PayOS70k, credit20k, voucher10k; COMPLETED/CLAIMED_LATE một lần; no-show0; CLAIMED_LATE không có thưởng check-in/check-out. Self checkout29:59 không bonus,30:00 có bonus; checkout hộ không bonus; trả bàn sớm vẫn được xử lý. Review thiếu ảnh/sai VISIT_VERIFICATIONS không thưởng; hợp lệ một lần/đơn, sửa/xóa/đăng lại không thưởng tiếp; nhánh late review theo D15. Test đảo điểm khi hoàn theo D7/D14.
- [ ] Test hai tab lock cùng voucher chỉ một thành công; PENDING_PAYMENT→LOCKED, PAID_CONFIRMED→USED; PAYMENT_FAILED và CANCELLED_REFUNDED còn hạn→AVAILABLE, hết hạn→EXPIRED; callback cũ không unlock voucher của đơn mới; không khôi phục cả voucher và điểm đổi.
- [ ] Implement policy Admin, earn/redeem/reserve/capture/release + expiry job; test pass, commit task.

### Task 5 — Rút tiền Owner và Admin đối soát

Create BE `wallet/service/WithdrawalService.java`, controllers Owner/Admin và entity/repository bank account/payout.
Interfaces: `requestWithdrawal(actor, storeId, amount, bankAccountId, key)`, `claimWithdrawal(admin, id)`, `rejectWithdrawal(admin, id, reason)`, `confirmPaid(admin, id, bankReference, evidenceId)`.
Test: `wallet/WithdrawalIntegrationTest.java`.

- [ ] Test hai lệnh rút vượt available, hai Admin claim cùng lệnh, reference trả tiền trùng, đổi bank sau khi tạo lệnh, MANAGER/CASHIER bị 403 cho cả đọc ví/lịch sử, ngân hàng và rút; Owner cơ sở khác bị cấm; min=100k thì99.999 thất bại/100k thành công khi đủ tiền, amount>available thất bại.
- [ ] Test reject trả available đúng một lần; PROCESSING chưa rõ không nhả khóa; PAID không confirm lại; audit đầy đủ.
- [ ] Implement và test pass; không gọi API chuyển tiền thật trong test; commit task.

### Task 6 — FE Owner/Admin/Cashier

Create FE: `src/app/merchant/wallet/page.tsx`, `src/app/admin/withdrawals/page.tsx`, `src/app/admin/vouchers/page.tsx`, `src/app/admin/finance/page.tsx`, `src/services/wallet.service.ts`, `src/services/voucher.service.ts`, components dưới `src/components/merchant/wallet/` và `src/components/admin/finance/`.
Modify: `src/app/admin/settings/page.tsx`, menu điều hướng tương ứng; trang booking/thu ngân xác định ở Task 0.
Consumes: OpenAPI Tasks 1–5. Giữ theme Owner/Admin hiện có, responsive, loading/error/empty/retry.

- [ ] Viết test hành vi chọn Store đúng ví, submit lặp, nhập tiền sai, mất quyền, API lỗi, refresh sau payout; mock theo contract thật.
- [ ] Owner thấy 3 số dư, lịch sử, ngân hàng, rút tiền và trạng thái; MANAGER/CASHIER không thấy menu và không truy cập route ví/ngân hàng; Cashier chỉ thấy gross ở đơn và có thao tác giao nước trễ đúng quyền, chặn bấm lặp.
- [ ] Admin thấy queue rút, chứng từ/QR, xác nhận/từ chối, cấu hình hoa hồng/điểm/voucher và báo cáo; không hiển thị chức năng chưa có API.
- [ ] Test desktop/mobile, TypeScript/lint/build, ghi lỗi baseline riêng; commit task.

### Task 7 — Mobile Customer và Owner

Create features `lib/features/wallet/`, `lib/features/loyalty/`, `lib/features/vouchers/` với data/domain/presentation; UseCase cho đọc ví/lịch sử, đổi voucher và rút tiền theo quyền.
Modify `lib/app/router/app_router.dart`, `app_routes.dart`, `lib/features/profile/presentation/profile_page.dart`; booking checkout/payment/detail thật ở Task 0; menu Merchant hiện có.
Consumes: API Tasks 1–5; DTO freezed/json_serializable, manual Riverpod, Result/Failure.

- [ ] Test repository mapping, UseCase success/failure, đổi điểm thiếu số dư, quote cũ, app resume lúc payment đang chờ, double tap.
- [ ] Customer thấy rõ số dư hoàn VNĐ và MoodPoints, lịch sử/đổi voucher/voucher cá nhân; checkout có toggle dùng credit, số dư khả dụng/đang khóa, hiển thị gross/giảm/credit/PayOS; kiểm tra đủ tiền bỏ QR, thiếu tiền chỉ QR phần thiếu, thất bại số dư phục hồi; không có nút rút.
- [ ] Owner có ví theo cơ sở và yêu cầu rút nếu D12 duyệt; che dữ liệu ngân hàng phù hợp.
- [ ] Chạy build_runner khi đổi DTO, `flutter analyze` và `flutter test` không lỗi; commit task.

### Task 8 — Đối soát, migration và nghiệm thu end-to-end

Create `finance/service/ReconciliationService.java`, báo cáo sai lệch và integration tests end-to-end; nối `src/app/admin/finance/page.tsx` Task 6.

- [ ] Đơn cũ: không áp rate hiện tại để tự cộng ví. Phân loại đã trả/đã phục vụ/đã hoàn; tạo baseline chỉ khi có căn cứ và người duyệt; backfill có dry-run, audit, key chống trùng.
- [ ] Nghiệm thu đơn 100k, giảm10k, rate15% → quán85k, sàn5k; thêm credit20k → PayOS70k và tổng nghĩa vụ vẫn đúng.
- [ ] Nghiệm thu payment→pending→completed→available→withdraw→locked→paid; cancel→refund credit→dùng credit đặt lần sau.
- [ ] Nghiệm thu replay webhook, concurrency, failed payout, no-show theo policy, refund sau payout, reward đã tiêu, voucher hết hạn và thay đổi config.
- [ ] Kiểm tra đối soát balance với ledger và gateway; có đường xử lý sai lệch, không tự xóa dữ liệu.
- [ ] Triển khai staging, kiểm tra theo từng role và feature flag; chỉ bật production sau khi nhóm duyệt và có kế hoạch rollback không làm mất ledger.

## 12. Lệnh kiểm tra và tiêu chí bàn giao

- BE: trong `D:/Learn/MoodCafe-BE/moodcafe-be`, dùng Maven wrapper `./mvnw.cmd test` nếu có, hoặc Maven được repo quy định. Integration concurrency phải dùng PostgreSQL thật/Testcontainers, không chỉ mock/H2. Không chạy test lên DB production.
- FE: trong `D:/Learn/MoodCafe-FE`, `npx tsc --noEmit`, `npm run lint`, `npm run build` và test framework sau khi xác định ở Task 0.
- Mobile: trong `D:/Learn/MoodCafe-MB`, `dart run build_runner build --delete-conflicting-outputs` nếu DTO đổi; `flutter analyze`; `flutter test`.
- Giao kèm OpenAPI, migration, dữ liệu demo local, tài khoản test theo role, sơ đồ sự kiện, báo cáo test và cách đối soát lỗi.
- Mỗi task kết thúc bằng commit riêng, không đưa secrets, không tự push/deploy nếu chưa được yêu cầu.

## 13. Checklist người duyệt

- [ ] Đã đọc và chọn/sửa D1–D15.
- [ ] Đã xác nhận dùng ví theo cơ sở hay theo Owner.
- [ ] Đã xác nhận hai ngăn VNĐ/MoodPoints và ba nhánh tiêu credit tại mục 5.2.
- [ ] Đã xác nhận ba mốc TABLE_EXPIRED/CLAIMED_LATE/EXPIRED_NO_SHOW, closing deadline quán24/7, chính sách hủy và khôi phục voucher còn hạn.
- [ ] Đã trả lời D15: đơn nhận nước trễ có được nhận thưởng Review xác thực sau đó hay không.
- [ ] Đã xác nhận tỷ lệ điểm, hoa hồng, giới hạn voucher và rút tiền.
- [ ] Đã cung cấp nhánh/commit có flow đặt bàn và phần requirement còn thiếu nếu có.
- [ ] Đồng ý phạm vi BE + FE + Mobile và thứ tự Tasks 0–8.

**Nội dung giao Antigravity:** Đọc requirement gốc và plan này. Chỉ dùng các quyết định đã được duyệt; bắt đầu bằng Task 0 để map đúng code đặt bàn hiện có. Không suy diễn bảng enum/trạng thái chưa thấy, không dùng default tiền/điểm như quyết định của khách hàng. Triển khai và kiểm thử lần lượt từng task; báo rõ task nào bị chặn bởi quyết định chưa duyệt.
