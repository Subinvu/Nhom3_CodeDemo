# ML Project - RFM KMeans

Short instructions (Vietnamese):

1) Huấn luyện mô hình

```bash
pip install -r requirements.txt
python rfm_kmeans.py
```

Kết quả: `scaler.joblib`, `kmeans.joblib`, `label_map.joblib` sẽ được tạo trong thư mục.

2) Chạy server Online (FastAPI)

```bash
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8000
```

3) Gọi API từ Java (ví dụ CURL)

```bash
curl -X POST "http://localhost:8000/predict" -H "Content-Type: application/json" -d '{"R":30, "F":5, "M":200}'
```

Server trả về JSON: `{"cluster_id": 1, "cluster_name": "Cụm VIP"}`
