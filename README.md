# Loan Management System (Fixed)
- Spring Boot 3.3 + Thymeleaf + JPA
- Oracle XE (official image) via Docker Compose
- Idempotent SQL (không lỗi user tồn tại), build fat-jar chuẩn (không còn 'no main manifest attribute')

## Quick start
```bash
# Windows PowerShell
powershell -ExecutionPolicy Bypass -File .\run.ps1
# hoặc CMD
run.bat
```

App:
- http://localhost:8080/
- http://localhost:8080/admin (officer/officer123 hoặc admin/admin123)
