package br.com.food.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;

import br.com.food.demo.repository.OrderStatusRepository;
import br.com.food.demo.repository.RoleRepository;

@SpringBootTest
class DemoApplicationTests {

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private OrderStatusRepository orderStatusRepository;

	@Test
	void contextLoads() {
		assertEquals(2, roleRepository.count());
		assertEquals(9, orderStatusRepository.count());
	}

}
