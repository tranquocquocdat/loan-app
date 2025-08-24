package com.loansystem.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@SequenceGenerator(name = "loan_seq", sequenceName = "LOAN_APP_SEQ", allocationSize = 1)
public class LoanApplication {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "loan_seq")
    private Long id;

    @ManyToOne(optional=false, fetch = FetchType.LAZY)
    private Customer customer;

    @NotNull @DecimalMin("1000000.00")
    private BigDecimal amount;

    @Min(3) @Max(60)
    private int termMonths;

    private String purpose;

    @NotNull @DecimalMin("0.00")
    private BigDecimal monthlyIncome;

    @Enumerated(EnumType.STRING)
    private LoanStatus status = LoanStatus.SUBMITTED;

    private LocalDateTime submittedAt = LocalDateTime.now();
    private LocalDateTime reviewedAt;
    private LocalDateTime assessedAt;
    private LocalDateTime disbursedAt;

    private String assessmentNote;
    private String rejectionReason;

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Customer getCustomer() { return customer; } public void setCustomer(Customer customer) { this.customer = customer; }
    public BigDecimal getAmount() { return amount; } public void setAmount(BigDecimal amount) { this.amount = amount; }
    public int getTermMonths() { return termMonths; } public void setTermMonths(int termMonths) { this.termMonths = termMonths; }
    public String getPurpose() { return purpose; } public void setPurpose(String purpose) { this.purpose = purpose; }
    public BigDecimal getMonthlyIncome() { return monthlyIncome; } public void setMonthlyIncome(BigDecimal monthlyIncome) { this.monthlyIncome = monthlyIncome; }
    public LoanStatus getStatus() { return status; } public void setStatus(LoanStatus status) { this.status = status; }
    public LocalDateTime getSubmittedAt() { return submittedAt; } public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; } public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public LocalDateTime getAssessedAt() { return assessedAt; } public void setAssessedAt(LocalDateTime assessedAt) { this.assessedAt = assessedAt; }
    public LocalDateTime getDisbursedAt() { return disbursedAt; } public void setDisbursedAt(LocalDateTime disbursedAt) { this.disbursedAt = disbursedAt; }
    public String getAssessmentNote() { return assessmentNote; } public void setAssessmentNote(String assessmentNote) { this.assessmentNote = assessmentNote; }
    public String getRejectionReason() { return rejectionReason; } public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}
