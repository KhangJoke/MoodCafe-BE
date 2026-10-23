# MoodCafe BE trên EC2 tiết kiệm

Terraform tạo hạ tầng; bước publish image và cài `.env` triển khai ứng dụng. Không có secret ứng dụng trong variables, state hoặc EC2 user-data. Thư mục này độc lập với Compose local hiện tại.

## Cấu hình mặc định

| Thành phần | Giá trị |
| --- | --- |
| Region | Singapore `ap-southeast-1` |
| EC2 | Một `t4g.small`, 2 vCPU/2 GiB, Ubuntu 24.04 ARM64 |
| CPU credits | Standard: hết credit giảm hiệu năng, không tính surplus CPU |
| Disk | Root 12 GB + data 10 GB, gp3 mã hóa; swap 2 GB trên root |
| Network | VPC/public subnet/Internet Gateway, một Elastic IP |
| Ingress | TCP 80/443; SSH tắt mặc định; không mở 8080/5432/6379 |
| Registry | Private ECR, immutable release tags |
| App | Java 21, PostgreSQL 17, Redis 8, Caddy proxy; Docker Compose |
| Backup | PostgreSQL custom-format dump lên private S3 mỗi ngày; giữ 30 ngày |

Không tạo NAT Gateway, load balancer, RDS, ElastiCache hoặc cluster. Đây là một máy demo có điểm lỗi duy nhất, không phải hạ tầng HA. Giới hạn RAM là điểm khởi đầu cần đo tải; heap Java 512 MB vẫn có RAM native bên ngoài. Redis dùng `noeviction`: khi đủ 64 MB, một số write sẽ lỗi thay vì xóa dữ liệu xác thực. Theo dõi và tăng giới hạn/instance khi cần.

Tổng disk mặc định là **22 GB**, không phải 20 GB: tách data volume để giữ dữ liệu khi EC2 thay thế. `prevent_destroy` bảo vệ data volume; bucket backup và ECR cũng không tự xóa khi còn nội dung.

