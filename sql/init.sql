-- ============================================================
-- HỆ THỐNG PHÂN CỤM KHÁCH HÀNG RFM & K-MEANS
-- Database: rfm_clustering
-- ============================================================

CREATE DATABASE IF NOT EXISTS rfm_clustering
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE rfm_clustering;

CREATE TABLE IF NOT EXISTS TaiKhoan (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS KhachHang (
    ma_kh           INT AUTO_INCREMENT PRIMARY KEY,
    ten_kh          VARCHAR(100)    NOT NULL,
    email           VARCHAR(150)    UNIQUE,
    sdt             VARCHAR(20),
    recency         DOUBLE          NOT NULL DEFAULT 0,
    frequency       INT             NOT NULL DEFAULT 0,
    monetary        DOUBLE          NOT NULL DEFAULT 0,
    cluster_id      INT             DEFAULT NULL,
    cluster_name    VARCHAR(50)     DEFAULT NULL,
    ngay_tao        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ngay_capnhat    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_cluster (cluster_id),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS DonHang (
    ma_dh           INT AUTO_INCREMENT PRIMARY KEY,
    ma_kh           INT             NOT NULL,
    ngay_dat        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tong_tien       DOUBLE          NOT NULL DEFAULT 0,
    CONSTRAINT fk_donhang_khachhang
        FOREIGN KEY (ma_kh) REFERENCES KhachHang(ma_kh)
        ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX idx_ma_kh (ma_kh),
    INDEX idx_ngay_dat (ngay_dat)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ChiTietDonHang (
    ma_ctdh         INT AUTO_INCREMENT PRIMARY KEY,
    ma_dh           INT             NOT NULL,
    ten_sp          VARCHAR(200)    NOT NULL,
    so_luong        INT             NOT NULL DEFAULT 1 CHECK (so_luong > 0),
    don_gia         DOUBLE          NOT NULL CHECK (don_gia >= 0),
    thanh_tien      DOUBLE          GENERATED ALWAYS AS (so_luong * don_gia) STORED,
    CONSTRAINT fk_chitiet_donhang
        FOREIGN KEY (ma_dh) REFERENCES DonHang(ma_dh)
        ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX idx_ma_dh (ma_dh)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- DỮ LIỆU MẪU — 16 khách hàng phân bổ đều 4 cụm
-- Dựa trên centroid thực tế của mô hình đã train:
--   VIP:          R~8,   F~11,  M~4257
--   Thường xuyên: R~55,  F~4,   M~1406
--   Mới:          R~14,  F~2,   M~440
--   Rời bỏ:       R~151, F~1.3, M~267
-- ============================================================

INSERT INTO KhachHang (ten_kh, email, sdt) VALUES
    ('Nguyễn Văn An',    'nguyenvanan1@gmail.com',     '0901000001'),
    ('Trần Thị Bình',    'tranthibinh2@gmail.com',     '0901000002'),
    ('Lê Văn Cường',     'levancuong3@gmail.com',      '0901000003'),
    ('Phạm Thị Dung',    'phamthidung4@gmail.com',     '0901000004'),
    ('Hoàng Văn Em',     'hoangvanem5@gmail.com',      '0901000005'),
    ('Đỗ Minh Phát',     'dominhphat6@gmail.com',      '0901000006'),
    ('Ngô Tấn Tài',      'ngotantai7@gmail.com',       '0901000007'),
    ('Lương Thị Mai',    'luongthimai8@gmail.com',     '0901000008'),
    ('Đặng Quang Vinh',  'dangquangvinh9@gmail.com',   '0901000009'),
    ('Bùi Văn Nam',      'buivannam10@gmail.com',      '0901000010'),
    ('Vũ Thị Yến',       'vuthiyen11@gmail.com',       '0901000011'),
    ('Phan Nhật Hùng',   'phannhathung12@gmail.com',   '0901000012'),
    ('Lý Thu Trang',     'lythutrang13@gmail.com',     '0901000013'),
    ('Châu Bảo Ngọc',   'chaubaongoc14@gmail.com',    '0901000014'),
    ('Hồ Bích Thủy',    'hobichthuy15@gmail.com',     '0901000015'),
    ('Trịnh Đức Long',  'trinhduclong16@gmail.com',   '0901000016');

INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (1, DATE_SUB(NOW(), INTERVAL 4 DAY), 422.47);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (1, 'Sổ tay bìa da', 1, 422.47);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (1, DATE_SUB(NOW(), INTERVAL 9 DAY), 1262.82);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (2, 'Đồng hồ treo tường', 3, 420.94);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (1, DATE_SUB(NOW(), INTERVAL 12 DAY), 377.15);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (3, 'Cốc gốm thủ công', 1, 377.15);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (1, DATE_SUB(NOW(), INTERVAL 16 DAY), 796.77);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (4, 'Đèn ngủ LED', 3, 265.59);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (1, DATE_SUB(NOW(), INTERVAL 16 DAY), 1105.11);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (5, 'Đồng hồ treo tường', 3, 368.37);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (1, DATE_SUB(NOW(), INTERVAL 49 DAY), 798.39);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (6, 'Khung ảnh gỗ', 3, 266.13);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (1, DATE_SUB(NOW(), INTERVAL 22 DAY), 427.64);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (7, 'Ví da nam', 1, 427.64);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (1, DATE_SUB(NOW(), INTERVAL 60 DAY), 283.36);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (8, 'Bình giữ nhiệt', 1, 283.36);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (1, DATE_SUB(NOW(), INTERVAL 36 DAY), 227.82);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (9, 'Bình giữ nhiệt', 1, 227.82);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (1, DATE_SUB(NOW(), INTERVAL 76 DAY), 381.12);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (10, 'Khăn lụa', 1, 381.12);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (1, DATE_SUB(NOW(), INTERVAL 114 DAY), 474.9);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (11, 'Túi canvas tote', 2, 237.45);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (1, DATE_SUB(NOW(), INTERVAL 125 DAY), 863.85);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (12, 'Đèn ngủ LED', 3, 287.95);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (1, DATE_SUB(NOW(), INTERVAL 100 DAY), 1119.63);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (13, 'Túi canvas tote', 3, 373.21);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (2, DATE_SUB(NOW(), INTERVAL 6 DAY), 495.57);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (14, 'Túi canvas tote', 1, 495.57);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (2, DATE_SUB(NOW(), INTERVAL 15 DAY), 850.17);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (15, 'Bình giữ nhiệt', 3, 283.39);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (2, DATE_SUB(NOW(), INTERVAL 16 DAY), 311.05);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (16, 'Khung ảnh gỗ', 1, 311.05);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (2, DATE_SUB(NOW(), INTERVAL 18 DAY), 382.74);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (17, 'Đồng hồ treo tường', 1, 382.74);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (2, DATE_SUB(NOW(), INTERVAL 30 DAY), 498.04);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (18, 'Khung ảnh gỗ', 2, 249.02);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (2, DATE_SUB(NOW(), INTERVAL 61 DAY), 531.76);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (19, 'Cốc gốm thủ công', 2, 265.88);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (2, DATE_SUB(NOW(), INTERVAL 42 DAY), 893.08);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (20, 'Ví da nam', 2, 446.54);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (2, DATE_SUB(NOW(), INTERVAL 55 DAY), 659.58);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (21, 'Bình giữ nhiệt', 3, 219.86);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (3, DATE_SUB(NOW(), INTERVAL 10 DAY), 1396.2);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (22, 'Khăn lụa', 3, 465.4);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (3, DATE_SUB(NOW(), INTERVAL 15 DAY), 279.46);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (23, 'Đồng hồ treo tường', 1, 279.46);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (3, DATE_SUB(NOW(), INTERVAL 32 DAY), 836.46);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (24, 'Ví da nam', 3, 278.82);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (3, DATE_SUB(NOW(), INTERVAL 46 DAY), 319.82);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (25, 'Nến thơm sáp ong', 1, 319.82);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (3, DATE_SUB(NOW(), INTERVAL 54 DAY), 348.05);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (26, 'Túi canvas tote', 1, 348.05);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (3, DATE_SUB(NOW(), INTERVAL 35 DAY), 1164.69);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (27, 'Ví da nam', 3, 388.23);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (3, DATE_SUB(NOW(), INTERVAL 82 DAY), 438.12);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (28, 'Đèn ngủ LED', 2, 219.06);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (3, DATE_SUB(NOW(), INTERVAL 80 DAY), 1076.19);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (29, 'Cốc gốm thủ công', 3, 358.73);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (3, DATE_SUB(NOW(), INTERVAL 42 DAY), 1213.53);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (30, 'Khung ảnh gỗ', 3, 404.51);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (4, DATE_SUB(NOW(), INTERVAL 8 DAY), 288.05);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (31, 'Khăn lụa', 1, 288.05);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (4, DATE_SUB(NOW(), INTERVAL 11 DAY), 1458.42);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (32, 'Khung ảnh gỗ', 3, 486.14);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (4, DATE_SUB(NOW(), INTERVAL 30 DAY), 1285.77);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (33, 'Túi canvas tote', 3, 428.59);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (4, DATE_SUB(NOW(), INTERVAL 29 DAY), 1357.5);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (34, 'Đèn ngủ LED', 3, 452.5);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (4, DATE_SUB(NOW(), INTERVAL 32 DAY), 245.85);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (35, 'Đồng hồ treo tường', 1, 245.85);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (4, DATE_SUB(NOW(), INTERVAL 63 DAY), 1426.65);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (36, 'Bình giữ nhiệt', 3, 475.55);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (4, DATE_SUB(NOW(), INTERVAL 68 DAY), 411.68);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (37, 'Khung ảnh gỗ', 2, 205.84);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (4, DATE_SUB(NOW(), INTERVAL 50 DAY), 652.14);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (38, 'Túi canvas tote', 3, 217.38);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (4, DATE_SUB(NOW(), INTERVAL 40 DAY), 419.57);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (39, 'Đồng hồ treo tường', 1, 419.57);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (4, DATE_SUB(NOW(), INTERVAL 53 DAY), 477.04);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (40, 'Đồng hồ treo tường', 2, 238.52);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (4, DATE_SUB(NOW(), INTERVAL 58 DAY), 838.56);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (41, 'Ví da nam', 3, 279.52);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (4, DATE_SUB(NOW(), INTERVAL 74 DAY), 1436.01);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (42, 'Sổ tay bìa da', 3, 478.67);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (4, DATE_SUB(NOW(), INTERVAL 92 DAY), 959.1);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (43, 'Bình giữ nhiệt', 3, 319.7);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (4, DATE_SUB(NOW(), INTERVAL 138 DAY), 939.76);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (44, 'Túi canvas tote', 2, 469.88);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (5, DATE_SUB(NOW(), INTERVAL 44 DAY), 234.52);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (45, 'Đèn ngủ LED', 1, 234.52);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (5, DATE_SUB(NOW(), INTERVAL 66 DAY), 151.8);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (46, 'Sổ tay bìa da', 1, 151.8);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (5, DATE_SUB(NOW(), INTERVAL 78 DAY), 752.72);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (47, 'Túi canvas tote', 2, 376.36);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (5, DATE_SUB(NOW(), INTERVAL 137 DAY), 419.0);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (48, 'Sổ tay bìa da', 2, 209.5);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (6, DATE_SUB(NOW(), INTERVAL 66 DAY), 536.34);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (49, 'Ví da nam', 2, 268.17);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (6, DATE_SUB(NOW(), INTERVAL 87 DAY), 347.16);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (50, 'Bình giữ nhiệt', 2, 173.58);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (6, DATE_SUB(NOW(), INTERVAL 122 DAY), 252.78);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (51, 'Túi canvas tote', 1, 252.78);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (6, DATE_SUB(NOW(), INTERVAL 114 DAY), 501.32);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (52, 'Túi canvas tote', 2, 250.66);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (7, DATE_SUB(NOW(), INTERVAL 42 DAY), 284.07);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (53, 'Ví da nam', 1, 284.07);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (7, DATE_SUB(NOW(), INTERVAL 62 DAY), 219.64);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (54, 'Túi canvas tote', 1, 219.64);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (7, DATE_SUB(NOW(), INTERVAL 100 DAY), 352.01);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (55, 'Cốc gốm thủ công', 1, 352.01);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (7, DATE_SUB(NOW(), INTERVAL 147 DAY), 399.82);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (56, 'Túi canvas tote', 1, 399.82);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (8, DATE_SUB(NOW(), INTERVAL 40 DAY), 271.41);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (57, 'Ví da nam', 1, 271.41);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (8, DATE_SUB(NOW(), INTERVAL 56 DAY), 191.16);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (58, 'Ví da nam', 1, 191.16);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (8, DATE_SUB(NOW(), INTERVAL 86 DAY), 763.26);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (59, 'Khung ảnh gỗ', 2, 381.63);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (8, DATE_SUB(NOW(), INTERVAL 124 DAY), 648.3);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (60, 'Nến thơm sáp ong', 2, 324.15);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (9, DATE_SUB(NOW(), INTERVAL 12 DAY), 146.25);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (61, 'Đèn ngủ LED', 1, 146.25);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (10, DATE_SUB(NOW(), INTERVAL 13 DAY), 36.02);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (62, 'Khăn lụa', 1, 36.02);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (11, DATE_SUB(NOW(), INTERVAL 4 DAY), 39.61);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (63, 'Nến thơm sáp ong', 1, 39.61);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (12, DATE_SUB(NOW(), INTERVAL 5 DAY), 133.42);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (64, 'Ví da nam', 1, 133.42);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (13, DATE_SUB(NOW(), INTERVAL 265 DAY), 66.31);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (65, 'Cốc gốm thủ công', 1, 66.31);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (14, DATE_SUB(NOW(), INTERVAL 227 DAY), 66.69);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (66, 'Đồng hồ treo tường', 1, 66.69);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (15, DATE_SUB(NOW(), INTERVAL 186 DAY), 73.58);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (67, 'Bình giữ nhiệt', 1, 73.58);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (15, DATE_SUB(NOW(), INTERVAL 223 DAY), 41.25);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (68, 'Nến thơm sáp ong', 1, 41.25);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (16, DATE_SUB(NOW(), INTERVAL 237 DAY), 94.32);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (69, 'Túi canvas tote', 1, 94.32);
INSERT INTO DonHang (ma_kh, ngay_dat, tong_tien) VALUES (16, DATE_SUB(NOW(), INTERVAL 267 DAY), 56.66);
INSERT INTO ChiTietDonHang (ma_dh, ten_sp, so_luong, don_gia) VALUES (70, 'Đèn ngủ LED', 1, 56.66);

-- Cập nhật RFM cho dữ liệu mẫu
SET SQL_SAFE_UPDATES = 0;

UPDATE KhachHang kh
SET
    recency  = (SELECT DATEDIFF(NOW(), MAX(dh.ngay_dat))
                FROM DonHang dh WHERE dh.ma_kh = kh.ma_kh),
    frequency = (SELECT COUNT(dh.ma_dh)
                 FROM DonHang dh WHERE dh.ma_kh = kh.ma_kh),
    monetary  = (SELECT IFNULL(SUM(dh.tong_tien), 0)
                 FROM DonHang dh WHERE dh.ma_kh = kh.ma_kh);

SET SQL_SAFE_UPDATES = 1;

-- VIEWS hỗ trợ Dashboard
CREATE OR REPLACE VIEW v_dashboard_stats AS
SELECT
    (SELECT COUNT(*) FROM KhachHang)                        AS tong_khach_hang,
    (SELECT COUNT(*) FROM DonHang)                          AS tong_don_hang,
    (SELECT IFNULL(SUM(tong_tien), 0) FROM DonHang)         AS tong_doanh_thu,
    (SELECT COUNT(DISTINCT cluster_id) FROM KhachHang
     WHERE cluster_id IS NOT NULL)                          AS so_cum;

CREATE OR REPLACE VIEW v_cluster_distribution AS
SELECT
    cluster_id,
    cluster_name,
    COUNT(*) AS so_luong,
    ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM KhachHang WHERE cluster_id IS NOT NULL), 2) AS ty_le
FROM KhachHang
WHERE cluster_id IS NOT NULL
GROUP BY cluster_id, cluster_name
ORDER BY cluster_id;

CREATE OR REPLACE VIEW v_rfm_by_cluster AS
SELECT
    cluster_id,
    cluster_name,
    ROUND(AVG(recency), 2)   AS avg_recency,
    ROUND(AVG(frequency), 2) AS avg_frequency,
    ROUND(AVG(monetary), 2)  AS avg_monetary,
    COUNT(*)                 AS so_luong
FROM KhachHang
WHERE cluster_id IS NOT NULL
GROUP BY cluster_id, cluster_name
ORDER BY cluster_id;

SELECT "Database rfm_clustering khởi tạo thành công!" AS status;
SELECT TABLE_NAME, TABLE_ROWS FROM information_schema.TABLES
WHERE TABLE_SCHEMA = "rfm_clustering" AND TABLE_TYPE = "BASE TABLE";

-- Thêm dữ liệu tài khoản mẫu
INSERT IGNORE INTO TaiKhoan (username, password, role) VALUES 
('admin', '{noop}admin', 'ADMIN'),
('cashier', '{noop}cashier', 'CASHIER');
