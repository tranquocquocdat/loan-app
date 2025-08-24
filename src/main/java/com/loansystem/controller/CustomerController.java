package com.loansystem.controller;

import com.loansystem.entity.LoanApplication;
import com.loansystem.service.CustomerLoanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerLoanService customerLoanService;

    /* ===== CUSTOMER DASHBOARD ===== */
    @GetMapping("/my-applications")
    public String myApplications(Authentication auth, Model model,
                                 @ModelAttribute("success") String success,
                                 @ModelAttribute("error") String error) {
        String username = auth.getName();
        List<LoanApplication> apps = customerLoanService.getApplicationsByCustomer(username);

        model.addAttribute("apps", apps);
        model.addAttribute("username", username);
        model.addAttribute("totalApplications", apps.size());

        // Statistics
        long submitted = apps.stream().mapToLong(a -> a.getStatus().name().equals("SUBMITTED") ? 1 : 0).sum();
        long underReview = apps.stream().mapToLong(a -> a.getStatus().name().equals("UNDER_REVIEW") ? 1 : 0).sum();
        long approved = apps.stream().mapToLong(a -> a.getStatus().name().equals("APPROVED") ? 1 : 0).sum();
        long disbursed = apps.stream().mapToLong(a -> a.getStatus().name().equals("DISBURSED") ? 1 : 0).sum();
        long rejected = apps.stream().mapToLong(a -> a.getStatus().name().equals("REJECTED") ? 1 : 0).sum();

        model.addAttribute("submitted", submitted);
        model.addAttribute("underReview", underReview);
        model.addAttribute("approved", approved);
        model.addAttribute("disbursed", disbursed);
        model.addAttribute("rejected", rejected);

        if (success != null && !success.trim().isEmpty()) {
            model.addAttribute("success", success);
        }
        if (error != null && !error.trim().isEmpty()) {
            model.addAttribute("error", error);
        }

        return "customer/dashboard";
    }

    /* ===== APPLY FOR LOAN ===== */
    @GetMapping("/")
    public String index(Authentication auth, Model model) {
        model.addAttribute("username", auth.getName());
        return "customer/apply";
    }

    @GetMapping("/apply")
    public String applyGet(Authentication auth, Model model) {
        model.addAttribute("username", auth.getName());
        return "customer/apply";
    }

    @PostMapping("/apply")
    public String apply(Authentication auth,
                        @RequestParam("fullName") String fullName,
                        @RequestParam("email") String email,
                        @RequestParam("phone") String phone,
                        @RequestParam("amount") BigDecimal amount,
                        @RequestParam("termMonths") int termMonths,
                        @RequestParam(value = "purpose", required = false) String purpose,
                        @RequestParam("monthlyIncome") BigDecimal monthlyIncome,
                        RedirectAttributes ra) {

        try {
            String username = auth.getName();
            LoanApplication app = customerLoanService.submitApplication(
                    username, fullName, email, phone, amount, termMonths, purpose, monthlyIncome);

            ra.addFlashAttribute("success", "Đã nộp hồ sơ thành công! Mã hồ sơ: #" + app.getId());
            log.info("Customer {} submitted loan application #{}", username, app.getId());
            return "redirect:/my-applications";

        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Lỗi nộp hồ sơ: " + ex.getMessage());
            log.error("Error submitting application for customer {}: {}", auth.getName(), ex.getMessage());
            return "redirect:/apply";
        }
    }

    /* ===== VIEW APPLICATION DETAIL ===== */
    @GetMapping("/customer/app/{id}")
    public String viewApplication(@PathVariable Long id, Authentication auth, Model model) {
        String username = auth.getName();

        try {
            LoanApplication app = customerLoanService.getApplication(username, id);
            model.addAttribute("app", app);
            model.addAttribute("username", username);
            return "customer/detail";

        } catch (Exception ex) {
            model.addAttribute("error", ex.getMessage());
            return "redirect:/my-applications";
        }
    }

    /* ===== STATUS CHECK (cũ - để tương thích) ===== */
    @GetMapping("/status/{id}")
    public String status(@PathVariable("id") Long id, Authentication auth, Model model) {
        try {
            String username = auth.getName();

            // Nếu là admin/officer thì có thể xem bất kỳ hồ sơ nào
            if (auth.getAuthorities().stream().anyMatch(a ->
                    a.getAuthority().startsWith("ROLE_ADMIN") ||
                            a.getAuthority().startsWith("ROLE_INTAKE") ||
                            a.getAuthority().startsWith("ROLE_ASSESSMENT") ||
                            a.getAuthority().startsWith("ROLE_DISBURSEMENT"))) {

                LoanApplication app = customerLoanService.getApplicationById(id);
                model.addAttribute("app", app);
                model.addAttribute("isOfficer", true);

            } else {
                // Customer chỉ xem được hồ sơ của mình
                LoanApplication app = customerLoanService.getApplication(username, id);
                model.addAttribute("app", app);
                model.addAttribute("isOfficer", false);
            }

            return "customer/status";

        } catch (Exception ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("app", null);
            return "customer/status";
        }
    }
}