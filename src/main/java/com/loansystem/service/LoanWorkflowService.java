package com.loansystem.service;

import com.loansystem.entity.Customer;
import com.loansystem.entity.LoanApplication;
import com.loansystem.entity.LoanStatus;
import com.loansystem.repository.CustomerRepository;
import com.loansystem.repository.LoanApplicationRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    @Transactional
    public LoanApplication submitApplication(
            String fullName,
            String email,
            String phone,
            BigDecimal amount,
            int termMonths,
            String purpose,
            BigDecimal monthlyIncome) {

        Customer c = Customer.builder().fullName(fullName).email(email).phone(phone).build();
        customerRepo.save(c);

        LoanApplication app =
                LoanApplication.builder()
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

    public List<LoanApplication> listAll() {
        return loanRepo.findAll();
    }

    public Optional<LoanApplication> get(Long id) {
        return loanRepo.findById(id);
    }

    @Transactional
    public LoanApplication moveToReview(Long id) {
        LoanApplication a = loanRepo.findById(id).orElseThrow();
        a.setStatus(LoanStatus.UNDER_REVIEW);
        a.setReviewedAt(LocalDateTime.now());
        return a;
    }

    @Transactional
    public LoanApplication completeAssessment(Long id, String note) {
        LoanApplication a = loanRepo.findById(id).orElseThrow();
        if (a.getStatus() != LoanStatus.UNDER_REVIEW && a.getStatus() != LoanStatus.SUBMITTED) {
            throw new IllegalStateException("Chỉ thẩm định khi đã tiếp nhận.");
        }
        a.setStatus(LoanStatus.ASSESSED);
        a.setAssessedAt(LocalDateTime.now());
        a.setAssessmentNote(note);
        return a;
    }

    @Transactional
    public LoanApplication approve(Long id, String note) {
        LoanApplication a = loanRepo.findById(id).orElseThrow();
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
        LoanApplication a = loanRepo.findById(id).orElseThrow();
        if (a.getStatus() != LoanStatus.ASSESSED && a.getStatus() != LoanStatus.UNDER_REVIEW) {
            throw new IllegalStateException("Chỉ từ chối sau khi tiếp nhận/thẩm định.");
        }
        a.setStatus(LoanStatus.REJECTED);
        a.setRejectedAt(LocalDateTime.now());
        a.setRejectionReason(reason);
        return a;
    }

    @Transactional
    public LoanApplication disburse(Long id) {
        LoanApplication a = loanRepo.findById(id).orElseThrow();
        if (a.getStatus() != LoanStatus.APPROVED) {
            throw new IllegalStateException("Chỉ giải ngân sau khi phê duyệt.");
        }
        a.setStatus(LoanStatus.DISBURSED);
        a.setDisbursedAt(LocalDateTime.now());
        return a;
    }
}
