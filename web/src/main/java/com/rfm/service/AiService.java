package com.rfm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rfm.dto.PredictResponse;
import com.rfm.dto.RfmRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class AiService {
    private static final Logger log = LoggerFactory.getLogger(AiService.class);


    private final RestTemplate restTemplate;

    public AiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }


    @Value("${ai.service.url}")
    private String aiServiceUrl;

    /**
     * Gọi Python FastAPI /predict để lấy cluster_id và cluster_name.
     *
     * @param r Recency  (số ngày từ lần mua cuối)
     * @param f Frequency (số lần mua hàng)
     * @param m Monetary  (tổng giá trị mua hàng)
     * @return PredictResponse chứa cluster_id và cluster_name
     */
    public PredictResponse predict(double r, double f, double m) {
        String url = aiServiceUrl + "/predict";
        RfmRequest request = new RfmRequest(r, f, m);

        log.info("Gọi AI Service: POST {} với R={}, F={}, M={}", url, r, f, m);

        try {
            PredictResponse response = restTemplate.postForObject(url, request, PredictResponse.class);
            if (response != null) {
                log.info("AI Response: cluster_id={}, cluster_name={}", response.getClusterId(), response.getClusterName());
            }
            return response;
        } catch (RestClientException e) {
            log.error("Lỗi kết nối AI Service tại {}: {}", url, e.getMessage());
            // Fallback: trả về cluster mặc định khi AI Service không khả dụng
            PredictResponse fallback = new PredictResponse();
            fallback.setClusterId(-1);
            fallback.setClusterName("Chưa phân cụm");
            return fallback;
        }
    }
}
