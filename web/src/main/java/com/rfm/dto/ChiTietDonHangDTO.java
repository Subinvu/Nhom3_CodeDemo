package com.rfm.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public class ChiTietDonHangDTO {

    @NotBlank(message = "Tên sản phẩm không được trống")
    private String tenSp;

    @Min(value = 1, message = "Số lượng tối thiểu là 1")
    private Integer soLuong;

    @Positive(message = "Đơn giá phải lớn hơn 0")
    private Double donGia;

    public ChiTietDonHangDTO(String tenSp, Integer soLuong, Double donGia) {
        this.tenSp = tenSp;
        this.soLuong = soLuong;
        this.donGia = donGia;
    }

    public ChiTietDonHangDTO() {
    }

    public Double getDonGia() {
        return donGia;
    }

    public void setDonGia(Double donGia) {
        this.donGia = donGia;
    }

    public Integer getSoLuong() {
        return soLuong;
    }

    public void setSoLuong(Integer soLuong) {
        this.soLuong = soLuong;
    }

    public String getTenSp() {
        return tenSp;
    }

    public void setTenSp(String tenSp) {
        this.tenSp = tenSp;
    }
}
