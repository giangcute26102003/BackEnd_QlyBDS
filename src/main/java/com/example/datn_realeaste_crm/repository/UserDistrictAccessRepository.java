package com.example.datn_realeaste_crm.repository;

import com.example.datn_realeaste_crm.entity.UserDistrictAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDistrictAccessRepository extends JpaRepository<UserDistrictAccess, Integer> {
    List<UserDistrictAccess> findByUserUserId(Integer userId);

    List<UserDistrictAccess> findByDistrictId(Integer districtId);

    Optional<UserDistrictAccess> findByUserUserIdAndDistrictId(Integer userId, Integer districtId);

    boolean existsByUserUserIdAndDistrictId(Integer userId, Integer districtId);

    void deleteByUserUserIdAndDistrictId(Integer userId, Integer districtId);
    
    void deleteByUserUserId(Integer userId);
}