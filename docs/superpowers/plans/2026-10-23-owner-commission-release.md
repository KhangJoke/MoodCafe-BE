# Phát hành hoa hồng Owner/Admin — 23/10/2026

## Code và kiểm tra

- BE application commit: `f809bdd7210743de6e026116ec3ed621ba21d7b9`.
- FE commit: `b8dca79a16b44b73855ae5925d53799983b5e580`, đã push `origin/main` và đối chiếu remote SHA.
- FE sửa 107 lỗi lint, gồm đồng bộ state với props/URL, ref trong render, icon, API DTO/pagination, escape JSX và xử lý response thiếu dữ liệu. Không tắt các rule lint.
- Dọn 145 import binding không dùng. Lint toàn repo: **0 lỗi, 75 cảnh báo**, exit 0.
- FE typecheck pass; `npm run build -- --webpack` pass. Dùng Webpack vì Turbopack ở máy local bị lỗi tạo Node process; không đổi script build mặc định.
- BE `./mvnw.cmd -Dmaven.test.skip=true package`: BUILD SUCCESS. Không viết hoặc chạy thêm test suite trong lượt này theo yêu cầu người dùng.
- Giữ các thay đổi FE đang có về ví/voucher customer và payment return, đưa cùng vào bản push.

## Kiểm tra trước deploy trên EC2

- EC2: `i-03046026169ea6eb5`, region `ap-southeast-1`.
- Backup S3 trước preflight, sau đó clone DB chính vào DB tạm và dùng Redis riêng.
- Launcher tạm loại ScheduledAnnotationBeanPostProcessor trước khi khởi tạo app: các cron không chạy trên dữ liệu clone. Launcher không phải test source và không thay entrypoint production.
- Preflight command: `d7025f57-e13f-4cd2-946d-1fe51e726f6a`, Success và có marker `PREFLIGHT_COMMISSION_V48_SUCCESS`.
- Flyway V48 và full application context pass trên PostgreSQL 17 hiện có.
- Owner đọc được policy, history, summary, danh sách và chi tiết commission qua API có xác thực.
- Hai đơn lịch sử 4.000đ có commission 600đ/net 3.400đ; một đơn REVERSED, một đơn PENDING. Backfill tạo HOLD 2 dòng tổng 1.200đ, REVERSE 1 dòng 600đ, không tạo biến động ví mới.
- Tài khoản không thuộc cơ sở bị chặn 403.
- Thử tạo override 10% trên clone; Owner đọc được mức mới và Admin xem được audit. Không đổi mức phí trên DB chính.
- Đã dọn container preflight, Redis tạm, DB clone và dump tạm bằng cleanup của preflight.

## Production

- Immutable tag: `release-20261023-owner-commission-v48-1`.
- ECR digest: `sha256:7b82b8a96cc9ccfe487fb62fadc5f0c6aa188949c0a9a02d7e8a0658c525a7ce`.
- JAR SHA256: `e798130da5168e3c525789b3e63a1ef604b0c1957b2baed88a391e9edd46777d`.
- Đóng JAR vào runtime ARM64 hiện có qua ECR API; không chạy Maven/Docker build trên EC2.
- Backup ngay trước deploy: `s3://moodcafe-255572711373-ap-southeast-1-backups/postgres/2026-10-23T16-07-34Z.dump`, 257.470 bytes, khoảng 23:07:34 giờ Việt Nam.
- Deploy command: `376c6648-87aa-4f60-9e2f-9eb07ea46425`, Success và có marker `DEPLOY_COMMISSION_V48_SUCCESS`.
- Force-recreate riêng backend; không thay PostgreSQL, Redis hoặc proxy.
- Đối chiếu actual container image, JAR SHA, label commit và `flyway_schema_history`: **48:true**.
- Kiểm tra OpenAPI có các endpoint Owner commission và Admin revision.
- Public `/api/configs/public`: 200; commission summary chưa đăng nhập: 401.
- `.image` cập nhật sau khi health và fingerprint đã khớp. Image trước deploy lưu ở `.image.before-commission-v48-20261023`.
- Commit tài liệu phát hành dùng `[skip ci]` để tránh workflow deploy tự động chạy chồng lên lần phát hành thủ công đã xác minh này; không thay cấu hình CI/CD của các lần push sau.

## Giới hạn

Không thực hiện thanh toán, hoàn tiền hoặc chuyển khoản thật để kiểm tra. Các thao tác Admin đổi phí trong preflight chỉ tác động DB clone đã được dọn. Vercel nhận commit từ FE main; việc push không tự chứng minh deployment Vercel đã Ready.
