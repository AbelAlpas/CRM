package com.example.CRM.dashboard.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DashboardConfiguration {
    @Bean
    Clock dashboardClock() {
        return Clock.systemDefaultZone();
    }
}
