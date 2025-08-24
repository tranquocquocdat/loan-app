package com.loansystem.controller;

import com.loansystem.entity.LoanStatus;
import com.loansystem.service.LoanWorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final LoanWorkflowService service;

    @GetMapping
    public String home() { return "redirect:/admin/intake"; }

    /* ===== Tab 1: PHÒNG TIẾP NHẬN (SUBMITTED) ===== */
    @GetMapping("/intake")
    public String intake(Model model,
                         @ModelAttribute("error") String error,
                         @ModelAttribute("success") String success) {
        model.addAttribute("apps", service.listByStatuses(LoanStatus.SUBMITTED));
        model.addAttribute("section", "intake");
        return "admin/list";
    }

    @GetMapping("/intake/app/{id}")
    public String intakeDetail(@PathVariable Long id,
                               @ModelAttribute("error") String error,
                               @ModelAttribute("success") String success,
                               Model model) {
        var app = service.get(id).orElseThrow();
        model.addAttribute("app", app);
        model.addAttribute("section", "intake");
        model.addAttribute("canAccept", service.isComplete(app));
        return "admin/detail";
    }

    /* ===== Tab 2: PHÒNG THẨM ĐỊNH (UNDER_REVIEW, ASSESSED) ===== */
    @GetMapping("/assessment")
    public String assessment(Model model,
                             @ModelAttribute("error") String error,
                             @ModelAttribute("success") String success) {
        model.addAttribute("apps",
                service.listByStatuses(LoanStatus.UNDER_REVIEW, LoanStatus.ASSESSED));
        model.addAttribute("section", "assessment");
        return "admin/list";
    }

    @GetMapping("/assessment/app/{id}")
    public String assessmentDetail(@PathVariable Long id,
                                   @ModelAttribute("error") String error,
                                   @ModelAttribute("success") String success,
                                   Model model) {
        model.addAttribute("app", service.get(id).orElseThrow());
        model.addAttribute("section", "assessment");
        return "admin/detail";
    }

    /* ===== Tab 3: PHÒNG GIẢI NGÂN (APPROVED, DISBURSED) ===== */
    @GetMapping("/disbursement")
    public String disbursement(Model model,
                               @ModelAttribute("error") String error,
                               @ModelAttribute("success") String success) {
        model.addAttribute("apps",
                service.listByStatuses(LoanStatus.APPROVED, LoanStatus.DISBURSED));
        model.addAttribute("section", "disbursement");
        return "admin/list";
    }

    @GetMapping("/disbursement/app/{id}")
    public String disbursementDetail(@PathVariable Long id,
                                     @ModelAttribute("error") String error,
                                     @ModelAttribute("success") String success,
                                     Model model) {
        model.addAttribute("app", service.get(id).orElseThrow());
        model.addAttribute("section", "disbursement");
        return "admin/detail";
    }

    /* ====== Nút “Chuyển đến phòng kế bên” (điều hướng tự động theo trạng thái) ====== */
    @PostMapping("/{id}/next")
    public String moveNext(@PathVariable Long id,
                           @RequestParam(value = "redirectTo", required = false) String redirectTo,
                           RedirectAttributes ra) {
        var before = service.get(id).orElseThrow().getStatus();
        try {
            var after = service.moveNext(id).getStatus();
            String msg = switch (before) {
                case SUBMITTED -> "Đã tiếp nhận hồ sơ #" + id;
                case UNDER_REVIEW -> "Đã hoàn tất thẩm định hồ sơ #" + id;
                case ASSESSED -> "Đã phê duyệt hồ sơ #" + id;
                case APPROVED -> "Đã giải ngân hồ sơ #" + id;
                default -> "Đã cập nhật hồ sơ #" + id;
            };
            ra.addFlashAttribute("success", msg);
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:" + (StringUtils.hasText(redirectTo) ? redirectTo : "/admin");
    }

    /* ====== Nút “Từ chối (lý do)” ở mọi tab ====== */
    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id,
                         @RequestParam("reason") String reason,
                         @RequestParam(value = "redirectTo", required = false) String redirectTo,
                         RedirectAttributes ra) {
        try {
            service.reject(id, reason);
            ra.addFlashAttribute("error", "Đã từ chối hồ sơ #" + id + ": " + reason);
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:" + (StringUtils.hasText(redirectTo) ? redirectTo : "/admin");
    }

    /* ===== Hợp đồng (giữ nguyên) ===== */
    @GetMapping("/{id}/contract")
    public String contract(@PathVariable("id") Long id, Model model) {
        var app = service.get(id).orElseThrow();
        model.addAttribute("app", app);
        return "admin/contract";
    }
}
