package com.moodcafe.finance.service;

import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.finance.abstraction.repository.CommissionPolicyRepository;
import com.moodcafe.finance.entity.CommissionPolicy;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.store.abstraction.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CommissionPolicyService {
    private final CommissionPolicyRepository repository;
    private final StoreRepository stores;
    private final CurrentUserService currentUser;
    private final JdbcTemplate jdbc;
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    public record PolicyView(UUID policyId, UUID storeId, String storeName, BigDecimal commissionRate,
            Instant effectiveFrom, Instant effectiveTo, boolean isActive, String status,
            UUID supersedesPolicyId, UUID changedBy, String changedByName, Instant createdAt, Instant updatedAt) {}
    public record EffectivePolicy(UUID policyId, String source, BigDecimal commissionRate,
            Instant effectiveFrom, Instant effectiveTo) {}

    @Transactional(readOnly = true)
    public List<PolicyView> list(UUID storeId) {
        return (storeId == null ? repository.findAll() : repository.findByStoreId(storeId)).stream()
                .sorted(Comparator.comparing(CommissionPolicy::getEffectiveFrom).reversed())
                .map(this::view).toList();
    }
    @Transactional(readOnly = true)
    public Map<String,Object> history(UUID storeId, int page, int size) {
        var result = repository.findRelevantPolicies(storeId, org.springframework.data.domain.PageRequest.of(page,size));
        var rows = result.getContent().stream().map(p -> {
            Map<String,Object> row = new LinkedHashMap<>();
            row.put("policyId",p.getPolicyId());row.put("source",p.getStoreId()==null?"GLOBAL":"STORE");
            row.put("commissionRate",p.getCommissionRate());row.put("effectiveFrom",p.getEffectiveFrom());row.put("effectiveTo",p.getEffectiveTo());
            Instant now=Instant.now();
            row.put("status",!Boolean.TRUE.equals(p.getIsActive())?"PAUSED":p.getEffectiveTo()!=null&&!now.isBefore(p.getEffectiveTo())?"EXPIRED":now.isBefore(p.getEffectiveFrom())?"SCHEDULED":"ACTIVE");
            return row;
        }).toList();
        return Map.of("content",rows,"totalElements",result.getTotalElements(),"totalPages",result.getTotalPages());
    }
    @Transactional(readOnly = true)
    public List<Map<String,Object>> audit(UUID policyId) {
        if (!repository.existsById(policyId)) throw new AppException(ErrorCode.NOT_FOUND,"Không tìm thấy chính sách");
        return jdbc.queryForList("""
            with recursive family as (
              select policy_id,supersedes_policy_id from commission_policies where policy_id=?
              union
              select p.policy_id,p.supersedes_policy_id from commission_policies p join family f
                on p.policy_id=f.supersedes_policy_id or p.supersedes_policy_id=f.policy_id
            )
            select a.audit_id as "auditId",a.policy_id as "policyId",a.action,a.commission_rate as "commissionRate",
              a.effective_from as "effectiveFrom",a.effective_to as "effectiveTo",a.occurred_at as "occurredAt",
              coalesce(u.full_name,'Hệ thống') as "actorName"
            from commission_policy_audit a left join users u on u.user_id=a.actor_id
            where a.policy_id in (select policy_id from family) order by a.occurred_at desc,a.audit_id desc
            """,policyId);
    }
    private PolicyView saved(CommissionPolicy p, String action) {
        CommissionPolicy saved = repository.saveAndFlush(p);
        jdbc.update("insert into commission_policy_audit(policy_id,action,actor_id,commission_rate,effective_from,effective_to,is_active) values (?,?,?,?,?,?,?)",
                saved.getPolicyId(), action, currentUser.getCurrentUserId(), saved.getCommissionRate(),
                java.sql.Timestamp.from(saved.getEffectiveFrom()), saved.getEffectiveTo()==null?null:java.sql.Timestamp.from(saved.getEffectiveTo()), saved.getIsActive());
        return view(saved);
    }
    public PolicyView view(CommissionPolicy p) {
        Instant now = Instant.now();
        String status = !Boolean.TRUE.equals(p.getIsActive()) ? "PAUSED"
                : p.getEffectiveTo() != null && !now.isBefore(p.getEffectiveTo()) ? "EXPIRED"
                : now.isBefore(p.getEffectiveFrom()) ? "SCHEDULED" : "ACTIVE";
        String storeName = p.getStoreId() == null ? null : stores.findById(p.getStoreId()).map(s -> s.getName()).orElse("Cơ sở không còn tồn tại");
        String actor = p.getChangedBy() == null ? "Hệ thống / Dữ liệu cũ" : jdbc.query(
                "select full_name from users where user_id = ?", (rs, n) -> rs.getString(1), p.getChangedBy())
                .stream().findFirst().orElse("Admin");
        return new PolicyView(p.getPolicyId(), p.getStoreId(), storeName, p.getCommissionRate(),
                p.getEffectiveFrom(), p.getEffectiveTo(), Boolean.TRUE.equals(p.getIsActive()), status,
                p.getSupersedesPolicyId(), p.getChangedBy(), actor, p.getCreatedAt(), p.getUpdatedAt());
    }
    @Transactional(readOnly = true)
    public EffectivePolicy effective(UUID storeId) {
        Instant now = Instant.now();
        List<CommissionPolicy> found = repository.findActiveStorePolicies(storeId, now);
        String source = "STORE";
        if (found.isEmpty()) { found = repository.findActiveGlobalPolicies(now); source = "GLOBAL"; }
        if (found.isEmpty()) return new EffectivePolicy(null, "FALLBACK", new BigDecimal("0.1500"), null, null);
        CommissionPolicy p = found.get(0);
        return new EffectivePolicy(p.getPolicyId(), source, p.getCommissionRate(), p.getEffectiveFrom(), p.getEffectiveTo());
    }
    // A database row serializes writes even when no policy exists for this scope yet.
    private void lockScope(UUID storeId) {
        String scope = storeId == null ? "GLOBAL" : storeId.toString();
        jdbc.update("insert into commission_policy_scopes(scope_key) values (?) on conflict do nothing", scope);
        jdbc.queryForObject("select scope_key from commission_policy_scopes where scope_key = ? for update", String.class, scope);
    }
    private void validate(UUID storeId, BigDecimal rate, Instant from, Instant to) {
        if (rate == null || rate.signum() < 0 || rate.compareTo(BigDecimal.ONE) > 0 || rate.stripTrailingZeros().scale() > 4)
            throw new AppException(ErrorCode.BAD_REQUEST, "Tỷ lệ hoa hồng phải từ 0% đến 100%, tối đa 2 số lẻ phần trăm");
        if (storeId != null && !stores.existsById(storeId)) throw new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy cơ sở");
        if (to != null && !to.isAfter(from)) throw new AppException(ErrorCode.BAD_REQUEST, "Ngày kết thúc phải sau ngày bắt đầu");
    }
    private boolean overlap(CommissionPolicy p, Instant from, Instant to) {
        return (p.getEffectiveTo() == null || p.getEffectiveTo().isAfter(from))
                && (to == null || to.isAfter(p.getEffectiveFrom()));
    }
    private List<CommissionPolicy> scope(UUID storeId) {
        return storeId == null ? repository.findByStoreIdIsNull() : repository.findByStoreId(storeId);
    }
    @Transactional
    public PolicyView create(UUID storeId, BigDecimal rate, Instant from, Instant to, Boolean active) {
        Instant start = from == null ? Instant.now() : from;
        validate(storeId, rate, start, to);
        if (start.isBefore(Instant.now().minusSeconds(5))) throw new AppException(ErrorCode.BAD_REQUEST, "Không tạo chính sách hồi tố");
        lockScope(storeId);
        boolean enabled = active == null || active;
        if (enabled && scope(storeId).stream().anyMatch(p -> Boolean.TRUE.equals(p.getIsActive()) && overlap(p, start, to)))
            throw new AppException(ErrorCode.CONFLICT, "Đã có chính sách trong khoảng này. Hãy dùng Đổi mức phí để lưu phiên bản mới.");
        return saved(CommissionPolicy.builder().storeId(storeId).commissionRate(rate)
                .effectiveFrom(start).effectiveTo(to).isActive(enabled).changedBy(currentUser.getCurrentUserId()).build(), "CREATE");
    }
    @Transactional
    public PolicyView revise(UUID id, BigDecimal rate, Instant from) {
        CommissionPolicy initial = repository.findById(id).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy chính sách"));
        lockScope(initial.getStoreId());
        entityManager.refresh(initial);
        CommissionPolicy old = initial;
        Instant start = from == null ? Instant.now() : from;
        validate(old.getStoreId(), rate, start, null);
        if (!Boolean.TRUE.equals(old.getIsActive()) || (old.getEffectiveTo() != null && !old.getEffectiveTo().isAfter(start))
                || !start.isAfter(old.getEffectiveFrom()) || start.isBefore(Instant.now().minusSeconds(5)))
            throw new AppException(ErrorCode.BAD_REQUEST, "Chỉ đổi phí từ chính sách còn hiệu lực tại thời điểm mới; không sửa hồi tố");
        Instant end = old.getEffectiveTo();
        if (scope(old.getStoreId()).stream().anyMatch(p -> !p.getPolicyId().equals(id) && Boolean.TRUE.equals(p.getIsActive()) && overlap(p, start, end)))
            throw new AppException(ErrorCode.CONFLICT, "Khoảng hiệu lực trùng chính sách khác");
        old.setEffectiveTo(start);
        saved(old, "SUPERSEDED");
        return saved(CommissionPolicy.builder().storeId(old.getStoreId()).commissionRate(rate)
                .effectiveFrom(start).effectiveTo(end).isActive(true).supersedesPolicyId(id)
                .changedBy(currentUser.getCurrentUserId()).build(), "REVISE");
    }
    @Transactional
    public PolicyView toggle(UUID id, boolean active) {
        CommissionPolicy p = repository.findById(id).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy chính sách"));
        lockScope(p.getStoreId());
        entityManager.refresh(p);
        if (active) {
            if (Boolean.TRUE.equals(p.getIsActive())) return view(p);
            Instant end = p.getEffectiveTo() != null && p.getEffectiveTo().isAfter(Instant.now()) ? p.getEffectiveTo() : null;
            Instant start = p.getEffectiveFrom().isBefore(Instant.now()) ? Instant.now() : p.getEffectiveFrom();
            if (scope(p.getStoreId()).stream().anyMatch(o -> !o.getPolicyId().equals(id) && Boolean.TRUE.equals(o.getIsActive()) && overlap(o, start, end)))
                throw new AppException(ErrorCode.CONFLICT, "Đã có chính sách khác đang áp dụng trong khoảng này");
            // Re-enable as a new version, preserving the original pause history.
            return saved(CommissionPolicy.builder().storeId(p.getStoreId()).commissionRate(p.getCommissionRate())
                    .effectiveFrom(start).effectiveTo(end).isActive(true).supersedesPolicyId(id)
                    .changedBy(currentUser.getCurrentUserId()).build(), "REACTIVATE");
        }
        if (!Boolean.TRUE.equals(p.getIsActive())) return view(p);
        Instant now = Instant.now();
        if (p.getEffectiveFrom().isBefore(now) && (p.getEffectiveTo() == null || p.getEffectiveTo().isAfter(now))) p.setEffectiveTo(now);
        p.setIsActive(false);
        p.setChangedBy(currentUser.getCurrentUserId());
        return saved(p, "PAUSE");
    }
}
