# Hướng Dẫn Tổng Hợp Danh Sách Quán Cà Phê (Dành Cho Người Không Rành Kỹ Thuật)

Tài liệu này hướng dẫn chi tiết cách điền thông tin các quán cà phê thật vào file mẫu Excel (`.xlsx`) hoặc CSV (`.csv`) để Admin import hàng loạt lên **MoodCafe**.

> [!TIP]
> **Khuyên dùng file Excel (`.xlsx`)**: File `moodcafe_stores_import_template.xlsx` (hoặc tải mới nhất ngay trên Web Admin tại mục **Quản lý & Duyệt quán → Import hàng loạt**) đã có sẵn 3 sheet:
> 1. **`Danh_sach_quan`**: Nơi bạn điền danh sách quán (đã có 2 dòng mẫu để tham khảo, khi nhập thật hãy xóa 2 dòng mẫu hoặc sửa đè lên).
> 2. **`Huong_dan_cot`**: Bảng tra cứu nhanh quy tắc từng cột.
> 3. **`Danh_sach_Tag_chuan`**: Bảng tổng hợp toàn bộ tên Thẻ (Tag) chuẩn đang có trên hệ thống để bạn copy-paste không bị sai chính tả.

---

## 1. Giải thích chi tiết từng cột

Các cột có dấu **`*`** là **BẮT BUỘC phải điền**.

| STT | Tên cột | Bắt buộc? | Cách điền chuẩn | Ví dụ ĐÚNG | Lỗi hay gặp (TRÁNH) |
| :-: | :--- | :-: | :--- | :--- | :--- |
| 1 | **Tên quán \*** | Bắt buộc | Ghi đúng tên quán (tối đa 255 ký tự). | `Katinat Saigon Kafe - Đồng Khởi` | Để trống hoặc ghi toàn ký tự đặc biệt |
| 2 | **Địa chỉ \*** | Bắt buộc | Số nhà, đường, phường, quận. | `156 Đồng Khởi, Bến Nghé, Quận 1` | Chỉ ghi tên đường chung chung |
| 3 | **Quận / Huyện** | Nên điền | Tên quận/huyện/thành phố thống nhất để app lọc theo quận. | `Quận 1`, `Quận 3`, `Bình Thạnh`, `Phú Nhuận`, `TP. Thủ Đức` | Lúc ghi `Q1`, lúc ghi `Quận 1`, lúc ghi `District 1` |
| 4 | **Vĩ độ (Latitude) \*** | Bắt buộc | Số **thứ nhất** lấy từ Google Maps. Ở Việt Nam luôn nằm khoảng **`8.0` đến `23.5`** (TP.HCM là `10.xxx`). Dùng dấu **chấm (`.`)**. | `10.7758421` | Dán nhầm Kinh độ (`106.xxx`) vào cột này |
| 5 | **Kinh độ (Longitude) \*** | Bắt buộc | Số **thứ hai** lấy từ Google Maps. Ở Việt Nam luôn nằm khoảng **`102.0` đến `110.0`** (TP.HCM là `106.xxx`). Dùng dấu **chấm (`.`)**. | `106.7031924` | Dán nhầm Vĩ độ (`10.xxx`) vào cột này |
| 6 | **Giờ mở cửa (HH:mm)** | Nên điền | Định dạng 24 giờ `HH:mm`. | `07:00` hoặc `08:30` | Ghi `7h sáng` hoặc `7:00 AM` |
| 7 | **Giờ đóng cửa (HH:mm)** | Nên điền | Định dạng 24 giờ `HH:mm`. Quán 24/7 ghi `00:00` và `23:59`. | `22:30` hoặc `23:00` | Ghi `10h30 tối` |
| 8 | **Giá thấp nhất (VNĐ)** | Nên điền | Chỉ ghi **số nguyên**, không có dấu chấm/phẩy hay chữ `đ`. Nếu để trống cả 2 cột giá, hệ thống tự để `30000 - 65000`. | `45000` | Ghi `45.000đ` hoặc `45k` |
| 9 | **Giá cao nhất (VNĐ)** | Nên điền | Chỉ ghi **số nguyên**, phải lớn hơn hoặc bằng Giá thấp nhất. | `85000` | Giá cao nhất nhỏ hơn giá thấp nhất |
| 10 | **Số điện thoại** | Tuỳ chọn | Số điện thoại liên hệ của quán. Trong Excel nên chọn định dạng ô là **Text** để không mất số `0` đầu. | `02838246810` | Bị Excel tự bỏ số `0` thành `2838246810` |
| 11 | **Email quán** | Tuỳ chọn | Email của quán (nếu có). Không có thì để trống. | `contact@katinat.vn` | Ghi chữ `Không có` (phải để trống hoặc đúng email) |
| 12 | **Mô tả quán** | Nên điền | 1–3 câu mô tả không gian, điểm đặc biệt, đồ uống nổi bật của quán. | `Không gian 2 tầng thoáng sáng, nhiều cây xanh, có ổ cắm ở từng bàn.` | Để quá sơ sài |
| 13 | **Danh sách Tag \*** | Bắt buộc | Chọn các Tag phù hợp từ mục 3 bên dưới, ngăn cách nhau bằng **dấu chấm phẩy (`;`)**. | `Yên tĩnh; Tối giản (Minimalism); Học bài / Chạy deadline; Wifi; Ổ cắm điện` | Tự bịa tên Tag không có trong danh sách; hoặc chọn **2 thẻ Độ yên tĩnh (NOISE)** cùng lúc |
| 14 | **Link ảnh quán** | Nên điền | Các đường link ảnh bắt đầu bằng `https://...`, nếu có nhiều ảnh thì ngăn cách bằng **dấu chấm phẩy (`;`)**. Ảnh đầu tiên là ảnh đại diện. | `https://.../anh1.jpg; https://.../anh2.jpg` | Dán đường dẫn file trên máy tính (`C:\Users\...`) |
| 15 | **Email chủ quán** | Tuỳ chọn theo dòng | Email tài khoản chủ quán (role `CUSTOMER`) trên hệ thống. Nếu để trống ở đây, Admin sẽ nhập 1 **Email chủ quán mặc định** lúc upload file. | `owner1@moodcafe.com` | Nhập email chưa từng đăng ký trên MoodCafe |

