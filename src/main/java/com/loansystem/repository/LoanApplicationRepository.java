package com.loansystem.repository;

import com.loansystem.entity.Customer;
import com.loansystem.entity.LoanApplication;
import com.loansystem.entity.LoanStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {
    List<LoanApplication> findByStatus(LoanStatus status);
    List<LoanApplication> findByStatusIn(Collection<LoanStatus> statuses);
    List<LoanApplication> findByCustomer(Customer customer);
    List<LoanApplication> findByCustomerOrderBySubmittedAtDesc(Customer customer);
}