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

    /* ===== TAB 1: PHÒNG TIẾP NHẬN ===== */
    @GetMapping("/intake")
    public String intake(Model model,
                         @ModelAttribute("error") String error,
                         @ModelAttribute("success") String success) {
        model.addAttribute("apps", service.listByStatuses(LoanStatus.SUBMITTED));
        model.addAttribute("section", "intake");
        return "admin/list";
    }

    /** Chi tiết hồ sơ - phòng tiếp nhận */
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

    @PostMapping("/{id}/intake/accept")
    public String intakeAccept(@PathVariable("id") Long id,
                               @RequestParam(value = "redirectTo", required = false) String redirectTo,
                               RedirectAttributes ra) {
        try {
            service.intakeAccept(id);
            ra.addFlashAttribute("success", "Đã tiếp nhận hồ sơ #" + id);
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:" + (StringUtils.hasText(redirectTo) ? redirectTo : "/admin/intake");
    }

    /* ===== TAB 2: PHÒNG THẨM ĐỊNH TÍN DỤNG ===== */
    @GetMapping("/assessment")
    public String assessment(Model model,
                             @ModelAttribute("error") String error,
                             @ModelAttribute("success") String success) {
        model.addAttribute("apps",
                service.listByStatuses(LoanStatus.UNDER_REVIEW, LoanStatus.ASSESSED));
        model.addAttribute("section", "assessment");
        return "admin/list";
    }

    /** Chi tiết hồ sơ - phòng thẩm định */
    @GetMapping("/assessment/app/{id}")
    public String assessmentDetail(@PathVariable Long id,
                                   @ModelAttribute("error") String error,
                                   @ModelAttribute("success") String success,
                                   Model model) {
        model.addAttribute("app", service.get(id).orElseThrow());
        model.addAttribute("section", "assessment");
        return "admin/detail";
    }

    @PostMapping("/{id}/assessment/check-blacklist")
    public String checkBlacklist(@PathVariable("id") Long id,
                                 @RequestParam(value = "redirectTo", required = false) String redirectTo,
                                 RedirectAttributes ra) {
        var before = service.get(id).orElseThrow().getStatus();
        var after = service.checkBlacklistOrReject(id).getStatus();
        if (after == LoanStatus.REJECTED) {
            ra.addFlashAttribute("error", "Hồ sơ #" + id + " bị từ chối do nằm trong blacklist.");
        } else if (before == LoanStatus.UNDER_REVIEW) {
            ra.addFlashAttribute("success", "Hồ sơ #" + id + " không nằm trong blacklist.");
        }
        return "redirect:" + (StringUtils.hasText(redirectTo) ? redirectTo : "/admin/assessment");
    }

    @PostMapping("/{id}/assessment/complete")
    public String assessmentComplete(@PathVariable("id") Long id,
                                     @RequestParam(value = "note", required = false) String note,
                                     @RequestParam(value = "redirectTo", required = false) String redirectTo,
                                     RedirectAttributes ra) {
        service.completeAssessment(id, note);
        ra.addFlashAttribute("success", "Đã hoàn tất thẩm định hồ sơ #" + id);
        return "redirect:" + (StringUtils.hasText(redirectTo) ? redirectTo : "/admin/assessment");
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable("id") Long id,
                          @RequestParam(value = "note", required = false) String note,
                          @RequestParam(value = "redirectTo", required = false) String redirectTo,
                          RedirectAttributes ra) {
        service.approve(id, note);
        ra.addFlashAttribute("success", "Đã phê duyệt hồ sơ #" + id);
        // sau approve, thường chuyển qua phòng giải ngân
        return "redirect:" + (StringUtils.hasText(redirectTo) ? redirectTo : "/admin/disbursement/app/" + id);
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable("id") Long id,
                         @RequestParam("reason") String reason,
                         @RequestParam(value = "redirectTo", required = false) String redirectTo,
                         RedirectAttributes ra) {
        service.reject(id, reason);
        ra.addFlashAttribute("error", "Đã từ chối hồ sơ #" + id + ": " + reason);
        return "redirect:" + (StringUtils.hasText(redirectTo) ? redirectTo : "/admin/assessment/app/" + id);
    }

    /* ===== TAB 3: PHÒNG GIẢI NGÂN ===== */
    @GetMapping("/disbursement")
    public String disbursement(Model model,
                               @ModelAttribute("error") String error,
                               @ModelAttribute("success") String success) {
        model.addAttribute("apps",
                service.listByStatuses(LoanStatus.APPROVED, LoanStatus.DISBURSED));
        model.addAttribute("section", "disbursement");
        return "admin/list";
    }

    /** Chi tiết hồ sơ - phòng giải ngân */
    @GetMapping("/disbursement/app/{id}")
    public String disbursementDetail(@PathVariable Long id,
                                     @ModelAttribute("error") String error,
                                     @ModelAttribute("success") String success,
                                     Model model) {
        model.addAttribute("app", service.get(id).orElseThrow());
        model.addAttribute("section", "disbursement");
        return "admin/detail";
    }

    @PostMapping("/{id}/disburse")
    public String disburse(@PathVariable("id") Long id,
                           @RequestParam(value = "redirectTo", required = false) String redirectTo,
                           RedirectAttributes ra) {
        service.disburse(id);
        ra.addFlashAttribute("success", "Đã giải ngân hồ sơ #" + id);
        return "redirect:" + (StringUtils.hasText(redirectTo) ? redirectTo : "/admin/disbursement/app/" + id);
    }

    /* ===== Hợp đồng ===== */
    @GetMapping("/{id}/contract")
    public String contract(@PathVariable("id") Long id, Model model) {
        var app = service.get(id).orElseThrow();
        model.addAttribute("app", app);
        return "admin/contract";
    }
}
