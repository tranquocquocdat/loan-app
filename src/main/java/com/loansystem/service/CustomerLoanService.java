package com.loansystem.service;

import com.loansystem.entity.Customer;
import com.loansystem.entity.LoanApplication;
import com.loansystem.entity.LoanStatus;
import com.loansystem.repository.CustomerRepository;
import com.loansystem.repository.LoanApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerLoanService {

    private final CustomerRepository customerRepo;
    private final LoanApplicationRepository loanRepo;

    /**
     * Nộp hồ sơ vay mới cho customer
     */
    @Transactional
    public LoanApplication submitApplication(String username, String fullName, String email, String phone,
                                           BigDecimal amount, int termMonths, String purpose, BigDecimal monthlyIncome) {
        
        log.info("Customer {} submitting loan application", username);

        // Tìm hoặc tạo customer record
        Customer customer = findOrCreateCustomer(username, fullName, email, phone);

        // Tạo hồ sơ vay mới
        LoanApplication app = LoanApplication.builder()
                .customer(customer)
                .amount(amount)
                .termMonths(termMonths)
                .purpose(purpose)
                .monthlyIncome(monthlyIncome)
                .status(LoanStatus.SUBMITTED)
                .submittedAt(LocalDateTime.now())
                .build();

        LoanApplication savedApp = loanRepo.save(app);
        log.info("Created loan application #{} for customer {}", savedApp.getId(), username);
        
        return savedApp;
    }

    /**
     * Lấy tất cả hồ sơ của customer
     */
    public List<LoanApplication> getApplicationsByCustomer(String username) {
        Optional<Customer> customerOpt = findCustomerByUsername(username);
        if (customerOpt.isEmpty()) {
            return List.of();
        }
        
        // Tìm tất cả hồ sơ của customer này
        return loanRepo.findByCustomer(customerOpt.get());
    }

    /**
     * Lấy một hồ sơ cụ thể của customer
     */
    public LoanApplication getApplication(String username, Long applicationId) {
        Customer customer = findCustomerByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin khách hàng"));

        LoanApplication app = loanRepo.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hồ sơ #" + applicationId));

        // Kiểm tra hồ sơ có thuộc về customer này không
        if (!app.getCustomer().getId().equals(customer.getId())) {
            throw new SecurityException("Bạn không có quyền xem hồ sơ này");
        }

        return app;
    }

    /**
     * Lấy hồ sơ bởi ID (cho admin/officer)
     */
    public LoanApplication getApplicationById(Long applicationId) {
        return loanRepo.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hồ sơ #" + applicationId));
    }

    /**
     * Tìm hoặc tạo customer record
     */
    private Customer findOrCreateCustomer(String username, String fullName, String email, String phone) {
        // Tìm customer theo username (trong thực tế có thể dùng email làm username)
        Optional<Customer> existingCustomer = findCustomerByUsername(username);
        
        if (existingCustomer.isPresent()) {
            // Cập nhật thông tin nếu cần
            Customer customer = existingCustomer.get();
            boolean updated = false;
            
            if (fullName != null && !fullName.equals(customer.getFullName())) {
                customer.setFullName(fullName);
                updated = true;
            }
            if (email != null && !email.equals(customer.getEmail())) {
                customer.setEmail(email);
                updated = true;
            }
            if (phone != null && !phone.equals(customer.getPhone())) {
                customer.setPhone(phone);
                updated = true;
            }
            
            if (updated) {
                return customerRepo.save(customer);
            }
            return customer;
        }

        // Tạo customer mới
        Customer newCustomer = Customer.builder()
                .fullName(fullName)
                .email(email)
                .phone(phone)
                .createdAt(LocalDateTime.now())
                .build();

        return customerRepo.save(newCustomer);
    }

    /**
     * Tìm customer theo username
     * Trong thực tế có thể dùng email làm username hoặc có bảng riêng cho user accounts
     */
    private Optional<Customer> findCustomerByUsername(String username) {
        // Giả định username = email hoặc có logic mapping khác
        // Có thể tìm theo email hoặc phone tùy logic business
        return customerRepo.findByEmail(username)
                .or(() -> customerRepo.findByPhone(username))
                .or(() -> {
                    // Fallback: tìm theo pattern username
                    if (username.contains("@")) {
                        return customerRepo.findByEmail(username);
                    }
                    return Optional.empty();
                });
    }

    /**
     * Thống kê cho customer
     */
    public CustomerStats getCustomerStats(String username) {
        List<LoanApplication> apps = getApplicationsByCustomer(username);
        
        long totalApplications = apps.size();
        long approved = apps.stream().mapToLong(a -> a.getStatus() == LoanStatus.APPROVED ? 1 : 0).sum();
        long disbursed = apps.stream().mapToLong(a -> a.getStatus() == LoanStatus.DISBURSED ? 1 : 0).sum();
        long rejected = apps.stream().mapToLong(a -> a.getStatus() == LoanStatus.REJECTED ? 1 : 0).sum();
        
        BigDecimal totalBorrowed = apps.stream()
                .filter(a -> a.getStatus() == LoanStatus.DISBURSED)
                .map(LoanApplication::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CustomerStats(totalApplications, approved, disbursed, rejected, totalBorrowed);
    }

    public static class CustomerStats {
        public final long totalApplications;
        public final long approved;
        public final long disbursed;
        public final long rejected;
        public final BigDecimal totalBorrowed;

        public CustomerStats(long totalApplications, long approved, long disbursed, long rejected, BigDecimal totalBorrowed) {
            this.totalApplications = totalApplications;
            this.approved = approved;
            this.disbursed = disbursed;
            this.rejected = rejected;
            this.totalBorrowed = totalBorrowed;
        }
    }
}