package edu.help.microservice.util;

import edu.help.microservice.dto.BarDTO;
import edu.help.microservice.dto.TagDTO;
import edu.help.microservice.entity.Bar;
import edu.help.microservice.entity.Tag;

public class DTOConverter {

    public static BarDTO convertToBarDTO(Bar bar) {
        BarDTO barDTO = new BarDTO();
        barDTO.setId(bar.getBarId());
        barDTO.setName(bar.getBarName());
        barDTO.setBarTag(bar.getBarTag());
        barDTO.setAddress(bar.getBarAddress());
        barDTO.setTagImage(bar.getTagImage());
        barDTO.setBarImage(bar.getBarImage());
        barDTO.setOpenHours(bar.getOpenHours());
        barDTO.setHappyHours(bar.getHappyHourTimes()); // No conversion needed if HashMapConverter is set up
        return barDTO;
    }

    public static TagDTO convertToTagDTO(Tag tag) {
        TagDTO tagDTO = new TagDTO();
        tagDTO.setCategoryId(tag.getCategoryId());
        tagDTO.setCategoryName(tag.getCategoryName());
        return tagDTO;
    }
}
