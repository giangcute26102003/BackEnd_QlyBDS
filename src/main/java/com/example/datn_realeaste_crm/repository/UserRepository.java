package com.example.datn_realeaste_crm.repository;


import com.example.datn_realeaste_crm.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);

    Optional<User> findAllByName(String name);

    boolean existsByEmail(String email);

    Optional<User> findByDepartmentDepartmentId(Integer departmentId);

    Long countByIsActiveTrue();

    Page<User> findAll(Specification<User> spec, Pageable pageable);


}