package br.com.food.demo.config;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import br.com.food.demo.entity.Item;
import br.com.food.demo.entity.OrderStatus;
import br.com.food.demo.entity.Role;
import br.com.food.demo.entity.User;
import br.com.food.demo.repository.ItemRepository;
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

    private static final List<ItemSeed> ITEMS = List.of(
            new ItemSeed("X-Burguer", "18.00", 30),
            new ItemSeed("X-Salada", "20.00", 30),
            new ItemSeed("Esfiha de Carne", "8.00", 30),
            new ItemSeed("Coca-Cola 600ml", "8.00", 30),
            new ItemSeed("Coca-Cola 2L", "14.00", 30)
    );

    private final RoleRepository roleRepository;
    private final OrderStatusRepository orderStatusRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseSeeder(
            RoleRepository roleRepository,
            OrderStatusRepository orderStatusRepository,
            UserRepository userRepository,
            ItemRepository itemRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.roleRepository = roleRepository;
        this.orderStatusRepository = orderStatusRepository;
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        ROLE_NAMES.forEach(this::createRoleIfMissing);
        ORDER_STATUS_NAMES.forEach(this::createOrderStatusIfMissing);
        ITEMS.forEach(this::createItemIfMissing);
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

    private void createItemIfMissing(ItemSeed itemSeed) {
        if (itemRepository.existsByNameIgnoreCase(itemSeed.name())) {
            return;
        }

        Item item = new Item();
        item.setName(itemSeed.name());
        item.setPrice(new BigDecimal(itemSeed.price()));
        item.setStock(itemSeed.stock());
        itemRepository.save(item);
    }

    private void createAdminIfMissing() {
        if (userRepository.existsByEmailIgnoreCase(ADMIN_EMAIL)) {
            return;
        }

        Role adminRole = roleRepository.findByName("Admin")
                .orElseThrow(() -> new IllegalStateException("Role Admin não cadastrada"));

        User admin = new User();
        admin.setName("Admin");
        admin.setAddress("Não se aplica");
        admin.setEmail(ADMIN_EMAIL);
        admin.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
        admin.setRole(adminRole);
        userRepository.save(admin);
    }

    private record ItemSeed(String name, String price, int stock) {
    }
}
