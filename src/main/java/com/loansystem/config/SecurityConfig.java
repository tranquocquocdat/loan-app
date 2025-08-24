package com.loansystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    // Chain cho khu vực admin + trang login/logout
    @Bean
    @Order(1)
    public SecurityFilterChain adminChain(HttpSecurity http) throws Exception {
        http
                // CHÚ Ý: thêm /login và /logout vào matcher của chain này
                .securityMatcher("/admin/**", "/login", "/logout")
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/login", "/logout").permitAll()
                        .anyRequest().hasRole("OFFICER"))
                .formLogin(fl -> fl
                        .loginPage("/login")                 // GET /login (render view)
                        .loginProcessingUrl("/login")        // POST /login (xử lý auth)
                        .defaultSuccessUrl("/admin", true)
                        .permitAll())
                .logout(lo -> lo
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll())
                .csrf(c -> c.disable());
        return http.build();
    }

    // Chain mặc định cho public routes (/, /apply, /status/**, /css/**, ...)
    @Bean
    @Order(2)
    public SecurityFilterChain appChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .csrf(c -> c.disable());
        return http.build();
    }

    @Bean
    public UserDetailsService users(PasswordEncoder pw) {
        return new InMemoryUserDetailsManager(
                User.withUsername("officer").password(pw.encode("officer")).roles("OFFICER").build(),
                User.withUsername("admin").password(pw.encode("admin")).roles("OFFICER").build()
        );
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
