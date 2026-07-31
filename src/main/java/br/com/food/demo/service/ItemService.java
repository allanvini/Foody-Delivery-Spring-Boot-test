package br.com.food.demo.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import br.com.food.demo.dto.ItemRequest;
import br.com.food.demo.dto.ItemResponse;
import br.com.food.demo.entity.Item;
import br.com.food.demo.repository.ItemRepository;
import br.com.food.demo.repository.OrderItemRepository;

@Service
public class ItemService {

    private final ItemRepository itemRepository;
    private final OrderItemRepository orderItemRepository;

    public ItemService(ItemRepository itemRepository, OrderItemRepository orderItemRepository) {
        this.itemRepository = itemRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional(readOnly = true)
    public List<ItemResponse> findAll() {
        return itemRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(ItemResponse::from)
                .toList();
    }

    @Transactional
    public ItemResponse create(ItemRequest request) {
        Item item = new Item();
        apply(item, request);
        return ItemResponse.from(itemRepository.save(item));
    }

    @Transactional
    public ItemResponse update(Long itemId, ItemRequest request) {
        Item item = findById(itemId);
        apply(item, request);
        return ItemResponse.from(item);
    }

    @Transactional
    public void delete(Long itemId) {
        Item item = findById(itemId);
        if (orderItemRepository.existsByItem_Id(itemId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Item não pode ser removido porque pertence a um pedido"
            );
        }
        itemRepository.delete(item);
    }

    private Item findById(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item não encontrado"));
    }

    private void apply(Item item, ItemRequest request) {
        item.setName(request.name().trim());
        item.setPrice(request.price());
        item.setStock(request.stock());
    }
}
