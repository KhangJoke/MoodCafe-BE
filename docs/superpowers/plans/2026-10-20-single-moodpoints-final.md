# Một ví MoodPoints — Implementation Plan chốt Cách 1

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Hoàn thiện ví Customer, hoàn đơn, điểm thưởng, voucher, hoa hồng và đối soát/rút tiền Owner; Customer chỉ có một số dư MoodPoints.

**Architecture:** BE là nguồn quyết định, dùng ledger bất biến, transaction, khóa số dư và idempotency. Điểm dùng đổi voucher; checkout chỉ áp voucher và thanh toán phần còn lại qua PayOS. Metadata nguồn điểm phục vụ đối soát nội bộ, không tạo ví Customer thứ hai.

**Tech Stack:** Java/Spring Boot, PostgreSQL/Flyway, PayOS; Next.js/TypeScript; Flutter/Riverpod theo Clean Architecture.

**Spec:** `2026-10-20-wallet-requirement-original.txt` cùng thư mục, review bổ sung và quyết định mới nhất của người dùng: **Cách 1, 1 MoodPoint = 1đ**.

**Trạng thái:** Chốt kiến trúc một ví điểm; chính sách chưa chốt nằm ở mục 3. Chỉ tạo plan, chưa triển khai sản phẩm.

**Ưu tiên:** Bản này thay thế plan `2026-10-20-wallet-loyalty-vouchers-commission.md` và toàn bộ hướng Customer hai số dư/tiêu ví trực tiếp trước đây. Không lấy các task/API/schema credit cũ để triển khai.

## 1. Tóm tắt gửi nhóm duyệt

1. Customer có **một ví, một số dư MoodPoints**. Điểm thưởng và điểm hoàn đơn cộng chung; không có ngăn VNĐ riêng, không nạp chủ động, rút tiền mặt hoặc chuyển cho người khác.
2. **1 MoodPoint = 1đ giá trị sử dụng.** Hoàn 90.000đ → cộng 90.000 điểm. Đổi voucher 20.000đ → trừ 20.000 điểm. Đây không phải tỷ lệ tích điểm từ chi tiêu.
3. Điểm chỉ dùng **đổi voucher cá nhân**. Đặt bàn chọn voucher, trả phần còn lại qua PayOS. Không toggle dùng ví, không khóa/trừ điểm ở checkout, không thanh toán kết hợp trực tiếp ví–PayOS.
4. Hủy đúng hạn: phần PayOS thực thu đủ điều kiện được hoàn thành điểm; voucher đã áp được xử lý riêng. Không vừa cộng điểm bằng giá trị voucher vừa khôi phục voucher.
5. Voucher AVAILABLE → LOCKED khi tạo đơn chờ thanh toán → USED khi thanh toán thành công. Thất bại/hủy đúng hạn trả AVAILABLE nếu còn hạn.
6. Ví quán vẫn VNĐ với pending/available/locked. TABLE_EXPIRED chỉ nhả bàn, giữ pending; COMPLETED/CLAIMED_LATE giải ngân; EXPIRED_NO_SHOW cuối ngày giải ngân cho quán nhưng khách nhận 0 điểm.
7. Spend Points tính trên gross_amount; check-in đúng giờ, self QR checkout đủ 30 phút, review xác thực có ảnh có rule thưởng riêng. Review sau CLAIMED_LATE còn một điểm cần xác nhận.
8. Chỉ OWNER đúng cơ sở có quyền ví quán/ngân hàng/rút tiền. MANAGER/CASHIER bị chặn cả route và API; Admin xử lý payout riêng; Cashier thấy gross để giao nước.
9. Hoa hồng snapshot từng đơn. Voucher không làm giảm phần quán được nhận. BE phải biết phần voucher được bảo đảm từ tiền hoàn và phần do sàn tài trợ, dù khách chỉ thấy một số dư.

**Ví dụ xuyên suốt:** Đơn cũ đã trả 90.000đ → hủy đúng hạn → +90.000 điểm → đổi voucher 90.000đ → đơn mới gross100.000đ → PayOS10.000đ. Hoa hồng15% thì quán vẫn nhận85.000đ.

## 2. Hiện trạng và phạm vi

