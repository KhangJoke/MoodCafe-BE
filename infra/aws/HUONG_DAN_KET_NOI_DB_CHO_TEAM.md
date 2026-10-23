# Kết nối DB MoodCafe AWS — hướng dẫn nhanh

## 1. Nhận quyền và cài công cụ

Nhờ Nhật Nam tạo IAM user riêng, thêm vào group `MoodCafe-DbAccess`, cấp key riêng và thông tin PostgreSQL (tên DB/user/password). Group đã được tạo trên account `255572711373`; chưa thêm user nào. Không dùng chung key Admin; chỉ có password DB thì chưa mở được tunnel. Group dành cho IAM users, không áp dụng trực tiếp cho danh tính SSO.

Cài [AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html), [Session Manager plugin](https://docs.aws.amazon.com/systems-manager/latest/userguide/install-plugin-windows.html) và [DBeaver](https://dbeaver.io/download/).

## 2. Cấu hình AWS trên Windows

Nếu được cấp IAM access key riêng, mở PowerShell:

```powershell
aws configure --profile moodcafe-team
```

Nhập key/secret riêng được cấp, region `ap-southeast-1`, output `json`.

## 3. Mở tunnel

```powershell
aws ssm start-session --profile moodcafe-team --region ap-southeast-1 --target i-03046026169ea6eb5 --document-name MoodCafe-PostgresTunnel --parameters "localPortNumber=15432"
```

Thấy **Waiting for connections…** là bình thường. Giữ cửa sổ này mở, đóng là mất kết nối. Mỗi người mở tunnel trên máy mình. Document đã khóa đích `172.18.0.3:5432`, chỉ cho chọn cổng local `15432` hoặc `15433`. Nếu IP container hoặc instance đổi, nhờ Nhật Nam cập nhật document/policy; thành viên không tự chọn host/port đích. Group không cấp shell, deploy hoặc quyền quản trị; quyền từ policy/group khác của user vẫn có thể mở rộng quyền.

## 4. Điền DBeaver

Chọn **New Database Connection → PostgreSQL**:

| Ô | Giá trị |
|---|---|
| Host | `127.0.0.1` |
| Port | `15432` |
| Database | Tên DB được cấp; dự kiến `mood_cafe` nếu chưa đổi |
| Username | User PostgreSQL được cấp |
| Password | Password PostgreSQL được cấp riêng |

Không bật SSH tunnel. Bấm **Test Connection → Finish**, tải driver nếu được hỏi.

## BE local (nếu cần)

Trong `D:\Learn\MoodCafe-BE\moodcafe-be\.env` (đổi đường dẫn nếu clone ở nơi khác):

```dotenv
DB_HOST=127.0.0.1
DB_PORT=15432
DB_NAME=TEN_DB_DUOC_CAP
DB_USERNAME=USER_DB_DUOC_CAP
DB_PASSWORD=MAT_KHAU_DB_DUOC_CAP
```

Thay giá trị mẫu, giữ Redis local chạy trên cổng 6379, rồi chạy BE bằng IDE/Maven. Không chạy toàn bộ Docker Compose local vì nó tạo DB riêng và có thể chiếm cổng tunnel.

**Đây là DB dùng chung với BE AWS.** Báo Nhật Nam backup và kiểm tra Flyway migration/seed trước lần chạy BE local đầu tiên. Không tự xóa/reset dữ liệu; không đưa key, password hoặc `.env` thật lên Git.

Lỗi nhanh: `AccessDenied` → thiếu quyền AWS; `SessionManagerPlugin is not found` → thiếu plugin; sai password → kiểm tra thông tin DB; connection refused/timeout → kiểm tra tunnel và IP container.

Tham khảo: [SSM của AWS](https://docs.aws.amazon.com/systems-manager/latest/userguide/session-manager-working-with-sessions-start.html#sessions-remote-port-forwarding), [DBeaver PostgreSQL](https://dbeaver.com/docs/dbeaver/Database-driver-PostgreSQL/).
