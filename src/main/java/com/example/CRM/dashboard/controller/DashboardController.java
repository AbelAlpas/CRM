package com.example.CRM.dashboard.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping({"/", "/dashboard"})
    public String showDashboard(Model model) {
        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("activeModule", "dashboard");
        return "dashboard/index";
    }
}
