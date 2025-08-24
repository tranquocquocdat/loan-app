package com.loansystem.service;

import com.loansystem.entity.Customer;
import com.loansystem.entity.LoanApplication;
import com.loansystem.entity.LoanStatus;
import com.loansystem.repository.CustomerRepository;
import com.loansystem.repository.LoanApplicationRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoanWorkflowService {

    private final CustomerRepository customerRepo;
    private final LoanApplicationRepository loanRepo;
    private final BlacklistService blacklistService;

    @Transactional
    public LoanApplication submitApplication(
            String fullName, String email, String phone,
            BigDecimal amount, int termMonths, String purpose, BigDecimal monthlyIncome) {

        var c = Customer.builder().fullName(fullName).email(email).phone(phone).build();
        customerRepo.save(c);

        var app = LoanApplication.builder()
                .customer(c)
                .amount(amount)
                .termMonths(termMonths)
                .purpose(purpose)
                .monthlyIncome(monthlyIncome)
                .status(LoanStatus.SUBMITTED)
                .submittedAt(LocalDateTime.now())
                .build();

        return loanRepo.save(app);
    }

    public List<LoanApplication> listByStatuses(LoanStatus... statuses) {
        return loanRepo.findByStatusIn(Arrays.asList(statuses));
    }

    public Optional<LoanApplication> get(Long id) { return loanRepo.findById(id); }

    /* ====== PHÒNG TIẾP NHẬN ====== */
    public boolean isComplete(LoanApplication a) {
        return a.getCustomer() != null
                && a.getCustomer().getFullName() != null && !a.getCustomer().getFullName().isBlank()
                && a.getCustomer().getPhone() != null && !a.getCustomer().getPhone().isBlank()
                && a.getCustomer().getEmail() != null && !a.getCustomer().getEmail().isBlank()
                && a.getAmount() != null && a.getAmount().compareTo(BigDecimal.ZERO) > 0
                && a.getTermMonths() > 0
                && a.getMonthlyIncome() != null && a.getMonthlyIncome().compareTo(BigDecimal.ZERO) > 0;
    }

    @Transactional
    public LoanApplication intakeAccept(Long id) {
        var a = loanRepo.findById(id).orElseThrow();
        if (!isComplete(a)) {
            throw new IllegalStateException("Hồ sơ chưa đầy đủ, không thể tiếp nhận.");
        }
        a.setStatus(LoanStatus.UNDER_REVIEW);
        a.setReviewedAt(LocalDateTime.now());
        return a;
    }

    /* ====== PHÒNG THẨM ĐỊNH ====== */
    @Transactional
    public LoanApplication completeAssessment(Long id, String note) {
        var a = loanRepo.findById(id).orElseThrow();
        if (a.getStatus() != LoanStatus.UNDER_REVIEW && a.getStatus() != LoanStatus.SUBMITTED) {
            throw new IllegalStateException("Chỉ thẩm định khi đã tiếp nhận.");
        }
        a.setStatus(LoanStatus.ASSESSED);
        a.setAssessedAt(LocalDateTime.now());
        a.setAssessmentNote(note);
        return a;
    }

    @Transactional
    public LoanApplication checkBlacklistOrReject(Long id) {
        var a = loanRepo.findById(id).orElseThrow();
        var customer = a.getCustomer();
        var reasonOpt = blacklistService.check(customer.getEmail(), customer.getPhone());
        if (reasonOpt.isPresent()) {
            a.setStatus(LoanStatus.REJECTED);
            a.setRejectedAt(LocalDateTime.now());
            a.setRejectionReason(reasonOpt.get());
        }
        return a;
    }

    @Transactional
    public LoanApplication approve(Long id, String note) {
        var a = loanRepo.findById(id).orElseThrow();
        if (a.getStatus() != LoanStatus.ASSESSED) {
            throw new IllegalStateException("Chỉ phê duyệt sau khi thẩm định.");
        }
        a.setStatus(LoanStatus.APPROVED);
        a.setApprovedAt(LocalDateTime.now());
        a.setApprovalNote(note);
        return a;
    }

    @Transactional
    public LoanApplication reject(Long id, String reason) {
        var a = loanRepo.findById(id).orElseThrow();
        if (a.getStatus() != LoanStatus.ASSESSED && a.getStatus() != LoanStatus.UNDER_REVIEW) {
            throw new IllegalStateException("Chỉ từ chối sau khi tiếp nhận/thẩm định.");
        }
        a.setStatus(LoanStatus.REJECTED);
        a.setRejectedAt(LocalDateTime.now());
        a.setRejectionReason(reason);
        return a;
    }

    /* ====== PHÒNG GIẢI NGÂN ====== */
    @Transactional
    public LoanApplication disburse(Long id) {
        var a = loanRepo.findById(id).orElseThrow();
        if (a.getStatus() != LoanStatus.APPROVED) {
            throw new IllegalStateException("Chỉ giải ngân sau khi phê duyệt.");
        }
        a.setStatus(LoanStatus.DISBURSED);
        a.setDisbursedAt(LocalDateTime.now());
        return a;
    }
}
