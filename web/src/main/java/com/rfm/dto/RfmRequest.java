package com.rfm.dto;


public class RfmRequest {
    private double r;
    private double f;
    private double m;

    public double getR() {
        return r;
    }

    public void setR(double r) {
        this.r = r;
    }

    public double getF() {
        return f;
    }

    public void setF(double f) {
        this.f = f;
    }

    public double getM() {
        return m;
    }

    public void setM(double m) {
        this.m = m;
    }

    public RfmRequest() {
    }

    public RfmRequest(double r, double f, double m) {
        this.r = r;
        this.f = f;
        this.m = m;
    }
}
