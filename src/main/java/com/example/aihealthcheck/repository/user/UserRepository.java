package com.example.aihealthcheck.repository.user;

import com.example.aihealthcheck.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByAccount(String account);
    Optional<User> findByAccountAndPassword(String account, String password);
    boolean existsByAccount(String account);
}
