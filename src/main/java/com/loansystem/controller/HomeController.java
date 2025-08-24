package com.loansystem.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Collection;

@Controller
public class HomeController {

    @RequestMapping("/admin/home")
    public String adminHome(Authentication authentication) {
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        // Admin có thể vào dashboard
        if (authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/admin/dashboard";
        }

        // Intake user chỉ vào phòng tiếp nhận
        if (authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_INTAKE"))) {
            return "redirect:/admin/intake";
        }

        // Assessment user chỉ vào phòng thẩm định
        if (authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ASSESSMENT"))) {
            return "redirect:/admin/assessment";
        }

        // Disbursement user chỉ vào phòng giải ngân
        if (authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_DISBURSEMENT"))) {
            return "redirect:/admin/disbursement";
        }

        // Default fallback
        return "redirect:/login";
    }

    @RequestMapping("/customer/home")
    public String customerHome(Authentication authentication) {
        // Customer về trang dashboard cá nhân
        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"))) {
            return "redirect:/my-applications";
        }

        // Admin cũng có thể truy cập customer home
        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/admin/dashboard";
        }

        return "redirect:/login";
    }
}