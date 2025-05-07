package edu.help.microservice.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.help.microservice.util.IntegerArrayConverter;
import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "items")
public class Item {

    @JsonIgnore
    @Column(name = "merchant_id")
    private Integer merchantId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Integer itemId;

    @Column(name = "name")
    private String name;

    @Column(name = "image")
    private String image;

    @Column(name = "category_ids")
    @Convert(converter = IntegerArrayConverter.class)
    private Integer[] categoryIds;  // ← this is your list of category IDs

    @Column(name = "description")
    private String description;

    @Column(name = "point_price")
    private Integer pointPrice;

    @Column(name = "regular_price")
    private Double regularPrice;

    @Column(name = "discount_price")
    private Double discountPrice;

    @Column(name = "tax_percent")
    private Double taxPercent;
}
