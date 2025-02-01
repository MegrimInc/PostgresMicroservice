// src/main/java/edu/help/microservice/dto/GetTipsRequest.java
package edu.help.microservice.dto;

public class GetTipsRequest {
    private String bartenderID;

    public String getBartenderID() {
        return bartenderID;
    }

    public void setBartenderID(String bartenderID) {
        this.bartenderID = bartenderID;
    }
}