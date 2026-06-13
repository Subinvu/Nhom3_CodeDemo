package com.rfm.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rfm.dto.DonHangRequest;
import com.rfm.entity.DonHang;
import com.rfm.service.DonHangService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/donhang")
public class DonHangController {
    private static final Logger log = LoggerFactory.getLogger(DonHangController.class);


    private final DonHangService donHangService;

    public DonHangController(DonHangService donHangService) {
        this.donHangService = donHangService;
    }


    /**
     * POST /api/donhang
     * Tạo đơn hàng mới → tính RFM → gọi AI predict → lưu cluster.
     *
     * Request body:
     * {
     *   "tenKh": "Nguyễn Văn A",
     *   "email": "a@gmail.com",
     *   "sdt": "0901234567",
     *   "chiTietList": [
     *     {"tenSp": "Áo thun", "soLuong": 2, "donGia": 150000}
     *   ]
     * }
     */
    @PostMapping
    public ResponseEntity<?> createDonHang(@Valid @RequestBody DonHangRequest request) {
        try {
            DonHang donHang = donHangService.createDonHang(request);
            KhachHangResult result = buildResult(donHang);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            log.error("Lỗi tạo đơn hàng: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("message", "Lỗi xử lý đơn hàng: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * GET /api/donhang/khachhang/{maKh}
     * Lịch sử đơn hàng của một khách hàng.
     */
    @GetMapping("/khachhang/{maKh}")
    public ResponseEntity<List<DonHangSummaryDto>> getByKhachHang(@PathVariable Integer maKh) {
        List<DonHangSummaryDto> list = donHangService.findByKhachHang(maKh).stream()
                .map(this::toSummaryDto)
                .toList();
        return ResponseEntity.ok(list);
    }

    /**
     * GET /api/donhang/all
     * Lấy toàn bộ danh sách đơn hàng (hỗ trợ phân trang)
     */
    @GetMapping("/all")
    public ResponseEntity<org.springframework.data.domain.Page<DonHangSummaryDto>> getAllDonHang(org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(donHangService.findAll(pageable).map(this::toSummaryDto));
    }

    /**
     * GET /api/donhang/details/{maDh}
     * Lấy chi tiết một đơn hàng cụ thể
     */
    @GetMapping("/details/{maDh}")
    public ResponseEntity<DonHangDetailDto> getDonHangDetails(@PathVariable Integer maDh) {
        return donHangService.findById(maDh)
                .map(donHang -> ResponseEntity.ok(toDetailDto(donHang)))
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Helper: mapping DTOs ──────────────────────────

    private DonHangSummaryDto toSummaryDto(DonHang dh) {
        String tenKh = dh.getKhachHang() != null ? dh.getKhachHang().getTenKh() : null;
        return new DonHangSummaryDto(dh.getMaDh(), tenKh, dh.getNgayDat(), dh.getTongTien());
    }

    private DonHangDetailDto toDetailDto(DonHang dh) {
        String tenKh = dh.getKhachHang() != null ? dh.getKhachHang().getTenKh() : null;
        List<ChiTietDto> chiTiet = dh.getChiTietList().stream()
                .map(ct -> new ChiTietDto(ct.getTenSp(), ct.getSoLuong(), ct.getDonGia(), ct.getThanhTien()))
                .toList();
        return new DonHangDetailDto(dh.getMaDh(), tenKh, dh.getNgayDat(), dh.getTongTien(), chiTiet);
    }

    // ── Helper: build response object ──────────────────────────
    private KhachHangResult buildResult(DonHang donHang) {
        var kh = donHang.getKhachHang();
        return new KhachHangResult(
                donHang.getMaDh(),
                kh.getMaKh(),
                kh.getTenKh(),
                kh.getEmail(),
                donHang.getTongTien(),
                kh.getRecency(),
                kh.getFrequency(),
                kh.getMonetary(),
                kh.getClusterId(),
                kh.getClusterName()
        );
    }

    // ── Response record ─────────────────────────────────────────
    public record KhachHangResult(
            Integer maDh,
            Integer maKh,
            String  tenKh,
            String  email,
            Double  tongTienDonHang,
            Double  recency,
            Integer frequency,
            Double  monetary,
            Integer clusterId,
            String  clusterName
    ) {}

    public record DonHangSummaryDto(
            Integer maDh,
            String tenKh,
            java.time.LocalDateTime ngayDat,
            Double tongTien
    ) {}

    public record DonHangDetailDto(
            Integer maDh,
            String tenKh,
            java.time.LocalDateTime ngayDat,
            Double tongTien,
            List<ChiTietDto> chiTietList
    ) {}

    public record ChiTietDto(
            String tenSp,
            Integer soLuong,
            Double donGia,
            Double thanhTien
    ) {}
}
