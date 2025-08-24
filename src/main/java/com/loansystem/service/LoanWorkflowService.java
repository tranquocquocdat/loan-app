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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
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

        log.info("Submitting new loan application for customer: {}", fullName);

        // Tạo customer mới
        var customer = Customer.builder()
                .fullName(fullName)
                .email(email)
                .phone(phone)
                .build();
        customerRepo.save(customer);

        // Tạo hồ sơ vay
        var app = LoanApplication.builder()
                .customer(customer)
                .amount(amount)
                .termMonths(termMonths)
                .purpose(purpose)
                .monthlyIncome(monthlyIncome)
                .status(LoanStatus.SUBMITTED)
                .submittedAt(LocalDateTime.now())
                .build();

        var savedApp = loanRepo.save(app);
        log.info("Created loan application with ID: {}", savedApp.getId());
        return savedApp;
    }

    public List<LoanApplication> listByStatuses(LoanStatus... statuses) {
        return loanRepo.findByStatusIn(Arrays.asList(statuses));
    }

    public Optional<LoanApplication> get(Long id) {
        return loanRepo.findById(id);
    }

    /* ====== PHÒNG TIẾP NHẬN ====== */

    /**
     * Kiểm tra tính đầy đủ của hồ sơ
     */
    public boolean isComplete(LoanApplication app) {
        var customer = app.getCustomer();
        return customer != null
                && isNotBlank(customer.getFullName())
                && isNotBlank(customer.getPhone())
                && isNotBlank(customer.getEmail())
                && app.getAmount() != null && app.getAmount().compareTo(new BigDecimal("1000000")) >= 0
                && app.getTermMonths() >= 3 && app.getTermMonths() <= 60
                && app.getMonthlyIncome() != null && app.getMonthlyIncome().compareTo(BigDecimal.ZERO) > 0;
    }

    private boolean isNotBlank(String str) {
        return str != null && !str.trim().isEmpty();
    }

    /**
     * Tiếp nhận hồ sơ - chuyển từ SUBMITTED -> UNDER_REVIEW
     */
    @Transactional
    public LoanApplication intakeAccept(Long id) {
        var app = loanRepo.findById(id).orElseThrow(() ->
                new IllegalArgumentException("Không tìm thấy hồ sơ ID: " + id));

        if (app.getStatus() != LoanStatus.SUBMITTED) {
            throw new IllegalStateException("Chỉ có thể tiếp nhận hồ sơ ở trạng thái SUBMITTED");
        }

        if (!isComplete(app)) {
            throw new IllegalStateException("Hồ sơ chưa đầy đủ thông tin, không thể tiếp nhận");
        }

        app.setStatus(LoanStatus.UNDER_REVIEW);
        app.setReviewedAt(LocalDateTime.now());

        log.info("Application {} moved to UNDER_REVIEW", id);
        return loanRepo.save(app);
    }

    /* ====== PHÒNG THẨM ĐỊNH TÍN DỤNG ====== */

    /**
     * Kiểm tra blacklist và tự động reject nếu có
     */
    @Transactional
    public LoanApplication checkBlacklistOrReject(Long id) {
        var app = loanRepo.findById(id).orElseThrow(() ->
                new IllegalArgumentException("Không tìm thấy hồ sơ ID: " + id));

        if (app.getStatus() != LoanStatus.UNDER_REVIEW) {
            log.warn("Application {} is not in UNDER_REVIEW status, current: {}", id, app.getStatus());
            return app;
        }

        var customer = app.getCustomer();
        var blacklistReason = blacklistService.check(customer.getEmail(), customer.getPhone());

        if (blacklistReason.isPresent()) {
            app.setStatus(LoanStatus.REJECTED);
            app.setRejectedAt(LocalDateTime.now());
            app.setRejectionReason("Blacklist: " + blacklistReason.get());

            log.warn("Application {} rejected due to blacklist: {}", id, blacklistReason.get());
        } else {
            log.info("Application {} passed blacklist check", id);
        }

        return loanRepo.save(app);
    }

    /**
     * Hoàn tất thẩm định - chuyển từ UNDER_REVIEW -> ASSESSED
     */
    @Transactional
    public LoanApplication completeAssessment(Long id, String note) {
        var app = loanRepo.findById(id).orElseThrow(() ->
                new IllegalArgumentException("Không tìm thấy hồ sơ ID: " + id));

        if (app.getStatus() != LoanStatus.UNDER_REVIEW) {
            throw new IllegalStateException("Chỉ có thể hoàn tất thẩm định khi hồ sơ đang UNDER_REVIEW");
        }

        app.setStatus(LoanStatus.ASSESSED);
        app.setAssessedAt(LocalDateTime.now());
        app.setAssessmentNote(note);

        log.info("Application {} assessment completed", id);
        return loanRepo.save(app);
    }

    /**
     * Phê duyệt hồ sơ - chuyển từ ASSESSED -> APPROVED
     */
    @Transactional
    public LoanApplication approve(Long id, String note) {
        var app = loanRepo.findById(id).orElseThrow(() ->
                new IllegalArgumentException("Không tìm thấy hồ sơ ID: " + id));

        if (app.getStatus() != LoanStatus.ASSESSED) {
            throw new IllegalStateException("Chỉ có thể phê duyệt hồ sơ đã được thẩm định (ASSESSED)");
        }

        // Kiểm tra điều kiện phê duyệt cơ bản
        if (!isEligibleForApproval(app)) {
            throw new IllegalStateException("Hồ sơ không đủ điều kiện phê duyệt");
        }

        app.setStatus(LoanStatus.APPROVED);
        app.setApprovedAt(LocalDateTime.now());
        app.setApprovalNote(note);

        log.info("Application {} approved", id);
        return loanRepo.save(app);
    }

    /**
     * Kiểm tra điều kiện phê duyệt
     */
    private boolean isEligibleForApproval(LoanApplication app) {
        // Tỷ lệ thu nhập/khoản vay tối thiểu
        BigDecimal monthlyPayment = calculateMonthlyPayment(app.getAmount(), app.getTermMonths());
        BigDecimal incomeRatio = monthlyPayment.divide(app.getMonthlyIncome(), 4, BigDecimal.ROUND_HALF_UP);

        // Thu nhập phải gấp ít nhất 3 lần khoản trả hàng tháng
        return incomeRatio.compareTo(new BigDecimal("0.33")) <= 0;
    }

    /**
     * Tính khoản trả hàng tháng ước tính (giả định lãi suất 12%/năm)
     */
    private BigDecimal calculateMonthlyPayment(BigDecimal principal, int termMonths) {
        BigDecimal monthlyRate = new BigDecimal("0.01"); // 1%/tháng
        BigDecimal factor = monthlyRate.multiply(
                monthlyRate.add(BigDecimal.ONE).pow(termMonths)
        ).divide(
                monthlyRate.add(BigDecimal.ONE).pow(termMonths).subtract(BigDecimal.ONE),
                4, BigDecimal.ROUND_HALF_UP
        );
        return principal.multiply(factor);
    }

    /**
     * Từ chối hồ sơ
     */
    @Transactional
    public LoanApplication reject(Long id, String reason) {
        var app = loanRepo.findById(id).orElseThrow(() ->
                new IllegalArgumentException("Không tìm thấy hồ sơ ID: " + id));

        if (app.getStatus() != LoanStatus.UNDER_REVIEW && app.getStatus() != LoanStatus.ASSESSED) {
            throw new IllegalStateException("Chỉ có thể từ chối hồ sơ đang thẩm định hoặc đã thẩm định");
        }

        app.setStatus(LoanStatus.REJECTED);
        app.setRejectedAt(LocalDateTime.now());
        app.setRejectionReason(reason);

        log.info("Application {} rejected with reason: {}", id, reason);
        return loanRepo.save(app);
    }

    /* ====== PHÒNG GIẢI NGÂN ====== */

    /**
     * Giải ngân - chuyển từ APPROVED -> DISBURSED
     */
    @Transactional
    public LoanApplication disburse(Long id) {
        var app = loanRepo.findById(id).orElseThrow(() ->
                new IllegalArgumentException("Không tìm thấy hồ sơ ID: " + id));

        if (app.getStatus() != LoanStatus.APPROVED) {
            throw new IllegalStateException("Chỉ có thể giải ngân cho hồ sơ đã được phê duyệt");
        }

        app.setStatus(LoanStatus.DISBURSED);
        app.setDisbursedAt(LocalDateTime.now());

        log.info("Application {} disbursed successfully", id);
        return loanRepo.save(app);
    }

    /* ====== UTILITY METHODS ====== */

    /**
     * Lấy tất cả hồ sơ theo trạng thái với thông tin customer
     */
    public List<LoanApplication> getAllApplications() {
        return loanRepo.findAll();
    }

    /**
     * Thống kê theo trạng thái
     */
    public long countByStatus(LoanStatus status) {
        return loanRepo.findByStatus(status).size();
    }

    /**
     * Kiểm tra quyền thao tác theo phòng ban
     */
    public boolean canModify(LoanApplication app, String department) {
        switch (department.toLowerCase()) {
            case "intake":
                return app.getStatus() == LoanStatus.SUBMITTED;
            case "assessment":
                return app.getStatus() == LoanStatus.UNDER_REVIEW || app.getStatus() == LoanStatus.ASSESSED;
            case "disbursement":
                return app.getStatus() == LoanStatus.APPROVED || app.getStatus() == LoanStatus.DISBURSED;
            default:
                return false;
        }
    }
}