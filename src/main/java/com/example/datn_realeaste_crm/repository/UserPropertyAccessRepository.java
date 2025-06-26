package com.example.datn_realeaste_crm.repository;

import com.example.datn_realeaste_crm.entity.UserPropertyAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPropertyAccessRepository extends JpaRepository<UserPropertyAccess, Integer> {
    List<UserPropertyAccess> findByUserUserId(Integer userId);

    List<UserPropertyAccess> findByPropertyPropertyId(Integer propertyId);

    Optional<UserPropertyAccess> findByUserUserIdAndPropertyPropertyId(Integer userId, Integer propertyId);

    boolean existsByUserUserIdAndPropertyPropertyId(Integer userId, Integer propertyId);

    void deleteByUserUserIdAndPropertyPropertyId(Integer userId, Integer propertyId);
}