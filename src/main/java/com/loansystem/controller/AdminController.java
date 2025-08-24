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

    // Mặc định điều hướng sang "Chờ xử lý"
    @GetMapping
    public String home() { return "redirect:/admin/pending"; }

    // TAB 1: Chờ xử lý = SUBMITTED, UNDER_REVIEW, ASSESSED
    @GetMapping("/pending")
    public String pending(Model model) {
        model.addAttribute("apps",
                service.listByStatuses(LoanStatus.SUBMITTED, LoanStatus.UNDER_REVIEW, LoanStatus.ASSESSED));
        model.addAttribute("LoanStatus", LoanStatus.class);
        model.addAttribute("section", "pending");
        return "admin/list";
    }

    // TAB 2: Đã duyệt = APPROVED, DISBURSED
    @GetMapping("/approved")
    public String approved(Model model) {
        model.addAttribute("apps",
                service.listByStatuses(LoanStatus.APPROVED, LoanStatus.DISBURSED));
        model.addAttribute("LoanStatus", LoanStatus.class);
        model.addAttribute("section", "approved");
        return "admin/list";
    }

    // TAB 3: Bị từ chối = REJECTED
    @GetMapping("/rejected")
    public String rejected(Model model) {
        model.addAttribute("apps", service.listByStatuses(LoanStatus.REJECTED));
        model.addAttribute("LoanStatus", LoanStatus.class);
        model.addAttribute("section", "rejected");
        return "admin/list";
    }

    // TAB 4: Lưu trữ = tất cả trạng thái kết thúc (đọc-only)
    @GetMapping("/archived")
    public String archived(Model model) {
        model.addAttribute("apps",
                service.listByStatuses(LoanStatus.APPROVED, LoanStatus.DISBURSED, LoanStatus.REJECTED));
        model.addAttribute("LoanStatus", LoanStatus.class);
        model.addAttribute("section", "archived");
        return "admin/list";
    }

    // ==== Actions ====
    @PostMapping("/{id}/review")
    public String review(@PathVariable("id") Long id) {
        service.moveToReview(id);
        return "redirect:/admin/pending";
    }

    @PostMapping("/{id}/assessment")
    public String assessment(@PathVariable("id") Long id,
                             @RequestParam(value = "note", required = false) String note) {
        service.completeAssessment(id, note);
        return "redirect:/admin/pending";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable("id") Long id,
                          @RequestParam(value = "note", required = false) String note) {
        service.approve(id, note);
        return "redirect:/admin/approved";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable("id") Long id,
                         @RequestParam("reason") String reason) {
        service.reject(id, reason);
        return "redirect:/admin/rejected";
    }

    @PostMapping("/{id}/disburse")
    public String disburse(@PathVariable("id") Long id) {
        service.disburse(id);
        return "redirect:/admin/approved";
    }

    @GetMapping("/{id}/contract")
    public String contract(@PathVariable("id") Long id, Model model) {
        var app = service.get(id).orElseThrow();
        model.addAttribute("app", app);
        return "admin/contract";
    }
}
