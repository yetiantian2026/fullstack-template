package com.yetiantian.campus_erp.service;

import com.yetiantian.campus_erp.domain.Application;
import com.yetiantian.campus_erp.domain.Member;
import com.yetiantian.campus_erp.domain.Project;
import com.yetiantian.campus_erp.domain.State;
import com.yetiantian.campus_erp.dto.ProjectSummary;
import com.yetiantian.campus_erp.repository.ApplicationRepository;
import com.yetiantian.campus_erp.repository.MemberRepository;
import com.yetiantian.campus_erp.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ApplicationService {

    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private ApplicationRepository applicationRepository;

    @Transactional
    public Application submit(Long projectId, Long applicantId, String purpose, BigDecimal amount) {

        Project project = projectRepository.findById(projectId).orElseThrow(() -> new RuntimeException("项目不存在"));
        Member applicant = memberRepository.findById(applicantId).orElseThrow(() -> new RuntimeException("申请人不存在"));

        if (purpose == null || purpose.trim().isEmpty()) {
            throw new RuntimeException("用途不能为空");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("金额必须大于0");
        }
        if (amount.scale() > 2) {
            throw new RuntimeException("金额最多两位小数");
        }

        BigDecimal frozenAmount = sumFrozen(projectId);
        BigDecimal usedAmount = sumUsed(projectId);

        BigDecimal available = project.getTotalAmount()
                .subtract(frozenAmount)
                .subtract(usedAmount);

        if (available.compareTo(amount) < 0) {
            throw new RuntimeException("经费不足");
        }

        Application application = new Application();
        application.setProject(project);
        application.setApplicant(applicant);
        application.setPurpose(purpose);
        application.setApplyAmount(amount);
        application.setState(State.PENDING_L1);
        application.setCreatedAt(LocalDateTime.now());

        return applicationRepository.save(application);
    }

    private Application findApplication(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("申请不存在"));
    }

    @Transactional
    public void approve(Long id, BigDecimal amount) {
        Application app = findApplication(id);

        if (app.getState() != State.PENDING_L1) {
            throw new RuntimeException("当前状态不允许审批");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0 || amount.compareTo(app.getApplyAmount()) > 0) {
            throw new RuntimeException("批准金额必须大于0且不能超过申请金额");
        }

        app.setApprovedAmount(amount);
        app.setState(State.APPROVED);
        applicationRepository.save(app);
    }

    @Transactional
    public void settle(Long id, BigDecimal actual) {
        Application app = findApplication(id);

        if (app.getState() != State.APPROVED) {
            throw new RuntimeException("没审批通过不能结算");
        }

        if (actual.compareTo(BigDecimal.ZERO) < 0 || actual.compareTo(app.getApprovedAmount()) > 0) {
            throw new RuntimeException("结算金额不合法");
        }

        app.setActualAmount(actual);
        app.setState(State.SETTLED);
        applicationRepository.save(app);
    }

    @Transactional
    public void reject(Long id, String reason) {
        Application app = findApplication(id);

        if (app.getState() != State.PENDING_L1) {
            throw new RuntimeException("当前状态不允许驳回");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new RuntimeException("驳回原因不能为空");
        }

        app.setReason(reason);
        app.setState(State.REJECTED);
        applicationRepository.save(app);
    }

    // 4. 取消
    @Transactional
    public void cancel(Long id, String reason) {
        Application app = findApplication(id);

        if (app.getState() != State.PENDING_L1 && app.getState() != State.APPROVED) {
            throw new RuntimeException("当前状态不允许取消");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new RuntimeException("取消原因不能为空");
        }

        app.setReason(reason);
        app.setState(State.CANCELED);
        applicationRepository.save(app);
    }

    public Application get(Long id) {
        return findApplication(id);
    }

    public List<Application> listByProject(Long projectId) {
        return applicationRepository.findByProjectId(projectId);
    }

    public BigDecimal sumFrozen(Long projectId) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Application a : applicationRepository.findByProjectId(projectId)) {
            if (a.getState() == State.PENDING_L1) {
                sum = sum.add(a.getApplyAmount());
            } else if (a.getState() == State.APPROVED) {
                sum = sum.add(a.getApprovedAmount());
            }
        }
        return sum;
    }

    public BigDecimal sumUsed(Long projectId) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Application a : applicationRepository.findByProjectId(projectId)) {
            if (a.getState() == State.SETTLED) {
                sum = sum.add(a.getActualAmount());
            }
        }
        return sum;
    }

    public ProjectSummary summary(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("项目不存在"));

        BigDecimal frozen = sumFrozen(projectId);
        BigDecimal used = sumUsed(projectId);
        BigDecimal available = project.getTotalAmount().subtract(frozen).subtract(used);

        return new ProjectSummary(project.getId(), project.getName(),
                project.getTotalAmount(), frozen, used, available);
    }
}