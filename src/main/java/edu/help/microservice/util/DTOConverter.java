package edu.help.microservice.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.help.microservice.dto.BarDTO;
import edu.help.microservice.dto.TagDTO;
import edu.help.microservice.entity.Bar;
import edu.help.microservice.entity.Tag;

import java.util.HashMap;
import java.util.Map;

public class DTOConverter {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static BarDTO convertToBarDTO(Bar bar) {
        BarDTO barDTO = new BarDTO();
        barDTO.setId(bar.getBarId());
        barDTO.setName(bar.getBarName());
        barDTO.setBarTag(bar.getBarTag());
        barDTO.setAddress(bar.getBarAddress());
        barDTO.setTagImage(bar.getTagImage());
        barDTO.setBarImage(bar.getBarImage());
        barDTO.setOpenHours(bar.getOpenHours());

        // Convert stored JSON string to Map<String, String>
        try {
            if (bar.getHappyHourTimes() != null && !bar.getHappyHourTimes().isEmpty()) {
                Map<String, String> happyHours = objectMapper.readValue(bar.getHappyHourTimes(), Map.class);
                barDTO.setHappyHours(happyHours);
            } else {
                barDTO.setHappyHours(new HashMap<>()); // Default empty map
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            barDTO.setHappyHours(new HashMap<>()); // Fallback to empty map on error
        }
        
        return barDTO;
    }

    public static TagDTO convertToTagDTO(Tag tag) {
        TagDTO tagDTO = new TagDTO();
        tagDTO.setCategoryId(tag.getCategoryId());
        tagDTO.setCategoryName(tag.getCategoryName());
        return tagDTO;
    }
}
