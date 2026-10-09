# CI/CD backend production

Pipeline này triển khai backend theo đường đi sau:

```text
push main -> GitHub Actions -> Maven verify -> Docker image -> Amazon ECR
                                                       |
                                                       v
                                  AWS Systems Manager -> EC2 -> /opt/moodcafe/deploy.sh
```

GitHub Actions không đăng nhập SSH vào máy chủ. Nó xác thực AWS qua OpenID Connect (OIDC), sau đó gửi một lệnh SSM đến đúng EC2. Máy EC2 pull image đã publish từ ECR và chạy script deploy sẵn có.

## Những gì giữ nguyên trên EC2

Runtime production ở `/opt/moodcafe` là nguồn cấu hình triển khai:

- `.env` chứa secret ứng dụng, database, mail và các cấu hình production.
- `Caddyfile`, `caddy-data/` và `caddy-config/` giữ reverse proxy cùng HTTPS certificate.
- `postgres/` và `redis/` giữ dữ liệu persistent.
- `deploy.sh` chỉ nhận image tag, pull image từ ECR, chạy `docker compose up -d` và kiểm tra endpoint `/api/configs/public`.

Do đó pipeline **không copy hoặc ghi đè `.env`**, không cấp SSH key, và không chạy `git pull` trên EC2.

## GitHub configuration

Repository variable bắt buộc:

| Name | Value |
| --- | --- |
| `AWS_ROLE_TO_ASSUME` | `arn:aws:iam::255572711373:role/moodcafe-backend` |

Role này cần trust GitHub OIDC `token.actions.githubusercontent.com`, giới hạn repository `KhangJoke/MoodCafe-BE` và branch `main`. Role cần quyền tối thiểu để push vào ECR `moodcafe-backend`, gọi `ssm:SendCommand` tới instance `i-03046026169ea6eb5`, và đọc trạng thái lệnh SSM.

## Khi nào deploy

- Pull request vào `main`: chỉ chạy Maven verify.
- Push vào `main`: Maven verify, build/push image, rồi deploy.
- `workflow_dispatch`: chạy thủ công cùng quy trình publish/deploy.

Mỗi image có tag dạng `release-YYYYMMDD-<7-ký-tự-commit>`. Tag chỉ được ghi vào `/opt/moodcafe/.image` khi health check trong `deploy.sh` thành công.

## Kiểm tra hoặc deploy thủ công trên EC2

Sau khi image đã tồn tại trong ECR, vào Session Manager và chạy:

```bash
sudo /opt/moodcafe/deploy.sh release-YYYYMMDD-abcdef0
sudo docker compose -f /opt/moodcafe/compose.yml ps
sudo docker compose -f /opt/moodcafe/compose.yml logs --tail=100 backend
```

Không chạy `docker compose down` trong `/opt/moodcafe`: lệnh đó có thể làm gián đoạn proxy và các service đang phục vụ production.
