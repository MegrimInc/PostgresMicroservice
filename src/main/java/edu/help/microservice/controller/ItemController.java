package edu.help.microservice.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import edu.help.microservice.dto.CreateItemRequestDTO;
import edu.help.microservice.dto.ItemDTO;
import edu.help.microservice.dto.UpdateItemRequestDTO;
import edu.help.microservice.service.ItemService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    /** Replace this with your real auth mechanism; for now we rely on header */
    private Integer merchantId(@RequestHeader("X-MERCHANT-ID") String header) {
        return Integer.valueOf(header);
    }

    /* ---------------------- ROUTES ---------------------- */

    @GetMapping
    public List<ItemDTO> menu(@RequestHeader("X-MERCHANT-ID") String header) {
        return itemService.getMenu(merchantId(header));
    }

    @PostMapping
    public ItemDTO create(@RequestHeader("X-MERCHANT-ID") String header,
                          @RequestBody CreateItemRequestDTO req) {
        return itemService.create(merchantId(header), req);
    }

    @PatchMapping("/{itemId}")
    public ItemDTO update(@RequestHeader("X-MERCHANT-ID") String header,
                          @PathVariable Integer itemId,
                          @RequestBody UpdateItemRequestDTO req) {
        return itemService.update(merchantId(header), itemId, req);
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> delete(@RequestHeader("X-MERCHANT-ID") String header,
                                       @PathVariable Integer itemId) {
        itemService.delete(merchantId(header), itemId);
        return ResponseEntity.noContent().build();
    }
}
