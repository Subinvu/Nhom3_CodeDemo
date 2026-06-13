package com.rfm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public class DonHangRequest {

    @NotBlank(message = "Tên khách hàng không được trống")
    @Size(max = 100)
    private String tenKh;

    @Size(max = 150)
    private String email;

    @Size(max = 20)
    private String sdt;

    @NotEmpty(message = "Giỏ hàng không được rỗng")
    @Valid
    private List<ChiTietDonHangDTO> chiTietList;


    public DonHangRequest(String tenKh, String email, String sdt) {
        this.tenKh = tenKh;
        this.email = email;
        this.sdt = sdt;
    }

    public DonHangRequest() {
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

    public List<ChiTietDonHangDTO> getChiTietList() {
        return chiTietList;
    }

    public void setChiTietList(List<ChiTietDonHangDTO> chiTietList) {
        this.chiTietList = chiTietList;
    }
}
