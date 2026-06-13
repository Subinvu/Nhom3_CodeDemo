package com.rfm.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "DonHang")
public class DonHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_dh")
    private Integer maDh;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_kh", nullable = false)
    private KhachHang khachHang;

    @Column(name = "ngay_dat")
    private LocalDateTime ngayDat;

    @Column(name = "tong_tien")
    private Double tongTien = 0.0;

    @OneToMany(mappedBy = "donHang", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ChiTietDonHang> chiTietList = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (ngayDat == null) {
            ngayDat = LocalDateTime.now();
        }
    }

    public Integer getMaDh() {
        return maDh;
    }

    public void setMaDh(Integer maDh) {
        this.maDh = maDh;
    }

    public KhachHang getKhachHang() {
        return khachHang;
    }

    public void setKhachHang(KhachHang khachHang) {
        this.khachHang = khachHang;
    }

    public LocalDateTime getNgayDat() {
        return ngayDat;
    }

    public void setNgayDat(LocalDateTime ngayDat) {
        this.ngayDat = ngayDat;
    }

    public Double getTongTien() {
        return tongTien;
    }

    public void setTongTien(Double tongTien) {
        this.tongTien = tongTien;
    }

    public List<ChiTietDonHang> getChiTietList() {
        return chiTietList;
    }

    public void setChiTietList(List<ChiTietDonHang> chiTietList) {
        this.chiTietList = chiTietList;
    }

    public DonHang() {
    }

    public DonHang(Integer maDh, KhachHang khachHang, LocalDateTime ngayDat, Double tongTien) {
        this.maDh = maDh;
        this.khachHang = khachHang;
        this.ngayDat = ngayDat;
        this.tongTien = tongTien;
    }
}