Theo [AWS T4g FAQ](https://aws.amazon.com/ec2/faqs/), ưu đãi hiện tại là 750 giờ `t4g.small`/tháng đến 31/12/2026, cộng gộp theo tài khoản/payer; sau đó instance đang chạy tính giá bình thường. EBS, ECR, backup, IPv4 và transfer không nằm trong miễn phí instance. Một IPv4 tính $0.005/giờ trước credit; Elastic IP vẫn tính tiền khi EC2 stop. Đặt AWS Budget cảnh báo, ví dụ $10/tháng, và xem Billing sau ngày đầu. Kiểm tra ưu đãi hiện hành trước khi apply; không bảo đảm hóa đơn $0. Nguồn: [IPv4 pricing](https://aws.amazon.com/vpc/pricing/), [EBS pricing](https://aws.amazon.com/ebs/pricing/).

## 1. Chuẩn bị trên máy Windows

- Terraform >= 1.9, AWS CLI v2, Docker Desktop Linux containers/buildx.
- AWS credentials cho tài khoản của bạn, có quyền EC2/VPC/IAM/ECR/S3/SSM tương ứng. Ưu tiên profile SSO; không dùng secret của root account.
- AWS CLI Session Manager plugin nếu mở terminal qua CLI; có thể dùng Session Manager trong AWS Console thay thế.
- Chưa có domain: endpoint HTTP chỉ dùng kiểm tra dữ liệu thử. HTTP không bảo vệ password/token trên đường truyền; dùng HTTPS trước khi đưa tài khoản thật vào app.

PowerShell, từ root repository BE:

```powershell
cd D:\Learn\MoodCafe-BE
# Nếu chưa có AWS profile:
aws configure sso
# Nếu dùng profile đã có, đặt tên thật của bạn:
$env:AWS_PROFILE = "ten-profile-cua-ban"
aws sso login
aws sts get-caller-identity

cd infra\aws
Copy-Item terraform.tfvars.example terraform.tfvars
terraform init
terraform fmt -check -recursive
terraform validate
terraform test
terraform plan -out=deploy.tfplan
```

Kiểm tra account ID, region, danh sách tài nguyên và dự toán chi phí. Không có API AWS nào được gọi khi chạy `terraform test` vì toàn bộ provider được mock; `plan` thật đọc AWS bằng credentials của bạn.

State đang dùng local backend. Giữ `terraform.tfstate` an toàn và backup nó ngoài repository; `.gitignore` bỏ state/tfvars/plan khỏi Git. Commit `.terraform.lock.hcl`. Nếu nhiều người cùng quản lý hạ tầng, cấu hình remote state với locking trước khi cùng apply.

## 2. Tạo hạ tầng (bước phát sinh phí)

Sau khi duyệt plan:

```powershell
terraform apply deploy.tfplan
terraform output
```

`apply` hoàn tất không đồng nghĩa BE đã chạy. Cloud-init cài Docker, AWS CLI ARM64, Session Manager agent, mount data disk, tạo swap và các file trong `/opt/moodcafe`. Chưa có secret/image nên ứng dụng chưa khởi động lần đầu.

Data disk được tìm theo serial volume ID, không đoán tên `nvme1n1`. Chỉ volume mới chưa có filesystem được format; volume ext4 cũ được mount lại. Volume có filesystem khác thì script dừng.

AMI Ubuntu ARM64 lấy từ [Canonical public SSM parameter](https://aws.github.io/graviton/amis_cf_sm.html). AMI được giữ nguyên sau lần tạo đầu để lần plan sau không tự thay máy do Ubuntu ra image mới.

## 3. Build và publish BE ARM64

Chạy test BE trên máy/CI trước khi build bản release. Dockerfile triển khai không thay Dockerfile local; dùng Temurin Java 21 hỗ trợ ARM64. Build trên Docker Desktop/CI, không build Maven trên EC2 2 GB.

```powershell
cd D:\Learn\MoodCafe-BE
# Ví dụ tag duy nhất, tránh push lại cùng tag immutable.
.\infra\aws\publish-image.ps1 -Tag release-20261006-1
```

Script lấy ECR URL/region từ Terraform outputs, login ECR, build `linux/arm64` và push, rồi logout. Profile máy bạn cần quyền push vào repository ECR. Role EC2 chỉ có quyền pull repository này, không push.

## 4. Cài secret và chạy BE

Vào AWS Console → EC2 → chọn instance → Connect → Session Manager. Hoặc chạy giá trị `terraform output -raw ssm_command` nếu đã cài plugin.

Trên EC2:

```bash
sudo cloud-init status --wait
sudo tail -n 40 /var/log/moodcafe-bootstrap.log
# Chỉ tạo file lần đầu, không ghi đè .env khi deploy lại.
sudo test -f /opt/moodcafe/.env || sudo install -m 600 /opt/moodcafe/.env.example /opt/moodcafe/.env
sudo nano /opt/moodcafe/.env
sudo /opt/moodcafe/deploy.sh release-20261006-1
```

Thay toàn bộ giá trị `CHANGE_ME`: password DB, JWT secret và SMTP. JWT secret cần đủ dài; có thể tạo bằng `openssl rand -hex 64`. Nếu migrate từ BE cũ và muốn giữ tính hợp lệ JWT, giữ issuer/audience/secret hiện tại. Copy Cloudinary/Google/Facebook/PayOS của dự án khi sử dụng các chức năng tương ứng. Script không tự lấy hoặc in secret từ môi trường local.

File `.env` dùng cú pháp Docker Compose: secret có ký tự `$` nên đặt trong dấu nháy đơn. Password DB không tự đổi chỉ bằng sửa `.env` sau khi database đã được tạo; đổi password trong PostgreSQL và `.env` đồng bộ.

`DB_HOST`, `DB_PORT`, `REDIS_HOST` và port BE được Compose đặt đúng cho mạng container. Redis không publish ra host, không dùng password trong cấu hình mạng private này. Tắt SMTP debug log để tránh log thông tin nhạy cảm.

BE mặc định chạy Flyway migration **và seed** theo source hiện tại; rà soát seed trước khi migrate dữ liệu thật. Nếu không muốn seed production, thêm `SPRING_FLYWAY_LOCATIONS=classpath:db/migration` vào `.env`. Không sửa checksum migration đã chạy.

Script kiểm tra `.env`, pull image và chờ `/api/configs/public` trả thành công tối đa 5 phút. Một container `running` chưa đủ để kết luận BE sẵn sàng. Nếu fail, dùng:

```bash
sudo bash -c 'cd /opt/moodcafe; export BACKEND_IMAGE=$(cat .image 2>/dev/null || echo unused); docker compose ps; docker compose logs --tail=80 backend'
```

Lần đầu `.image` chỉ có sau deploy thành công; `unused` trong lệnh trên chỉ để Compose parse khi đọc logs, không khởi động container mới.

## 5. Kiểm tra và kết nối Mobile

```powershell
cd D:\Learn\MoodCafe-BE\infra\aws
$apiUrl = terraform output -raw api_base_url
curl.exe "$apiUrl/configs/public"
```

Endpoint được xuất dạng `http://<elastic-ip>/api`, port 80, không có `:8080`. EC2 chưa có app thì proxy chưa chạy; apply hạ tầng đơn thuần chưa cung cấp API.

Trong Mobile, đổi `API_BASE_URL` trong `.env` và `config/dev.json` sang output này rồi dừng/chạy lại app. Code hiện tại ưu tiên `.env` hơn dart-define. Đây là cấu hình HTTP kiểm tra ban đầu, không dùng tài khoản thật qua mạng công cộng.

Có domain sau này: tạo DNS A trỏ Elastic IP, đặt `domain_name` trong `terraform.tfvars`, plan và apply vào thời gian bảo trì. User-data thay đổi sẽ **thay EC2**, có downtime; data volume giữ nguyên, script dùng lại `.env`/image đã chọn. Caddy cấp HTTPS nếu DNS và port 80/443 đúng. Cập nhật Mobile, Google redirect và PayOS callback/return URLs nếu cần. Đổi domain không tự sửa các giá trị bên trong `.env`.

## 6. Backup, restore và vận hành

Backup mỗi ngày khoảng 02:00 giờ Việt Nam (+ tối đa 5 phút), file `postgres/<UTC-timestamp>.dump`, giữ 30 ngày. `.env` không được gửi vào S3; lưu bản secret riêng an toàn. Redis không nằm trong PostgreSQL dump; nếu Redis mất, session/token cache có thể cần đăng nhập lại.

```bash
sudo /opt/moodcafe/backup.sh
sudo systemctl list-timers moodcafe-backup.timer
sudo journalctl -u moodcafe-backup.service --no-pager -n 30
free -h
df -h /opt/moodcafe /
sudo docker stats --no-stream
```

Thử restore trên môi trường thử trước khi tin vào backup. Khi cần phục hồi dữ liệu thật, xác nhận file dump và dừng ghi dữ liệu trước. Ví dụ trong terminal root (`sudo -i`), sau khi tải dump từ bucket output:

```bash
cd /opt/moodcafe
export BACKEND_IMAGE=$(cat .image)
docker compose stop backend
# Lệnh sau thay thế nội dung DB bằng dump đã chọn.
docker compose exec -T postgres sh -c 'exec pg_restore --clean --if-exists --no-owner -U "$POSTGRES_USER" -d "$POSTGRES_DB"' < restore.dump
docker compose up -d backend
```

S3 dump chứa dữ liệu DB nhạy cảm dù không chứa `.env`. Bucket private, encryption SSE-S3, policy bắt buộc TLS; không gửi dump lên Git. Role EC2 chỉ đọc/ghi prefix `postgres/` của bucket này.

Deploy bản mới: publish tag duy nhất rồi chạy `deploy.sh <tag>` trên EC2. Rollback bằng tag cũ tương thích schema; script không tự rollback database migration. Đừng đổi tag `latest` vì ECR dùng immutable tags.

Theo dõi disk root 12 GB: Docker images/logs/swap dùng ổ này. Backup trước các thay đổi dữ liệu hoặc major PostgreSQL version. Không nâng PostgreSQL major bằng đổi tag khi chưa có quy trình migrate.

`terraform destroy` sẽ bị chặn bởi `prevent_destroy` trên data volume. Khi muốn xóa toàn bộ, backup, xác nhận chủ đích xóa dữ liệu rồi sửa protection; bucket có dump và ECR có image cần dọn riêng. EC2 stop vẫn tính EBS/Elastic IP; xóa máy không tự làm hóa đơn biến mất. Module không chạy destroy hoặc apply tự động.

## Kiểm tra local không tạo tài nguyên

```powershell
terraform init -backend=false
terraform fmt -check -recursive
terraform validate
terraform test
```

Mock tests kiểm tra loại instance, Standard credits, IMDSv2, ingress, encrypted disks, giới hạn user-data 16 KiB, backup bật/tắt, domain HTTPS và chặn SSH mở toàn Internet. Validation/mock tests không thay thế kiểm tra cloud-init, app readiness và restore sau deploy thật. Terraform chuẩn hóa bootstrap và các file runtime sang LF khi đưa lên EC2, kể cả checkout trên Windows dùng CRLF. Docker được cấu hình chờ mount ổ dữ liệu trước khi tự chạy lại container sau reboot.
