package br.com.food.demo.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import br.com.food.demo.entity.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @EntityGraph(attributePaths = "item")
    List<OrderItem> findAllByOrder_IdIn(Collection<Long> orderIds);

    boolean existsByItem_Id(Long itemId);
}
