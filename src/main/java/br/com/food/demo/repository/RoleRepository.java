package br.com.food.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.food.demo.entity.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(String name);
}
