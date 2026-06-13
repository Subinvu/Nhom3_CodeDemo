package com.rfm.repository;

import com.rfm.entity.DonHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DonHangRepository extends JpaRepository<DonHang, Integer> {

    List<DonHang> findByKhachHang_MaKhOrderByNgayDatDesc(Integer maKh);

    // Tổng doanh thu
    @Query("SELECT COALESCE(SUM(d.tongTien), 0) FROM DonHang d")
    Double sumTongTien();

    // Ngày đặt hàng cuối của một khách hàng
    @Query("SELECT MAX(d.ngayDat) FROM DonHang d WHERE d.khachHang.maKh = :maKh")
    java.time.LocalDateTime findMaxNgayDatByMaKh(@Param("maKh") Integer maKh);

    // Tổng doanh thu của một khách hàng
    @Query("SELECT COALESCE(SUM(d.tongTien), 0) FROM DonHang d WHERE d.khachHang.maKh = :maKh")
    Double sumTongTienByMaKh(@Param("maKh") Integer maKh);

    // Số đơn hàng của một khách hàng
    long countByKhachHang_MaKh(Integer maKh);
}
