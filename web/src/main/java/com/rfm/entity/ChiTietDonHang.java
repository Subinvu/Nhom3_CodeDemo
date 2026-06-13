package com.rfm.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ChiTietDonHang")
public class ChiTietDonHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_ctdh")
    private Integer maCtdh;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_dh", nullable = false)
    private DonHang donHang;

    @Column(name = "ten_sp", nullable = false, length = 200)
    private String tenSp;

    @Column(name = "so_luong", nullable = false)
    private Integer soLuong;

    @Column(name = "don_gia", nullable = false)
    private Double donGia;

    /**
     * Tổng tiền = số lượng × đơn giá.
     * Được tính bằng cột GENERATED trong MySQL,
     * ánh xạ ở đây chỉ để đọc, không ghi.
     */
    @Column(name = "thanh_tien", insertable = false, updatable = false)
    private Double thanhTien;

    public Integer getMaCtdh() {
        return maCtdh;
    }

    public void setMaCtdh(Integer maCtdh) {
        this.maCtdh = maCtdh;
    }

    public DonHang getDonHang() {
        return donHang;
    }

    public void setDonHang(DonHang donHang) {
        this.donHang = donHang;
    }

    public String getTenSp() {
        return tenSp;
    }

    public void setTenSp(String tenSp) {
        this.tenSp = tenSp;
    }

    public Integer getSoLuong() {
        return soLuong;
    }

    public void setSoLuong(Integer soLuong) {
        this.soLuong = soLuong;
    }

    public Double getDonGia() {
        return donGia;
    }

    public void setDonGia(Double donGia) {
        this.donGia = donGia;
    }

    public Double getThanhTien() {
        return thanhTien;
    }

    public void setThanhTien(Double thanhTien) {
        this.thanhTien = thanhTien;
    }

    public ChiTietDonHang() {
    }

    public ChiTietDonHang(Integer maCtdh, DonHang donHang, String tenSp, Integer soLuong, Double donGia, Double thanhTien) {
        this.maCtdh = maCtdh;
        this.donHang = donHang;
        this.tenSp = tenSp;
        this.soLuong = soLuong;
        this.donGia = donGia;
        this.thanhTien = thanhTien;
    }
}
