package br.com.food.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.food.demo.entity.Item;

public interface ItemRepository extends JpaRepository<Item, Long> {

    boolean existsByNameIgnoreCase(String name);
}