- Requirement gốc dừng ở tiêu đề 3.2. Review bổ sung được giữ, riêng hai số dư và tiêu ví trực tiếp đã bị quyết định mới thay thế.
- Đối chiếu checkout local ngày20/10/2026: BE có `payment/service/PayOSService.java`, `payment/controller/PaymentController.java`, liên kết SubscriptionPayment và module configuration. FE có layout Admin/merchant/settings/profile; Mobile có profile/router và thanh toán gói dịch vụ.
- Checkout đã đọc chưa tìm được flow đặt bàn mà nhóm nói đã có. Task0 phải xác định đúng branch/commit và map file thật, không tự dựng booking thứ hai hoặc dùng nhầm payment subscription.
- Phạm vi: BE + FE Admin/Owner/Cashier + Mobile Customer/Owner. Customer web chỉ bổ sung nếu có booking web.
- API, entity và đường dẫn mới trong plan là thiết kế đề xuất, không phải tính năng đã tồn tại.

## 3. Quyết định và chính sách còn cần duyệt

**Đã chốt:** một số dư MoodPoints; 1đ hoàn =1 điểm; 1 điểm đổi =1đ voucher; điểm không rút; checkout không trừ ví trực tiếp. Không hỏi lại những lựa chọn này.

| Mã | Chính sách còn mở | Đề xuất để nhóm duyệt |
|---|---|---|
| P1 | Ví quán theo Store hay Owner | Mỗi Store một ví, Owner xem tổng hợp và rút theo cơ sở. |
| P2 | Mệnh giá voucher | Catalog mệnh giá cố định, point_cost bằng face_value_vnd. Nhóm chọn các mức; chưa thêm nhập mệnh giá tùy ý. Điểm dư giữ trong ví. |
| P3 | Hạn dùng điểm/voucher | Điểm REFUND không hết hạn. Voucher hết hạn có giá trị REFUND thì hoàn phần đó về điểm một lần; REWARD theo policy. Đây là đề xuất bảo toàn tiền hoàn, cần duyệt. |
| P4 | Tích thưởng | Chốt earn rate, bonus, trần, hạn dùng và thu hồi. **1 điểm=1đ không có nghĩa chi1đ được thưởng1điểm.** |
| P5 | Review sau CLAIMED_LATE | Đề xuất nhận nước trễ chỉ thưởng Spend, sau đó vẫn được thưởng Review có ảnh một lần; không check-in/checkout bonus. Review trước có hai câu chưa thống nhất, cần xác nhận. |
| P6 | Hủy muộn/lỗi quán/hoàn một phần | Chốt tỷ lệ, phí, voucher, điểm đã thưởng. Chưa chốt thì không tự triển khai nhánh này. |
| P7 | Hoa hồng | Global mặc định + override Store, có hiệu lực theo thời gian; chốt rate và rounding.15% là ví dụ. |
| P8 | Rút tiền | Chốt min/max/phí/xác minh ngân hàng/SLA;100.000đ chỉ là ví dụ min_payout_amount. |
| P9 | Deadline theo cơ sở | Ngày phục vụ, ca qua nửa đêm, quán24/7, đóng bất thường và ranh giới đúng giờ/free_cancel_hours. Đề xuất snapshot timezone Asia/Ho_Chi_Minh. |
| P10 | Nguồn voucher/chống lỗ | Lưu allocation nội bộ; ưu tiên tiêu điểm REFUND trước, rồi REWARD. Chỉ phần REWARD discount bị giới hạn bởi commission; marketing vượt mức cần ngân sách riêng, ngoài đợt đầu. |

P2/P3/P10 là đề xuất kỹ thuật/nghiệp vụ đi kèm, không giả định đã được người dùng duyệt chỉ vì đã chốt một ví.

## 4. Global Constraints

