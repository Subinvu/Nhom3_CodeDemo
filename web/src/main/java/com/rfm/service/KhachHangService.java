package com.rfm.service;

import com.rfm.entity.KhachHang;
import com.rfm.repository.KhachHangRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class KhachHangService {

    private final KhachHangRepository khachHangRepository;
    private final AiService aiService;

    public KhachHangService(KhachHangRepository khachHangRepository, AiService aiService) {
        this.khachHangRepository = khachHangRepository;
        this.aiService = aiService;
    }


    /**
     * Tìm hoặc tạo mới khách hàng theo email.
     * Nếu email rỗng → luôn tạo mới.
     */
    @Transactional
    public KhachHang findOrCreate(String tenKh, String email, String sdt) {
        if (email != null && !email.isBlank()) {
            Optional<KhachHang> existing = khachHangRepository.findByEmail(email);
            if (existing.isPresent()) {
                KhachHang kh = existing.get();
                // Cập nhật tên nếu thay đổi
                kh.setTenKh(tenKh);
                if (sdt != null && !sdt.isBlank()) kh.setSdt(sdt);
                return khachHangRepository.save(kh);
            }
        }

        KhachHang newKh = new KhachHang();
        newKh.setTenKh(tenKh);
        newKh.setEmail(email);
        newKh.setSdt(sdt);
        return khachHangRepository.save(newKh);
    }

    public Optional<KhachHang> findById(Integer id) {
        return khachHangRepository.findById(id);
    }

    /** Lấy danh sách phân trang (cho Data Table). */
    public Page<KhachHang> findAll(Pageable pageable) {
        return khachHangRepository.findAll(pageable);
    }

    /** Tìm kiếm theo keyword (tên hoặc email). */
    public Page<KhachHang> search(String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return khachHangRepository.findAll(pageable);
        }
        return khachHangRepository.searchByKeyword(keyword.trim(), pageable);
    }

    /** Lấy danh sách theo cluster (dùng cho export .md). */
    public List<KhachHang> findByCluster(Integer clusterId) {
        return khachHangRepository.findByClusterIdOrderByMonetaryDesc(clusterId);
    }

    public long count() {
        return khachHangRepository.count();
    }

    /** Xóa khách hàng theo ID */
    @Transactional
    public void deleteById(Integer id) {
        khachHangRepository.deleteById(id);
    }

    /** Cập nhật thông tin khách hàng */
    @Transactional
    public KhachHang updateKhachHang(Integer id, String tenKh, String email, String sdt) {
        KhachHang kh = khachHangRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));
        kh.setTenKh(tenKh);
        kh.setEmail(email);
        kh.setSdt(sdt);
        return khachHangRepository.save(kh);
    }

    /** Phân cụm đồng loạt các khách hàng chưa được phân cụm (clusterId = null hoặc -1) */
    @Transactional
    public int syncClusters() {
        List<KhachHang> unclustered = khachHangRepository.findAll().stream()
            .filter(kh -> kh.getClusterId() == null || kh.getClusterId() == -1)
            .toList();

        int count = 0;
        for (KhachHang kh : unclustered) {
            com.rfm.dto.PredictResponse predict = aiService.predict(
                kh.getRecency() != null ? kh.getRecency() : 0.0,
                kh.getFrequency() != null ? kh.getFrequency() : 0,
                kh.getMonetary() != null ? kh.getMonetary() : 0.0
            );

            if (predict != null && predict.getClusterId() != null && predict.getClusterId() != -1) {
                kh.setClusterId(predict.getClusterId());
                kh.setClusterName(predict.getClusterName());
                khachHangRepository.save(kh);
                count++;
            }
        }
        return count;
    }
}
