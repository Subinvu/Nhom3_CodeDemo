package com.rfm.dto;


public class DashboardStats {
    private Long   tongKhachHang;
    private Long   tongDonHang;
    private Double tongDoanhThu;
    private Integer soCum;


    public DashboardStats(Long tongKhachHang, Long tongDonHang, Double tongDoanhThu, Integer soCum) {
        this.tongKhachHang = tongKhachHang;
        this.tongDonHang = tongDonHang;
        this.tongDoanhThu = tongDoanhThu;
        this.soCum = soCum;
    }

    public DashboardStats() {
    }

    public Long getTongKhachHang() {
        return tongKhachHang;
    }

    public void setTongKhachHang(Long tongKhachHang) {
        this.tongKhachHang = tongKhachHang;
    }

    public Long getTongDonHang() {
        return tongDonHang;
    }

    public void setTongDonHang(Long tongDonHang) {
        this.tongDonHang = tongDonHang;
    }

    public Double getTongDoanhThu() {
        return tongDoanhThu;
    }

    public void setTongDoanhThu(Double tongDoanhThu) {
        this.tongDoanhThu = tongDoanhThu;
    }

    public Integer getSoCum() {
        return soCum;
    }

    public void setSoCum(Integer soCum) {
        this.soCum = soCum;
    }
}