- BE tính giá, tiền hoàn, điểm, trạng thái và quyền. Không tin amount, funding source hoặc balance client gửi.
- Điểm/tiền VNĐ nguyên, không float/double. Điểm bigint; tiền BigDecimal; đề xuất HALF_UP tới đồng sau khi duyệt.
- Không số dư âm; ledger không sửa/xóa, điều chỉnh bằng bút toán đảo. Unique key cho refund/reward/redeem/capture/payout/expiry.
- Transaction bao trùm trạng thái, ledger, balance, outbox. PayOS network ngoài DB lock; timeout là chưa rõ kết quả.
- Metadata REFUND/REWARD không là hai ví hoặc hai số dư trên UI. Không buộc khách chọn nguồn.
- Giá và policy snapshot; sửa config không đổi đơn/voucher đã chốt.
- Hoàn tiền chỉ cộng điểm; không tự phát voucher ngay lúc hoàn (đó là Cách2, ngoài lựa chọn hiện tại).
- Mobile tuân thủ AGENTS.md: presentation → domain ← data, UseCase/Result/Failure, manual Riverpod, freezed/json_serializable.
- Migration mới, không sửa checksum migration đã chạy; không kiểm thử hoặc seed tiền trên DB production.

## 5. Nghiệp vụ chi tiết

### 5.1 Điểm và đổi voucher

`CustomerWallet.points_balance` là số dư duy nhất. Các PointLot lưu nguồn REFUND/REWARD, sourceId, issued/remaining points và expiry. Tổng remaining points bằng balance. Điểm đã đổi voucher không còn nằm trong balance.

Đổi voucher nguyên tử: khóa ví/lots → kiểm tra điểm/quota → trừ đúng point_cost → cấp voucher AVAILABLE → lưu allocation nguồn. Bất kỳ bước lỗi đều rollback.

Theo P10 đề xuất: REFUND trước theo ngày cấp, sau đó REWARD ưu tiên sắp hết hạn. Allocation lưu trên voucher và giữ nguyên khi voucher được khôi phục sau hủy. Không cần hai loại voucher hiển thị cho khách.

Đợt1 voucher đổi điểm là mệnh giá cố định, point_cost=face_value_vnd>0; voucher phần trăm không thuộc catalog đổi điểm đợt này. Không áp voucher lớn hơn gross: báo rõ đơn chưa đạt giá trị, không đốt mất phần chênh.

### 5.2 Booking và PayOS

Giữ các trường `gross_amount`, `voucher_discount`, `customer_paid_amount`, `commission_amount`, `merchant_net_amount`.

- gross = min_spend_amount theo booking thật; Task0 xác minh số bàn/phụ phí.
- customer_paid = gross − voucher_discount = tiền PayOS cần thu.
- commission = round(gross × commission_rate_snapshot).
- merchant_net = gross − commission, không bị trừ voucher.
- Một voucher/đơn. PENDING_PAYMENT khóa voucher, **không trừ/khóa thêm điểm ví**.
- PayOS amount>0: tạo link đúng số đó. Amount=0: bỏ PayOS, kiểm tra quyền/eligibility rồi xác nhận PAID_CONFIRMED nội bộ, ghi payment record0, voucher USED, pending quán trong một transaction. Không chờ webhook.

### 5.3 Funding của voucher

Snapshot `voucher_refund_funded_amount` + `voucher_reward_funded_amount` = voucher_discount.

| Gross100k, commission15k, net quán85k | PayOS | Nguồn hoàn cũ giải trừ | Sàn ròng trước phí |
|---|---:|---:|---:|
| Voucher10k toàn REWARD |90k|0|5k|
| Voucher90k toàn REFUND |10k|90k|15k|
| Voucher90k gồm REFUND80k+REWARD10k |10k|80k|5k|

`platform_net_revenue = commission_amount − voucher_reward_funded_amount` trước phí/chi phí khác và chỉ ghi nhận theo chính sách settlement. Công thức cũ trừ toàn voucher chỉ đúng khi voucher hoàn toàn là khuyến mãi.

Theo P10: reward-funded discount≤commission và tổng discount≤gross. REFUND funded không bị chặn chỉ vì vượt commission: nguồn đó là nghĩa vụ từ tiền đã thu trước, không phải ngân sách marketing mới. Không ghi lại tiền PayOS cũ thành doanh thu lần hai.

Catalog/preview phải cho khách biết điều kiện trước khi đổi. Không nhận allocation client tự khai; thiếu nguồn bảo đảm thì không capture.

### 5.4 Refund và voucher restore

