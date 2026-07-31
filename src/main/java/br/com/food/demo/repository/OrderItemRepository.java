package br.com.food.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.food.demo.entity.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
