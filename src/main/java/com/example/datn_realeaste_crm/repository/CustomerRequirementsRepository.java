package com.example.datn_realeaste_crm.repository;

import com.example.datn_realeaste_crm.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRequirementsRepository extends JpaRepository<CustomerRequirements, Integer> {
    
    @Query("SELECT cr FROM CustomerRequirements cr JOIN FETCH cr.customer c JOIN FETCH c.user WHERE cr.customer.customerId = :customerId")
    Page<CustomerRequirements> findByCustomerCustomerId(@Param("customerId") Integer customerId, Pageable pageable);
    
    @Query("SELECT cr FROM CustomerRequirements cr JOIN FETCH cr.customer c JOIN FETCH c.user WHERE cr.requirementId = :requirementId")
    Optional<CustomerRequirements> findByIdWithCustomerAndUser(@Param("requirementId") Integer requirementId);
    
    @Query("SELECT cr FROM CustomerRequirements cr JOIN FETCH cr.customer c JOIN FETCH c.user WHERE c.user.userId = :userId")
    Page<CustomerRequirements> findByCustomerUserUserId(@Param("userId") Integer userId, Pageable pageable);
    
    @Query("SELECT cr FROM CustomerRequirements cr JOIN FETCH cr.customer c JOIN FETCH c.user")
    Page<CustomerRequirements> findAllWithCustomerAndUser(Pageable pageable);
    
    boolean existsByRequirementIdAndCustomerUserUserId(Integer requirementId, Integer userId);
}