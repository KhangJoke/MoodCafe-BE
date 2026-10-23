# Plan triển khai quản lý hoa hồng cho Owner và Admin

**Người thực hiện:** Codex, triển khai tuần tự trong phiên hiện tại khi người dùng yêu cầu bắt đầu.
**Phạm vi:** BE + FE. Mobile giữ nguyên.
**Cách kiểm tra do người dùng chốt:** Không viết test tự động mới, không thêm thư viện test, không chạy test suite. Kiểm tra build, lint/typecheck và thao tác thực tế. Quy tắc này áp dụng cho công việc tiếp theo của project đến khi người dùng đổi yêu cầu.

## 1. Mục tiêu và quyết định nghiệp vụ

- Owner xem được chính sách hoa hồng đang áp dụng, phí từng booking, lịch sử thay đổi và tình trạng quyết toán.
- Admin chỉnh được tỷ lệ toàn sàn hoặc riêng từng cơ sở. Owner chỉ xem và đối soát.
- Ví dụ không voucher: đơn 4.000đ, hoa hồng 15% = 600đ, quán nhận 3.400đ.
- Commission tính trên gross_amount, dùng BigDecimal và làm tròn HALF_UP đến VNĐ nguyên. Không lấy tiền PayOS trừ tiền quán để suy ra commission khi có voucher.
- Policy cơ sở ưu tiên hơn toàn sàn; không có policy hợp lệ thì dùng fallback hiện hành 15% và ghi rõ nguồn.
- Đổi mức phí tạo phiên bản lịch sử mới; không ghi đè snapshot báo giá/booking đã chốt. Quote còn hạn giữ giá đã chốt; hết hạn lấy quote mới.
- Giữ nguyên 1 MoodPoint = 1đ, đổi điểm lấy voucher trước checkout, hoàn tiền vào MoodPoints, không thêm thanh toán kết hợp ví–PayOS.
- Hủy phiên PayOS không hủy booking và không đảo commission; chỉ xử lý khi booking thực sự bị hủy/hoàn hoặc hết hạn theo nghiệp vụ.

## 2. Hiện trạng đã kiểm tra

- BE đã có policy, tính commission và snapshot commission_amount/merchant_net_amount/commission_rate_snapshot trên booking.
- Admin FE đã có trang commission-policies nhưng mới tạo và bật/tắt, chọn cơ sở bằng UUID.
- Owner FE chỉ thấy net trong ví, chưa có bảng kê gross/commission/net.
- Chưa có ledger hoa hồng riêng để theo dõi HOLD/SETTLE/REVERSE và đối soát.
- Không đưa commission vào DTO chung của customer/nhân viên; tạo API Owner riêng.

## 3. Trình tự thực hiện

### Bước 1 — Kiểm tra workspace và khóa phạm vi

- [ ] Kiểm tra main, git status và thay đổi hiện có ở BE/FE. Không ghi đè công việc đang dở.
- [ ] Đọc AGENTS.md của từng repo; đọc tài liệu Next.js tại node_modules theo hướng dẫn repo trước khi sửa FE.
- [ ] Xác định đường booking/payment đang chạy thực tế và tất cả điểm capture, settle, refund; chỉ tích hợp đường reservation cũ nếu còn hoạt động để tránh ghi hai lần.
- [ ] Xác định migration version tiếp theo còn trống. Không sửa migration đã chạy.
- [ ] Ghi nhận build/lint hiện trạng để phân biệt lỗi có sẵn với lỗi do thay đổi.

### Bước 2 — Hoàn thiện cấu hình phí trên BE

Files: finance/controller/AdminCommissionPolicyController.java; finance/entity/CommissionPolicy.java; finance/abstraction/repository/CommissionPolicyRepository.java; tạo CommissionPolicyService và DTO cần thiết.

- [ ] Chuyển logic thay đổi policy vào service transaction.
- [ ] Giữ tương thích API hiện có; bổ sung POST /api/admin/commission-policies/{policyId}/revisions nhận commissionRate và effectiveFrom.
- [ ] Validate tỷ lệ 0–100%, tối đa 4 chữ số thập phân dạng rate; store phải tồn tại; khoảng hiệu lực hợp lệ.
- [ ] Trả tên/địa chỉ cơ sở, policy nguồn và trạng thái hiệu lực thực tế.
- [ ] Lưu người thay đổi, thời điểm và supersedesPolicyId. Thay rate tạo phiên bản mới.
- [ ] Khoảng hiệu lực [effectiveFrom, effectiveTo); không chồng lấn cùng phạm vi. Khóa theo global/store khi ghi để chặn race giữa Admin.
- [ ] Policy tương lai không làm policy hiện tại ngừng sớm. Bật lại policy cũng phải kiểm tra chồng lấn.
- [ ] Bổ sung policyId snapshot nullable trên quote/booking nếu chưa có. Dữ liệu cũ giữ số tiền/rate, không đoán nguồn policy.
- [ ] Kiểm tra thủ công: global 15%, override cơ sở 10%, tắt override quay về global; quote mới đổi rate, booking cũ giữ rate.

