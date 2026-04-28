package com.example.userservice.repository;

import com.example.userservice.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    Page<User> findAllByIdIn(List<Long> ids, Pageable pageable);

    List<User> findAllByIdIn(List<Long> ids);
}
