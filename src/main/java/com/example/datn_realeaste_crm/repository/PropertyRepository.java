package com.example.datn_realeaste_crm.repository;


import com.example.datn_realeaste_crm.entity.AvailabilityStatus;
import com.example.datn_realeaste_crm.entity.Property;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;


@Repository
public interface PropertyRepository extends JpaRepository<Property, Integer>, JpaSpecificationExecutor<Property> {

    @Query("SELECT p FROM Property p WHERE p.availability = 2")
    Page<Property> findAllApproved(Pageable pageable);

    @Query("SELECT p FROM Property p WHERE p.availability = 1")
    Page<Property> findAllPending(Pageable pageable);

    @Query("SELECT p FROM Property p WHERE p.availability = 0")
    Page<Property> findAllRejected(Pageable pageable);

    Page<Property> findByUser_UserId(Integer userId, Pageable pageable);

    Page<Property> findByDepartment_DepartmentId(Integer departmentId, Pageable pageable);

    List<Property> findByPropertyTypeAndDistrictIdAndBedroomsGreaterThanEqual(String propertyType, Integer districtId, Integer bedrooms);

    List<Property> findByDistrictIdIn(List<Integer> districtIds);

    long countByAvailability(AvailabilityStatus availability);

    long countByUserUserId(Integer userId);

    long countByUserUserIdAndAvailability(Integer userId, AvailabilityStatus availability);

    @Query("SELECT AVG(p.price) FROM Property p WHERE p.availability = 2")
    BigDecimal findAveragePrice();
    
    /**
     * Find properties accessible by user through user_district_access table
     * with availability != 0 and ordered by creation date descending
     */
    @Query("SELECT p FROM Property p " +
           "WHERE p.district.id IN (" +
           "    SELECT uda.district.id FROM UserDistrictAccess uda " +
           "    WHERE uda.user.userId = :userId" +
           ") " +
           "AND p.availability != 0 " +
           "ORDER BY p.createdAt DESC")
    Page<Property> findAccessiblePropertiesByUserId(Integer userId, Pageable pageable);
    
    /**
     * Find properties accessible by user with advanced filtering
     */
    @Query("SELECT p FROM Property p " +
           "WHERE p.district.id IN (" +
           "    SELECT uda.district.id FROM UserDistrictAccess uda " +
           "    WHERE uda.user.userId = :userId" +
           ") " +
           "AND p.availability != 0 " +
           "AND (:propertyType IS NULL OR p.propertyType = :propertyType) " +
           "AND (:districtId IS NULL OR p.district.id = :districtId) " +
           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
           "AND (:minBedrooms IS NULL OR p.bedrooms >= :minBedrooms) " +
           "AND (:maxBedrooms IS NULL OR p.bedrooms <= :maxBedrooms) " +
           "AND (:minBathrooms IS NULL OR p.bathrooms >= :minBathrooms) " +
           "AND (:maxBathrooms IS NULL OR p.bathrooms <= :maxBathrooms) " +
           "AND (:minSize IS NULL OR p.size >= :minSize) " +
           "AND (:maxSize IS NULL OR p.size <= :maxSize) " +
           "AND (:floor IS NULL OR p.floor = :floor) " +
           "AND (:availability IS NULL OR p.availability = :availability) " +
           "AND (:keyword IS NULL OR LOWER(p.addressProperty) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Property> findAccessiblePropertiesWithFilter(
            Integer userId,
            String propertyType,
            Integer districtId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Integer minBedrooms,
            Integer maxBedrooms,
            Integer minBathrooms,
            Integer maxBathrooms,
            BigDecimal minSize,
            BigDecimal maxSize,
            Integer floor,
            Integer availability,
            String keyword,
            Pageable pageable);
}