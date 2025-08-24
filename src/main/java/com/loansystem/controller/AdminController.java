package com.loansystem.controller;

import com.loansystem.entity.LoanStatus;
import com.loansystem.service.LoanWorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final LoanWorkflowService service;

    @GetMapping
    public String home() { return "redirect:/admin/dashboard"; }

    /* ===== DASHBOARD TỔNG QUAN ===== */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("submitted", service.countByStatus(LoanStatus.SUBMITTED));
        model.addAttribute("underReview", service.countByStatus(LoanStatus.UNDER_REVIEW));
        model.addAttribute("assessed", service.countByStatus(LoanStatus.ASSESSED));
        model.addAttribute("approved", service.countByStatus(LoanStatus.APPROVED));
        model.addAttribute("rejected", service.countByStatus(LoanStatus.REJECTED));
        model.addAttribute("disbursed", service.countByStatus(LoanStatus.DISBURSED));
        model.addAttribute("section", "dashboard");
        return "admin/dashboard";
    }

    /* ===== TAB 1: PHÒNG TIẾP NHẬN ===== */
    @GetMapping("/intake")
    public String intake(Model model,
                         @ModelAttribute("error") String error,
                         @ModelAttribute("success") String success) {
        model.addAttribute("apps", service.listByStatuses(LoanStatus.SUBMITTED));
        model.addAttribute("section", "intake");
        addFlashMessages(model, error, success);
        return "admin/list";
    }

    /** Chi tiết hồ sơ - phòng tiếp nhận */
    @GetMapping("/intake/app/{id}")
    public String intakeDetail(@PathVariable Long id,
                               @ModelAttribute("error") String error,
                               @ModelAttribute("success") String success,
                               Model model) {
        var app = service.get(id).orElseThrow(() -> 
            new IllegalArgumentException("Không tìm thấy hồ sơ"));
        model.addAttribute("app", app);
        model.addAttribute("section", "intake");
        model.addAttribute("canAccept", service.isComplete(app));
        model.addAttribute("canModify", service.canModify(app, "intake"));
        addFlashMessages(model, error, success);
        return "admin/detail";
    }

    @PostMapping("/{id}/intake/accept")
    public String intakeAccept(@PathVariable("id") Long id,
                               @RequestParam(value = "redirectTo", required = false) String redirectTo,
                               RedirectAttributes ra) {
        try {
            service.intakeAccept(id);
            ra.addFlashAttribute("success", "Đã tiếp nhận hồ sơ #" + id + " thành công");
            log.info("Application {} accepted by intake", id);
            // Tự động chuyển đến phòng thẩm định sau khi tiếp nhận
            return "redirect:/admin/assessment/app/" + id;
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Lỗi tiếp nhận: " + ex.getMessage());
            log.error("Error accepting application {}: {}", id, ex.getMessage());
            return "redirect:" + (StringUtils.hasText(redirectTo) ? redirectTo : "/admin/intake");
        }
    }

    /* ===== TAB 2: PHÒNG THẨM ĐỊNH TÍN DỤNG ===== */
    @GetMapping("/assessment")
    public String assessment(Model model,
                             @ModelAttribute("error") String error,
                             @ModelAttribute("success") String success) {
        model.addAttribute("apps",
                service.listByStatuses(LoanStatus.UNDER_REVIEW, LoanStatus.ASSESSED));
        model.addAttribute("section", "assessment");
        addFlashMessages(model, error, success);
        return "admin/list";
    }

    /** Chi tiết hồ sơ - phòng thẩm định */
    @GetMapping("/assessment/app/{id}")
    public String assessmentDetail(@PathVariable Long id,
                                   @ModelAttribute("error") String error,
                                   @ModelAttribute("success") String success,
                                   Model model) {
        var app = service.get(id).orElseThrow(() -> 
            new IllegalArgumentException("Không tìm thấy hồ sơ"));
        model.addAttribute("app", app);
        model.addAttribute("section", "assessment");
        model.addAttribute("canModify", service.canModify(app, "assessment"));
        addFlashMessages(model, error, success);
        return "admin/detail";
    }

    @PostMapping("/{id}/assessment/check-blacklist")
    public String checkBlacklist(@PathVariable("id") Long id,
                                 @RequestParam(value = "redirectTo", required = false) String redirectTo,
                                 RedirectAttributes ra) {
        try {
            var beforeStatus = service.get(id).orElseThrow().getStatus();
            var app = service.checkBlacklistOrReject(id);
            
            if (app.getStatus() == LoanStatus.REJECTED) {
                ra.addFlashAttribute("error", "Hồ sơ #" + id + " bị từ chối do nằm trong blacklist: " + app.getRejectionReason());
            } else if (beforeStatus == LoanStatus.UNDER_REVIEW) {
                ra.addFlashAttribute("success", "Hồ sơ #" + id + " đã vượt qua kiểm tra blacklist");
            }
            log.info("Blacklist check completed for application {}", id);
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Lỗi kiểm tra blacklist: " + ex.getMessage());
            log.error("Error checking blacklist for application {}: {}", id, ex.getMessage());
        }
        return "redirect:" + (StringUtils.hasText(redirectTo) ? redirectTo : "/admin/assessment");
    }

    @PostMapping("/{id}/assessment/complete")
    public String assessmentComplete(@PathVariable("id") Long id,
                                     @RequestParam(value = "note", required = false) String note,
                                     @RequestParam(value = "redirectTo", required = false) String redirectTo,
                                     RedirectAttributes ra) {
        try {
            service.completeAssessment(id, note);
            ra.addFlashAttribute("success", "Đã hoàn tất thẩm định hồ sơ #" + id + ". Có thể đưa ra quyết định phê duyệt.");
            log.info("Assessment completed for application {}", id);
            // Ở lại trang thẩm định để tiếp tục quyết định
            return "redirect:/admin/assessment/app/" + id;
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Lỗi hoàn tất thẩm định: " + ex.getMessage());
            log.error("Error completing assessment for application {}: {}", id, ex.getMessage());
            return "redirect:" + (StringUtils.hasText(redirectTo) ? redirectTo : "/admin/assessment");
        }
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable("id") Long id,
                          @RequestParam(value = "note", required = false) String note,
                          @RequestParam(value = "redirectTo", required = false) String redirectTo,
                          RedirectAttributes ra) {
        try {
            service.approve(id, note);
            ra.addFlashAttribute("success", "Đã phê duyệt hồ sơ #" + id + " thành công!");
            log.info("Application {} approved", id);
            // Sau khi approve, chuyển sang trang disbursement
            return "redirect:/admin/disbursement/app/" + id;
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Lỗi phê duyệt: " + ex.getMessage());
            log.error("Error approving application {}: {}", id, ex.getMessage());
            return "redirect:" + (StringUtils.hasText(redirectTo) ? redirectTo : "/admin/assessment/app/" + id);
        }
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable("id") Long id,
                         @RequestParam("reason") String reason,
                         @RequestParam(value = "redirectTo", required = false) String redirectTo,
                         RedirectAttributes ra) {
        try {
            if (reason == null || reason.trim().isEmpty()) {
                throw new IllegalArgumentException("Phải nhập lý do từ chối");
            }
            service.reject(id, reason);
            ra.addFlashAttribute("error", "Đã từ chối hồ sơ #" + id + ". Lý do: " + reason);
            log.info("Application {} rejected with reason: {}", id, reason);
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Lỗi từ chối: " + ex.getMessage());
            log.error("Error rejecting application {}: {}", id, ex.getMessage());
        }
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
        addFlashMessages(model, error, success);
        return "admin/list";
    }

    /** Chi tiết hồ sơ - phòng giải ngân */
    @GetMapping("/disbursement/app/{id}")
    public String disbursementDetail(@PathVariable Long id,
                                     @ModelAttribute("error") String error,
                                     @ModelAttribute("success") String success,
                                     Model model) {
        var app = service.get(id).orElseThrow(() -> 
            new IllegalArgumentException("Không tìm thấy hồ sơ"));
        model.addAttribute("app", app);
        model.addAttribute("section", "disbursement");
        model.addAttribute("canModify", service.canModify(app, "disbursement"));
        addFlashMessages(model, error, success);
        return "admin/detail";
    }

    @PostMapping("/{id}/disburse")
    public String disburse(@PathVariable("id") Long id,
                           @RequestParam(value = "redirectTo", required = false) String redirectTo,
                           RedirectAttributes ra) {
        try {
            service.disburse(id);
            ra.addFlashAttribute("success", "Đã giải ngân thành công cho hồ sơ #" + id + "!");
            log.info("Application {} disbursed successfully", id);
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Lỗi giải ngân: " + ex.getMessage());
            log.error("Error disbursing application {}: {}", id, ex.getMessage());
        }
        return "redirect:" + (StringUtils.hasText(redirectTo) ? redirectTo : "/admin/disbursement/app/" + id);
    }

    /* ===== HỢP ĐỒNG VÀ TIỆN ÍCH ===== */
    @GetMapping("/{id}/contract")
    public String contract(@PathVariable("id") Long id, Model model) {
        var app = service.get(id).orElseThrow(() -> 
            new IllegalArgumentException("Không tìm thấy hồ sơ"));
        model.addAttribute("app", app);
        return "admin/contract";
    }
    
    /* ===== UTILITY METHODS ===== */
    private void addFlashMessages(Model model, String error, String success) {
        if (StringUtils.hasText(error)) {
            model.addAttribute("error", error);
        }
        if (StringUtils.hasText(success)) {
            model.addAttribute("success", success);
        }
    }
}