---

## 2. Cách lấy Tọa độ GPS chuẩn từ Google Maps (trong 10 giây)

> [!IMPORTANT]
> Tọa độ này rất quan trọng vì app MoodCafe dùng nó để xác thực khi khách hàng đến tận quán check-in (bán kính 150m). **Lấy sai tọa độ thì khách đến đúng quán vẫn không check-in được.**

1. Mở [Google Maps](https://maps.google.com) trên máy tính và tìm đúng tên + địa chỉ quán cà phê.
2. Zoom sát vào vị trí chấm đỏ của quán.
3. **Click chuột phải** ngay lên chấm đỏ của quán → dòng đầu tiên hiện ra một cặp số dạng:
   **`10.7758421, 106.7031924`**
4. Bấm chuột trái vào cặp số đó để copy tự động.
5. Dán tách 2 số vào Excel:
   - Số đứng trước (ở TP.HCM bắt đầu bằng **`10.`**) → Dán vào cột **Vĩ độ (Latitude) \***
   - Số đứng sau (ở TP.HCM bắt đầu bằng **`106.`**) → Dán vào cột **Kinh độ (Longitude) \***

---

## 3. Danh sách toàn bộ 50 Thẻ (Tag) chuẩn của MoodCafe

Bạn hãy **copy chính xác** tên thẻ bên dưới (không tự đổi chữ). Các thẻ cách nhau bằng dấu chấm phẩy (`;`).
*(Mỗi quán nên chọn từ 4 đến 7 thẻ để hiển thị đẹp nhất; hệ thống tự động lấy tối đa 4 thẻ đầu tiên làm thẻ nổi bật).*

### Nhóm 1: Độ yên tĩnh & Âm thanh (`NOISE`) — CHỈ CHỌN TỐI ĐA 1 THẺ / QUÁN
* `Yên tĩnh`
* `Khá yên tĩnh`
* `Bình thường`
* `Khá sôi động`
* `Sôi động`

### Nhóm 2: Vibe & Phong cách (`VIBE`)
* `Tối giản (Minimalism)`
* `Cổ điển (Vintage / Retro)`
* `Sân vườn nhiệt đới (Tropical Garden)`
* `Công xưởng (Industrial)`
* `Hiện đại & Sang trọng (Modern Luxury)`
* `Phong cách Nhật Bản (Japanese / Zen)`
* `Hàn Quốc (Korean Aesthetic)`
* `Đông Dương (Indochine)`
* `Gỗ mộc ấm cúng (Rustic Wood)`
* `Tầng thượng thoáng đãng (Rooftop)`
* `Nghệ thuật & Sáng tạo (Art Studio)`
* `Chill Lofi & Nhẹ nhàng`
* `Chữa lành & Trầm lắng (Healing)`
* `Lãng mạn & Thơ mộng`
* `Năng động & Trẻ trung`

### Nhóm 3: Mục đích sử dụng (`PURPOSE`)
* `Học bài / Chạy deadline`
* `Làm việc nhóm / Họp`
* `Hẹn hò lãng mạn`
* `Tụ tập bạn bè`
* `Thư giãn / Đọc sách`
* `Chụp ảnh / Sống ảo`
* `Gặp gỡ đối tác / Bán hàng`
* `Chơi Boardgame & Giải trí`
* `Thưởng thức cà phê đặc sản`
* `Trò chuyện riêng tư`
* `Nghe nhạc & Thư giãn`
* `Ngắm đường phố`
* `Làm việc cá nhân dài giờ`
* `Tâm sự sâu lắng`
* `Ăn nhẹ & Uống trà`

### Nhóm 4: Tiện ích quán (`AMENITY`)
* `Máy lạnh`
* `Wifi`
* `Ổ cắm điện`
* `Bãi đỗ xe máy`
* `Bãi đỗ ô tô`
* `Thân thiện thú cưng`
* `Nước lọc miễn phí`
* `Bàn lớn làm việc`
* `Khu vực ngoài trời`
* `Ghế êm ái`
* `Không hút thuốc`
* `Thanh toán thẻ/QR`
* `Phục vụ bánh ngọt`
* `Mở cửa khuya`
* `Nhà vệ sinh riêng`

---

## 4. Cách lấy Link ảnh quán

* **Cách 1 (Đã có link ảnh online)**: Nếu ảnh nằm trên Facebook, Website quán, Cloudinary, Unsplash... hãy chuột phải vào ảnh → **Sao chép địa chỉ hình ảnh (Copy image address)** và dán vào cột `Link ảnh quán` (link phải bắt đầu bằng `https://...`).
* **Cách 2 (Ảnh đang lưu trên máy tính)**: Vào Web Admin → **Quản lý & Duyệt quán → Import hàng loạt** → bấm **"Công cụ phụ: Upload ảnh từ máy tính để lấy link Cloudinary"** → chọn ảnh để hệ thống tự upload và bấm **Copy** dán vào Excel.

---

## 5. Quy trình khuyên dùng khi nhập 100 quán

1. Nhập thử **2–3 quán đầu tiên** vào file Excel.
2. Admin đăng nhập Web → vào **Quản lý & Duyệt quán → Import hàng loạt** → kéo thả file vào để xem bảng kiểm tra lỗi.
3. Nhập **Email chủ quán mặc định** (ví dụ tài khoản chủ quán đại diện `owner1@moodcafe.com`), chọn trạng thái `ACTIVE`, bấm **Tiến hành Import**.
4. Kiểm tra quán vừa lên trên app/web khách hàng xem ảnh, thẻ tag, giờ mở cửa và vị trí bản đồ đã chuẩn chưa.
5. Sau khi ổn, nhập tiếp 97 quán còn lại (có thể giữ nguyên file cũ, hệ thống có chức năng **tự bỏ qua quán đã tồn tại** nên không sợ bị trùng).
