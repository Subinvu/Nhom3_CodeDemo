package com.rfm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PredictResponse {

    @JsonProperty("cluster_id")
    private Integer clusterId;

    @JsonProperty("cluster_name")
    private String clusterName;

    private Double r;
    private Double f;
    private Double m;

    public PredictResponse(Integer clusterId, String clusterName, Double r, Double f, Double m) {
        this.clusterId = clusterId;
        this.clusterName = clusterName;
        this.r = r;
        this.f = f;
        this.m = m;
    }

    public PredictResponse() {
    }

    public Integer getClusterId() {
        return clusterId;
    }

    public void setClusterId(Integer clusterId) {
        this.clusterId = clusterId;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public Double getR() {
        return r;
    }

    public void setR(Double r) {
        this.r = r;
    }

    public Double getF() {
        return f;
    }

    public void setF(Double f) {
        this.f = f;
    }

    public Double getM() {
        return m;
    }

    public void setM(Double m) {
        this.m = m;
    }
}
