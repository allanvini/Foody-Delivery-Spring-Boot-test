package br.com.food.demo.dto;

import java.math.BigDecimal;

import br.com.food.demo.entity.Item;

public record ItemResponse(
        Long id,
        String name,
        BigDecimal price,
        Integer stock
) {

    public static ItemResponse from(Item item) {
        return new ItemResponse(item.getId(), item.getName(), item.getPrice(), item.getStock());
    }
}
