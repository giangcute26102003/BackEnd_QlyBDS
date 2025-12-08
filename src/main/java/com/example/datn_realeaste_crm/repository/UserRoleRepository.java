package com.example.datn_realeaste_crm.repository;

import com.example.datn_realeaste_crm.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {
    
    List<UserRole> findByUserUserId(Integer userId);
    
    List<UserRole> findByRoleRoleId(Integer roleId);
    
    Optional<UserRole> findByUserUserIdAndRoleRoleId(Integer userId, Integer roleId);
    
    void deleteByUserUserIdAndRoleRoleId(Integer userId, Integer roleId);
    
    void deleteByUserUserId(Integer userId);
}
