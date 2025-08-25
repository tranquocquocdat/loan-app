# 🏦 FLOW HỆ THỐNG QUẢN LÝ VAY TÍN DỤNG

## 📋 TỔNG QUAN HỆ THỐNG

Hệ thống được thiết kế theo mô hình **2 phần chính** với **6 vai trò người dùng** và **quy trình xử lý 4 giai đoạn**.

---

## 👥 CÁC VAI TRÒ NGƯỜI DÙNG

### 🔐 **PHẦN 1: KHÁCH HÀNG**
- **Tài khoản mẫu:**
  - `customer1/customer123`
  - `customer2/customer123`
  - `john/john123`
  - `jane/jane123`

- **Chức năng:**
  - 📝 Nộp hồ sơ vay tín dụng mới
  - 👁️ Theo dõi trạng thái hồ sơ của mình
  - 📋 Xem danh sách tất cả hồ sơ đã nộp
  - 📄 Xem hợp đồng (khi được phê duyệt)

### 🏢 **PHẦN 2: NGÂN HÀNG - 5 VAI TRÒ**

#### 👑 **1. QUẢN TRỊ VIÊN (ADMIN)**
- **Tài khoản:** `admin/admin123`
- **Quyền hạn:**
  - Truy cập tất cả các phòng ban
  - Xem dashboard tổng quan
  - Thực hiện tất cả các thao tác của mọi phòng ban
  - Theo dõi toàn bộ quy trình

#### 📥 **2. PHÒNG TIẾP NHẬN**
- **Tài khoản:** `intake/intake123`
- **Nhiệm vụ:**
  - Kiểm tra tính đầy đủ của hồ sơ
  - Tiếp nhận hồ sơ hợp lệ
  - Chuyển hồ sơ sang phòng thẩm định
- **Hồ sơ xử lý:** Trạng thái `SUBMITTED`

#### 🔍 **3. PHÒNG THẨM ĐỊNH TÍN DỤNG**
- **Tài khoản:** `assessment/assessment123`
- **Nhiệm vụ:**
  - Thẩm định tín dụng khách hàng
  - Kiểm tra blacklist (email, số điện thoại)
  - Đánh giá khả năng trả nợ
  - Đưa ra quyết định phê duyệt/từ chối
- **Hồ sơ xử lý:** Trạng thái `UNDER_REVIEW`, `ASSESSED`

#### 💰 **4. PHÒNG GIẢI NGÂN**
- **Tài khoản:** `disbursement/disbursement123`
- **Nhiệm vụ:**
  - Thực hiện giải ngân cho hồ sơ đã phê duyệt
  - Hoàn tất quy trình cho vay
- **Hồ sơ xử lý:** Trạng thái `APPROVED`, `DISBURSED`

---

## 🔄 QUY TRÌNH XỬ LÝ HỒ SƠ (4 GIAI ĐOẠN)

### **GIAI ĐOẠN 1: NỘP HỒ SƠ**
```
Khách hàng → Nộp hồ sơ → Trạng thái: SUBMITTED
```
- Khách hàng điền thông tin:
  - Thông tin cá nhân (họ tên, email, SĐT, thu nhập)
  - Thông tin khoản vay (số tiền, kỳ hạn, mục đích)
- Hệ thống tự động tạo mã hồ sơ và gửi link theo dõi

### **GIAI ĐOẠN 2: PHÒNG TIẾP NHẬN**
```
SUBMITTED → Kiểm tra → Tiếp nhận → UNDER_REVIEW
```
**Quy trình:**
1. Nhân viên tiếp nhận kiểm tra tính đầy đủ:
   - ✅ Họ tên, email, SĐT có đầy đủ không
   - ✅ Số tiền vay ≥ 1,000,000 VND
   - ✅ Kỳ hạn 3-60 tháng
   - ✅ Thu nhập > 0
2. Nếu đầy đủ → Nhấn **"Tiếp nhận hồ sơ"**
3. Hồ sơ chuyển sang `UNDER_REVIEW` và tự động chuyển phòng thẩm định

### **GIAI ĐOẠN 3: PHÒNG THẨM ĐỊNH TÍN DỤNG**
```
UNDER_REVIEW → Thẩm định → ASSESSED → Quyết định → APPROVED/REJECTED
```
**Quy trình:**
1. **Kiểm tra Blacklist:**
   - Nhấn **"Kiểm tra Blacklist"**
   - Hệ thống check email/SĐT trong danh sách đen
   - Nếu có → Tự động từ chối (`REJECTED`)
   - Nếu sạch → Tiếp tục thẩm định

2. **Hoàn tất Thẩm định:**
   - Nhấn **"Hoàn tất thẩm định"**
   - Nhập ghi chú đánh giá
   - Trạng thái chuyển thành `ASSESSED`

3. **Quyết định cuối cùng:**
   - **Phê duyệt:** Nhấn **"Phê duyệt"** → `APPROVED`
   - **Từ chối:** Nhấn **"Từ chối"** + nhập lý do → `REJECTED`

### **GIAI ĐOẠN 4: PHÒNG GIẢI NGÂN**
```
APPROVED → Giải ngân → DISBURSED
```
**Quy trình:**
1. Nhận hồ sơ đã phê duyệt
2. Nhấn **"Thực hiện giải ngân"**
3. Trạng thái chuyển thành `DISBURSED` (hoàn tất)

---

## 📊 CÁC TRẠNG THÁI HỒ SƠ

