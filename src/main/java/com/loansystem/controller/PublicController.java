package com.loansystem.controller;

import com.loansystem.entity.LoanApplication;
import com.loansystem.service.LoanWorkflowService;
import java.math.BigDecimal;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class PublicController {

    private final LoanWorkflowService service;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    // GET /apply -> hiển thị lại form
    @GetMapping("/apply")
    public String applyGet() { return "index"; }

    @PostMapping("/apply")
    public String apply(
            @RequestParam("fullName") String fullName,
            @RequestParam("email") String email,
            @RequestParam("phone") String phone,
            @RequestParam("amount") BigDecimal amount,
            @RequestParam("termMonths") int termMonths,
            @RequestParam(value = "purpose", required = false) String purpose,
            @RequestParam("monthlyIncome") BigDecimal monthlyIncome,
            Model model) {

        LoanApplication app =
                service.submitApplication(
                        fullName, email, phone, amount, termMonths, purpose, monthlyIncome);
        model.addAttribute("app", app);
        return "submit_success";
    }

    @GetMapping("/status/{id}")
    public String status(@PathVariable("id") Long id, Model model) {
        Optional<LoanApplication> app = service.get(id);
        model.addAttribute("app", app.orElse(null));
        return "status";
    }
}
