# AWS BE deployment — 23/10/2026

## Phạm vi đã được duyệt

Người dùng yêu cầu deploy lên server hiện có; sau khi phát hiện local cũ hơn server, đã duyệt: **hợp nhất, giữ tính năng server rồi deploy**. Chỉ triển khai BE; không tự commit/push, không thay FE/Mobile.

## Điểm xuất phát và bảo toàn

- AWS profile có sẵn `moodcafe-new`, account255572711373, regionSingapore `ap-southeast-1`.
- EC2 `i-03046026169ea6eb5`, public IP47.130.228.240, `t4g.small`.
- Image gốc `release-20261019-a5852b4`, repo `moodcafe-backend` trong ECR cùng account.
- Local main ban đầu006d396, server a5852b4. Lưu toàn bộ tracked/untracked bằng stash `codex-predeploy-main-20261023`, fast-forward main lên a5852b4, apply lại stash và giải quyết conflict. Stash được giữ làm bản phục hồi.
- Backup PostgreSQL đã được `/opt/moodcafe/backup.sh` ghi lên private S3 trước thao tác deploy.
- Xóa13 image BE cũ không dùng, giữ current và release liền trước; root disk66%, khoảng3.7GB trống sau dọn. Không xóa volume/DB/container chính.

## Hợp nhất để tương thích

- Giữ nguyên deployed migrationsV1–V40, không sửa lịch sử/checksum DB.
- Renumber wallet migrations thànhV41–V44; tái sử dụng V40 table_categories/max_capacity/BIGINT, giữ dữ liệu booking.
- Reservation category dùng JPA entity/bean name riêng, đọc cùng bảng qua JDBC BIGINT; tránh trùng entity/repository với booking.
- Giữ PayOSService abstraction/PayOSServiceImpl hiện tại; tích hợp reservation, giữ subscription/sponsored/legacy booking dispatch và order info.
- Kiểm tra độc lập phát hiện SDK verify không nhận custom POJO; đã tái hiện test đỏ. Bản sửa convert sang Map tương thích SDK, bảo toàn unknown signed fields qua JsonAnySetter/Getter. Bốn test SDK thật đã pass: signed payload, Spring Jackson3 binding, tampered amount, tampered account metadata. Giữ fail-closed.

## Quy trình kiểm tra/deploy

- Clean package với `-Dtest=*Test,*IT`; không skip test để phát hành.
- Không chạy Docker daemon hay Maven build trên EC2. Đóng JAR vào một layer mới của runtime ARM64 hiện tại qua ECR API; giữ label SHA256 JAR và immutable release tag.
- Clone PostgreSQL thành `moodcafe_deploycheck_20261023`, dùng Redis container riêng và runner loại scheduled annotation processor. Chỉ preflight dùng runner này; không thay cơ chế scheduling production.
- Flyway V41–V44 + Hibernate full context và HTTP public config đã pass trên clone.
- OpenAPI mới170 paths, có legacy booking, reservation, customer wallet và commission.
- Chạy image gốc trên cloneV44 thành công, HTTP200. So sánh OpenAPI144 paths cũ với170 paths mới: không mất path/method nào.
- Chỉ sau khi các gate pass mới backup lần cuối, pull/up riêng service backend, giữ PostgreSQL/Redis/proxy hiện tại; kiểm tra public API rồi cập nhật `.image`.

## Kết quả cuối

Image cuối: `release-20261023-wallet-merge-2`.

- ECR digest: `sha256:b43b8eeb2ca1677c963d2fe9c747c14f911a0881c7bf94397ceb67d485ffd780`.
- JAR SHA256: `8a9e38d6e485683f9d21e427eed60513996aa7e3e0cee855de51cdf3d5ee2968`.
- Clean package275 tests,0failures,0errors,1skipped (manual reset utility disabled).
- Final image full context trên DB cloneV44 thành công, public configHTTP200; well-formed webhook giảHTTP400 với SDK `Data not integrity`.
- Live deploy thành công, SSM command `d845ab7e-3de0-4625-8003-c318eb70c27c` trả Success/DEPLOY_READY. Image và fingerprint khớp bản đã kiểm thử, restart count0 ở lần xác minh sau deploy.
- Backup ngay trước deploy: `s3://moodcafe-255572711373-ap-southeast-1-backups/postgres/2026-10-22T17-42-17Z.dump`,184039 bytes (00:42:17 ngày23/10 giờ Việt Nam). Có metadata object xác nhận trên S3.
- DB chính đã applyV41,V42,V43,V44 thành công. PostgreSQL/Redis/proxy không bị thay container.
- Public `http://47.130.228.240/api/configs/public` trả200; OpenAPI170 paths và không mất operation cũ; public store list200; danh mục bàn legacy trên cơ sở hiện hữu200; ví chưa đăng nhập401; unsigned webhook đầy đủ400.
- Probe cơ sở UUID582b75bc-1ced-43c1-a849-be08350f98d3 trả404 STORE_NOT_FOUND vì fixture đó không tồn tại trong DB tài khoản AWS này; kiểm tra lại bằng storeId từ public list đã pass200. Không coi404 fixture sai là regression.
- Đã dọn3 container app kiểm thử, Redis tạm, DB clone và dump tạm. DB chính/backup S3/image rollback được giữ. Sau deploy root disk69%/3.3GB trống, data disk7%/8.7GB trống, RAM available khoảng849MB.
- `.image.previous-20261023` giữ image trước deploy; `.image` là release mới. Image cũ đã chứng minh chạy được với schemaV44 trên clone.
- Main vẫn ởa5852b4 cùng phần code hợp nhất trong working tree; không commit/push. Stash bảo toàn code trước hợp nhất vẫn còn.
- Tag `release-20261023-wallet-merge-1` chỉ dùng preflight trước sửa SDK; không đưa vào service chính.

## Giới hạn

Triển khai không đồng nghĩa đóng12 lỗi nghiệp vụ trong `2026-10-22-antigravity-completion-rereview.md`. Hai module booking/reservation được bảo toàn API nhưng chưa chứng minh mọi đơn booking cũ đi qua ví mới. Không kiểm thử chuyển tiền thật; cần nghiệm thu UI và tài chính riêng.

Credentials người dùng gửi trong chat không được ghi vào source, report hoặc artifact. Dùng profile có sẵn; cần rotate cặp key đã lộ.
