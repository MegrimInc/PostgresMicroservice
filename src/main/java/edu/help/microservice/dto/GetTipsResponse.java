package edu.help.microservice.dto;

public class GetTipsResponse {
    private double tipTotal;

    public GetTipsResponse(double tipTotal) {
        this.tipTotal = tipTotal;
    }

    public double getTipTotal() {
        return tipTotal;
    }

    public void setTipTotal(double tipTotal) {
        this.tipTotal = tipTotal;
    }
}