# 📚 HƯỚNG DẪN CHẠY DỰ ÁN — Hệ Thống Phân Cụm Khách Hàng RFM & K-Means

> **Nhóm 3 — Môn Khai Phá Dữ Liệu**  
> Kiến trúc gồm 3 thành phần chạy song song:  
> `[Python ML API]` ←→ `[Spring Boot Web App]` ←→ `[MySQL Database]`

---

## 📁 Cấu Trúc Thư Mục

```
Nhom3_CodeDemo/
├── ml_project/
│   └── ml_project/
│       ├── Online Retail.xlsx      ← File dữ liệu gốc (BẮT BUỘC có)
│       ├── rfm_kmeans.py           ← Script huấn luyện mô hình K-Means
│       ├── main.py                 ← FastAPI server (AI Service)
│       ├── requirements.txt        ← Danh sách thư viện Python
│       ├── kmeans.joblib           ← File mô hình (tạo sau khi train)
│       ├── scaler.joblib           ← File scaler (tạo sau khi train)
│       └── label_map.joblib        ← File nhãn cụm (tạo sau khi train)
├── sql/
│   └── init.sql                    ← Script khởi tạo database MySQL
└── web/
    ├── pom.xml                     ← Cấu hình Maven (Spring Boot)
    └── src/
        └── main/
            └── resources/
                └── application.properties  ← Cấu hình kết nối DB & AI
```

---

## ✅ Yêu Cầu Cài Đặt Trước

| Công cụ | Phiên bản khuyến nghị | Mục đích |
|---|---|---|
| **Python** | 3.9 – 3.12 | Chạy mô hình ML |
| **Java JDK** | 17 trở lên | Chạy Spring Boot |
| **Maven** | 3.8+ | Build dự án Java |
| **MySQL** | 8.0+ | Cơ sở dữ liệu |
| **IDE** | IntelliJ / VS Code | (Tùy chọn) |

---

## 🚀 BƯỚC 1 — Cài Đặt & Khởi Tạo Database MySQL

### 1.1. Mở MySQL và chạy script khởi tạo

Mở **MySQL Workbench** hoặc dùng dòng lệnh:

```bash
mysql -u root -p < sql/init.sql
```

**Hoặc** mở MySQL Workbench → `File > Open SQL Script` → chọn file `sql/init.sql` → nhấn **⚡ Execute**.

Script này sẽ tự động:
- Tạo database `rfm_clustering`
- Tạo các bảng: `TaiKhoan`, `KhachHang`, `DonHang`, `ChiTietDonHang`
- Nhập 16 khách hàng mẫu phân bổ đều 4 cụm
- Tạo các VIEW hỗ trợ dashboard
- Tạo tài khoản đăng nhập mẫu

### 1.2. Tài khoản đăng nhập có sẵn

| Username | Password | Vai trò |
|---|---|---|
| `admin` | `admin` | Quản trị viên |
| `cashier` | `cashier` | Thu ngân |

---

## 🤖 BƯỚC 2 — Huấn Luyện Mô Hình Python (ML Service)

> ⚠️ **Lưu ý:** Đã có sẵn các file `.joblib` trong thư mục. Nếu muốn train lại từ đầu, thực hiện bước này. Nếu không cần train lại, bỏ qua và chuyển sang Bước 3.

### 2.1. Mở terminal, di chuyển vào thư mục ML

```bash
cd ml_project/ml_project
```

### 2.2. Cài đặt các thư viện Python

```bash
pip install -r requirements.txt
```

Các thư viện được cài:
- `fastapi==0.95.1` — Web framework cho API
- `uvicorn[standard]==0.22.0` — ASGI server
- `scikit-learn==1.3.2` — Thuật toán K-Means
- `pandas==2.2.2` — Xử lý dữ liệu
- `numpy==1.26.4` — Tính toán số học
- `joblib==1.3.2` — Lưu/load mô hình
- `openpyxl==3.1.2` — Đọc file Excel

### 2.3. Chạy script huấn luyện

```bash
python rfm_kmeans.py
```

