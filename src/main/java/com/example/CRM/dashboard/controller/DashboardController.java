package com.example.CRM.dashboard.controller;

import com.example.CRM.dashboard.model.DashboardPeriod;
import com.example.CRM.dashboard.service.DashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class DashboardController {
    private final DashboardService dashboard;

    public DashboardController(DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping({"/", "/dashboard"})
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SALES')")
    public String showDashboard(@RequestParam(required = false) String period,
                                Authentication authentication, Model model) {
        DashboardPeriod selectedPeriod = DashboardPeriod.fromParam(period);
        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("activeModule", "dashboard");
        model.addAttribute("periods", DashboardPeriod.values());
        model.addAttribute("dashboard", dashboard.getDashboard(authentication, selectedPeriod));
        return "dashboard/index";
    }
}
