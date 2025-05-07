// src/main/java/edu/help/microservice/service/ItemService.java
package edu.help.microservice.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.help.microservice.dto.CreateItemRequestDTO;
import edu.help.microservice.dto.ItemDTO;
import edu.help.microservice.dto.UpdateItemRequestDTO;
import edu.help.microservice.entity.Item;
import edu.help.microservice.repository.ItemRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepo;

    /* ----------------- Public CRUD API ----------------- */

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
                .pointPrice(req.getPointPrice())
                .regularPrice(req.getRegularPrice())
                .discountPrice(req.getDiscountPrice())
                .taxPercent(req.getTaxPercent())
                .categoryIds(req.getCategoryIds())
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
        if (req.getPointPrice()   != null) item.setPointPrice(req.getPointPrice());
        if (req.getRegularPrice() != null) item.setRegularPrice(req.getRegularPrice());
        if (req.getDiscountPrice()!= null) item.setDiscountPrice(req.getDiscountPrice());
        if (req.getTaxPercent()   != null) item.setTaxPercent(req.getTaxPercent());
        if (req.getCategoryIds()  != null) item.setCategoryIds(req.getCategoryIds());

        return toDto(item);   // item is managed, so JPA flushes automatically
    }

    @Transactional
    public void delete(Integer merchantId, Integer itemId) {
        itemRepo.findById(itemId)
                .filter(i -> i.getMerchantId().equals(merchantId))
                .ifPresent(itemRepo::delete);
    }

    /* --------------------- Helpers --------------------- */
    private ItemDTO toDto(Item i) {
        return ItemDTO.builder()
                .itemId(i.getItemId())
                .name(i.getName())
                .description(i.getDescription())
                .pointPrice(i.getPointPrice())
                .regularPrice(i.getRegularPrice())
                .discountPrice(i.getDiscountPrice())
                .taxPercent(i.getTaxPercent())
                .categoryIds(i.getCategoryIds())
                .build();
    }
}
