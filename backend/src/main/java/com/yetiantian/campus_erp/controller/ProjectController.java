package com.yetiantian.campus_erp.controller;

import com.yetiantian.campus_erp.dto.ProjectSummary;
import com.yetiantian.campus_erp.service.ApplicationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ApplicationService service;

    public ProjectController(ApplicationService service) {
        this.service = service;
    }

    @GetMapping("/{id}/summary")
    public ProjectSummary summary(@PathVariable Long id) {
        return service.summary(id);
    }
}