package com.example.datn_realeaste_crm.repository;

import com.example.datn_realeaste_crm.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Integer> {
    Optional<Permission> findByPermissionName(String permissionName);

    List<Permission> findByResource(String resource);
    
    List<Permission> findByAction(String action);
}