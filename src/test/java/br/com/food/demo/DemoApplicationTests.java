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
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
				.andExpect(jsonPath("$.accessToken").doesNotExist())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.role").value("ADMIN"))
				.andReturn();

		List<String> setCookies = loginResult.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
		String accessTokenCookie = findSetCookie(setCookies, "access_token");
		assertTrue(accessTokenCookie.contains("Path=/api"));
		assertTrue(accessTokenCookie.contains("Secure"));
		assertTrue(accessTokenCookie.contains("HttpOnly"));
		assertTrue(accessTokenCookie.contains("SameSite=Strict"));
		assertTrue(accessTokenCookie.contains("Max-Age=3600"));

		String userDataCookie = findSetCookie(setCookies, "user-data");
		assertTrue(userDataCookie.contains("Path=/"));
		assertTrue(userDataCookie.contains("Secure"));
		assertFalse(userDataCookie.contains("HttpOnly"));
		assertTrue(userDataCookie.contains("SameSite=Strict"));
		assertTrue(userDataCookie.contains("Max-Age=3600"));
		String userData = decodeUserData(cookieValue(userDataCookie));
		assertEquals("ADMIN", JsonPath.read(userData, "$.role"));
		assertEquals("admin@admin.com", JsonPath.read(userData, "$.email"));

		String accessToken = cookieValue(accessTokenCookie);
		Jwt jwt = jwtDecoder.decode(accessToken);
		assertEquals("admin@admin.com", jwt.getClaimAsString("email"));
		assertEquals("ADMIN", jwt.getClaimAsStringList("roles").getFirst());

		mockMvc.perform(get("/api/items")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(post("/api/auth/login")
				.cookie(new Cookie("access_token", "token-antigo"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "admin@admin.com",
						  "password": "1234"
						}
						"""))
				.andExpect(status().isOk());
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
	void swaggerDocumentationIsPublicAndDescribesTheApi() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.info.title").value("Foody Delivery API"))
				.andExpect(jsonPath("$.components.securitySchemes.cookieAuth").exists())
				.andExpect(jsonPath("$.components.securitySchemes.cookieAuth.in").value("cookie"))
				.andExpect(jsonPath("$.components.schemas.SessionResponse.properties.accessToken").doesNotExist())
				.andExpect(jsonPath("$.paths['/api/auth/login'].post.requestBody").exists())
				.andExpect(jsonPath("$.paths['/api/items'].post.requestBody").exists())
				.andExpect(jsonPath("$.paths['/api/orders'].post.requestBody").exists())
				.andExpect(jsonPath("$.paths['/api/admin/orders'].get").exists());

		mockMvc.perform(get("/swagger-ui.html"))
				.andExpect(status().is3xxRedirection());
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
				.andExpect(jsonPath("$.user.role").value("USER"))
				.andExpect(jsonPath("$.user.password").doesNotExist())
				.andExpect(jsonPath("$.token.accessToken").doesNotExist())
				.andExpect(jsonPath("$.token.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.token.expiresIn").value(3600))
				.andExpect(jsonPath("$.token.role").value("USER"))
				.andReturn();

		List<String> registerCookies = registerResult.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
		String accessToken = cookieValue(findSetCookie(registerCookies, "access_token"));
		String userData = decodeUserData(cookieValue(findSetCookie(registerCookies, "user-data")));
		assertEquals("USER", JsonPath.read(userData, "$.role"));

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
				.cookie(new Cookie("access_token", accessToken)))
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
		List<String> clearedCookies = logoutResult.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
		assertTrue(findSetCookie(clearedCookies, "access_token").contains("Max-Age=0"));
		assertTrue(findSetCookie(clearedCookies, "user-data").contains("Max-Age=0"));
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
		String adminToken = cookieValue(findSetCookie(
				adminLoginResult.getResponse().getHeaders(HttpHeaders.SET_COOKIE),
				"access_token"
		));

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
		String customerToken = cookieValue(findSetCookie(
				registerResult.getResponse().getHeaders(HttpHeaders.SET_COOKIE),
				"access_token"
		));

		mockMvc.perform(post("/api/items")
				.cookie(new Cookie("access_token", customerToken))
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
				.cookie(new Cookie("access_token", adminToken))
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
				.cookie(new Cookie("access_token", adminToken))
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
				.cookie(new Cookie("access_token", customerToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(9))
				.andExpect(jsonPath("$[0].name").value("Aguardando confirmação"));

		var createOrderResult = mockMvc.perform(post("/api/orders")
				.cookie(new Cookie("access_token", customerToken))
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
				.cookie(new Cookie("access_token", customerToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").value(orderId));

		mockMvc.perform(get("/api/admin/orders")
				.cookie(new Cookie("access_token", customerToken)))
				.andExpect(status().isForbidden());

		var adminOrdersResult = mockMvc.perform(get("/api/admin/orders")
				.cookie(new Cookie("access_token", adminToken)))
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
				.cookie(new Cookie("access_token", customerToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content(updateStatusPayload))
				.andExpect(status().isForbidden());

		mockMvc.perform(patch("/api/orders/{orderId}/status", orderId)
				.cookie(new Cookie("access_token", adminToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content(updateStatusPayload))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.orderId").value(orderId))
				.andExpect(jsonPath("$.statusId").value(confirmedStatus.getId()))
				.andExpect(jsonPath("$.status").value("Pedido Confirmado"));

		mockMvc.perform(delete("/api/items/{itemId}", itemId)
				.cookie(new Cookie("access_token", adminToken)))
				.andExpect(status().isConflict());

		var disposableItemResult = mockMvc.perform(post("/api/items")
				.cookie(new Cookie("access_token", adminToken))
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
				.cookie(new Cookie("access_token", adminToken)))
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

	private String findSetCookie(List<String> cookies, String name) {
		return cookies.stream()
				.filter(cookie -> cookie.startsWith(name + "="))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Cookie não encontrado: " + name));
	}

	private String cookieValue(String setCookie) {
		int valueStart = setCookie.indexOf('=') + 1;
		int valueEnd = setCookie.indexOf(';', valueStart);
		return setCookie.substring(valueStart, valueEnd);
	}

	private String decodeUserData(String value) {
		byte[] decoded = Base64.getUrlDecoder().decode(value);
		return new String(decoded, StandardCharsets.UTF_8);
	}

}
