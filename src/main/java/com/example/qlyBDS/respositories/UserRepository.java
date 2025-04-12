package com.example.qlyBDS.respositories;

import com.example.qlyBDS.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {



    // Tìm user bằng email
    Optional<User> findByEmail(String email);

    // Kiểm tra email đã tồn tại chưa
    boolean existsByEmail(String email);

    // Tìm user bằng số điện thoại
    Optional<User> findByPhoneNumber(String phoneNumber);

    // Kiểm tra số điện thoại đã tồn tại chưa
    boolean existsByPhoneNumber(String phoneNumber);

    // Tìm tất cả user theo role
    List<User> findByRoleRoleId(Integer roleId);

    // Tìm user active
    List<User> findByIsActiveTrue();


    // Tìm kiếm user theo keyword (email, name, phone)
    @Query("SELECT u FROM User u WHERE " +
            "(:keyword IS NULL OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "u.phoneNumber LIKE CONCAT('%', :keyword, '%'))")
    Page<User> searchUsers(@Param("keyword") String keyword, Pageable pageable);

    // Tìm kiếm user theo nhiều điều kiện
    @Query("SELECT u FROM User u WHERE " +
            "(:keyword IS NULL OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "u.phoneNumber LIKE CONCAT('%', :keyword, '%')) " +
            "AND (:roleId IS NULL OR u.role.roleId = :roleId) " +
            "AND (:isActive IS NULL OR u.isActive = :isActive)")
    Page<User> findUsers(
            @Param("keyword") String keyword,
            @Param("roleId") Integer roleId,
            @Param("isActive") Boolean isActive,
            Pageable pageable);

    // Đếm số user theo role
    @Query("SELECT COUNT(u) FROM User u WHERE u.role.roleId = :roleId")
    long countByRoleId(@Param("roleId") Integer roleId);

    // Lấy chi tiết user bao gồm role
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.role WHERE u.userId = :userId")
    Optional<User> findDetailById(@Param("userId") Integer userId);

    // Tìm users được tạo trong khoảng thời gian
    @Query("SELECT u FROM User u WHERE " +
            "u.createdAt >= :startDate AND u.createdAt <= :endDate")
    List<User> findByCreatedAtBetween(
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate);

    // Tìm users được cập nhật trong khoảng thời gian
    @Query("SELECT u FROM User u WHERE " +
            "u.updatedAt >= :startDate AND u.updatedAt <= :endDate")
    List<User> findByUpdatedAtBetween(
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate);

    // Tìm users có property trong district
//    @Query("SELECT DISTINCT u FROM User u " +
//            "JOIN u.properties p " +
//            "WHERE p.district.id = :districtId")
//    List<User> findByDistrictId(@Param("districtId") Integer districtId);
//
//    // Tìm users có favorite property
//    @Query("SELECT DISTINCT u FROM User u " +
//            "JOIN u.favorites f " +
//            "WHERE f.property.propertyId = :propertyId")
//    List<User> findByFavoritePropertyId(@Param("propertyId") Integer propertyId);

    // Cập nhật trạng thái active của user
    @Query("UPDATE User u SET u.isActive = :isActive, u.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE u.userId = :userId")
    void updateActiveStatus(
            @Param("userId") Integer userId,
            @Param("isActive") boolean isActive);

    // Xóa users không active trong khoảng thời gian
    @Query("DELETE FROM User u WHERE u.isActive = false AND " +
            "u.updatedAt < :beforeDate")
    void deleteInactiveUsers(@Param("beforeDate") java.time.LocalDateTime beforeDate);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET " +
            "u.name = :fullName, " +
            "u.phoneNumber = :phoneNumber, " +
            "u.email = : email ,"+
            "u.address = :address, " +
            "u.dob = :dateOfBirth, " +
            "u.password = :password " +
            "WHERE u.userId = :userId")
    int updateUser(
            @Param("userId") Long userId,
            @Param("fullName") String fullName,
            @Param("phoneNumber") String phoneNumber,
            @Param("address") String address,
            @Param("dateOfBirth") LocalDate dateOfBirth,
            @Param("facebookAccountId") Long facebookAccountId,
            @Param("googleAccountId") Long googleAccountId,
            @Param("password") String password
    );
}
