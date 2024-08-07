package edu.help.microservice.dto;

import java.util.List;

public class ResponseDTO {
    private List<BarDTO> bars;
    private List<TagDTO> tags;

    // Getters and Setters
    public List<BarDTO> getBars() {
        return bars;
    }

    public void setBars(List<BarDTO> bars) {
        this.bars = bars;
    }

    public List<TagDTO> getTags() {
        return tags;
    }

    public void setTags(List<TagDTO> tags) {
        this.tags = tags;
    }
}
