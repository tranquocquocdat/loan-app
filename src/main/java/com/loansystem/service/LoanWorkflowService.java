package com.loansystem.service;

import com.loansystem.entity.*;
import com.loansystem.repository.CustomerRepository;
import com.loansystem.repository.LoanApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class LoanWorkflowService {

    private final CustomerRepository customerRepo;
    private final LoanApplicationRepository loanRepo;

    public LoanWorkflowService(CustomerRepository customerRepo, LoanApplicationRepository loanRepo) {
        this.customerRepo = customerRepo;
        this.loanRepo = loanRepo;
    }

    @Transactional
    public LoanApplication submitApplication(String fullName, String email, String phone,
                                             BigDecimal amount, int termMonths, String purpose,
                                             BigDecimal monthlyIncome) {
        Customer c = new Customer();
        c.setFullName(fullName);
        c.setEmail(email);
        c.setPhone(phone);
        customerRepo.save(c);

        LoanApplication app = new LoanApplication();
        app.setCustomer(c);
        app.setAmount(amount);
        app.setTermMonths(termMonths);
        app.setPurpose(purpose);
        app.setMonthlyIncome(monthlyIncome);
        app.setStatus(LoanStatus.SUBMITTED);
        app.setSubmittedAt(LocalDateTime.now());
        return loanRepo.save(app);
    }

    public List<LoanApplication> listAll() { return loanRepo.findAll(); }
    public Optional<LoanApplication> get(Long id) { return loanRepo.findById(id); }

    @Transactional
    public LoanApplication moveToReview(Long id) {
        LoanApplication app = loanRepo.findById(id).orElseThrow();
        app.setStatus(LoanStatus.UNDER_REVIEW);
        app.setReviewedAt(LocalDateTime.now());
        return app;
    }

    @Transactional
    public LoanApplication assess(Long id, boolean approve, String noteOrReason) {
        LoanApplication app = loanRepo.findById(id).orElseThrow();
        app.setAssessedAt(LocalDateTime.now());
        if (approve) {
            app.setStatus(LoanStatus.ASSESSED_APPROVED);
            app.setAssessmentNote(noteOrReason);
        } else {
            app.setStatus(LoanStatus.ASSESSED_REJECTED);
            app.setRejectionReason(noteOrReason);
        }
        return app;
    }

    @Transactional
    public LoanApplication disburse(Long id) {
        LoanApplication app = loanRepo.findById(id).orElseThrow();
        if (app.getStatus() != LoanStatus.ASSESSED_APPROVED) {
            throw new IllegalStateException("Application is not approved yet");
        }
        app.setStatus(LoanStatus.DISBURSED);
        app.setDisbursedAt(LocalDateTime.now());
        return app;
    }
}