**Quá trình sẽ thực hiện:**
1. Đọc file `Online Retail.xlsx` (~500k dòng)
2. Làm sạch dữ liệu (loại đơn hủy, giá trị âm)
3. Tính toán RFM (Recency, Frequency, Monetary)
4. Áp dụng Log Transformation + StandardScaler
5. Tìm số cụm tối ưu (Silhouette Score, k = 2 → 8)
6. Huấn luyện K-Means với k = 4 (cố định cho production)
7. Tự động gán nhãn 4 cụm theo đặc điểm RFM

**Output mong đợi trên màn hình:**
```
--- BẮT ĐẦU QUÁ TRÌNH HUẤN LUYỆN MÔ HÌNH ---
[1/5] Đang đọc và làm sạch dữ liệu...
[2/5] Đang tính toán các chỉ số Recency, Frequency, Monetary...
[3/5] Đang áp dụng Log Transformation và Chuẩn hóa (Scale)...
[4/5] Đang tìm số cụm tối ưu và huấn luyện K-Means...
========================================
Chọn số cụm tốt nhất (Silhouette): k=4, Silhouette=0.xxxx
Chỉ số Inertia (WCSS): xxxxx.xx
...
[5/5] Đang tự động gán nhãn cụm thông minh...
HUẤN LUYỆN THÀNH CÔNG! Đã lưu các file: 'scaler.joblib', 'kmeans.joblib', 'label_map.joblib'
```

**3 file được tạo ra:**
- `scaler.joblib` — Bộ chuẩn hóa dữ liệu
- `kmeans.joblib` — Mô hình phân cụm đã train
- `label_map.joblib` — Bản đồ nhãn 4 cụm khách hàng

---

## ⚡ BƯỚC 3 — Khởi Động FastAPI (AI Service)

### 3.1. Chắc chắn đang trong thư mục ML

```bash
cd ml_project/ml_project
```

### 3.2. Chạy FastAPI server

```bash
uvicorn main:app --host 0.0.0.0 --port 8000
```

**Hoặc:**

```bash
python main.py
```

