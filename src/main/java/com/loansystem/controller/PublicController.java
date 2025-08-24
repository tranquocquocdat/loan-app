package com.loansystem.controller;

import com.loansystem.entity.LoanApplication;
import com.loansystem.service.LoanWorkflowService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Optional;

@Controller
public class PublicController {

    private final LoanWorkflowService service;
    public PublicController(LoanWorkflowService service) { this.service = service; }

    @GetMapping("/")
    public String index() { return "index"; }

    @PostMapping("/apply")
    public String apply(@RequestParam String fullName,
                        @RequestParam String email,
                        @RequestParam String phone,
                        @RequestParam BigDecimal amount,
                        @RequestParam int termMonths,
                        @RequestParam(required = false) String purpose,
                        @RequestParam BigDecimal monthlyIncome,
                        Model model) {
        LoanApplication app = service.submitApplication(fullName, email, phone, amount, termMonths, purpose, monthlyIncome);
        model.addAttribute("app", app);
        return "submit_success";
    }

    @GetMapping("/status/{id}")
    public String status(@PathVariable Long id, Model model) {
        Optional<LoanApplication> app = service.get(id);
        model.addAttribute("app", app.orElse(null));
        return "status";
    }
}
