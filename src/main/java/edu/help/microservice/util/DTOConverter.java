package edu.help.microservice.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.help.microservice.dto.MerchantDTO;
import edu.help.microservice.entity.Merchant;


import java.util.HashMap;
import java.util.Map;

public class DTOConverter {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static MerchantDTO convertToMerchantDTO(Merchant merchant) {
        MerchantDTO merchantDto = new MerchantDTO();
        merchantDto.setId(merchant.getId());
        merchantDto.setName(merchant.getName());
        merchantDto.setNickname(merchant.getNickname());      
        merchantDto.setCity(merchant.getCity());
        merchantDto.setStateOrProvince(merchant.getStateOrProvince());
        merchantDto.setCountry(merchant.getCountry());
        merchantDto.setAddress(merchant.getAddress());
        merchantDto.setLogoImage(merchant.getLogoImage());    
        merchantDto.setStoreImage(merchant.getStoreImage());  
        merchantDto.setOpen(merchant.getOpen());             
    
        // Handle discountSchedule (JSON to Map)
        try {
            if (merchant.getDiscountSchedule() != null && !merchant.getDiscountSchedule().isEmpty()) {
                Map<String, String> discountSchedule = objectMapper.readValue(merchant.getDiscountSchedule(), Map.class);
                merchantDto.setDiscountSchedule(discountSchedule);
            } else {
                merchantDto.setDiscountSchedule(new HashMap<>());
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            merchantDto.setDiscountSchedule(new HashMap<>());
        }
    
        merchantDto.setSignupBonus(merchant.getBonus());
        return merchantDto;
    }
}