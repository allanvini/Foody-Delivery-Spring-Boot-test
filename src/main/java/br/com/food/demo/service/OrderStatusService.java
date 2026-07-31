package br.com.food.demo.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.food.demo.dto.OrderStatusResponse;
import br.com.food.demo.repository.OrderStatusRepository;

@Service
public class OrderStatusService {

    private final OrderStatusRepository orderStatusRepository;

    public OrderStatusService(OrderStatusRepository orderStatusRepository) {
        this.orderStatusRepository = orderStatusRepository;
    }

    @Transactional(readOnly = true)
    public List<OrderStatusResponse> findAll() {
        return orderStatusRepository.findAllByOrderByIdAsc().stream()
                .map(OrderStatusResponse::from)
                .toList();
    }
}
