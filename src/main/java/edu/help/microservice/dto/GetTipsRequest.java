// src/main/java/edu/help/microservice/dto/GetTipsRequest.java
package edu.help.microservice.dto;

public class GetTipsRequest {
    private String stationID;
    private String merchantID;


    public String getMerchanttenderID() {
        return stationID;
    }
    public String getMerchantID() {
        return merchantID;
    }

    public void setMerchanttenderID(String stationID) {
        this.stationID = stationID;
    }
    public void setMerchantID(String merchantID) {
        this.merchantID = merchantID;
    }

}