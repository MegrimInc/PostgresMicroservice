package edu.help.microservice.dto;

import edu.help.microservice.entity.Item;
import edu.help.microservice.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryResponse {
    private List<Item> items;
    private List<Category> categories;
}