package com.yetiantian.campus_erp;
import com.yetiantian.campus_erp.domain.Application;
import com.yetiantian.campus_erp.domain.Member;
import com.yetiantian.campus_erp.domain.Project;
import com.yetiantian.campus_erp.domain.State;
import com.yetiantian.campus_erp.repository.ApplicationRepository;
import com.yetiantian.campus_erp.repository.MemberRepository;
import com.yetiantian.campus_erp.repository.ProjectRepository;
import com.yetiantian.campus_erp.service.ApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
public class ApplicationServiceTest {

    @Autowired
    private ApplicationService service;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Test
    void 正常审批流程() {

        Project p = projectRepository.save(new Project("测试项目", new BigDecimal("10000")));

        Member m = memberRepository.save(new Member("测试成员", "成员"));

        Application app = service.submit(p.getId(), m.getId(), "买传感器", new BigDecimal("600"));

        assertThat(app.getState()).isEqualTo(State.PENDING_L1);

        service.approve(app.getId(), new BigDecimal("600"));

        Application updated = applicationRepository.findById(app.getId()).orElseThrow();
        assertThat(updated.getState()).isEqualTo(State.APPROVED);
        assertThat(updated.getApprovedAmount()).isEqualByComparingTo("600");
    }

    @Test
    void 驳回后冻结自动释放() {
        Project p = projectRepository.save(new Project("测试项目", new BigDecimal("10000")));
        Member m = memberRepository.save(new Member("测试成员", "成员"));
        Application app = service.submit(p.getId(), m.getId(), "买鼠标", new BigDecimal("100"));

        assertThat(service.sumFrozen(p.getId())).isEqualByComparingTo("100");

        service.reject(app.getId(), "预算不够了");

        assertThat(service.sumFrozen(p.getId())).isEqualByComparingTo("0");   // ← 自动归零
    }

    @Test
    void 结算流程() {
        Project p = projectRepository.save(new Project("测试项目", new BigDecimal("10000")));
        Member m = memberRepository.save(new Member("测试成员", "成员"));
        Application app = service.submit(p.getId(), m.getId(), "买设备", new BigDecimal("600"));

        // 批 500，结算 460
        service.approve(app.getId(), new BigDecimal("500"));
        service.settle(app.getId(), new BigDecimal("460"));

        Application u = applicationRepository.findById(app.getId()).orElseThrow();
        assertThat(u.getState()).isEqualTo(State.SETTLED);
        assertThat(u.getActualAmount()).isEqualByComparingTo("460");
    }

    @Test
    void 结算超过批准金额要报错() {
        Project p = projectRepository.save(new Project("测试项目", new BigDecimal("10000")));
        Member m = memberRepository.save(new Member("测试成员", "成员"));
        Application app = service.submit(p.getId(), m.getId(), "买设备", new BigDecimal("600"));
        service.approve(app.getId(), new BigDecimal("500"));

        assertThatThrownBy(() -> service.settle(app.getId(), new BigDecimal("600")))
                .hasMessageContaining("结算金额不合法");

    }

    @Test
    void 结算允许零元() {
        Project p = projectRepository.save(new Project("零元项目", new BigDecimal("1000")));
        Member m = memberRepository.save(new Member("测试成员", "成员"));
        Application app = service.submit(p.getId(), m.getId(), "测试零元", new BigDecimal("100"));

        service.approve(app.getId(), new BigDecimal("100"));
        service.settle(app.getId(), BigDecimal.ZERO);

        Application u = applicationRepository.findById(app.getId()).orElseThrow();
        assertThat(u.getState()).isEqualTo(State.SETTLED);
        assertThat(u.getActualAmount()).isEqualByComparingTo("0");
    }

}