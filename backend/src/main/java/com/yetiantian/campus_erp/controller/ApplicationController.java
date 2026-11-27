package com.yetiantian.campus_erp.controller;

import com.yetiantian.campus_erp.domain.Application;
import com.yetiantian.campus_erp.service.ApplicationService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService service;

    public ApplicationController(ApplicationService service) {
        this.service = service;
    }

    // 请求体
    public record SubmitRequest(Long projectId, String purpose, BigDecimal amount) { }
    public record AmountRequest(BigDecimal amount) { }
    public record ReasonRequest(String reason) { }

    @PostMapping
    public Application submit(@RequestHeader("X-User-Id") Long userId,
                              @RequestBody SubmitRequest req) {
        return service.submit(req.projectId(), userId, req.purpose(), req.amount());
    }

    @PostMapping("/{id}/approve")
    public Application approve(@PathVariable Long id, @RequestBody AmountRequest req) {
        service.approve(id, req.amount());
        return service.get(id);
    }

    @PostMapping("/{id}/settle")
    public Application settle(@PathVariable Long id, @RequestBody AmountRequest req) {
        service.settle(id, req.amount());
        return service.get(id);
    }

    @PostMapping("/{id}/reject")
    public Application reject(@PathVariable Long id, @RequestBody ReasonRequest req) {
        service.reject(id, req.reason());
        return service.get(id);
    }

    @PostMapping("/{id}/cancel")
    public Application cancel(@PathVariable Long id, @RequestBody ReasonRequest req) {
        service.cancel(id, req.reason());
        return service.get(id);
    }

    @GetMapping("/{id}")
    public Application get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping
    public List<Application> list(@RequestParam Long projectId) {
        return service.listByProject(projectId);
    }
}