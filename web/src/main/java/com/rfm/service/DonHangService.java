package com.rfm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rfm.dto.ChiTietDonHangDTO;
import com.rfm.dto.DonHangRequest;
import com.rfm.dto.PredictResponse;
import com.rfm.entity.ChiTietDonHang;
import com.rfm.entity.DonHang;
import com.rfm.entity.KhachHang;
import com.rfm.repository.ChiTietDonHangRepository;
import com.rfm.repository.DonHangRepository;
import com.rfm.repository.KhachHangRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class DonHangService {
    private static final Logger log = LoggerFactory.getLogger(DonHangService.class);


    private final DonHangRepository        donHangRepository;
    private final ChiTietDonHangRepository chiTietDonHangRepository;
    private final KhachHangRepository      khachHangRepository;
    private final KhachHangService         khachHangService;
    private final AiService                aiService;

    public DonHangService(DonHangRepository        donHangRepository, ChiTietDonHangRepository chiTietDonHangRepository, KhachHangRepository      khachHangRepository, KhachHangService         khachHangService, AiService                aiService) {
        this.donHangRepository = donHangRepository;
        this.chiTietDonHangRepository = chiTietDonHangRepository;
        this.khachHangRepository = khachHangRepository;
        this.khachHangService = khachHangService;
        this.aiService = aiService;
    }


    /**
     * Luồng chính: Tạo đơn hàng → Tính RFM → Gọi AI → Lưu Cluster.
     *
     * @param request DTO chứa thông tin KH + giỏ hàng
     * @return DonHang vừa tạo (kèm thông tin KH đã được cập nhật cluster)
     */
    @Transactional
    public DonHang createDonHang(DonHangRequest request) {
        log.info("Bắt đầu tạo đơn hàng cho KH: {}", request.getTenKh());

        // ── Bước 1: Tìm hoặc tạo Khách Hàng ──────────────────────
        KhachHang khachHang = khachHangService.findOrCreate(
                request.getTenKh(),
                request.getEmail(),
                request.getSdt()
        );

        // ── Bước 2: Tạo Đơn Hàng ─────────────────────────────────
        DonHang donHang = new DonHang();
        donHang.setKhachHang(khachHang);
        donHang.setNgayDat(LocalDateTime.now());
        donHang.setTongTien(0.0);
        donHang = donHangRepository.save(donHang);

        // ── Bước 3: Lưu Chi Tiết Đơn Hàng + Tính tổng tiền ───────
        double tongTien = 0.0;
        List<ChiTietDonHang> chiTietList = new ArrayList<>();

        for (ChiTietDonHangDTO item : request.getChiTietList()) {
            ChiTietDonHang ct = new ChiTietDonHang();
            ct.setDonHang(donHang);
            ct.setTenSp(item.getTenSp());
            ct.setSoLuong(item.getSoLuong());
            ct.setDonGia(item.getDonGia());
            chiTietList.add(chiTietDonHangRepository.save(ct));
            tongTien += item.getSoLuong() * item.getDonGia();
        }

        // Cập nhật tổng tiền đơn hàng
        donHang.setTongTien(tongTien);
        donHang.setChiTietList(chiTietList);
        donHangRepository.save(donHang);

        log.info("Đã lưu đơn hàng maDh={}, tongTien={}", donHang.getMaDh(), tongTien);

        // ── Bước 4: Tính RFM từ toàn bộ lịch sử đơn hàng ────────
        double[] rfm = tinhRFM(khachHang.getMaKh());
        double recency   = rfm[0];
        double frequency = rfm[1];
        double monetary  = rfm[2];

        log.info("RFM của KH maKh={}: R={}, F={}, M={}", khachHang.getMaKh(), recency, frequency, monetary);

        // Cập nhật RFM vào KhachHang
        khachHang.setRecency(recency);
        khachHang.setFrequency((int) frequency);
        khachHang.setMonetary(monetary);

        // ── Bước 5: Gọi AI Service để lấy Cluster ────────────────
        // Giữ nguyên monetary, không chia 25000 vì UI đã đổi sang dùng Bảng Anh (£)
        PredictResponse predict = aiService.predict(recency, frequency, monetary);

        if (predict != null) {
            khachHang.setClusterId(predict.getClusterId());
            khachHang.setClusterName(predict.getClusterName());
            log.info("Cluster KH maKh={}: id={}, name={}",
                    khachHang.getMaKh(), predict.getClusterId(), predict.getClusterName());
        }

        // Lưu KhachHang đã cập nhật
        khachHangRepository.save(khachHang);

        return donHang;
    }

    /**
     * Tính chỉ số RFM cho một khách hàng từ lịch sử đơn hàng trong DB.
     *
     * R (Recency)   = số ngày từ đơn hàng gần nhất đến hôm nay
     * F (Frequency) = số lượng đơn hàng
     * M (Monetary)  = tổng doanh thu
     *
     * @return double[] {recency, frequency, monetary}
     */
    private double[] tinhRFM(Integer maKh) {
        LocalDateTime maxNgayDat = donHangRepository.findMaxNgayDatByMaKh(maKh);
        long  recency   = (maxNgayDat != null)
                          ? ChronoUnit.DAYS.between(maxNgayDat, LocalDateTime.now())
                          : 0L;
        long  frequency = donHangRepository.countByKhachHang_MaKh(maKh);
        double monetary = donHangRepository.sumTongTienByMaKh(maKh);

        return new double[]{(double) recency, (double) frequency, monetary};
    }

    /** Lấy lịch sử đơn hàng của một khách hàng. */
    @Transactional(readOnly = true)
    public List<DonHang> findByKhachHang(Integer maKh) {
        return donHangRepository.findByKhachHang_MaKhOrderByNgayDatDesc(maKh);
    }

    /** Lấy tất cả đơn hàng (có phân trang) */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<DonHang> findAll(org.springframework.data.domain.Pageable pageable) {
        return donHangRepository.findAll(pageable);
    }

    /** Lấy thông tin chi tiết của 1 đơn hàng */
    @Transactional(readOnly = true)
    public java.util.Optional<DonHang> findById(Integer maDh) {
        return donHangRepository.findById(maDh);
    }

    @Transactional(readOnly = true)
    public long count() {
        return donHangRepository.count();
    }

    @Transactional(readOnly = true)
    public Double tongDoanhThu() {
        Double total = donHangRepository.sumTongTien();
        return total != null ? total : 0.0;
    }
}
