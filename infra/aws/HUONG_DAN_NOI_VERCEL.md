# Nối FE Vercel với BE AWS

FE: `https://mood-cafe-fe.vercel.app`.
BE HTTPS dự kiến: `https://47-130-228-240.sslip.io/api`.

**Lần này deploy image BE, không chạy `terraform apply`.** Thay `domain_name` hoặc
asset bootstrap có thể làm Terraform thay EC2 (`user_data_replace_on_change=true`).
Các file Terraform/bootstrap hiện có không được thay đổi trong bản sửa này.

## 1. Build trên máy Windows

Mở Docker Desktop, rồi chạy PowerShell:

```powershell
cd D:\Learn\MoodCafe-BE
$env:AWS_PROFILE = "moodcafe-new"
.\infra\aws\publish-image.ps1 -Tag release-20261007-1
```

Nếu tag đã tồn tại trong ECR, dùng tag mới, ví dụ `release-20261007-2`, ở cả build và deploy.

## 2. Cập nhật trên EC2

Vào SSM bằng tài khoản Admin/deploy (group `MoodCafe-DbAccess` không có quyền deploy):

```powershell
aws ssm start-session --profile moodcafe-new --region ap-southeast-1 --target i-03046026169ea6eb5
```

Backup DB trước khi deploy vì image mới có thể chạy migration:

```sh
sudo /opt/moodcafe/backup.sh
sudo nano /opt/moodcafe/.env
```

Giữ nguyên DB, JWT và các secret hiện có. Thêm/sửa đúng ba dòng, tránh trùng key:

```dotenv
CORS_ALLOWED_ORIGINS=https://mood-cafe-fe.vercel.app,http://localhost:3000
COOKIE_SECURE=true
COOKIE_SAME_SITE=None
```

Origin không có dấu `/` cuối, không dùng `*`. FE preview Vercel có domain khác phải
được thêm rõ ràng nếu cần; không tự cho phép mọi preview. Local HTTP dùng
`COOKIE_SECURE=false`, `COOKIE_SAME_SITE=Lax` trong môi trường local riêng.

```sh
sudo chmod 600 /opt/moodcafe/.env
sudo /opt/moodcafe/deploy.sh release-20261007-1
sudo nano /opt/moodcafe/Caddyfile
```

Dán nội dung `infra/aws/runtime/Caddyfile.vercel.example` vào Caddyfile EC2.
Validate rồi reload, chỉ tiếp tục nếu validate thành công:

```sh
sudo docker exec moodcafe-proxy-1 caddy validate --config /etc/caddy/Caddyfile
sudo docker exec moodcafe-proxy-1 caddy reload --config /etc/caddy/Caddyfile
```

Nếu file trong container vẫn là cấu hình cũ do bind mount khi editor thay file,
tạo lại **chỉ proxy**, không xóa volume/DB:

```sh
cd /opt/moodcafe
sudo sh -c 'export BACKEND_IMAGE="$(cat .image)"; docker compose --env-file .env -f compose.yml up -d --force-recreate --no-deps proxy'
```

Caddy tự xin chứng chỉ khi hostname trỏ đúng IP, port 80/443 truy cập được từ
Internet. Có thể cần đợi cấp chứng chỉ; xem lỗi bằng
`sudo docker logs --tail 80 moodcafe-proxy-1`. Không bỏ kiểm tra TLS bằng `curl -k`.

## 3. Kiểm tra HTTPS + CORS trước khi đổi FE

Trên EC2:

```sh
curl -i https://47-130-228-240.sslip.io/api/configs/public
curl -i -X OPTIONS https://47-130-228-240.sslip.io/api/auth/login -H 'Origin: https://mood-cafe-fe.vercel.app' -H 'Access-Control-Request-Method: POST' -H 'Access-Control-Request-Headers: content-type,authorization,x-cookies-enabled'
```

GET phải trả `200` và JSON. OPTIONS phải trả `200`, có
`Access-Control-Allow-Origin: https://mood-cafe-fe.vercel.app` và
`Access-Control-Allow-Credentials: true`.

## 4. Cập nhật Vercel

Project → Settings → Environment Variables → **Production**:

```dotenv
NEXT_PUBLIC_API_URL=https://47-130-228-240.sslip.io/api
NEXT_PUBLIC_WS_URL=wss://47-130-228-240.sslip.io/ws
```

Redeploy FE sau khi đổi biến. Kiểm tra đăng nhập, API có Bearer token, refresh sau
khi access token hết hạn, đăng xuất và WebSocket. Refresh cookie phải có
`HttpOnly; Secure; SameSite=None`. HTTP IP cũ không dùng được secure refresh cookie.

Một số trình duyệt chặn third-party cookies dù có `SameSite=None`: nếu refresh
không gửi cookie, cần phương án cùng site/domain hoặc proxy; CORS không khắc phục
được chính sách này. Không đưa secret BE vào biến `NEXT_PUBLIC_*`.

## Lưu ý

`sslip.io` là DNS bên thứ ba phù hợp demo, không phải domain riêng của dự án;
DNS/cấp chứng chỉ có thể bị giới hạn. Cấu hình Caddy thủ công này cần áp dụng lại
nếu EC2 được thay bằng bootstrap hiện tại. Khi có domain thật, chuyển sang cấu
hình Terraform quản lý domain trong một lần triển khai có kế hoạch.

Tham khảo: [Caddy HTTPS](https://caddyserver.com/docs/automatic-https),
[sslip.io](https://sslip.io/),
[Cookies](https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/Cookies),
[Vercel environment variables](https://vercel.com/docs/environment-variables).
