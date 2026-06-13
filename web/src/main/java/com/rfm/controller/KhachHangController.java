package com.rfm.controller;

import com.rfm.entity.KhachHang;
import com.rfm.service.KhachHangService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/khachhang")
public class KhachHangController {

    private final KhachHangService khachHangService;

    public KhachHangController(KhachHangService khachHangService) {
        this.khachHangService = khachHangService;
    }


    /**
     * GET /api/khachhang?page=0&size=10&sort=maKh&q=keyword
     * Lấy danh sách khách hàng có phân trang + tìm kiếm.
     */
    @GetMapping
    public ResponseEntity<Page<KhachHang>> getAll(
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "10")  int size,
            @RequestParam(defaultValue = "maKh") String sort,
            @RequestParam(required = false)     String q) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sort).descending());
        Page<KhachHang> result = (q != null && !q.isBlank())
                ? khachHangService.search(q, pageable)
                : khachHangService.findAll(pageable);

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/khachhang/{id}
     * Lấy thông tin chi tiết 1 khách hàng.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Integer id) {
        return khachHangService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/khachhang/search?q=keyword
     * Tìm kiếm nhanh (dùng cho autocomplete).
     */
    @GetMapping("/search")
    public ResponseEntity<Page<KhachHang>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(khachHangService.search(q, pageable));
    }

    /**
     * DELETE /api/khachhang/{id}
     * Xóa khách hàng.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCustomer(@PathVariable Integer id) {
        khachHangService.deleteById(id);
        return ResponseEntity.ok().build();
    }

    /**
     * PUT /api/khachhang/{id}
     * Sửa thông tin khách hàng.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCustomer(@PathVariable Integer id, @RequestBody Map<String, String> payload) {
        String tenKh = payload.get("tenKh");
        String email = payload.get("email");
        String sdt = payload.get("sdt");
        return ResponseEntity.ok(khachHangService.updateKhachHang(id, tenKh, email, sdt));
    }

    /**
     * POST /api/khachhang/sync-clusters
     * Kích hoạt phân cụm cho các KH chưa phân cụm.
     */
    @PostMapping("/sync-clusters")
    public ResponseEntity<?> syncClusters() {
        int count = khachHangService.syncClusters();
        return ResponseEntity.ok(Map.of("message", "Đã phân cụm thành công " + count + " khách hàng."));
    }
}
