package br.com.food.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import br.com.food.demo.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = "status")
    List<Order> findAllByUserIdOrderByIdDesc(Long userId);

    @EntityGraph(attributePaths = {"status", "user", "user.role"})
    List<Order> findAllByOrderByIdDesc();
}
