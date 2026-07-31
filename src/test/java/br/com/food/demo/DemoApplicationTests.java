package br.com.food.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

import br.com.food.demo.entity.Item;
import br.com.food.demo.entity.Order;
import br.com.food.demo.entity.OrderItem;
import br.com.food.demo.entity.OrderStatus;
import br.com.food.demo.entity.Role;
import br.com.food.demo.entity.User;
import br.com.food.demo.repository.ItemRepository;
import br.com.food.demo.repository.OrderItemRepository;
import br.com.food.demo.repository.OrderRepository;
import br.com.food.demo.repository.OrderStatusRepository;
import br.com.food.demo.repository.RoleRepository;
import br.com.food.demo.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
class DemoApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private OrderStatusRepository orderStatusRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ItemRepository itemRepository;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private OrderItemRepository orderItemRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtDecoder jwtDecoder;

	@Test
	void contextLoads() {
		assertEquals(2, roleRepository.count());
		assertEquals(9, orderStatusRepository.count());
	}

	@Test
	void completeAuthenticationAndProtectedRoutesFlow() throws Exception {
		String email = "cliente@example.com";
		String rawPassword = "senha-segura";

		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "cliente@example.com",
						  "password": "senha-segura"
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("cliente"))
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(jsonPath("$.role").value("User"))
				.andExpect(jsonPath("$.password").doesNotExist());

		User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
		assertFalse(rawPassword.equals(user.getPassword()));
		assertTrue(passwordEncoder.matches(rawPassword, user.getPassword()));

		createOrderFor(user);
		createOrderForAnotherUser();

		String loginResponse = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "cliente@example.com",
						  "password": "senha-segura"
						}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.expiresIn").value(3600))
				.andReturn()
				.getResponse()
				.getContentAsString();

		String accessToken = JsonPath.read(loginResponse, "$.accessToken");
		Jwt jwt = jwtDecoder.decode(accessToken);
		assertEquals(user.getId().toString(), jwt.getSubject());
		assertEquals(email, jwt.getClaimAsString("email"));
		assertEquals("USER", jwt.getClaimAsStringList("roles").getFirst());

		mockMvc.perform(get("/api/items"))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(get("/api/items")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Hambúrguer"));

		mockMvc.perform(get("/api/orders")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].status").value("Aguardando confirmação"))
				.andExpect(jsonPath("$[0].total").value(29.90))
				.andExpect(jsonPath("$[0].items[0].name").value("Hambúrguer"));
	}

	private void createOrderFor(User user) {
		Item item = new Item();
		item.setName("Hambúrguer");
		item.setPrice(new BigDecimal("29.90"));
		item.setStock(10);
		item = itemRepository.save(item);

		Order order = new Order();
		order.setUser(user);
		order.setStatus(defaultOrderStatus());
		order.setTotal(new BigDecimal("29.90"));
		order = orderRepository.save(order);

		OrderItem orderItem = new OrderItem();
		orderItem.setOrder(order);
		orderItem.setItem(item);
		orderItemRepository.save(orderItem);
	}

	private void createOrderForAnotherUser() {
		Role role = roleRepository.findByName("User").orElseThrow();

		User anotherUser = new User();
		anotherUser.setName("Outro cliente");
		anotherUser.setEmail("outro@example.com");
		anotherUser.setPassword(passwordEncoder.encode("outra-senha"));
		anotherUser.setRole(role);
		anotherUser = userRepository.save(anotherUser);

		Order order = new Order();
		order.setUser(anotherUser);
		order.setStatus(defaultOrderStatus());
		order.setTotal(new BigDecimal("10.00"));
		orderRepository.save(order);
	}

	private OrderStatus defaultOrderStatus() {
		return orderStatusRepository.findByName("Aguardando confirmação").orElseThrow();
	}

}
