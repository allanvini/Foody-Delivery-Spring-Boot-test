package br.com.food.demo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.food.demo.dto.ItemRequest;
import br.com.food.demo.dto.ItemResponse;
import br.com.food.demo.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import static br.com.food.demo.config.OpenApiConfig.COOKIE_AUTH;

@RestController
@RequestMapping("/api/items")
@Tag(name = "Itens", description = "Cardápio e estoque")
@SecurityRequirement(name = COOKIE_AUTH)
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @GetMapping
    @Operation(summary = "Listar itens")
    public List<ItemResponse> findAll() {
        return itemService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar item", description = "Disponível somente para administradores.")
    public ItemResponse create(@Valid @RequestBody ItemRequest request) {
        return itemService.create(request);
    }

    @PutMapping("/{itemId}")
    @Operation(summary = "Editar item", description = "Disponível somente para administradores.")
    public ItemResponse update(@PathVariable Long itemId, @Valid @RequestBody ItemRequest request) {
        return itemService.update(itemId, request);
    }

    @DeleteMapping("/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remover item", description = "Disponível somente para administradores.")
    public void delete(@PathVariable Long itemId) {
        itemService.delete(itemId);
    }
}