### Bước 3 — Sổ hoa hồng và vòng đời tiền

Files mới: finance/entity/BookingCommissionEntry.java; repository; finance/service/BookingCommissionService.java; migration mới. Tích hợp booking/service/BookingServiceImpl.java và các điểm thanh toán thực tế.

- [ ] Ledger bất biến gồm bookingId, storeId, event HOLD/SETTLE/REVERSE, amount, currency VND, businessKey unique, source/actor, occurredAt và liên kết bút toán gốc.
- [ ] hold(bookingId), settle(bookingId), reverse(bookingId) đọc snapshot đã khóa; dùng chung transaction với cập nhật ví và booking.
- [ ] PAID_CONFIRMED: HOLD commission, merchant net vào pending.
- [ ] CHECKED_IN và TABLE_EXPIRED: giữ pending.
- [ ] COMPLETED, CLAIMED_LATE hoặc EXPIRED_NO_SHOW cuối ngày: SETTLE commission; net pending sang available.
- [ ] CANCELLED_REFUNDED hợp lệ: REVERSE commission; đảo net pending; hoàn số tiền thực trả thành MoodPoints và khôi phục voucher theo nghiệp vụ hiện có.
- [ ] Chưa trả tiền hoặc chỉ hủy phiên PayOS: không ghi commission.
- [ ] Mỗi sự kiện có idempotency ở DB, transaction lock và unique business key; callback/check/cron lặp không tạo giao dịch trùng.
- [ ] Số tiền event dương, reversal nêu rõ đảo từ pending hay settled; tổng reverse không vượt khoản gốc. Không cho số dư âm khi xử lý bất thường.
- [ ] Backfill đơn cũ theo snapshot và bằng chứng capture/settlement; idempotent, không làm biến động ví lần nữa. Thiếu bằng chứng thì đánh dấu cần đối soát.
- [ ] Kiểm tra thủ công một đơn 4.000đ: hold 600đ và net pending 3.400đ; settle một lần; một đơn khác hủy hợp lệ thì đảo đúng tiền.

### Bước 4 — API tài chính cho Owner

Tạo OwnerCommissionController, OwnerCommissionQueryService và DTO riêng; tái sử dụng StoreFinancialSecurityService.

- [ ] GET /api/merchant/stores/{storeId}/commission-policy: policy thực tế và nguồn STORE/GLOBAL/FALLBACK.
- [ ] GET /api/merchant/stores/{storeId}/commission-policies: lịch sử liên quan cơ sở, phân trang.
- [ ] GET /api/merchant/stores/{storeId}/commission-summary?from=&to=: thống kê phát sinh trong kỳ.
- [ ] GET /api/merchant/stores/{storeId}/commission-bookings?from=&to=&status=&bookingCode=&page=&size=: bảng kê booking.
- [ ] GET /api/merchant/stores/{storeId}/commission-bookings/{bookingId}: snapshot và timeline đối soát.
- [ ] Chỉ OWNER của đúng store truy cập; chặn customer, MANAGER, CASHIER, Owner cơ sở khác. Admin dùng API Admin.
- [ ] Summary tách số dư hiện tại với phát sinh trong kỳ; pending/settled/reversed tính từ ledger. Danh sách booking lọc theo paidAt, reversal tính theo occurredAt; không cộng lẫn hai loại kỳ.
- [ ] Thời gian [from,to), Asia/Ho_Chi_Minh trên UI và UTC ISO ở API; tối đa 366 ngày, page size tối đa 100, sort ổn định.
- [ ] Detail trả breakdown voucher/funding khi cần giải thích vì sao customer paid khác gross; không tính lại snapshot từ policy hiện tại.

### Bước 5 — FE bảng kê Owner

Sửa src/app/merchant/wallet/page.tsx; tạo OwnerCommissionTab.tsx và CommissionBookingDetail.tsx trong src/components/merchant/wallet/; tạo src/services/owner-commission.service.ts và type/query key tương ứng.