1. PayOS đã xác minh, đủ điều kiện hoàn, trừ các lần đã hoàn → cộng đúng số MoodPoints REFUND1:1; refundId unique.
2. PAYMENT_FAILED: LOCKED→AVAILABLE nếu còn hạn. CANCELLED_REFUNDED đúng hạn: USED→AVAILABLE nếu còn hạn. Không trả thêm điểm đổi voucher cùng lúc khôi phục voucher.
3. Theo P3 đề xuất, hết hạn: EXPIRED, hoàn phần REFUND allocation về điểm một lần; REWARD theo policy. Không vừa restore voucher vừa hoàn allocation thành điểm.
4. Voucher USED trong đơn còn hiệu lực không được expiry job trả điểm. Voucher LOCKED khi gateway chưa rõ không được nhả/chuyển điểm vội. Restore/unlock kiểm tra usage/reservationId để callback cũ không nhả voucher của đơn mới.
5. Đơn100k/voucher REWARD10k/PayOS90k hủy: +90k điểm và restore voucher10k còn hạn, không+100k điểm.
6. Đơn100k/voucher REFUND90k/PayOS10k hủy: +10k điểm và restore voucher90k; nếu voucher hết hạn theo P3 thì +90k điểm allocation thay cho voucher. Tổng giá trị trả lại100k, không110k/190k.
7. Đơn voucher100%, PayOS0: không tạo khoản hoàn PayOS mới, chỉ xử lý voucher.
8. Quán đã được giải ngân/rút: đưa phải thu/đối soát theo P6, không âm ví hoặc tự rút ngược ngân hàng.
9. Thu hồi thưởng bám nguồn REWARD. Nếu đã tiêu, reward debt/khóa thưởng tương lai theo policy; không âm thầm cấn vào điểm REFUND.

## 6. State machine đơn và ví quán

| Sự kiện | Ví quán | Customer |
|---|---|---|
| Redeem voucher | Không đổi | Trừ điểm, cấp AVAILABLE |
| PENDING_PAYMENT | Chưa cộng | Voucher LOCKED, balance không đổi |
| PAID_CONFIRMED | pending+net | Voucher USED; dedup payment |
| PAYMENT_FAILED đã xác minh | Không đổi | Nhả voucher, không trả thêm điểm đổi |
| CHECKED_IN đúng giờ | Giữ pending | Check-in bonus một lần |
| COMPLETED | pending−net, available+net | Spend; checkout bonus nếu đủ điều kiện |
| TABLE_EXPIRED quá grace | Chỉ nhả bàn, giữ pending | Chưa cộng điểm, còn quyền nhận nước |
| CLAIMED_LATE | pending−net, available+net | Spend, không check-in/checkout bonus; Review theo P5 |
| EXPIRED_NO_SHOW hết ngày phục vụ | pending−net, available+net |0 MoodPoints|
| CANCELLED_REFUNDED | Đảo pending/nghĩa vụ phù hợp | PayOS hoàn→điểm, voucher theo5.4 |
| Owner yêu cầu rút | available−X, locked+X | Không đổi |
| Reject trước khi chuyển | locked−X, available+X | Không đổi |
| Confirm payout | locked−X, ghi PAID | Không đổi |

TABLE_EXPIRED khác PAYMENT_FAILED. Cron theo closing deadline/timezone/ngày phục vụ; CLAIMED_LATE và no-show tranh chấp cùng lock/version, chỉ một settlement. CHECKED_IN chưa checkout không là no-show. Job chạy bù phải idempotent.

Thanh toán đến sau hủy/hết hạn thanh toán: ghi tiền đã thu một lần, đưa đối soát/hoàn thành điểm theo policy; không hồi sinh bàn đã cấp người khác.

## 7. Bốn nguồn điểm thưởng

| Nguồn | Điều kiện | Chống lặp |
|---|---|---|
| SPEND | COMPLETED/CLAIMED_LATE, tính theo gross kể cả dùng voucher | reservation+SPEND unique |
| ON_TIME_CHECK_IN | CHECKED_IN hợp lệ, trong khung đúng giờ | reservation+type unique |
| SELF_CHECK_OUT | Khách tự QR, giờ server checkout−checkin≥30 phút | QR chống replay, một lần/đơn |
| VERIFIED_REVIEW | Có ảnh upload hợp lệ, VISIT_VERIFICATIONS đúng khách/đơn CHECKED_IN; late theoP5 | Một lần/đơn; sửa/xóa/đăng lại không thưởng thêm |

