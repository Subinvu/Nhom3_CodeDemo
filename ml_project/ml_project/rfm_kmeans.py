import pandas as pd
import numpy as np
from sklearn.preprocessing import StandardScaler
from sklearn.cluster import KMeans
from sklearn.metrics import silhouette_score
import joblib

print("--- BẮT ĐẦU QUÁ TRÌNH HUẤN LUYỆN MÔ HÌNH ---")

# ==========================================
# 1. ĐỌC VÀ LÀM SẠCH DỮ LIỆU (DATA CLEANING)
# ==========================================
print("[1/5] Đang đọc và làm sạch dữ liệu...")

# Đọc file dữ liệu bán hàng Online Retail
df = pd.read_excel('Online Retail.xlsx')

# Chuyển đổi InvoiceNo và UnitPrice về đúng định dạng
df['InvoiceNo'] = df['InvoiceNo'].astype(str)
df['UnitPrice'] = pd.to_numeric(df['UnitPrice'], errors='coerce')

# Lọc bỏ đơn hàng bị hủy (InvoiceNo bắt đầu bằng 'C') và các giá trị <= 0
df_cleaned = df[
    (~df['InvoiceNo'].str.startswith('C', na=False)) & 
    (df['UnitPrice'] > 0) & 
    (df['Quantity'] > 0) &
    (df['CustomerID'].notna()) # Loại bỏ các dòng không có mã khách hàng
].copy()

# Tính tổng tiền cho mỗi dòng sản phẩm
df_cleaned['TotalSum'] = df_cleaned['Quantity'] * df_cleaned['UnitPrice']

# ==========================================
# 2. TÍNH TOÁN GIÁ TRỊ R - F - M
# ==========================================
print("[2/5] Đang tính toán các chỉ số Recency, Frequency, Monetary...")

# Thiết lập ngày snapshot (sau ngày cuối cùng trong dữ liệu 1 ngày)
snapshot_date = df_cleaned['InvoiceDate'].max() + pd.Timedelta(days=1)

rfm = df_cleaned.groupby('CustomerID').agg({
    'InvoiceDate': lambda x: (snapshot_date - x.max()).days, # Recency (Số ngày chưa mua lại)
    'InvoiceNo': 'nunique',                                 # Frequency (Số lần mua hàng)
    'TotalSum': 'sum'                                       # Monetary (Tổng số tiền chi tiêu)
}).reset_index()

rfm.columns = ['CustomerID', 'Recency', 'Frequency', 'Monetary']

# ==========================================
# 3. TIỀN XỬ LÝ DỮ LIỆU (KHỬ LỆCH & CHUẨN HÓA)
# ==========================================
print("[3/5] Đang áp dụng Log Transformation và Chuẩn hóa (Scale)...")

# Bước cực kỳ quan trọng: Sử dụng Log để đưa phân phối lệch về phân phối chuẩn
rfm_log = np.log1p(rfm[['Recency', 'Frequency', 'Monetary']])

# Chuẩn hóa Z-score với StandardScaler
scaler = StandardScaler()
X_scaled = scaler.fit_transform(rfm_log)

# ==========================================
# 4. TRAIN K-MEANS VÀ ĐÁNH GIÁ MÔ HÌNH
# ==========================================
print("[4/5] Đang tìm số cụm tối ưu và huấn luyện K-Means...")

# Tự động chọn số cụm dựa vào Silhouette Score trong khoảng k=2..8
best_k = 4
best_score = -1
scores = {}
K_RANGE = range(2, 9)
for k in K_RANGE:
    km = KMeans(n_clusters=k, init='k-means++', random_state=42)
    labels = km.fit_predict(X_scaled)
    try:
        if len(X_scaled) < 50000:
            s = silhouette_score(X_scaled, labels)
        else:
            s = silhouette_score(X_scaled, labels, sample_size=10000, random_state=42)
    except Exception:
        s = -1
    scores[k] = s
    if s > best_score:
        best_score = s
        best_k = k

# Huấn luyện KMeans cuối cùng với best_k
kmeans = KMeans(n_clusters=best_k, init='k-means++', random_state=42)
rfm['Cluster'] = kmeans.fit_predict(X_scaled)