- [ ] Thêm tab Hoa hồng & Đối soát trong Ví doanh thu & Rút tiền.
- [ ] Hiện mức phí đang áp dụng, nguồn, ngày hiệu lực và lịch sử; giải thích từng đơn giữ rate lúc chốt.
- [ ] Bộ lọc ngày, mã đơn, trạng thái; mặc định tháng hiện tại. Loading/error/empty/retry và phân trang server.
- [ ] Các số tổng: giá trị đơn đã trả trong kỳ, hoa hồng chờ quyết toán, đã quyết toán, đã đảo; nhãn rõ để không nhầm với số dư ví hiện tại.
- [ ] Bảng từng đơn: mã, thời gian trả tiền, gross, rate, phí, net quán nhận, trạng thái.
- [ ] Chi tiết đơn có công thức gross − phí = net; voucher hiển thị riêng cùng giải thích funding, không suy diễn chênh lệch PayOS là phí.
- [ ] Liên kết giao dịch ví đến booking đúng reference; Owner không có nút tự đổi phí/rút tiền commission của sàn.
- [ ] Đổi store phải xóa detail/selection cũ, query key chứa storeId và filter, bỏ response lỗi thời.
- [ ] Dùng theme Owner hiện có; kiểm tra 375px, 768px, desktop; định dạng tiền vi-VN.

### Bước 6 — FE chỉnh phí Admin

Sửa src/app/admin/commission-policies/page.tsx, src/services/commission.service.ts, breadcrumb AdminHeader.tsx; dùng API cơ sở hiện có từ admin.service.ts.

- [ ] Thêm Đổi mức phí gọi revision API, giữ phạm vi policy gốc.
- [ ] Tìm/chọn cơ sở theo tên và địa chỉ thay nhập UUID, phân trang tìm kiếm server.
- [ ] Nhập phần trăm 0–100, tối đa 2 chữ số thập phân; chọn hiệu lực ngay hoặc tương lai.
- [ ] Hiển thị policy đang hiệu lực/sắp hiệu lực/hết hiệu lực/tạm dừng, lịch sử trước/sau và người thay đổi.
- [ ] Thông báo rõ chỉ báo giá mới từ thời điểm hiệu lực nhận rate mới; booking đã chốt giữ nguyên.
- [ ] Khi tắt override hiển thị mức global/fallback tiếp quản; chặn lưu lặp và hiển thị lỗi BE rõ ràng.
- [ ] Không thay policy server đang dùng trong quá trình code. Thao tác kiểm tra rate dùng môi trường test hoặc cơ sở test được chỉ định.

## 4. Kiểm tra bàn giao — không viết tests

- [ ] BE: chạy ./mvnw.cmd -Dmaven.test.skip=true package trong MoodCafe-BE/moodcafe-be. Không có Java lint plugin trong pom hiện tại; không tuyên bố BE đã pass lint khi chưa có công cụ.
- [ ] FE: npx tsc --noEmit, npm run lint, npm run build. Sửa lỗi trong phạm vi; lỗi có sẵn ngoài phạm vi phải ghi rõ.
- [ ] Thao tác API/UI thật với dữ liệu test: Admin đổi rate → customer đặt và trả tiền → Owner xem commission/net → hoàn tất → kiểm tra ví khả dụng.
- [ ] Case đơn riêng hủy hoàn trước hạn: net pending bị đảo, commission reversed, số MoodPoints/voucher hoàn đúng.
- [ ] Case hủy phiên PayOS rồi trả lại: booking giữ nguyên, chỉ một HOLD commission.
- [ ] Refresh, callback/check lặp, đổi cơ sở, lọc ngày và quyền sai: tiền không tăng đôi, dữ liệu không lẫn store, API trả từ chối đúng.
- [ ] So sánh booking snapshot, ledger và merchant wallet transactions. Nếu không thể thao tác vì thiếu tài khoản/quyền hoặc môi trường không chạy, báo đúng bước chưa xác minh; build thành công không thay thế kiểm tra luồng.
- [ ] Ghi lại kết quả build/lint/manual flow, migration cần chạy và điểm còn thiếu. Không tự phát sinh chuyển khoản/rút tiền thật để kiểm tra.

## 5. Triển khai và phạm vi hoàn thành

- [ ] Hoàn thiện code và kiểm tra trước. Commit/push/deploy theo yêu cầu triển khai của người dùng; không coi plan này là một lần triển khai đã thực hiện.
- [ ] Khi deploy: backup DB, chạy migration mới, kiểm tra health và phiên bản chạy, kiểm tra lại policy/Owner read API.
- [ ] Migrations chỉ thêm mới; rollback ứng dụng phải tương thích dữ liệu, không xóa sổ tiền hoặc sửa lịch sử để quay lui.

Hoàn thành khi Owner tra được khoản 600đ thuộc đơn nào và đang ở giai đoạn nào; Admin đổi được phí có lịch sử; booking cũ không bị tính lại; gross/commission/net đối chiếu được và build/lint cùng các bước thủ công đã kiểm tra có kết quả rõ ràng.