29:59 không bonus checkout,30:00 có; checkout hộ không bonus. Khách vẫn được trả bàn sớm và nhận Spend nếu đủ điều kiện hoàn tất.

Earn rate độc lập tỷ lệ1:1: nếu P4 chọn1% thì gross100k thưởng1.000 điểm, không100.000 điểm. Đây chỉ là ví dụ, không tự đặt config thật.

Review phải có quan hệ booking/verification; không tin client khai “đã đến”. Không thêm AI xác minh ảnh nếu chưa có requirement.

## 8. Owner payout và RBAC

- Merchant wallet VNĐ pending/available/locked; đề xuất theo Store.
- OWNER đúng Store được đọc ví/lịch sử/ngân hàng/rút. MANAGER/CASHIER403 cả đọc/ghi; FE ẩn menu/chặn route; Admin dùng endpoint riêng.
- Cashier chỉ thấy gross trong thông tin tài chính giao nước, không lộ balances/commission/net/bank.
- `min_payout_amount≤amount≤available_balance` trong transaction khóa ví. Min100k chỉ là fixture:99.999fail,100kpass khi đủ, vượt available fail.
- Snapshot ngân hàng/amount/fee/net payout; sửa ngân hàng sau đó không đổi lệnh cũ.
- REQUESTED→PROCESSING→PAID; REQUESTED→REJECTED/CANCELLED; chưa rõ chuyển tiền→RECONCILIATION_REQUIRED và giữ locked.
- Admin claim nguyên tử; confirm PAID cần reference/chứng từ/audit, không tự duyệt lệnh của mình. VietQR không chứng minh tiền đã chuyển.
- Không retry chuyển tiền hoặc nhả locked chỉ vì timeout; xác minh chưa chuyển mới reject/release.

## 9. Data và API đề xuất

### Data

- `customer_wallets(user_id unique, points_balance bigint, version)`.
- `point_lots(id,user_id,source_type,source_id,issued_points,remaining_points,expires_at)`; `point_transactions(business_key unique,...)`.
- `voucher_redemptions`, `voucher_point_allocations(voucher_id,lot_id,allocated_points,funding_type)`.
- `voucher_templates`, `customer_vouchers` AVAILABLE/LOCKED/USED/EXPIRED/REVOKED; usage history, lock reservationId,holdUntil.
- Merchant wallets, withdrawals, bank accounts, commission/reward policies, refunds, payment events, outbox.
- Financial ledger cho cash clearing, công nợ quán, nghĩa vụ hoàn, reward/marketing cost. Point ledger và VNĐ ledger cân bằng theo unit, có liên kết quy đổi1:1; không cộng điểm vào tiền ngân hàng.
- Hoàn: nghĩa vụ đơn cũ→điểm REFUND; redeem: điểm→voucher; capture: giải trừ nghĩa vụ voucher vào đơn mới. Không nhân đôi tài sản.
- Booking snapshot5 số tiền + funding split/rate/policy versions/claim deadline/check-in/out; VISIT_VERIFICATIONS theo code thật.

### API

| Actor | Endpoint đề xuất | Contract |
|---|---|---|
| Customer | GET `/api/me/wallet`; GET `/api/me/wallet/transactions` | pointsBalance,conversionRateVnd=1; lịch sử phân trang, không hai source balances |
| Customer | GET `/api/voucher-catalog`; GET `/api/me/vouchers`; POST `/api/me/vouchers/redeem` | templateId/key, server allocate lots |
| Booking | POST `/api/reservations/quote` hoặc endpoint thật | voucherId tùy chọn, trả gross/discount/PayOS; không direct-wallet input |
| Owner | GET `/api/merchant/stores/{storeId}/wallet`, `/wallet/transactions`; GET/POST `.../withdrawals` | Scope Store,3 số dư VNĐ |
| Owner | GET/POST/PATCH `/api/merchant/bank-accounts` | Chỉ OWNER đúng phạm vi |
| Admin | GET `/api/admin/withdrawals`; POST `/{id}/process`, `/{id}/reject`, `/{id}/confirm-paid` | State machine/evidence |
| Admin | `/api/admin/commission-policies`, `/api/admin/vouchers`, `/api/admin/loyalty-policies` | Config/audit |
| Admin | GET `/api/admin/finance/reconciliation` | Tiền thu, nghĩa vụ points/voucher REFUND, reward cost, quán và sai lệch |

