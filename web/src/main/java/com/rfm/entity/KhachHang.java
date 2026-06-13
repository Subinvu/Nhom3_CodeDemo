package com.rfm.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "KhachHang")
public class KhachHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_kh")
    private Integer maKh;

    @Column(name = "ten_kh", nullable = false, length = 100)
    private String tenKh;

    @Column(name = "email", unique = true, length = 150)
    private String email;

    @Column(name = "sdt", length = 20)
    private String sdt;

    @Column(name = "recency")
    private Double recency = 0.0;

    @Column(name = "frequency")
    private Integer frequency = 0;

    @Column(name = "monetary")
    private Double monetary = 0.0;

    @Column(name = "cluster_id")
    private Integer clusterId;

    @Column(name = "cluster_name", length = 50)
    private String clusterName;

    @Column(name = "ngay_tao", updatable = false)
    private LocalDateTime ngayTao;

    @Column(name = "ngay_capnhat")
    private LocalDateTime ngayCapnhat;

    @OneToMany(mappedBy = "khachHang", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<DonHang> danhSachDonHang = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        ngayTao     = LocalDateTime.now();
        ngayCapnhat = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        ngayCapnhat = LocalDateTime.now();
    }


    public Integer getMaKh() {
        return maKh;
    }

    public void setMaKh(Integer maKh) {
        this.maKh = maKh;
    }

    public String getTenKh() {
        return tenKh;
    }

    public void setTenKh(String tenKh) {
        this.tenKh = tenKh;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSdt() {
        return sdt;
    }

    public void setSdt(String sdt) {
        this.sdt = sdt;
    }

    public Double getRecency() {
        return recency;
    }

    public void setRecency(Double recency) {
        this.recency = recency;
    }

    public Integer getFrequency() {
        return frequency;
    }

    public void setFrequency(Integer frequency) {
        this.frequency = frequency;
    }

    public Double getMonetary() {
        return monetary;
    }

    public void setMonetary(Double monetary) {
        this.monetary = monetary;
    }

    public Integer getClusterId() {
        return clusterId;
    }

    public void setClusterId(Integer clusterId) {
        this.clusterId = clusterId;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public LocalDateTime getNgayTao() {
        return ngayTao;
    }

    public void setNgayTao(LocalDateTime ngayTao) {
        this.ngayTao = ngayTao;
    }

    public LocalDateTime getNgayCapnhat() {
        return ngayCapnhat;
    }

    public void setNgayCapnhat(LocalDateTime ngayCapnhat) {
        this.ngayCapnhat = ngayCapnhat;
    }

    public List<DonHang> getDanhSachDonHang() {
        return danhSachDonHang;
    }

    public void setDanhSachDonHang(List<DonHang> danhSachDonHang) {
        this.danhSachDonHang = danhSachDonHang;
    }

    public KhachHang() {
    }

    public KhachHang(Integer maKh, String tenKh, String email, String sdt, Double recency, Integer frequency, Double monetary, Integer clusterId, String clusterName, LocalDateTime ngayTao, LocalDateTime ngayCapnhat) {
        this.maKh = maKh;
        this.tenKh = tenKh;
        this.email = email;
        this.sdt = sdt;
        this.recency = recency;
        this.frequency = frequency;
        this.monetary = monetary;
        this.clusterId = clusterId;
        this.clusterName = clusterName;
        this.ngayTao = ngayTao;
        this.ngayCapnhat = ngayCapnhat;
    }
}
