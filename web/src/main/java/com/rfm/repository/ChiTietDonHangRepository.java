package com.rfm.repository;

import com.rfm.entity.ChiTietDonHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChiTietDonHangRepository extends JpaRepository<ChiTietDonHang, Integer> {

    List<ChiTietDonHang> findByDonHang_MaDh(Integer maDh);
}
