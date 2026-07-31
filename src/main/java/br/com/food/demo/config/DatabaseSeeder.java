package br.com.food.demo.config;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.com.food.demo.entity.OrderStatus;
import br.com.food.demo.entity.Role;
import br.com.food.demo.entity.User;
import br.com.food.demo.repository.OrderStatusRepository;
import br.com.food.demo.repository.RoleRepository;
import br.com.food.demo.repository.UserRepository;

@Component
public class DatabaseSeeder implements ApplicationRunner {

    private static final String ADMIN_EMAIL = "admin@admin.com";
    private static final String ADMIN_PASSWORD = "1234";

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
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseSeeder(
            RoleRepository roleRepository,
            OrderStatusRepository orderStatusRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.roleRepository = roleRepository;
        this.orderStatusRepository = orderStatusRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ROLE_NAMES.forEach(this::createRoleIfMissing);
        ORDER_STATUS_NAMES.forEach(this::createOrderStatusIfMissing);
        createAdminIfMissing();
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

    private void createAdminIfMissing() {
        if (userRepository.existsByEmailIgnoreCase(ADMIN_EMAIL)) {
            return;
        }

        Role adminRole = roleRepository.findByName("Admin")
                .orElseThrow(() -> new IllegalStateException("Role Admin não cadastrada"));

        User admin = new User();
        admin.setName("Administrador");
        admin.setEmail(ADMIN_EMAIL);
        admin.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
        admin.setRole(adminRole);
        userRepository.save(admin);
    }
}
