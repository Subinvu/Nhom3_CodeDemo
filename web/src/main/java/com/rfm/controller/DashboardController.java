package com.rfm.controller;

import com.rfm.dto.DashboardStats;
import com.rfm.service.DashboardService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }


    /**
     * GET /api/dashboard/stats
     * Trả về 4 số liệu tổng quan cho stat cards.
     */
    @GetMapping("/stats")
    public ResponseEntity<DashboardStats> getStats() {
        return ResponseEntity.ok(dashboardService.getStats());
    }

    /**
     * GET /api/dashboard/clusters
     * Phân bổ KH theo cụm — dữ liệu cho Pie Chart.
     */
    @GetMapping("/clusters")
    public ResponseEntity<List<Map<String, Object>>> getClusters() {
        return ResponseEntity.ok(dashboardService.getClusterDistribution());
    }

    /**
     * GET /api/dashboard/rfm-scatter
     * Dữ liệu Scatter Plot: F vs M, màu theo cluster_id.
     */
    @GetMapping("/rfm-scatter")
    public ResponseEntity<List<Map<String, Object>>> getScatter() {
        return ResponseEntity.ok(dashboardService.getScatterData());
    }

    /**
     * GET /api/dashboard/rfm-bar
     * RFM trung bình từng cụm — dữ liệu cho Bar Chart.
     */
    @GetMapping("/rfm-bar")
    public ResponseEntity<List<Map<String, Object>>> getBar() {
        return ResponseEntity.ok(dashboardService.getRfmByCluster());
    }

    /**
     * GET /api/dashboard/export
     * Xuất danh sách KH theo cụm dưới dạng file .md (download).
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportMarkdown() {
        String content  = dashboardService.exportMarkdown();
        byte[] bytes    = content.getBytes(StandardCharsets.UTF_8);
        String filename = "rfm_clusters_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                + ".md";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_TYPE, "text/markdown; charset=UTF-8")
                .contentLength(bytes.length)
                .body(bytes);
    }
}
