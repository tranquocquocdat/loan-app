package com.loansystem.controller;

import com.loansystem.service.LoanWorkflowService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final LoanWorkflowService service;

    public AdminController(LoanWorkflowService service) { this.service = service; }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("apps", service.listAll());
        return "admin/list";
    }

    @PostMapping("/{id}/review")
    public String review(@PathVariable Long id) {
        service.moveToReview(id);
        return "redirect:/admin";
    }

    @PostMapping("/{id}/assess")
    public String assess(@PathVariable Long id, @RequestParam boolean approve, @RequestParam(required = false) String note) {
        service.assess(id, approve, note);
        return "redirect:/admin";
    }

    @PostMapping("/{id}/disburse")
    public String disburse(@PathVariable Long id) {
        service.disburse(id);
        return "redirect:/admin";
    }
}
