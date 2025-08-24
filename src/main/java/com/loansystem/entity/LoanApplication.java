package com.loansystem.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@SequenceGenerator(name = "loan_seq", sequenceName = "LOAN_APP_SEQ", allocationSize = 1)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class LoanApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "loan_seq")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @NotNull
    @DecimalMin("1000000.00")
    @Column(precision = 15, scale = 2)
    private BigDecimal amount;

    @Min(3)
    @Max(60)
    @Column(name = "term_months")
    private int termMonths;

    @Column(length = 500)
    private String purpose;

    @NotNull
    @DecimalMin("0.00")
    @Column(name = "monthly_income", precision = 15, scale = 2)
    private BigDecimal monthlyIncome;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private LoanStatus status = LoanStatus.SUBMITTED;

    // Timestamps
    @Builder.Default
    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt = LocalDateTime.now();

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "assessed_at")
    private LocalDateTime assessedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "disbursed_at")
    private LocalDateTime disbursedAt;

    // Notes and reasons
    @Column(name = "assessment_note", length = 1000)
    private String assessmentNote;

    @Column(name = "approval_note", length = 1000)
    private String approvalNote;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    // === BUSINESS LOGIC METHODS ===

    /**
     * Tính toán khoản trả hàng tháng ước tính
     * Sử dụng công thức annuity với lãi suất 12%/năm
     */
    public BigDecimal calculateMonthlyPayment() {
        return calculateMonthlyPayment(new BigDecimal("0.12")); // 12% annual rate
    }

    /**
     * Tính toán khoản trả hàng tháng với lãi suất tùy chỉnh
     */
    public BigDecimal calculateMonthlyPayment(BigDecimal annualRate) {
        if (amount == null || termMonths <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal monthlyRate = annualRate.divide(new BigDecimal("12"), 6, RoundingMode.HALF_UP);

        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return amount.divide(new BigDecimal(termMonths), 2, RoundingMode.HALF_UP);
        }

        BigDecimal onePlusRate = monthlyRate.add(BigDecimal.ONE);
        BigDecimal factor = onePlusRate.pow(termMonths);

        BigDecimal numerator = monthlyRate.multiply(factor);
        BigDecimal denominator = factor.subtract(BigDecimal.ONE);

        BigDecimal paymentFactor = numerator.divide(denominator, 6, RoundingMode.HALF_UP);

        return amount.multiply(paymentFactor).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Tính tỷ lệ thu nhập/khoản vay (Debt-to-Income Ratio)
     */
    public BigDecimal getDebtToIncomeRatio() {
        if (monthlyIncome == null || monthlyIncome.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal monthlyPayment = calculateMonthlyPayment();
        return monthlyPayment.divide(monthlyIncome, 4, RoundingMode.HALF_UP);
    }

    /**
     * Kiểm tra xem hồ sơ có đủ điều kiện phê duyệt cơ bản không
     */
    public boolean isEligibleForApproval() {
        // Kiểm tra tỷ lệ thu nhập (không quá 33%)
        BigDecimal dtiRatio = getDebtToIncomeRatio();
        if (dtiRatio.compareTo(new BigDecimal("0.33")) > 0) {
            return false;
        }

        // Kiểm tra số tiền tối thiểu và tối đa
        if (amount.compareTo(new BigDecimal("1000000")) < 0 ||
                amount.compareTo(new BigDecimal("2000000000")) > 0) {
            return false;
        }

        // Kiểm tra kỳ hạn hợp lệ
        return termMonths >= 3 && termMonths <= 60;
    }

    /**
     * Tính số ngày xử lý từ khi nộp hồ sơ
     */
    public long getDaysFromSubmission() {
        return ChronoUnit.DAYS.between(submittedAt, LocalDateTime.now());
    }

    /**
     * Tính thời gian xử lý cho từng giai đoạn
     */
    public long getDaysInStatus() {
        LocalDateTime now = LocalDateTime.now();
        switch (status) {
            case SUBMITTED:
                return ChronoUnit.DAYS.between(submittedAt, now);
            case UNDER_REVIEW:
                return reviewedAt != null ? ChronoUnit.DAYS.between(reviewedAt, now) : 0;
            case ASSESSED:
                return assessedAt != null ? ChronoUnit.DAYS.between(assessedAt, now) : 0;
            case APPROVED:
                return approvedAt != null ? ChronoUnit.DAYS.between(approvedAt, now) : 0;
            case REJECTED:
                return rejectedAt != null ? ChronoUnit.DAYS.between(rejectedAt, now) : 0;
            case DISBURSED:
                return 0; // Đã hoàn tất
            default:
                return 0;
        }
    }

    /**
     * Kiểm tra xem hồ sơ có bị trễ hạn xử lý không
     */
    public boolean isOverdue() {
        long daysInStatus = getDaysInStatus();
        switch (status) {
            case SUBMITTED:
                return daysInStatus > 1; // Quá 1 ngày chưa tiếp nhận
            case UNDER_REVIEW:
                return daysInStatus > 3; // Quá 3 ngày chưa thẩm định xong
            case ASSESSED:
                return daysInStatus > 2; // Quá 2 ngày chưa quyết định
            case APPROVED:
                return daysInStatus > 5; // Quá 5 ngày chưa giải ngân
            default:
                return false;
        }
    }

    /**
     * Lấy trạng thái hiển thị cho người dùng
     */
    public String getStatusDisplayName() {
        switch (status) {
            case SUBMITTED:
                return "Đã nộp hồ sơ";
            case UNDER_REVIEW:
                return "Đang thẩm định";
            case ASSESSED:
                return "Chờ quyết định";
            case APPROVED:
                return "Đã phê duyệt";
            case REJECTED:
                return "Đã từ chối";
            case DISBURSED:
                return "Đã giải ngân";
            default:
                return status.name();
        }
    }

    /**
     * Lấy màu sắc cho trạng thái
     */
    public String getStatusColor() {
        switch (status) {
            case SUBMITTED:
                return "#0891b2"; // blue
            case UNDER_REVIEW:
                return "#ca8a04"; // yellow
            case ASSESSED:
                return "#7c3aed"; // purple
            case APPROVED:
                return "#059669"; // green
            case REJECTED:
                return "#dc2626"; // red
            case DISBURSED:
                return "#16a34a"; // bright green
            default:
                return "#6b7280"; // gray
        }
    }

    /**
     * Kiểm tra xem có thể chỉnh sửa hồ sơ không
     */
    public boolean canModify() {
        return status == LoanStatus.SUBMITTED || status == LoanStatus.UNDER_REVIEW;
    }

    /**
     * Kiểm tra xem có thể hủy hồ sơ không
     */
    public boolean canCancel() {
        return status != LoanStatus.DISBURSED && status != LoanStatus.REJECTED;
    }

    /**
     * Lấy bước tiếp theo trong quy trình
     */
    public String getNextStep() {
        switch (status) {
            case SUBMITTED:
                return "Chờ phòng tiếp nhận xử lý";
            case UNDER_REVIEW:
                return "Đang thực hiện thẩm định tín dụng";
            case ASSESSED:
                return "Chờ quyết định phê duyệt";
            case APPROVED:
                return "Chờ phòng giải ngân xử lý";
            case REJECTED:
                return "Hồ sơ đã bị từ chối";
            case DISBURSED:
                return "Hoàn tất quy trình";
            default:
                return "Không xác định";
        }
    }

    /**
     * Tính tổng số tiền phải trả
     */
    public BigDecimal getTotalPayment() {
        BigDecimal monthlyPayment = calculateMonthlyPayment();
        return monthlyPayment.multiply(new BigDecimal(termMonths));
    }

    /**
     * Tính tổng tiền lãi
     */
    public BigDecimal getTotalInterest() {
        return getTotalPayment().subtract(amount);
    }

    /**
     * Định dạng số tiền VND
     */
    public String getFormattedAmount() {
        if (amount == null) return "0 VND";
        return String.format("%,.0f VND", amount);
    }

    public String getFormattedMonthlyIncome() {
        if (monthlyIncome == null) return "0 VND";
        return String.format("%,.0f VND", monthlyIncome);
    }

    public String getFormattedMonthlyPayment() {
        BigDecimal payment = calculateMonthlyPayment();
        return String.format("%,.0f VND", payment);
    }

    // === OVERRIDE METHODS ===

    @Override
    public String toString() {
        return String.format("LoanApplication{id=%d, customer='%s', amount=%s, status=%s}",
                id,
                customer != null ? customer.getFullName() : "null",
                getFormattedAmount(),
                status);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LoanApplication)) return false;
        LoanApplication that = (LoanApplication) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}