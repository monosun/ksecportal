package com.monosun.secportal.policy.service;

import com.monosun.secportal.audit.service.AuditLogService;
import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.auth.repository.UserRepository;
import com.monosun.secportal.common.exception.BusinessException;
import com.monosun.secportal.common.exception.ResourceNotFoundException;
import com.monosun.secportal.policy.dto.PolicyDto;
import com.monosun.secportal.policy.entity.Policy;
import com.monosun.secportal.policy.entity.PolicyAcknowledgment;
import com.monosun.secportal.policy.repository.PolicyAcknowledgmentRepository;
import com.monosun.secportal.policy.repository.PolicyArticleRepository;
import com.monosun.secportal.policy.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PolicyService {

    private final PolicyRepository policyRepository;
    private final PolicyArticleRepository articleRepository;
    private final PolicyAcknowledgmentRepository ackRepository;
    private final UserRepository userRepository;
    private final PolicyArticleService articleService;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public Page<PolicyDto.Summary> list(Policy.Status status, Policy.Category category, String keyword, Pageable pageable) {
        Page<Policy> page = policyRepository.search(status, category, keyword, pageable);
        Map<Long, Long> articleCounts = countArticles(page.getContent());
        return page.map(p -> PolicyDto.Summary.from(p, articleCounts.getOrDefault(p.getId(), 0L)));
    }

    /** 현재 페이지 정책들의 조 개수를 한 번의 집계 쿼리로 가져온다. */
    private Map<Long, Long> countArticles(List<Policy> policies) {
        if (policies.isEmpty()) return Map.of();
        List<Long> ids = policies.stream().map(Policy::getId).toList();
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : articleRepository.countByPolicyIds(ids)) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }

    @Transactional(readOnly = true)
    public PolicyDto.Response get(Long id, Long userId) {
        Policy policy = findById(id);
        long ackCount = ackRepository.countByPolicyId(id);
        boolean acknowledged = ackRepository.existsByPolicyIdAndUserId(id, userId);
        return PolicyDto.Response.from(policy, ackCount, acknowledged);
    }

    @Transactional
    public PolicyDto.Response create(PolicyDto.CreateRequest request, Long authorId) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new BusinessException("User not found"));
        Policy policy = Policy.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .category(request.getCategory())
                .version(request.getVersion() != null ? request.getVersion() : "1.0")
                .effectiveDate(request.getEffectiveDate())
                .author(author)
                .build();
        Policy saved = policyRepository.save(policy);
        articleService.sync(saved);   // 제목 → 지침/장, 본문 → 조 세분화
        auditLogService.log("POLICY_CREATED", "POLICY", saved.getId(), saved.getTitle());
        return PolicyDto.Response.from(saved, 0, false);
    }

    @Transactional
    public PolicyDto.Response update(Long id, PolicyDto.UpdateRequest request) {
        Policy policy = findById(id);
        if (request.getTitle() != null) policy.setTitle(request.getTitle());
        if (request.getContent() != null) policy.setContent(request.getContent());
        if (request.getCategory() != null) policy.setCategory(request.getCategory());
        if (request.getStatus() != null) policy.setStatus(request.getStatus());
        if (request.getVersion() != null) policy.setVersion(request.getVersion());
        if (request.getEffectiveDate() != null) policy.setEffectiveDate(request.getEffectiveDate());
        // 제목·본문이 바뀌면 지침/장/조 구조가 달라지므로 다시 세분화한다.
        if (request.getTitle() != null || request.getContent() != null) {
            articleService.sync(policy);
        }
        auditLogService.log("POLICY_UPDATED", "POLICY", id, policy.getTitle());
        long ackCount = ackRepository.countByPolicyId(id);
        return PolicyDto.Response.from(policy, ackCount, false);
    }

    @Transactional
    public void delete(Long id) {
        Policy policy = findById(id);
        auditLogService.log("POLICY_DELETED", "POLICY", id, policy.getTitle());
        policyRepository.delete(policy);
    }

    /** 목록에서 선택한 정책 일괄 삭제 — 존재하지 않는 ID 는 건너뛰고 실제 삭제 건수를 돌려준다. */
    @Transactional
    public int deleteAll(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return 0;
        List<Policy> policies = policyRepository.findAllById(ids);
        for (Policy policy : policies) {
            auditLogService.log("POLICY_DELETED", "POLICY", policy.getId(), policy.getTitle());
        }
        policyRepository.deleteAll(policies);
        return policies.size();
    }

    @Transactional
    public void acknowledge(Long policyId, Long userId) {
        if (ackRepository.existsByPolicyIdAndUserId(policyId, userId)) {
            throw new BusinessException("Already acknowledged");
        }
        Policy policy = findById(policyId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));
        ackRepository.save(PolicyAcknowledgment.builder()
                .policy(policy)
                .user(user)
                .build());
        auditLogService.log("POLICY_ACKNOWLEDGED", "POLICY", policyId, policy.getTitle());
    }

    private Policy findById(Long id) {
        return policyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Policy", id));
    }
}
