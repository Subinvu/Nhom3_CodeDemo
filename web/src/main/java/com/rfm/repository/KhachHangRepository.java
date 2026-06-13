package com.rfm.repository;

import com.rfm.entity.KhachHang;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KhachHangRepository extends JpaRepository<KhachHang, Integer> {

    Optional<KhachHang> findByEmail(String email);

    boolean existsByEmail(String email);

    // Tìm kiếm theo tên hoặc email (cho Data Table)
    @Query("SELECT k FROM KhachHang k WHERE " +
           "LOWER(k.tenKh) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(k.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "k.sdt LIKE CONCAT('%', :keyword, '%')")
    Page<KhachHang> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // Lấy danh sách có phân trang
    Page<KhachHang> findAll(Pageable pageable);

    // Lấy tất cả theo cluster để xuất .md
    List<KhachHang> findByClusterIdOrderByMonetaryDesc(Integer clusterId);

    // Đếm theo cluster
    @Query("SELECT k.clusterName, COUNT(k) FROM KhachHang k WHERE k.clusterId IS NOT NULL GROUP BY k.clusterName, k.clusterId ORDER BY k.clusterId")
    List<Object[]> countByCluster();

    // RFM scatter data
    @Query("SELECT k.maKh, k.tenKh, k.recency, k.frequency, k.monetary, k.clusterId, k.clusterName FROM KhachHang k WHERE k.clusterId IS NOT NULL")
    List<Object[]> findRfmScatterData();

    // RFM avg by cluster
    @Query("SELECT k.clusterId, k.clusterName, AVG(k.recency), AVG(k.frequency), AVG(k.monetary), COUNT(k) FROM KhachHang k WHERE k.clusterId IS NOT NULL GROUP BY k.clusterId, k.clusterName ORDER BY k.clusterId")
    List<Object[]> findRfmAvgByCluster();
}
