package edu.help.microservice.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.help.microservice.dto.MerchantDto;
import edu.help.microservice.dto.TagDTO;
import edu.help.microservice.entity.Merchant;
import edu.help.microservice.entity.Tag;

import java.util.HashMap;
import java.util.Map;

public class DTOConverter {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static MerchantDto convertToMerchantDTO(Merchant merchant) {
        MerchantDto merchantDto = new MerchantDto();
        merchantDto.setId(merchant.getMerchantId());
        merchantDto.setName(merchant.getMerchantName());
        merchantDto.setMerchantTag(merchant.getMerchantTag());
        merchantDto.setAddress(merchant.getMerchantAddress());
        merchantDto.setTagImage(merchant.getTagImage());
        merchantDto.setMerchantImage(merchant.getMerchantImage());
        merchantDto.setOpenHours(merchant.getOpenHours());

        // Convert stored JSON string to Map<String, String>
        try {
            if (merchant.getHappyHourTimes() != null && !merchant.getHappyHourTimes().isEmpty()) {
                Map<String, String> happyHours = objectMapper.readValue(merchant.getHappyHourTimes(), Map.class);
                merchantDto.setHappyHours(happyHours);
            } else {
                merchantDto.setHappyHours(new HashMap<>()); // Default empty map
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            merchantDto.setHappyHours(new HashMap<>()); // Fallback to empty map on error
        }
        
        return merchantDto;
    }

    public static TagDTO convertToTagDTO(Tag tag) {
        TagDTO tagDTO = new TagDTO();
        tagDTO.setCategoryId(tag.getCategoryId());
        tagDTO.setCategoryName(tag.getCategoryName());
        return tagDTO;
    }
}