Errors: thiếu điểm, voucher invalid/locked/expired, sai scope, funding không hợp lệ, stale quote, idempotency conflict. Key gắn actor+operation+payload; cùng key khác payload phải conflict. Không có API client tùy ý cộng điểm.

## 10. Review Focus

1. Webhook/refund/reward lặp: chỉ một biến động — Tasks2/4.
2. Hai tab redeem/lock voucher: không vượt số dư/quota — Tasks2/3.
3. Refund có voucher và expiry: không trả trùng điểm/voucher — Tasks3/4.
4. Voucher REFUND vượt hoa hồng: quán đủ tiền, đối soát không ghi sai marketing — Tasks1/8.
5. Late claim đối đầu cron, payout chưa rõ, reward đã tiêu khi hoàn: không âm hoặc mất nghĩa vụ — Tasks4/5/8.

## 11. Tasks Antigravity

Thứ tự **0→1→2→3→4→5→6/7→8**. File booking/migration version phải map ởTask0. Mỗi task viết test fail, implement tối thiểu, test pass rồi commit riêng. Chính sách chưa duyệt chỉ chặn nhánh phụ thuộc nó.

### Task0 — Map code và duyệt

- [ ] Xác định branch/commit BE/FE/MB có booking, entity/service/controller/job/DTO/routes/tests thật; không tự đổi branch có thay đổi chưa commit.
- [ ] Map enum đặt bàn, quyền Store, min_spend/số bàn/phụ phí, gateway payment purpose.
- [ ] Chốt P1–P10; baseline tests; OpenAPI/ERD/bút toán. Xác định có dữ liệu hai ngăn đã triển khai không, không mặc định là có.

### Task1 — Pricing và hoa hồng

BE create `finance/service/ReservationPricingService.java`, `finance/dto/ReservationFinancialSnapshot.java`, `finance/entity/CommissionPolicy.java`, `finance/controller/AdminCommissionPolicyController.java` dưới `com/moodcafe/`; modify booking thật và configuration khi phù hợp.
Interface `quote(ReservationPricingInput)->ReservationFinancialSnapshot`: store/category/user/voucher, không direct-wallet. Stub validated voucher funding ở task này; tích hợp thật Task4.
Test `finance/ReservationPricingServiceTest.java`.

- [ ] Test100k/rate15%: REWARD10k→PayOS90k/net85k/platform5k; REFUND90k→PayOS10k/net85k/platform15k; mixed80k+10k→platform5k. REWARD20k vượt commission15k bị chặn; REFUND20k không chặn vì commission.
- [ ] Test voucher>gross, quote expiry, rounding và config mới không đổi snapshot cũ.
- [ ] Implement migration/admin config/pricing; test pass, commit.

### Task2 — Ví điểm và ledger

BE create `wallet/service/CustomerWalletService.java`, `MerchantWalletService.java`, `LedgerService.java`, lots/transactions/repositories/migration.
Interfaces `creditRefundPoints(RefundPointCommand)`, `creditRewardPoints(RewardPointCommand)`, `allocatePoints(userId,points,businessKey)->PointAllocation`, `post(LedgerCommand)`.
Tests `wallet/PointWalletIntegrationTest.java`, `wallet/LedgerIntegrationTest.java` PostgreSQL.

- [ ] Test refund90k→90k điểm; reward1k→balance91k; API một số dư; sum lots=balance.
- [ ] Test refundId replay một lần; concurrent allocate không âm; nguồn theoP10; rollback khi lỗi; scope user/Store.
- [ ] Implement locks/idempotency/ledger cân bằng unit; test pass, commit.

### Task3 — Voucher funding/lifecycle

