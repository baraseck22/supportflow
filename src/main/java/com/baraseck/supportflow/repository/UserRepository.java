package com.baraseck.supportflow.repository;

import com.baraseck.supportflow.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import com.baraseck.supportflow.entity.Role;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    List<User> findByActiveTrueAndRoleInOrderByFirstNameAscLastNameAsc(Collection<Role> roles);
}
