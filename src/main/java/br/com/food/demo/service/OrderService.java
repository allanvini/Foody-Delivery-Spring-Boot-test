package br.com.food.demo.service;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import br.com.food.demo.dto.CreateOrderItemRequest;
import br.com.food.demo.dto.CreateOrderRequest;
import br.com.food.demo.dto.OrderItemResponse;
import br.com.food.demo.dto.OrderResponse;
import br.com.food.demo.dto.UpdateOrderStatusRequest;
import br.com.food.demo.dto.UpdateOrderStatusResponse;
import br.com.food.demo.entity.Item;
import br.com.food.demo.entity.Order;
import br.com.food.demo.entity.OrderItem;
import br.com.food.demo.entity.OrderStatus;
import br.com.food.demo.entity.User;
import br.com.food.demo.repository.ItemRepository;
import br.com.food.demo.repository.OrderItemRepository;
import br.com.food.demo.repository.OrderRepository;
import br.com.food.demo.repository.OrderStatusRepository;
import br.com.food.demo.repository.UserRepository;

@Service
public class OrderService {

    private static final String INITIAL_STATUS = "Aguardando confirmação";

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ItemRepository itemRepository;
    private final OrderStatusRepository orderStatusRepository;
    private final UserRepository userRepository;

    public OrderService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            ItemRepository itemRepository,
            OrderStatusRepository orderStatusRepository,
            UserRepository userRepository
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.itemRepository = itemRepository;
        this.orderStatusRepository = orderStatusRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findByUserId(Long userId) {
        List<Order> orders = orderRepository.findAllByUserIdOrderByIdDesc(userId);
        if (orders.isEmpty()) {
            return List.of();
        }

        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        Map<Long, List<OrderItemResponse>> itemsByOrderId = orderItemRepository
                .findAllByOrder_IdIn(orderIds)
                .stream()
                .collect(Collectors.groupingBy(
                        orderItem -> orderItem.getOrder().getId(),
                        Collectors.mapping(OrderItemResponse::from, Collectors.toList())
                ));

        return orders.stream()
                .map(order -> OrderResponse.from(
                        order,
                        itemsByOrderId.getOrDefault(order.getId(), List.of())
                ))
                .toList();
    }

    @Transactional
    public OrderResponse create(Long userId, CreateOrderRequest request) {
        Set<Long> requestedItemIds = request.items().stream()
                .map(CreateOrderItemRequest::id)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (requestedItemIds.size() != request.items().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O pedido contém itens duplicados");
        }

        Map<Long, Item> itemsById = itemRepository.findAllById(requestedItemIds).stream()
                .collect(Collectors.toMap(Item::getId, Function.identity()));
        if (itemsById.size() != requestedItemIds.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Um ou mais itens não foram encontrados");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
        OrderStatus initialStatus = orderStatusRepository.findByName(INITIAL_STATUS)
                .orElseThrow(() -> new IllegalStateException("Status inicial não cadastrado"));

        BigDecimal total = BigDecimal.ZERO;
        for (CreateOrderItemRequest requestedItem : request.items()) {
            Item item = itemsById.get(requestedItem.id());
            if (item.getStock() < requestedItem.quantity()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Estoque insuficiente para o item " + item.getName()
                );
            }
            item.setStock(item.getStock() - requestedItem.quantity());
            total = total.add(item.getPrice().multiply(BigDecimal.valueOf(requestedItem.quantity())));
        }

        Order order = new Order();
        order.setUser(user);
        order.setStatus(initialStatus);
        order.setTotal(total);
        order.setObservations(normalizeObservations(request.observations()));
        Order savedOrder = orderRepository.save(order);

        List<OrderItem> orderItems = request.items().stream()
                .map(requestedItem -> createOrderItem(
                        savedOrder,
                        itemsById.get(requestedItem.id()),
                        requestedItem.quantity()
                ))
                .toList();
        List<OrderItemResponse> itemResponses = orderItemRepository.saveAll(orderItems).stream()
                .map(OrderItemResponse::from)
                .toList();

        return OrderResponse.from(savedOrder, itemResponses);
    }

    @Transactional
    public UpdateOrderStatusResponse updateStatus(Long orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado"));
        OrderStatus status = orderStatusRepository.findById(request.statusId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Status não encontrado"));

        order.setStatus(status);
        return new UpdateOrderStatusResponse(order.getId(), status.getId(), status.getName());
    }

    private OrderItem createOrderItem(Order order, Item item, Integer quantity) {
        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setItem(item);
        orderItem.setQuantity(quantity);
        return orderItem;
    }

    private String normalizeObservations(String observations) {
        if (observations == null || observations.isBlank()) {
            return null;
        }
        return observations.trim();
    }
}