BE create module `voucher/` theo pattern entity/dto/abstraction/service/controller hiện có; `VoucherRedemptionService.java`.
Interfaces `redeemVoucher(userId,templateId,key)`, `lockVoucher(userId,voucherId,reservationId,holdUntil)`, `captureVoucher(reservationId)`, `releaseVoucher(reservationId)`, `restoreVoucherForRefund(refundId,usageId)`, `expireVoucher(voucherId)`.
Tests `voucher/VoucherFundingIntegrationTest.java`, `VoucherLifecycleTest.java`.

- [ ] Đổi20k trừ20k; thiếu/quota/lỗi cấp rollback; point_cost=face_value, allocation mixed giữ nguyên.
- [ ] Hai tab lock một thắng; PAYMENT_FAILED trả AVAILABLE không cộng điểm; restore replay chỉ một lần; callback cũ không nhả voucher dùng ở đơn mới.
- [ ] TheoP3 test expired funded value trả điểm một lần, không vừa AVAILABLE vừa credit; USED/LOCKED payment chưa rõ không trả điểm.
- [ ] Implement catalog/preview/redemption/state/expiry, test pass, commit.

### Task4 — Payment/refund/settlement/reward

Modify `payment/service/PayOSService.java`, `payment/controller/PaymentController.java`, webhook/booking services/jobs thật.
Create `finance/service/ReservationSettlementService.java`, `ReservationRefundService.java`, module `loyalty/` RewardService/policies.
Interfaces `captureVerifiedPayment(VerifiedPayment)`, `confirmZeroPayReservation(reservationId)`, `settleReservation(reservationId,eventType)`, `refundReservation(RefundCommand)`, `awardReward(RewardEvent)`, `reverseReward(sourceId,reason)`.
Tests `finance/ReservationMoneyLifecycleIntegrationTest.java`, `loyalty/RewardLifecycleTest.java`.

- [ ] Sai chữ ký/amount/reference không ghi tiền; polling+webhook một capture; không trộn subscription.
- [ ] Voucher90k/gross100k→PayOS10k, checkout không trừ điểm; voucher100k→zero payment, không link, PAID_CONFIRMED/pending/USED nguyên tử.
- [ ] Refund hai ví dụ mục5.4, replay/expiry không nhân giá trị; zero-pay hủy không sinh PayOS refund; partial refund chỉ khiP6 duyệt.
- [ ] TABLE_EXPIRED chỉ nhả bàn; CLAIMED_LATE giải ngân+spend; no-show giải ngân/0điểm; concurrent job/late claim, ca qua nửa đêm theoP9.
- [ ] Spend theo gross dù PayOS10k/0; self checkout29:59/30:00, checkout hộ, review thiếu ảnh/sai verification/edit/xóa/đăng lại; late review theoP5.
- [ ] Late payment, timeout reconcile, reward đã tiêu/refund sau payout theo policy; outbox replay một lần. Test pass, commit.

### Task5 — Owner payout/Admin

BE create `wallet/service/WithdrawalService.java`, merchant/admin controllers, bank/payout repositories.
Interfaces `requestWithdrawal(actor,storeId,amount,bankAccountId,key)`, `claimWithdrawal(admin,id)`, `rejectWithdrawal(admin,id,reason)`, `confirmPaid(admin,id,reference,evidenceId)`.
Test `wallet/WithdrawalIntegrationTest.java`.

- [ ] OWNER scope đúng; MANAGER/CASHIER403 cả đọc ví/ngân hàng/rút; Owner Store khác403.
- [ ] Min fixture100k:99.999fail/100kpass khi đủ/vượt available fail; concurrent không âm; bank snapshot.
- [ ] Hai Admin claim, reject/confirm replay, reference trùng, chưa rõ giữ locked; tự duyệt lệnh mình bị cấm. Implement/test pass/commit.

### Task6 — FE

Create `src/app/merchant/wallet/page.tsx`, `src/app/admin/withdrawals/page.tsx`, `src/app/admin/vouchers/page.tsx`, `src/app/admin/finance/page.tsx`, `src/services/wallet.service.ts`, `voucher.service.ts` và components. Modify admin settings/navigation/booking-cashier thật.

