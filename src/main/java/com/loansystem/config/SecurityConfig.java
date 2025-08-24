package com.loansystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    // Chain cho khu vực admin + login/logout
    @Bean
    @Order(1)
    public SecurityFilterChain adminChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/admin/**", "/login", "/logout")
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/login", "/logout").permitAll()

                        // Dashboard - chỉ admin có thể xem tất cả
                        .requestMatchers("/admin", "/admin/dashboard").hasRole("ADMIN")

                        // Phòng tiếp nhận
                        .requestMatchers("/admin/intake/**").hasAnyRole("ADMIN", "INTAKE")

                        // Phòng thẩm định
                        .requestMatchers("/admin/assessment/**").hasAnyRole("ADMIN", "ASSESSMENT")

                        // Phòng giải ngân
                        .requestMatchers("/admin/disbursement/**").hasAnyRole("ADMIN", "DISBURSEMENT")

                        // Actions chỉ admin hoặc role tương ứng
                        .requestMatchers("/admin/*/intake/**").hasAnyRole("ADMIN", "INTAKE")
                        .requestMatchers("/admin/*/assessment/**").hasAnyRole("ADMIN", "ASSESSMENT")
                        .requestMatchers("/admin/*/disburse").hasAnyRole("ADMIN", "DISBURSEMENT")

                        // Contract và utility - tất cả officer có thể xem
                        .requestMatchers("/admin/*/contract").hasAnyRole("ADMIN", "INTAKE", "ASSESSMENT", "DISBURSEMENT")

                        .anyRequest().hasAnyRole("ADMIN", "INTAKE", "ASSESSMENT", "DISBURSEMENT"))
                .formLogin(fl -> fl
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/admin/home", true)
                        .permitAll())
                .logout(lo -> lo
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll())
                .csrf(c -> c.disable());
        return http.build();
    }

    // Chain cho customer routes - BỎ "/" để tránh xung đột
    @Bean
    @Order(2)
    public SecurityFilterChain customerChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/apply", "/my-applications", "/status/**", "/customer/**")
                .authorizeHttpRequests(a -> a
                        // Apply cần đăng nhập
                        .requestMatchers("/apply").hasAnyRole("CUSTOMER", "ADMIN")

                        // Xem hồ sơ cá nhân
                        .requestMatchers("/my-applications", "/customer/**").hasRole("CUSTOMER")

                        // Xem status cụ thể - cần đăng nhập
                        .requestMatchers("/status/**").hasAnyRole("CUSTOMER", "ADMIN", "INTAKE", "ASSESSMENT", "DISBURSEMENT")

                        .anyRequest().authenticated())
                .formLogin(fl -> fl
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/customer/home", true)
                        .permitAll())
                .logout(lo -> lo
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll())
                .csrf(c -> c.disable());
        return http.build();
    }

    // Chain đặc biệt cho route "/" - redirect dựa trên role
    @Bean
    @Order(3)
    public SecurityFilterChain homeChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/")
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/").hasAnyRole("CUSTOMER", "ADMIN")
                        .anyRequest().authenticated())
                .formLogin(fl -> fl
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/", true)
                        .permitAll())
                .logout(lo -> lo
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll())
                .csrf(c -> c.disable());
        return http.build();
    }

    // Chain cho static resources và public routes
    @Bean
    @Order(4)
    public SecurityFilterChain publicChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/css/**", "/js/**", "/images/**", "/favicon.ico",
                        "/public-apply", "/public-status/**")
                .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .csrf(c -> c.disable());
        return http.build();
    }

    @Bean
    public UserDetailsService users(PasswordEncoder pw) {
        return new InMemoryUserDetailsManager(
                // Admin - toàn quyền
                User.withUsername("admin").password(pw.encode("admin123")).roles("ADMIN").build(),

                // Phòng tiếp nhận
                User.withUsername("intake").password(pw.encode("intake123")).roles("INTAKE").build(),

                // Phòng thẩm định
                User.withUsername("assessment").password(pw.encode("assessment123")).roles("ASSESSMENT").build(),

                // Phòng giải ngân
                User.withUsername("disbursement").password(pw.encode("disbursement123")).roles("DISBURSEMENT").build(),

                // Khách hàng demo
                User.withUsername("customer1").password(pw.encode("customer123")).roles("CUSTOMER").build(),
                User.withUsername("customer2").password(pw.encode("customer123")).roles("CUSTOMER").build(),
                User.withUsername("john").password(pw.encode("john123")).roles("CUSTOMER").build(),
                User.withUsername("jane").password(pw.encode("jane123")).roles("CUSTOMER").build()
        );
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}