**Kiểm tra server đã chạy:**  
Mở trình duyệt → truy cập: [http://localhost:8000/docs](http://localhost:8000/docs)

Bạn sẽ thấy giao diện Swagger UI với endpoint `/predict`.

### 3.3. Test API thủ công (tuỳ chọn)

Dùng **PowerShell** hoặc **curl** để test:

```powershell
# PowerShell
Invoke-RestMethod -Uri "http://localhost:8000/predict" `
  -Method POST `
  -ContentType "application/json" `
  -Body '{"R": 10, "F": 8, "M": 5000}'
```

```bash
# bash / cmd
curl -X POST "http://localhost:8000/predict" \
  -H "Content-Type: application/json" \
  -d '{"R": 10, "F": 8, "M": 5000}'
```

**Kết quả trả về:**
```json
{
  "cluster_id": 0,
  "cluster_name": "Cụm VIP"
}
```

| Tham số | Ý nghĩa | Ví dụ |
|---|---|---|
| `R` (Recency) | Số ngày kể từ lần mua cuối | `10` = mua 10 ngày trước |
| `F` (Frequency) | Số lần mua hàng | `8` = đã mua 8 lần |
| `M` (Monetary) | Tổng tiền đã chi tiêu | `5000` = chi 5000 đơn vị |

---

## 🌐 BƯỚC 4 — Cấu Hình & Chạy Spring Boot Web App

### 4.1. Cập nhật cấu hình kết nối database

Mở file `web/src/main/resources/application.properties`:

```properties
# Thay đổi thông tin kết nối MySQL nếu cần
spring.datasource.url=jdbc:mysql://localhost:3306/rfm_clustering?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh
spring.datasource.username=root
spring.datasource.password=sinbu123   ← ĐỔI THÀNH MẬT KHẨU MySQL CỦA BẠN

# URL của FastAPI (giữ nguyên nếu chạy local)
ai.service.url=http://localhost:8000
```

> ⚠️ **Quan trọng:** Thay `sinbu123` bằng mật khẩu MySQL thực tế của máy bạn.

### 4.2. Build và chạy Spring Boot

Di chuyển vào thư mục `web`:

```bash
cd web
```

**Cách 1 — Dùng Maven Wrapper:**
```bash
./mvnw spring-boot:run
```

**Cách 2 — Dùng Maven đã cài:**
```bash
mvn spring-boot:run
```

**Cách 3 — Build JAR rồi chạy:**
```bash
mvn clean package -DskipTests
java -jar target/rfm-clustering-1.0.0.jar
```

**Cách 4 — Dùng IntelliJ IDEA:**  
Mở thư mục `web` → Tìm class có `@SpringBootApplication` → Nhấn **▶ Run**.

### 4.3. Truy cập ứng dụng web

Mở trình duyệt → [http://localhost:8080](http://localhost:8080)

**Đăng nhập với:**
- Username: `admin` / Password: `admin` *(Quản trị viên)*
- Username: `cashier` / Password: `cashier` *(Thu ngân)*

---

## 🔄 Thứ Tự Khởi Động (Quan Trọng!)

Để hệ thống hoạt động đúng, **khởi động theo đúng thứ tự sau**:

```
1. MySQL Database  →  2. FastAPI (port 8000)  →  3. Spring Boot (port 8080)
```

| Bước | Dịch vụ | Port | Kiểm tra |
|---|---|---|---|
| **1** | MySQL Database | 3306 | Kết nối qua Workbench |
| **2** | FastAPI (Python) | 8000 | http://localhost:8000/docs |
| **3** | Spring Boot (Java) | 8080 | http://localhost:8080 |

---

## ❓ Xử Lý Lỗi Thường Gặp

### ❌ Lỗi: `ModuleNotFoundError`
```
Nguyên nhân: Chưa cài thư viện Python
Giải pháp:   pip install -r requirements.txt
```

### ❌ Lỗi: `FileNotFoundError: Online Retail.xlsx`
```
Nguyên nhân: File dữ liệu không có hoặc chạy sai thư mục
Giải pháp:   cd ml_project/ml_project trước khi chạy python rfm_kmeans.py
```

### ❌ Lỗi: `Communications link failure` (Spring Boot)
```
Nguyên nhân: MySQL chưa chạy hoặc sai mật khẩu
Giải pháp:   Kiểm tra MySQL đang chạy + sửa password trong application.properties
```

### ❌ Lỗi: `Connection refused` khi gọi AI Service
```
Nguyên nhân: FastAPI chưa được khởi động
Giải pháp:   Chạy uvicorn main:app --host 0.0.0.0 --port 8000 trước
```

### ❌ Lỗi: `Port 8080 already in use`
```
Nguyên nhân: Cổng 8080 đang bị chiếm
Giải pháp:   Thêm server.port=8081 vào application.properties
```

### ❌ Lỗi: `Access Denied for user 'root'@'localhost'`
```
Nguyên nhân: Sai mật khẩu MySQL
Giải pháp:   Sửa spring.datasource.password trong application.properties
```

---

## 📊 Mô Tả 4 Cụm Khách Hàng

| Cụm | Tên nhãn | Đặc điểm |
|---|---|---|
| 🥇 **VIP** | Khách hàng VIP | R thấp (~8 ngày), F cao (~11 lần), M cao (~4257) |
| 🥈 **Thường xuyên** | Khách hàng thường xuyên / Tiềm năng | R ~55 ngày, F ~4 lần, M ~1406 |
| 🥉 **Mới** | Khách hàng mới / Cần chú ý | R ~14 ngày, F ~2 lần, M ~440 |
| ⚠️ **Rời bỏ** | Khách hàng rời bỏ / Nguy cơ cao | R ~151 ngày, F ~1.3 lần, M ~267 |

---

## 📝 Ghi Chú Bổ Sung

- **File `.joblib` đã có sẵn** — Không cần train lại nếu chỉ muốn demo nhanh.
- **K = 4 cố định** — Hệ thống luôn dùng 4 cụm dù Silhouette Score gợi ý số khác, đảm bảo nhất quán với dashboard.
- **Nhãn cụm được gán tự động** — Dựa trên giá trị RFM thực tế của từng cụm sau khi train.
- **Spring Boot cổng 8080**, FastAPI cổng **8000** — Không đổi cổng nếu không thay `application.properties`.