- [ ] Owner3 số dư VNĐ/lịch sử/ngân hàng/rút; MANAGER/CASHIER không menu/route; Cashier gross và thao tác giao nước trễ đúng quyền.
- [ ] Admin queue payout/QR/evidence/config/reconciliation; không biến nguồn điểm nội bộ thành hai ví Customer.
- [ ] Customer web nếu có: một balance, đổi voucher, checkout voucher+PayOS, không toggle tiêu ví.
- [ ] Test quyền/loading/error/retry/validation/double-submit/responsive; TypeScript/lint/build và tests pass; commit.

### Task7 — Mobile

Create `lib/features/wallet/`, `loyalty/`, `vouchers/` đầy đủ data/domain/presentation/UseCases/manual Riverpod.
Modify `lib/app/router/app_router.dart`, `app_routes.dart`, `lib/features/profile/presentation/profile_page.dart`, booking checkout/detail thật.

- [ ] Một “Số dư MoodPoints”, chú thích “1 MoodPoint=1đ khi đổi voucher”; lịch sử hoàn đơn/thưởng/đổi; không hai card/nguồn/nút rút/nạp.
- [ ] Catalog giá điểm=giá voucher và điều kiện trước redeem; voucher status/expiry; checkout gross−voucher=PayOS, zero-pay nội bộ.
- [ ] Owner wallet đúng quyền; test DTO/UseCase/thiếu điểm/rollback/double tap/expired/locked/resume pending.
- [ ] Build_runner nếu DTO đổi, flutter analyze/test; commit.

### Task8 — Migration/đối soát/E2E

Create `finance/service/ReconciliationService.java`, report/tests. Không seed tiền vào tài khoản thật.

- [ ] Backfill có chứng cứ/dry-run/audit/key; không dùng rate hiện tại tính lại đơn cũ. Nếu đã có hai ngăn: migration riêng chuyển VNĐ khả dụng→REFUND points1:1, giải quyết khóa/đơn pending trước, không đếm trùng. Nếu chưa có, không tạo migration chuyển giả.
- [ ] Đối soát cash PayOS, nghĩa vụ điểm/voucher REFUND, reward cost, quán3 ngăn, payout; projection khớp ledger.
- [ ] E2E hoàn90k→90k điểm→voucher90k→đơn100k trả10k→quán85k; hủy lại không nhân giá trị; mixed/zero-pay/expiry đúng nguồn.
- [ ] E2E late/no-show/reward30phút/review/payout timeout/RBAC/concurrency; staging/demo/OpenAPI/test report/runbook.
- [ ] Production riêng sau khi được phép; rollback không xóa ledger/reset số dư.

## 12. Verification và bàn giao

- BE tại `D:/Learn/MoodCafe-BE/moodcafe-be`: `./mvnw.cmd test`, PostgreSQL/Testcontainers cho concurrency.
- FE: `npx tsc --noEmit`, `npm run lint`, `npm run build`, tests theo repo; ghi baseline riêng.
- Mobile: DTO đổi thì `dart run build_runner build --delete-conflicting-outputs`; `flutter analyze`; `flutter test`.
- Giao API/ERD/migration/demo/bút toán/test report. Không secrets, không tự deploy/chuyển tiền thật.

## 13. Checklist duyệt

- [x] Chốt Cách1: một số dư MoodPoints,1 điểm=1đ; hoàn thành điểm, điểm đổi voucher.
- [x] Bỏ Customer hai số dư và thanh toán trực tiếp ví–PayOS.
- [ ] Chốt P1–P10, đặc biệt catalog/expiry/funding allocation/late review.
- [ ] Xác nhận branch/commit booking và phạm vi FE/MB.
- [ ] Cho phép triển khai task sau khi chính sách liên quan đầy đủ.

**Giao Antigravity:** Dùng bản plan này. Một số dư Customer, hoàn thành MoodPoints1:1, đổi voucher trước, checkout chỉ voucher+PayOS. Không dựng lại ví VNĐ hoặc tiêu ví trực tiếp. Phân biệt quyết định đã chốt với P1–P10 còn đề xuất; map booking thật rồi triển khai theo task, kiểm thử nguồn tiền và refund không nhân đôi giá trị.
