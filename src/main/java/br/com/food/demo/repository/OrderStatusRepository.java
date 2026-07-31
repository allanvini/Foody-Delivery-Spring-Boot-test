package br.com.food.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.food.demo.entity.OrderStatus;

public interface OrderStatusRepository extends JpaRepository<OrderStatus, Long> {

    Optional<OrderStatus> findByName(String name);
}
