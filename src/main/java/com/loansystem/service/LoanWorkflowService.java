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
    private final BlacklistService blacklistService; // nếu chưa dùng có thể giữ nguyên

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

    /* ====== Kiểm tra đủ thông tin để tiếp nhận ====== */
    public boolean isComplete(LoanApplication a) {
        return a.getCustomer() != null
                && a.getCustomer().getFullName() != null && !a.getCustomer().getFullName().isBlank()
                && a.getCustomer().getPhone() != null && !a.getCustomer().getPhone().isBlank()
                && a.getCustomer().getEmail() != null && !a.getCustomer().getEmail().isBlank()
                && a.getAmount() != null && a.getAmount().compareTo(BigDecimal.ZERO) > 0
                && a.getTermMonths() > 0
                && a.getMonthlyIncome() != null && a.getMonthlyIncome().compareTo(BigDecimal.ZERO) > 0;
    }

    /* ====== “Chuyển đến phòng kế bên” ======
       SUBMITTED -> UNDER_REVIEW
       UNDER_REVIEW -> ASSESSED
       ASSESSED -> APPROVED
       APPROVED -> DISBURSED
       (REJECTED, DISBURSED: không cho chuyển) */
    @Transactional
    public LoanApplication moveNext(Long id) {
        var a = loanRepo.findById(id).orElseThrow();
        switch (a.getStatus()) {
            case SUBMITTED -> {
                if (!isComplete(a)) {
                    throw new IllegalStateException("Hồ sơ chưa đầy đủ, không thể tiếp nhận.");
                }
                a.setStatus(LoanStatus.UNDER_REVIEW);
                a.setReviewedAt(LocalDateTime.now());
            }
            case UNDER_REVIEW -> {
                a.setStatus(LoanStatus.ASSESSED);
                a.setAssessedAt(LocalDateTime.now());
            }
            case ASSESSED -> {
                a.setStatus(LoanStatus.APPROVED);
                a.setApprovedAt(LocalDateTime.now());
            }
            case APPROVED -> {
                a.setStatus(LoanStatus.DISBURSED);
                a.setDisbursedAt(LocalDateTime.now());
            }
            default -> throw new IllegalStateException("Không thể chuyển bước từ trạng thái hiện tại.");
        }
        return a;
    }

    /* ====== Từ chối ở mọi bước chưa kết thúc ====== */
    @Transactional
    public LoanApplication reject(Long id, String reason) {
        var a = loanRepo.findById(id).orElseThrow();
        var st = a.getStatus();
        if (st == LoanStatus.DISBURSED) {
            throw new IllegalStateException("Hồ sơ đã giải ngân, không thể từ chối.");
        }
        // REJECTED lần 2: cho phép cập nhật lý do
        a.setStatus(LoanStatus.REJECTED);
        a.setRejectedAt(LocalDateTime.now());
        a.setRejectionReason(reason);
        return a;
    }
}
