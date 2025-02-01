// src/main/java/edu/help/microservice/dto/GetTipsRequest.java
package edu.help.microservice.dto;

public class GetTipsRequest {
    private String bartenderID;
    private String barID;


    public String getBartenderID() {
        return bartenderID;
    }
    public String getBarID() {
        return barID;
    }

    public void setBartenderID(String bartenderID) {
        this.bartenderID = bartenderID;
    }
    public void setBarID(String barID) {
        this.barID = barID;
    }

}