print("\n" + "="*40)
print(f"Chọn số cụm tốt nhất (Silhouette): k={best_k}, Silhouette={best_score:.4f}")
print(f"Chỉ số Inertia (WCSS): {kmeans.inertia_:.2f}")
print("Chi tiết Silhouette theo k:")
for k, s in scores.items():
    print(f"  k={k}: {s:.4f}")
print("="*40)

# ✅ KHÓA CỨNG k=4 CHO PRODUCTION
# Dù Silhouette tìm ra best_k bất kỳ, hệ thống luôn dùng k=4 để:
# - Đảm bảo Dashboard hiển thị đúng 4 cụm cố định
# - Nhất quán với nhãn VIP / Thường xuyên / Mới / Rời bỏ
# - Phù hợp thiết kế hệ thống CRM thực tế
PRODUCTION_K = 4
if best_k != PRODUCTION_K:
    print(f"\n[INFO] Silhouette chọn k={best_k}, nhưng hệ thống production dùng k={PRODUCTION_K} (cố định).")
    kmeans = KMeans(n_clusters=PRODUCTION_K, init='k-means++', random_state=42)
    rfm['Cluster'] = kmeans.fit_predict(X_scaled)
else:
    print(f"\n[INFO] Silhouette xác nhận k={PRODUCTION_K} là tối ưu — không cần override.")
print("="*40 + "\n")

# ==========================================
# 5. ĐỊNH DANH CỤM ĐỘNG (DYNAMIC LABELING)
# ==========================================
# ==========================================
# 5. ĐỊNH DANH CỤM ĐỘNG (DYNAMIC LABELING)
# ==========================================
print("[5/5] Đang tự động gán nhãn cụm thông minh dựa trên bản chất dữ liệu...")

# Tính toán giá trị trung bình RFM thực tế của từng cụm
cluster_profile = rfm.groupby('Cluster').agg({
    'Recency': 'mean',
    'Frequency': 'mean',
    'Monetary': 'mean'
}).reset_index()

# Đánh hạng thứ tự dựa trên nguyên tắc: R thấp là tốt, F cao là tốt, M cao là tốt
cluster_profile['Rank_R'] = cluster_profile['Recency'].rank(ascending=False) 
cluster_profile['Rank_F'] = cluster_profile['Frequency'].rank(ascending=True) 
cluster_profile['Rank_M'] = cluster_profile['Monetary'].rank(ascending=True)  

# Tính tổng điểm xếp hạng
cluster_profile['Total_Rank'] = cluster_profile['Rank_R'] + cluster_profile['Rank_F'] + cluster_profile['Rank_M']
# Sắp xếp các cụm theo Total_Rank (cụm tốt nhất ở trên)
cluster_profile = cluster_profile.sort_values(by='Total_Rank', ascending=False).reset_index(drop=True)

# Chuẩn bị danh sách nhãn theo độ ưu tiên (tùy biến nếu số cụm khác 4)
default_labels = [
    'VIP',
    'Khách hàng thường xuyên / Tiềm năng',
    'Khách hàng mới / Cần chú ý',
    'Khách hàng rời bỏ / Nguy cơ cao'
]

label_mapping = {}
num_clusters = kmeans.n_clusters
for idx, row in cluster_profile.iterrows():
    cluster_id = int(row['Cluster'])
    if idx < len(default_labels):
        label_mapping[cluster_id] = default_labels[idx]
    else:
        label_mapping[cluster_id] = f'Nhóm {idx+1}'

# Lưu lại label_map dạng dictionary thuần để file API load dễ dàng hơn
joblib.dump(label_mapping, 'label_map.joblib')

# ==========================================
# 6. XUẤT FILE MÔ HÌNH
# ==========================================
joblib.dump(scaler, 'scaler.joblib')
joblib.dump(kmeans, 'kmeans.joblib')

print("HẤN LUYỆN THÀNH CÔNG! Đã lưu các file: 'scaler.joblib', 'kmeans.joblib', 'label_map.joblib'")