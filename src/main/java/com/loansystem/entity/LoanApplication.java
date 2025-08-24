package com.loansystem.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

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
    private Customer customer;

    @NotNull
    @DecimalMin("1000000.00")
    private BigDecimal amount;

    @Min(3)
    @Max(60)
    private int termMonths;

    private String purpose;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal monthlyIncome;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private LoanStatus status = LoanStatus.SUBMITTED;

    @Builder.Default private LocalDateTime submittedAt = LocalDateTime.now();
    private LocalDateTime reviewedAt;   // B1
    private LocalDateTime assessedAt;   // B2
    private LocalDateTime approvedAt;   // B3 (approve)
    private LocalDateTime rejectedAt;   // B3 (reject)
    private LocalDateTime disbursedAt;  // B4

    private String assessmentNote;
    private String approvalNote;
    private String rejectionReason;
}
