package br.com.food.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import jakarta.servlet.http.Cookie;
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

		User admin = userRepository.findByEmailIgnoreCase("admin@admin.com").orElseThrow();
		Role adminRole = roleRepository.findByName("Admin").orElseThrow();
		assertEquals(adminRole.getId(), admin.getRole().getId());
		assertTrue(passwordEncoder.matches("1234", admin.getPassword()));
	}

	@Test
	void seededAdminCanAuthenticate() throws Exception {
		var loginResult = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "admin@admin.com",
						  "password": "1234"
						}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andReturn();

		String loginResponse = loginResult.getResponse().getContentAsString();
		String setCookie = loginResult.getResponse().getHeader(HttpHeaders.SET_COOKIE);
		assertTrue(setCookie.contains("access_token="));
		assertTrue(setCookie.contains("Path=/api"));
		assertTrue(setCookie.contains("Secure"));
		assertTrue(setCookie.contains("HttpOnly"));
		assertTrue(setCookie.contains("SameSite=Strict"));
		assertTrue(setCookie.contains("Max-Age=3600"));

		String accessToken = JsonPath.read(loginResponse, "$.accessToken");
		Jwt jwt = jwtDecoder.decode(accessToken);
		assertEquals("admin@admin.com", jwt.getClaimAsString("email"));
		assertEquals("ADMIN", jwt.getClaimAsStringList("roles").getFirst());
	}

	@Test
	void corsAllowsConfiguredFrontendWithCredentials() throws Exception {
		String frontendOrigin = "http://localhost:5173";

		var corsResponse = mockMvc.perform(options("/api/orders")
				.header(HttpHeaders.ORIGIN, frontendOrigin)
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse();

		assertEquals(frontendOrigin, corsResponse.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals("true", corsResponse.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
	}

	@Test
	void completeAuthenticationAndProtectedRoutesFlow() throws Exception {
		String email = "cliente@example.com";
		String rawPassword = "senha-segura";

		var registerResult = mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "cliente@example.com",
						  "password": "senha-segura"
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.user.name").value("cliente"))
				.andExpect(jsonPath("$.user.email").value(email))
				.andExpect(jsonPath("$.user.role").value("User"))
				.andExpect(jsonPath("$.user.password").doesNotExist())
				.andExpect(jsonPath("$.token.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.token.expiresIn").value(3600))
				.andReturn();

		String registerResponse = registerResult.getResponse().getContentAsString();
		String accessToken = JsonPath.read(registerResponse, "$.token.accessToken");
		String registerCookie = registerResult.getResponse().getHeader(HttpHeaders.SET_COOKIE);
		assertTrue(registerCookie.contains("access_token="));

		User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
		assertFalse(rawPassword.equals(user.getPassword()));
		assertTrue(passwordEncoder.matches(rawPassword, user.getPassword()));

		Jwt jwt = jwtDecoder.decode(accessToken);
		assertEquals(user.getId().toString(), jwt.getSubject());
		assertEquals(email, jwt.getClaimAsString("email"));
		assertEquals("USER", jwt.getClaimAsStringList("roles").getFirst());

		createOrderFor(user);
		createOrderForAnotherUser();

		mockMvc.perform(get("/api/items"))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(get("/api/items")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Hambúrguer"));

		mockMvc.perform(get("/api/orders")
				.cookie(new Cookie("access_token", accessToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].status").value("Aguardando confirmação"))
				.andExpect(jsonPath("$[0].total").value(59.80))
				.andExpect(jsonPath("$[0].observations").value("Entregar na portaria"))
				.andExpect(jsonPath("$[0].items[0].name").value("Hambúrguer"))
				.andExpect(jsonPath("$[0].items[0].quantity").value(2))
				.andExpect(jsonPath("$[0].items[0].observations").doesNotExist());

		var logoutResult = mockMvc.perform(post("/api/auth/logout")
				.cookie(new Cookie("access_token", accessToken)))
				.andExpect(status().isNoContent())
				.andReturn();
		String clearedCookie = logoutResult.getResponse().getHeader(HttpHeaders.SET_COOKIE);
		assertTrue(clearedCookie.contains("access_token="));
		assertTrue(clearedCookie.contains("Max-Age=0"));
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
		order.setTotal(new BigDecimal("59.80"));
		order.setObservations("Entregar na portaria");
		order = orderRepository.save(order);

		OrderItem orderItem = new OrderItem();
		orderItem.setOrder(order);
		orderItem.setItem(item);
		orderItem.setQuantity(2);
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
