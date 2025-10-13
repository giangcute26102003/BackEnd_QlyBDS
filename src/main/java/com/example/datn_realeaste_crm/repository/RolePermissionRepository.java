package com.example.datn_realeaste_crm.repository;

import com.example.datn_realeaste_crm.entity.RolePermission;
import com.example.datn_realeaste_crm.entity.RolePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {
    List<RolePermission> findByRoleRoleId(Integer roleId);
    
    List<RolePermission> findByPermissionPermissionId(Integer permissionId);

    @Modifying
    @Query("DELETE FROM RolePermission rp WHERE rp.role.roleId = :roleId")
    void deleteByRoleRoleId(Integer roleId);
}