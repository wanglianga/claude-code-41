package com.example.ridersafety.repository;

import com.example.ridersafety.model.Role;
import com.example.ridersafety.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    List<User> findByRoleAndStation_Id(Role role, Long stationId);

    List<User> findByRole(Role role);
}
