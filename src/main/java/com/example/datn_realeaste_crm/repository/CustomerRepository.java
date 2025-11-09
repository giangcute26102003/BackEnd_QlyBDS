package com.example.datn_realeaste_crm.repository;


import com.example.datn_realeaste_crm.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {
    
    // Hash-based lookups for encrypted fields
    Optional<Customer> findByEmailHash(byte[] emailHash);
    
    Optional<Customer> findByPhoneHash(byte[] phoneHash);
    
    boolean existsByEmailHash(byte[] emailHash);
    
    boolean existsByPhoneHash(byte[] phoneHash);
    
    @Query("SELECT c FROM Customer c JOIN FETCH c.user WHERE c.user.userId = :userId")
    Page<Customer> findByUserUserId(@Param("userId") Integer userId, Pageable pageable);
    
    @Query("SELECT c FROM Customer c JOIN FETCH c.user")
    Page<Customer> findAllWithUser(Pageable pageable);
    
    @Query("SELECT c FROM Customer c JOIN FETCH c.user WHERE c.customerId = :customerId")
    Optional<Customer> findByIdWithUser(@Param("customerId") Integer customerId);
    
    boolean existsByCustomerIdAndUserUserId(Integer customerId, Integer userId);
}