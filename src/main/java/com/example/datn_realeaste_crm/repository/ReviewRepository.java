package com.example.datn_realeaste_crm.repository;

import com.example.datn_realeaste_crm.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {
    
    Page<Review> findByPropertyPropertyId(Integer propertyId, Pageable pageable);
    
    Page<Review> findByUserUserId(Integer userId, Pageable pageable);
    
    Page<Review> findByUserUserIdAndPropertyPropertyId(Integer userId, Integer propertyId, Pageable pageable);
    
    List<Review> findByUserUserIdAndPropertyPropertyId(Integer userId, Integer propertyId);
    
    long countByUserUserId(Integer userId);
}