package com.loansystem.repository;
import com.loansystem.entity.LoanApplication;
import com.loansystem.entity.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {
    List<LoanApplication> findByStatus(LoanStatus status);
}
