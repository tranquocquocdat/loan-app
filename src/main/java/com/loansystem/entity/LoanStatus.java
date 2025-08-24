package com.loansystem.entity;

public enum LoanStatus {
    SUBMITTED,      // Nộp hồ sơ
    UNDER_REVIEW,   // B1: Tiếp nhận & kiểm tra
    ASSESSED,       // B2: Thẩm định hoàn tất (chưa quyết định)
    APPROVED,       // B3: Phê duyệt
    REJECTED,       // B3: Từ chối
    DISBURSED       // B4: Giải ngân
}
