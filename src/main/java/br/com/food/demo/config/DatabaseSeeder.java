package br.com.food.demo.config;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.com.food.demo.entity.OrderStatus;
import br.com.food.demo.entity.Role;
import br.com.food.demo.repository.OrderStatusRepository;
import br.com.food.demo.repository.RoleRepository;

@Component
public class DatabaseSeeder implements ApplicationRunner {

    private static final List<String> ROLE_NAMES = List.of(
            "User",
            "Admin"
    );

    private static final List<String> ORDER_STATUS_NAMES = List.of(
            "Aguardando confirmação",
            "Pedido Confirmado",
            "Preparando",
            "Pronto para retirada",
            "Entregador a caminho",
            "Pedido retirado pelo entregador",
            "Saiu para entrega",
            "Pedido entregue",
            "Cancelado"
    );

    private final RoleRepository roleRepository;
    private final OrderStatusRepository orderStatusRepository;

    public DatabaseSeeder(
            RoleRepository roleRepository,
            OrderStatusRepository orderStatusRepository
    ) {
        this.roleRepository = roleRepository;
        this.orderStatusRepository = orderStatusRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ROLE_NAMES.forEach(this::createRoleIfMissing);
        ORDER_STATUS_NAMES.forEach(this::createOrderStatusIfMissing);
    }

    private void createRoleIfMissing(String name) {
        if (roleRepository.findByName(name).isEmpty()) {
            roleRepository.save(new Role(name));
        }
    }

    private void createOrderStatusIfMissing(String name) {
        if (orderStatusRepository.findByName(name).isEmpty()) {
            orderStatusRepository.save(new OrderStatus(name));
        }
    }
}
