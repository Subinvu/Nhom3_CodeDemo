package com.rfm.service;

import com.rfm.dto.DashboardStats;
import com.rfm.entity.KhachHang;
import com.rfm.repository.KhachHangRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final KhachHangRepository khachHangRepository;
    private final DonHangService      donHangService;
    private final KhachHangService    khachHangService;

    public DashboardService(KhachHangRepository khachHangRepository, DonHangService      donHangService, KhachHangService    khachHangService) {
        this.khachHangRepository = khachHangRepository;
        this.donHangService = donHangService;
        this.khachHangService = khachHangService;
    }


    /** Thống kê tổng quan cho 4 stat cards. */
    public DashboardStats getStats() {
        long   tongKH       = khachHangService.count();
        long   tongDH       = donHangService.count();
        double tongDT       = donHangService.tongDoanhThu();
        List<Object[]> cum  = khachHangRepository.countByCluster();
        int    soCum        = cum.size();

        DashboardStats stats = new DashboardStats();
        stats.setTongKhachHang(tongKH);
        stats.setTongDonHang(tongDH);
        stats.setTongDoanhThu(tongDT);
        stats.setSoCum(soCum);
        return stats;
    }

    /**
     * Phân bổ khách hàng theo cụm — dùng cho Pie Chart.
     * @return List of {cluster_id, cluster_name, so_luong, ty_le}
     */
    public List<Map<String, Object>> getClusterDistribution() {
        List<Object[]> rows = khachHangRepository.countByCluster();
        long total = rows.stream().mapToLong(r -> ((Number) r[1]).longValue()).sum();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("cluster_name", row[0]);
            map.put("so_luong",     row[1]);
            map.put("ty_le", total > 0
                    ? Math.round(((Number) row[1]).doubleValue() * 100.0 / total * 100.0) / 100.0
                    : 0.0);
            result.add(map);
        }
        return result;
    }

    /**
     * Dữ liệu Scatter Plot (Frequency vs Monetary, màu theo cluster).
     * @return List of {ma_kh, ten_kh, recency, frequency, monetary, cluster_id, cluster_name}
     */
    public List<Map<String, Object>> getScatterData() {
        List<Object[]> rows = khachHangRepository.findRfmScatterData();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("ma_kh",        row[0]);
            map.put("ten_kh",       row[1]);
            map.put("recency",      row[2]);
            map.put("frequency",    row[3]);
            map.put("monetary",     row[4]);
            map.put("cluster_id",   row[5]);
            map.put("cluster_name", row[6]);
            result.add(map);
        }
        return result;
    }

    /**
     * RFM trung bình theo cụm — dùng cho Bar Chart.
     * @return List of {cluster_id, cluster_name, avg_recency, avg_frequency, avg_monetary, so_luong}
     */
    public List<Map<String, Object>> getRfmByCluster() {
        List<Object[]> rows = khachHangRepository.findRfmAvgByCluster();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("cluster_id",    row[0]);
            map.put("cluster_name",  row[1]);
            map.put("avg_recency",   row[2] != null ? Math.round(((Number)row[2]).doubleValue()*100)/100.0 : 0);
            map.put("avg_frequency", row[3] != null ? Math.round(((Number)row[3]).doubleValue()*100)/100.0 : 0);
            map.put("avg_monetary",  row[4] != null ? Math.round(((Number)row[4]).doubleValue()*100)/100.0 : 0);
            map.put("so_luong",      row[5]);
            result.add(map);
        }
        return result;
    }

    /**
     * Xuất toàn bộ danh sách khách hàng theo cụm dưới dạng Markdown.
     */
    public String exportMarkdown() {
        List<KhachHang> allKh = khachHangRepository.findAll();

        // Nhóm theo cluster
        Map<String, List<KhachHang>> grouped = new LinkedHashMap<>();
        List<KhachHang> chuaPhanCum = new ArrayList<>();

        for (KhachHang kh : allKh) {
            if (kh.getClusterName() == null) {
                chuaPhanCum.add(kh);
            } else {
                grouped.computeIfAbsent(kh.getClusterName(), k -> new ArrayList<>()).add(kh);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("# Báo Cáo Phân Cụm Khách Hàng RFM\n\n");
        sb.append("> Xuất ngày: ").append(java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))).append("\n\n");
        sb.append("---\n\n");

        // Tổng quan
        sb.append("## Tổng Quan\n\n");
        sb.append("| Cụm | Số Khách Hàng |\n");
        sb.append("|-----|---------------|\n");
        for (Map.Entry<String, List<KhachHang>> entry : grouped.entrySet()) {
            sb.append("| ").append(entry.getKey()).append(" | ")
              .append(entry.getValue().size()).append(" |\n");
        }
        if (!chuaPhanCum.isEmpty()) {
            sb.append("| Chưa phân cụm | ").append(chuaPhanCum.size()).append(" |\n");
        }
        sb.append("\n---\n\n");

        // Chi tiết từng cụm
        for (Map.Entry<String, List<KhachHang>> entry : grouped.entrySet()) {
            sb.append("## ").append(entry.getKey()).append("\n\n");
            sb.append("| Mã KH | Tên KH | Email | SĐT | R (ngày) | F (lần) | M (£) |\n");
            sb.append("|-------|--------|-------|-----|----------|---------|----------|\n");
            for (KhachHang kh : entry.getValue()) {
                sb.append("| ").append(kh.getMaKh())
                  .append(" | ").append(kh.getTenKh())
                  .append(" | ").append(kh.getEmail() != null ? kh.getEmail() : "")
                  .append(" | ").append(kh.getSdt() != null ? kh.getSdt() : "")
                  .append(" | ").append(String.format("%.0f", kh.getRecency()))
                  .append(" | ").append(kh.getFrequency())
                  .append(" | ").append(String.format("%,.0f", kh.getMonetary()))
                  .append(" |\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}
