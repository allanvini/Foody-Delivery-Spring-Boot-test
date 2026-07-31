package br.com.food.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.food.demo.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
