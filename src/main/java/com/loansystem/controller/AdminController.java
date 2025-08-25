package com.loansystem.controller;

import com.loansystem.entity.LoanStatus;
import com.loansystem.service.LoanWorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
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
    public String home() {
        return "redirect:/admin/dashboard";
    }

    /* ===== DASHBOARD TỔNG QUAN ===== */
    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication auth) {
        try {
            log.info("Loading dashboard for user: {}", auth.getName());

            model.addAttribute("submitted", service.countByStatus(LoanStatus.SUBMITTED));
            model.addAttribute("underReview", service.countByStatus(LoanStatus.UNDER_REVIEW));
            model.addAttribute("assessed", service.countByStatus(LoanStatus.ASSESSED));
            model.addAttribute("approved", service.countByStatus(LoanStatus.APPROVED));
            model.addAttribute("rejected", service.countByStatus(LoanStatus.REJECTED));
            model.addAttribute("disbursed", service.countByStatus(LoanStatus.DISBURSED));
            model.addAttribute("section", "dashboard");

            // Add user role info
            String userRole = getUserRole(auth);
            model.addAttribute("userRole", userRole);

            log.info("Dashboard loaded successfully with role: {}", userRole);
            return "admin/dashboard";

        } catch (Exception e) {
            log.error("Error loading dashboard for user {}: {}", auth.getName(), e.getMessage(), e);
            model.addAttribute("error", "Lỗi tải dashboard: " + e.getMessage());
            return "admin/dashboard";
        }
    }

    /* ===== TAB 1: PHÒNG TIẾP NHẬN ===== */
    @GetMapping("/intake")
    public String intake(Model model, Authentication auth,
                         @ModelAttribute("error") String error,
                         @ModelAttribute("success") String success) {
        try {
            log.info("Loading intake page for user: {}", auth.getName());

            var apps = service.listByStatuses(LoanStatus.SUBMITTED);
            model.addAttribute("apps", apps);
            model.addAttribute("section", "intake");
            model.addAttribute("userRole", getUserRole(auth));

            addFlashMessages(model, error, success);

            log.info("Intake page loaded with {} applications", apps.size());
            return "admin/list";

        } catch (Exception e) {
            log.error("Error loading intake page for user {}: {}", auth.getName(), e.getMessage(), e);
            model.addAttribute("error", "Lỗi tải danh sách tiếp nhận: " + e.getMessage());
            model.addAttribute("apps", java.util.Collections.emptyList());
            model.addAttribute("section", "intake");
            model.addAttribute("userRole", getUserRole(auth));
            return "admin/list";
        }
    }

    /** Chi tiết hồ sơ - phòng tiếp nhận */
    @GetMapping("/intake/app/{id}")
    public String intakeDetail(@PathVariable Long id, Authentication auth,
                               @ModelAttribute("error") String error,
                               @ModelAttribute("success") String success,
                               Model model) {
        try {
            log.info("Loading intake detail for application {} by user: {}", id, auth.getName());

            var app = service.get(id).orElseThrow(() ->
                    new IllegalArgumentException("Không tìm thấy hồ sơ #" + id));

            model.addAttribute("app", app);
            model.addAttribute("section", "intake");
            model.addAttribute("canAccept", service.isComplete(app));
            model.addAttribute("canModify", service.canModify(app, "intake"));
            model.addAttribute("userRole", getUserRole(auth));

            addFlashMessages(model, error, success);

            log.info("Intake detail loaded for application {} with status: {}", id, app.getStatus());
            return "admin/detail";

        } catch (IllegalArgumentException e) {
            log.warn("Application {} not found for user {}: {}", id, auth.getName(), e.getMessage());
            model.addAttribute("error", e.getMessage());
            return "redirect:/admin/intake";
        } catch (Exception e) {
            log.error("Error loading intake detail for application {} by user {}: {}",
                    id, auth.getName(), e.getMessage(), e);
            model.addAttribute("error", "Lỗi tải chi tiết hồ sơ: " + e.getMessage());
            return "redirect:/admin/intake";
        }
    }

    @PostMapping("/{id}/intake/accept")
    public String intakeAccept(@PathVariable("id") Long id, Authentication auth,
                               @RequestParam(value = "redirectTo", required = false) String redirectTo,
                               RedirectAttributes ra) {
        try {
            log.info("User {} attempting to accept application {}", auth.getName(), id);

            var app = service.intakeAccept(id);
            ra.addFlashAttribute("success",
                    String.format("Đã tiếp nhận hồ sơ #%d thành công. Trạng thái: %s",
                            id, app.getStatus()));

            log.info("Application {} accepted by user {} - new status: {}",
                    id, auth.getName(), app.getStatus());

            // Tự động chuyển đến phòng thẩm định sau khi tiếp nhận
            return "redirect:/admin/assessment/app/" + id;

        } catch (IllegalArgumentException e) {
            log.warn("Invalid application {} for acceptance by user {}: {}",
                    id, auth.getName(), e.getMessage());
            ra.addFlashAttribute("error", "Lỗi tiếp nhận: " + e.getMessage());
        } catch (IllegalStateException e) {
            log.warn("Application {} in invalid state for acceptance by user {}: {}",
                    id, auth.getName(), e.getMessage());
            ra.addFlashAttribute("error", "Lỗi trạng thái: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error accepting application {} by user {}: {}",
                    id, auth.getName(), e.getMessage(), e);
            ra.addFlashAttribute("error",
                    String.format("Lỗi hệ thống khi tiếp nhận hồ sơ #%d: %s", id, e.getMessage()));
        }

        return "redirect:" + (StringUtils.hasText(redirectTo) ? redirectTo : "/admin/intake");
    }

    /* ===== TAB 2: PHÒNG THẨM ĐỊNH TÍN DỤNG ===== */
    @GetMapping("/assessment")
    public String assessment(Model model, Authentication auth,
                             @ModelAttribute("error") String error,
                             @ModelAttribute("success") String success) {
        try {
            log.info("Loading assessment page for user: {}", auth.getName());

            var apps = service.listByStatuses(LoanStatus.UNDER_REVIEW, LoanStatus.ASSESSED);
            model.addAttribute("apps", apps);
            model.addAttribute("section", "assessment");
            model.addAttribute("userRole", getUserRole(auth));

            addFlashMessages(model, error, success);

            log.info("Assessment page loaded with {} applications", apps.size());
            return "admin/list";

        } catch (Exception e) {
            log.error("Error loading assessment page for user {}: {}", auth.getName(), e.getMessage(), e);
            model.addAttribute("error", "Lỗi tải danh sách thẩm định: " + e.getMessage());
            model.addAttribute("apps", java.util.Collections.emptyList());
            model.addAttribute("section", "assessment");
            model.addAttribute("userRole", getUserRole(auth));
            return "admin/list";
        }
    }

    /** Chi tiết hồ sơ - phòng thẩm định */
    @GetMapping("/assessment/app/{id}")
    public String assessmentDetail(@PathVariable Long id, Authentication auth,
                                   @ModelAttribute("error") String error,
                                   @ModelAttribute("success") String success,
                                   Model model) {
        try {
            log.info("Loading assessment detail for application {} by user: {}", id, auth.getName());

            var app = service.get(id).orElseThrow(() ->
                    new IllegalArgumentException("Không tìm thấy hồ sơ #" + id));

            model.addAttribute("app", app);
            model.addAttribute("section", "assessment");
            model.addAttribute("canModify", service.canModify(app, "assessment"));
            model.addAttribute("userRole", getUserRole(auth));

            addFlashMessages(model, error, success);

            log.info("Assessment detail loaded for application {} with status: {}", id, app.getStatus());
            return "admin/detail";

        } catch (IllegalArgumentException e) {
            log.warn("Application {} not found for user {}: {}", id, auth.getName(), e.getMessage());
            model.addAttribute("error", e.getMessage());
            return "redirect:/admin/assessment";
        } catch (Exception e) {
            log.error("Error loading assessment detail for application {} by user {}: {}",
                    id, auth.getName(), e.getMessage(), e);
            model.addAttribute("error", "Lỗi tải chi tiết hồ sơ: " + e.getMessage());
            return "redirect:/admin/assessment";
        }
    }

    @PostMapping("/{id}/assessment/check-blacklist")
    public String checkBlacklist(@PathVariable("id") Long id, Authentication auth,
                                 @RequestParam(value = "redirectTo", required = false) String redirectTo,
                                 RedirectAttributes ra) {
        try {
            log.info("User {} checking blacklist for application {}", auth.getName(), id);

            var beforeApp = service.get(id).orElseThrow(() ->
                    new IllegalArgumentException("Không tìm thấy hồ sơ #" + id));
            var beforeStatus = beforeApp.getStatus();

            var app = service.checkBlacklistOrReject(id);

            if (app.getStatus() == LoanStatus.REJECTED) {
                ra.addFlashAttribute("error",
                        String.format("Hồ sơ #%d bị từ chối do nằm trong blacklist: %s",
                                id, app.getRejectionReason()));
                log.warn("Application {} rejected due to blacklist by user {}: {}",
                        id, auth.getName(), app.getRejectionReason());
            } else if (beforeStatus == LoanStatus.UNDER_REVIEW) {
                ra.addFlashAttribute("success",
                        String.format("Hồ sơ #%d đã vượt qua kiểm tra blacklist", id));
                log.info("Application {} passed blacklist check by user {}", id, auth.getName());
            }

        } catch (IllegalArgumentException e) {
            log.warn("Invalid application {} for blacklist check by user {}: {}",
                    id, auth.getName(), e.getMessage());
            ra.addFlashAttribute("error", "Lỗi kiểm tra: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error checking blacklist for application {} by user {}: {}",
                    id, auth.getName(), e.getMessage(), e);
            ra.addFlashAttribute("error",
                    String.format("Lỗi hệ thống khi kiểm tra blacklist hồ sơ #%d: %s", id, e.getMessage()));
        }

        return "redirect:" + (StringUtils.hasText(redirectTo) ? redirectTo : "/admin/assessment");
    }

    @PostMapping("/{id}/assessment/complete")
    public String assessmentComplete(@PathVariable("id") Long id, Authentication auth,
                                     @RequestParam(value = "note", required = false) String note,
                                     @RequestParam(value = "redirectTo", required = false) String redirectTo,
                                     RedirectAttributes ra) {
        try {
            log.info("User {} completing assessment for application {} with note: {}",
                    auth.getName(), id, note);

            var app = service.completeAssessment(id, note);
            ra.addFlashAttribute("success",
                    String.format("Đã hoàn tất thẩm định hồ sơ #%d. Trạng thái: %s",
                            id, app.getStatus()));

            log.info("Assessment completed for application {} by user {} - new status: {}",
                    id, auth.getName(), app.getStatus());

            // Ở lại trang thẩm định để tiếp tục quyết định
            return "redirect:/admin/assessment/app/" + id;

        } catch (IllegalArgumentException e) {
            log.warn("Invalid application {} for assessment completion by user {}: {}",
                    id, auth.getName(), e.getMessage());
            ra.addFlashAttribute("error", "Lỗi hoàn tất thẩm định: " + e.getMessage());
        } catch (IllegalStateException e) {
            log.warn("Application {} in invalid state for assessment completion by user {}: {}",
                    id, auth.getName(), e.getMessage());
            ra.addFlashAttribute("error", "Lỗi trạng thái: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error completing assessment for application {} by user {}: {}",
                    id, auth.getName(), e.getMessage(), e);
            ra.addFlashAttribute("error",
                    String.format("Lỗi hệ thống khi hoàn tất thẩm định hồ sơ #%d: %s", id, e.getMessage()));
        }

        return "redirect:" + (StringUtils.hasText(redirectTo) ? redirectTo : "/admin/assessment");
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable("id") Long id, Authentication auth,
                          @RequestParam(value = "note", required = false) String note,
                          @RequestParam(value = "redirectTo", required = false) String redirectTo,
                          RedirectAttributes ra) {
        try {
            log.info("User {} approving application {} with note: {}",
                    auth.getName(), id, note);

            var app = service.approve(id, note);
            ra.addFlashAttribute("success",
                    String.format("Đã phê duyệt hồ sơ #%d thành công! Trạng thái: %s",
                            id, app.getStatus()));

            log.info("Application {} approved by user {} - new status: {}",
                    id, auth.getName(), app.getStatus());

            // Sau khi approve, chuyển sang trang disbursement
            return "redirect:/admin/disbursement/app/" + id;

        } catch (IllegalArgumentException e) {
            log.warn("Invalid application {} for approval by user {}: {}",
                    id, auth.getName(), e.getMessage());
            ra.addFlashAttribute("error", "Lỗi phê duyệt: " + e.getMessage());
        } catch (IllegalStateException e) {
            log.warn("Application {} in invalid state for approval by user {}: {}",
                    id, auth.getName(), e.getMessage());
            ra.addFlashAttribute("error", "Lỗi trạng thái: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error approving application {} by user {}: {}",
                    id, auth.getName(), e.getMessage(), e);
            ra.addFlashAttribute("error",
                    String.format("Lỗi hệ thống khi phê duyệt hồ sơ #%d: %s", id, e.getMessage()));
        }

        return "redirect:" + (StringUtils.hasText(redirectTo) ? redirectTo : "/admin/assessment/app/" + id);
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable("id") Long id, Authentication auth,
                         @RequestParam("reason") String reason,
                         @RequestParam(value = "redirectTo", required = false) String redirectTo,
                         RedirectAttributes ra) {
        try {
            log.info("User {} rejecting application {} with reason: {}",
                    auth.getName(), id, reason);

            if (reason == null || reason.trim().isEmpty()) {
                throw new IllegalArgumentException("Phải nhập lý do từ chối");
            }

            var app = service.reject(id, reason);
            ra.addFlashAttribute("error",
                    String.format("Đã từ chối hồ sơ #%d. Lý do: %s. Trạng thái: %s",
                            id, reason, app.getStatus()));

            log.info("Application {} rejected by user {} with reason: {} - new status: {}",
                    id, auth.getName(), reason, app.getStatus());

        } catch (IllegalArgumentException e) {
            log.warn("Invalid input for rejecting application {} by user {}: {}",
                    id, auth.getName(), e.getMessage());
            ra.addFlashAttribute("error", "Lỗi từ chối: " + e.getMessage());
        } catch (IllegalStateException e) {
            log.warn("Application {} in invalid state for rejection by user {}: {}",
                    id, auth.getName(), e.getMessage());
            ra.addFlashAttribute("error", "Lỗi trạng thái: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error rejecting application {} by user {}: {}",
                    id, auth.getName(), e.getMessage(), e);
            ra.addFlashAttribute("error",
                    String.format("Lỗi hệ thống khi từ chối hồ sơ #%d: %s", id, e.getMessage()));
        }

        return "redirect:" + (StringUtils.hasText(redirectTo) ? redirectTo : "/admin/assessment/app/" + id);
    }

    /* ===== TAB 3: PHÒNG GIẢI NGÂN ===== */
    @GetMapping("/disbursement")
    public String disbursement(Model model, Authentication auth,
                               @ModelAttribute("error") String error,
                               @ModelAttribute("success") String success) {
        try {
            log.info("Loading disbursement page for user: {}", auth.getName());

            var apps = service.listByStatuses(LoanStatus.APPROVED, LoanStatus.DISBURSED);
            model.addAttribute("apps", apps);
            model.addAttribute("section", "disbursement");
            model.addAttribute("userRole", getUserRole(auth));

            addFlashMessages(model, error, success);

            log.info("Disbursement page loaded with {} applications", apps.size());
            return "admin/list";

        } catch (Exception e) {
            log.error("Error loading disbursement page for user {}: {}", auth.getName(), e.getMessage(), e);
            model.addAttribute("error", "Lỗi tải danh sách giải ngân: " + e.getMessage());
            model.addAttribute("apps", java.util.Collections.emptyList());
            model.addAttribute("section", "disbursement");
            model.addAttribute("userRole", getUserRole(auth));
            return "admin/list";
        }
    }

    /** Chi tiết hồ sơ - phòng giải ngân */
    @GetMapping("/disbursement/app/{id}")
    public String disbursementDetail(@PathVariable Long id, Authentication auth,
                                     @ModelAttribute("error") String error,
                                     @ModelAttribute("success") String success,
                                     Model model) {
        try {
            log.info("Loading disbursement detail for application {} by user: {}", id, auth.getName());

            var app = service.get(id).orElseThrow(() ->
                    new IllegalArgumentException("Không tìm thấy hồ sơ #" + id));

            model.addAttribute("app", app);
            model.addAttribute("section", "disbursement");
            model.addAttribute("canModify", service.canModify(app, "disbursement"));
            model.addAttribute("userRole", getUserRole(auth));

            addFlashMessages(model, error, success);

            log.info("Disbursement detail loaded for application {} with status: {}", id, app.getStatus());
            return "admin/detail";

        } catch (IllegalArgumentException e) {
            log.warn("Application {} not found for user {}: {}", id, auth.getName(), e.getMessage());
            model.addAttribute("error", e.getMessage());
            return "redirect:/admin/disbursement";
        } catch (Exception e) {
            log.error("Error loading disbursement detail for application {} by user {}: {}",
                    id, auth.getName(), e.getMessage(), e);
            model.addAttribute("error", "Lỗi tải chi tiết hồ sơ: " + e.getMessage());
            return "redirect:/admin/disbursement";
        }
    }

    @PostMapping("/{id}/disburse")
    public String disburse(@PathVariable("id") Long id, Authentication auth,
                           @RequestParam(value = "redirectTo", required = false) String redirectTo,
                           RedirectAttributes ra) {
        try {
            log.info("User {} disbursing application {}", auth.getName(), id);

            var app = service.disburse(id);
            ra.addFlashAttribute("success",
                    String.format("Đã giải ngân thành công cho hồ sơ #%d! Trạng thái: %s",
                            id, app.getStatus()));

            log.info("Application {} disbursed successfully by user {} - new status: {}",
                    id, auth.getName(), app.getStatus());

        } catch (IllegalArgumentException e) {
            log.warn("Invalid application {} for disbursement by user {}: {}",
                    id, auth.getName(), e.getMessage());
            ra.addFlashAttribute("error", "Lỗi giải ngân: " + e.getMessage());
        } catch (IllegalStateException e) {
            log.warn("Application {} in invalid state for disbursement by user {}: {}",
                    id, auth.getName(), e.getMessage());
            ra.addFlashAttribute("error", "Lỗi trạng thái: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error disbursing application {} by user {}: {}",
                    id, auth.getName(), e.getMessage(), e);
            ra.addFlashAttribute("error",
                    String.format("Lỗi hệ thống khi giải ngân hồ sơ #%d: %s", id, e.getMessage()));
        }

        return "redirect:" + (StringUtils.hasText(redirectTo) ? redirectTo : "/admin/disbursement/app/" + id);
    }

    /* ===== HỢP ĐỒNG VÀ TIỆN ÍCH ===== */
    @GetMapping("/{id}/contract")
    public String contract(@PathVariable("id") Long id, Authentication auth, Model model) {
        try {
            log.info("Loading contract for application {} by user: {}", id, auth.getName());

            var app = service.get(id).orElseThrow(() ->
                    new IllegalArgumentException("Không tìm thấy hồ sơ #" + id));

            model.addAttribute("app", app);

            log.info("Contract loaded for application {} with status: {}", id, app.getStatus());
            return "admin/contract";

        } catch (IllegalArgumentException e) {
            log.warn("Application {} not found for contract by user {}: {}", id, auth.getName(), e.getMessage());
            model.addAttribute("error", e.getMessage());
            return "redirect:/admin/dashboard";
        } catch (Exception e) {
            log.error("Error loading contract for application {} by user {}: {}",
                    id, auth.getName(), e.getMessage(), e);
            model.addAttribute("error", "Lỗi tải hợp đồng: " + e.getMessage());
            return "redirect:/admin/dashboard";
        }
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

    private String getUserRole(Authentication auth) {
        try {
            if (auth != null && auth.getAuthorities() != null) {
                var authorities = auth.getAuthorities();

                if (authorities.stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) {
                    return "ADMIN";
                } else if (authorities.stream().anyMatch(a -> "ROLE_INTAKE".equals(a.getAuthority()))) {
                    return "INTAKE";
                } else if (authorities.stream().anyMatch(a -> "ROLE_ASSESSMENT".equals(a.getAuthority()))) {
                    return "ASSESSMENT";
                } else if (authorities.stream().anyMatch(a -> "ROLE_DISBURSEMENT".equals(a.getAuthority()))) {
                    return "DISBURSEMENT";
                } else if (authorities.stream().anyMatch(a -> "ROLE_CUSTOMER".equals(a.getAuthority()))) {
                    return "CUSTOMER";
                }
            }
        } catch (Exception e) {
            log.warn("Error determining user role for {}: {}",
                    auth != null ? auth.getName() : "null", e.getMessage());
        }
        return "UNKNOWN";
    }

    /* ===== GLOBAL ERROR HANDLER ===== */
    @ExceptionHandler(Exception.class)
    public String handleGlobalError(Exception e, Model model, Authentication auth) {
        log.error("Global error handler caught exception for user {}: {}",
                auth != null ? auth.getName() : "anonymous", e.getMessage(), e);

        model.addAttribute("error", "Lỗi hệ thống: " + e.getMessage());
        model.addAttribute("userRole", getUserRole(auth));

        // Determine where to redirect based on user role
        String userRole = getUserRole(auth);
        switch (userRole) {
            case "ADMIN":
                return "redirect:/admin/dashboard";
            case "INTAKE":
                return "redirect:/admin/intake";
            case "ASSESSMENT":
                return "redirect:/admin/assessment";
            case "DISBURSEMENT":
                return "redirect:/admin/disbursement";
            case "CUSTOMER":
                return "redirect:/my-applications";
            default:
                return "redirect:/login";
        }
    }
}