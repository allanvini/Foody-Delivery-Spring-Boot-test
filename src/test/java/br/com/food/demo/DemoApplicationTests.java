package br.com.food.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

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
		assertEquals("Admin", admin.getName());
		assertEquals("Não se aplica", admin.getAddress());
		assertTrue(passwordEncoder.matches("1234", admin.getPassword()));
	}

	@Test
	void registerRequiresNameAndAddress() throws Exception {
		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "sem-dados@example.com",
						  "password": "senha-segura"
						}
						"""))
				.andExpect(status().isBadRequest());
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
						  "name": "Cliente Teste",
						  "address": "Rua das Flores, 123",
						  "email": "cliente@example.com",
						  "password": "senha-segura"
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.user.name").value("Cliente Teste"))
				.andExpect(jsonPath("$.user.email").value(email))
				.andExpect(jsonPath("$.user.address").value("Rua das Flores, 123"))
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

	@Test
	void adminItemManagementAndCustomerOrderFlow() throws Exception {
		var adminLoginResult = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "admin@admin.com",
						  "password": "1234"
						}
						"""))
				.andExpect(status().isOk())
				.andReturn();
		String adminToken = JsonPath.read(
				adminLoginResult.getResponse().getContentAsString(),
				"$.accessToken"
		);

		var registerResult = mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "name": "Cliente Pedido",
						  "address": "Rua do Pedido, 10",
						  "email": "pedido@example.com",
						  "password": "senha-segura"
						}
						"""))
				.andExpect(status().isCreated())
				.andReturn();
		String customerToken = JsonPath.read(
				registerResult.getResponse().getContentAsString(),
				"$.token.accessToken"
		);

		mockMvc.perform(post("/api/items")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "name": "Item sem permissão",
						  "price": 1.00,
						  "stock": 1
						}
						"""))
				.andExpect(status().isForbidden());

		var createItemResult = mockMvc.perform(post("/api/items")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "name": "Pizza de teste",
						  "price": 25.50,
						  "stock": 5
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Pizza de teste"))
				.andReturn();
		Number itemIdValue = JsonPath.read(createItemResult.getResponse().getContentAsString(), "$.id");
		long itemId = itemIdValue.longValue();

		mockMvc.perform(put("/api/items/{itemId}", itemId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "name": "Pizza grande de teste",
						  "price": 30.00,
						  "stock": 5
						}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Pizza grande de teste"))
				.andExpect(jsonPath("$.price").value(30.00));

		mockMvc.perform(get("/api/order-statuses")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(9))
				.andExpect(jsonPath("$[0].name").value("Aguardando confirmação"));

		var createOrderResult = mockMvc.perform(post("/api/orders")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "items": [
						    { "id": %d, "quantity": 2 }
						  ],
						  "observations": "Sem cebola"
						}
						""".formatted(itemId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("Aguardando confirmação"))
				.andExpect(jsonPath("$.total").value(60.00))
				.andExpect(jsonPath("$.observations").value("Sem cebola"))
				.andExpect(jsonPath("$.items[0].id").value(itemId))
				.andExpect(jsonPath("$.items[0].quantity").value(2))
				.andReturn();
		Number orderIdValue = JsonPath.read(createOrderResult.getResponse().getContentAsString(), "$.id");
		long orderId = orderIdValue.longValue();

		assertEquals(3, itemRepository.findById(itemId).orElseThrow().getStock());

		mockMvc.perform(get("/api/orders")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").value(orderId));

		mockMvc.perform(get("/api/admin/orders")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
				.andExpect(status().isForbidden());

		var adminOrdersResult = mockMvc.perform(get("/api/admin/orders")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andReturn();
		List<String> orderUserEmails = JsonPath.read(
				adminOrdersResult.getResponse().getContentAsString(),
				"$[?(@.id == " + orderId + ")].user.email"
		);
		assertEquals(List.of("pedido@example.com"), orderUserEmails);

		OrderStatus confirmedStatus = orderStatusRepository.findByName("Pedido Confirmado").orElseThrow();
		String updateStatusPayload = """
				{
				  "statusId": %d
				}
				""".formatted(confirmedStatus.getId());

		mockMvc.perform(patch("/api/orders/{orderId}/status", orderId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(updateStatusPayload))
				.andExpect(status().isForbidden());

		mockMvc.perform(patch("/api/orders/{orderId}/status", orderId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(updateStatusPayload))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.orderId").value(orderId))
				.andExpect(jsonPath("$.statusId").value(confirmedStatus.getId()))
				.andExpect(jsonPath("$.status").value("Pedido Confirmado"));

		mockMvc.perform(delete("/api/items/{itemId}", itemId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
				.andExpect(status().isConflict());

		var disposableItemResult = mockMvc.perform(post("/api/items")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "name": "Item descartável",
						  "price": 5.00,
						  "stock": 1
						}
						"""))
				.andExpect(status().isCreated())
				.andReturn();
		Number disposableItemIdValue = JsonPath.read(
				disposableItemResult.getResponse().getContentAsString(),
				"$.id"
		);

		mockMvc.perform(delete("/api/items/{itemId}", disposableItemIdValue.longValue())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
				.andExpect(status().isNoContent());
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
		anotherUser.setAddress("Avenida Central, 456");
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
