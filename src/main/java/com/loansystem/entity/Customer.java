package com.loansystem.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Entity
@SequenceGenerator(name = "customer_seq", sequenceName = "CUSTOMER_SEQ", allocationSize = 1)
public class Customer {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "customer_seq")
    private Long id;

    @NotBlank private String fullName;
    @Email private String email;
    @NotBlank private String phone;
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getFullName() { return fullName; } public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; } public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; } public void setPhone(String phone) { this.phone = phone; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
