package com.loansystem.controller;

import com.loansystem.entity.LoanStatus;
import com.loansystem.service.LoanWorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final LoanWorkflowService service;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("apps", service.listAll());
        model.addAttribute("LoanStatus", LoanStatus.class);
        return "admin/list";
    }

    @PostMapping("/{id}/review")
    public String review(@PathVariable Long id) {
        service.moveToReview(id);
        return "redirect:/admin";
    }

    @PostMapping("/{id}/assessment")
    public String assessment(@PathVariable Long id, @RequestParam(required = false) String note) {
        service.completeAssessment(id, note);
        return "redirect:/admin";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, @RequestParam(required = false) String note) {
        service.approve(id, note);
        return "redirect:/admin";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id, @RequestParam String reason) {
        service.reject(id, reason);
        return "redirect:/admin";
    }

    @PostMapping("/{id}/disburse")
    public String disburse(@PathVariable Long id) {
        service.disburse(id);
        return "redirect:/admin";
    }
}