| Trạng thái | Mô tả | Phòng ban xử lý |
|------------|-------|-----------------|
| `SUBMITTED` | 📝 Đã nộp hồ sơ, chờ tiếp nhận | Phòng Tiếp Nhận |
| `UNDER_REVIEW` | 🔍 Đang thẩm định tín dụng | Phòng Thẩm Định |
| `ASSESSED` | ⚖️ Đã thẩm định xong, chờ quyết định | Phòng Thẩm Định |
| `APPROVED` | ✅ Đã phê duyệt, chờ giải ngân | Phòng Giải Ngân |
| `REJECTED` | ❌ Đã từ chối | Kết thúc |
| `DISBURSED` | 💰 Đã giải ngân hoàn tất | Kết thúc |

---

## 🖥️ GIAO DIỆN HỆ THỐNG

### **KHÁCH HÀNG:**
- `/` - Trang chủ (redirect đến apply nếu đã login)
- `/apply` - Form nộp hồ sơ
- `/my-applications` - Dashboard khách hàng
- `/customer/app/{id}` - Chi tiết hồ sơ

### **NGÂN HÀNG:**
- `/admin/dashboard` - Dashboard tổng quan (chỉ Admin)
- `/admin/intake` - Phòng Tiếp Nhận
- `/admin/assessment` - Phòng Thẩm Định
- `/admin/disbursement` - Phòng Giải Ngân
- `/admin/{section}/app/{id}` - Chi tiết hồ sơ theo phòng
- `/admin/{id}/contract` - Xem hợp đồng

---

## 🔐 PHÂN QUYỀN HỆ THỐNG

### **Quyền truy cập theo vai trò:**

| Vai trò | Dashboard | Tiếp Nhận | Thẩm Định | Giải Ngân | Khách Hàng |
|---------|-----------|-----------|-----------|-----------|------------|
| **Admin** | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Intake** | ❌ | ✅ | ❌ | ❌ | ❌ |
| **Assessment** | ❌ | ❌ | ✅ | ❌ | ❌ |
| **Disbursement** | ❌ | ❌ | ❌ | ✅ | ❌ |
| **Customer** | ❌ | ❌ | ❌ | ❌ | ✅ |

### **Thao tác theo trạng thái:**

| Trạng thái | Tiếp Nhận | Thẩm Định | Giải Ngân |
|------------|-----------|-----------|-----------|
| `SUBMITTED` | ✅ Tiếp nhận | ❌ | ❌ |
| `UNDER_REVIEW` | ❌ | ✅ Check blacklist<br>✅ Thẩm định | ❌ |
| `ASSESSED` | ❌ | ✅ Phê duyệt/Từ chối | ❌ |
| `APPROVED` | ❌ | ❌ | ✅ Giải ngân |
| `REJECTED` | ❌ | ❌ | ❌ |
| `DISBURSED` | ❌ | ❌ | ❌ |

---

## 🚀 LUỒNG ĐIỀU HƯỚNG TỰ ĐỘNG

### **Sau mỗi thao tác thành công:**
1. **Tiếp nhận hồ sơ** → Tự động chuyển đến Phòng Thẩm Định
2. **Phê duyệt hồ sơ** → Tự động chuyển đến Phòng Giải Ngân
3. **Hoàn tất thẩm định** → Ở lại để tiếp tục quyết định

### **Navigation giữa các phòng:**
- Tab navigation ở đầu trang
- Nút "Đến phòng tiếp theo" ở cuối trang chi tiết
- Breadcrumb để quay lại

---

## 📈 TÍNH NĂNG BỔ SUNG

### **Dashboard & Thống kê:**
- Số lượng hồ sơ theo trạng thái
- Workflow diagram trực quan
- Quick actions cho từng phòng ban

### **Blacklist System:**
- Kiểm tra email và số điện thoại
- Tự động từ chối nếu có trong blacklist
- Dữ liệu mẫu: `fraud@example.com`, `0900000000`

### **Loan Calculator:**
- Tính khoản trả hàng tháng (12% lãi suất/năm)
- Kiểm tra tỷ lệ thu nhập/khoản vay (khuyến khích ≤33%)
- Validation realtime

### **Error Handling:**
- Log chi tiết mọi thao tác
- Flash messages cho user feedback
- Exception handling toàn diện
- Role-based error redirection

---

## 🛠️ SETUP & DEMO

### **Chạy hệ thống:**
```bash
# Windows
run.bat

# PowerShell
run.ps1
```

### **Truy cập:**
- **Ứng dụng:** http://localhost:8080/
- **Login:** http://localhost:8080/login
- **Admin Dashboard:** http://localhost:8080/admin

### **Demo Flow hoàn chỉnh:**
1. Login `customer1/customer123` → Nộp hồ sơ
2. Login `intake/intake123` → Tiếp nhận
3. Login `assessment/assessment123` → Thẩm định → Phê duyệt
4. Login `disbursement/disbursement123` → Giải ngân
5. Quay lại `customer1` → Xem kết quả

---

## 🎯 KẾT LUẬN

Hệ thống được thiết kế theo **workflow thực tế của ngân hàng**, đảm bảo:
- **Phân quyền rõ ràng** theo vai trò
- **Quy trình chặt chẽ** không bỏ sót bước nào
- **Trách nhiệm phân định** rõ ràng giữa các phòng ban
- **Audit trail** đầy đủ với timestamp và log
- **User experience** tốt với navigation tự động
- **Error handling** toàn diện với thông báo chi tiết