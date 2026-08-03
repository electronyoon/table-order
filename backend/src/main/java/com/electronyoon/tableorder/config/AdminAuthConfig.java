package com.electronyoon.tableorder.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdminAuthConfig {

    @Bean
    public FilterRegistrationBean<AdminAuthFilter> adminAuthFilter(
            @Value("${app.admin-token}") String adminToken
    ) {
        FilterRegistrationBean<AdminAuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new AdminAuthFilter(adminToken));
        registration.addUrlPatterns("/admin/*");
        return registration;
    }
}
