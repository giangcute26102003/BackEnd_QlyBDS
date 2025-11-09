package com.example.datn_realeaste_crm.repository;


import com.example.datn_realeaste_crm.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    // Hash-based lookups for encrypted fields
    Optional<User> findByEmailHash(byte[] emailHash);
    
    Optional<User> findByPhoneHash(byte[] phoneHash);

    boolean existsByEmailHash(byte[] emailHash);
    
    boolean existsByPhoneHash(byte[] phoneHash);

    Optional<User> findAllByName(String name);
    
    List<User> findByDepartmentDepartmentId(Integer departmentId);

    Long countByIsActiveTrue();
    
    Long countByIsActive(Boolean isActive);

    Page<User> findAll(Specification<User> spec, Pageable pageable);

}