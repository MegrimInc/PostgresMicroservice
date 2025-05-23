// src/main/java/edu/help/microservice/service/ItemService.java
package edu.help.microservice.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import edu.help.microservice.dto.*;
import edu.help.microservice.entity.Item;
import edu.help.microservice.repository.ItemRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepo;

    public List<ItemDTO> getMenu(Integer merchantId) {
        return itemRepo.findByMerchantId(merchantId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public ItemDTO create(Integer merchantId, CreateItemRequestDTO req) {
        Item saved = itemRepo.save(Item.builder()
                .merchantId(merchantId)
                .name(req.getName())
                .description(req.getDescription())
                .regularPrice(req.getRegularPrice())
                .discountPrice(req.getDiscountPrice())
                .pointPrice(req.getPointPrice())
                .taxPercent(req.getTaxPercent())
                .gratuityPercent(req.getGratuityPercent())
                .categoryIds(req.getCategoryIds())
                .image(req.getImage())                  // ← persist url
                .build());
        return toDto(saved);
    }

    @Transactional
    public ItemDTO update(Integer merchantId, Integer itemId, UpdateItemRequestDTO req) {
        Item item = itemRepo.findById(itemId)
                .filter(i -> i.getMerchantId().equals(merchantId))
                .orElseThrow(() -> new IllegalArgumentException("Item not found"));

        if (req.getName()         != null) item.setName(req.getName());
        if (req.getDescription()  != null) item.setDescription(req.getDescription());
        if (req.getRegularPrice() != null) item.setRegularPrice(req.getRegularPrice());
        if (req.getDiscountPrice()!= null) item.setDiscountPrice(req.getDiscountPrice());
        if (req.getPointPrice()   != null) item.setPointPrice(req.getPointPrice());
        if (req.getTaxPercent()   != null) item.setTaxPercent(req.getTaxPercent());
        if (req.getGratuityPercent()!=null) item.setGratuityPercent(req.getGratuityPercent());
        if (req.getCategoryIds()  != null) item.setCategoryIds(req.getCategoryIds());
        if (req.getImage()        != null) item.setImage(req.getImage());   // ← persist url

        return toDto(item);
    }

    public void delete(Integer merchantId, Integer itemId) {
        itemRepo.findById(itemId)
                .filter(i -> i.getMerchantId().equals(merchantId))
                .ifPresent(itemRepo::delete);
    }

    /* ---------- helper ---------- */
    private ItemDTO toDto(Item i) {
        return ItemDTO.builder()
                .itemId(i.getItemId())
                .name(i.getName())
                .description(i.getDescription())
                .regularPrice(i.getRegularPrice())
                .discountPrice(i.getDiscountPrice())
                .pointPrice(i.getPointPrice())
                .taxPercent(i.getTaxPercent())
                .gratuityPercent(i.getGratuityPercent())
                .categoryIds(i.getCategoryIds())
                .image(i.getImage())                     // ← return url
                .build();
    }
}
