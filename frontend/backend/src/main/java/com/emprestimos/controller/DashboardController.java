package com.emprestimos.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.emprestimos.dto.response.DashboardResponse;
import com.emprestimos.security.SecurityUtils;
import com.emprestimos.service.DashboardService;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/resumo")
    public DashboardResponse resumo() {
        return dashboardService.resumo(SecurityUtils.usuarioIdAtual());
    }
}