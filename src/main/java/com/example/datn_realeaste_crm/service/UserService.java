package com.example.datn_realeaste_crm.service;

import com.example.datn_realeaste_crm.dto.request.*;
import com.example.datn_realeaste_crm.dto.response.*;
import com.example.datn_realeaste_crm.entity.*;
import com.example.datn_realeaste_crm.exception.ResourceAlreadyExistsException;
import com.example.datn_realeaste_crm.exception.ResourceNotFoundException;
import com.example.datn_realeaste_crm.repository.*;
import com.example.datn_realeaste_crm.security.DepartmentAuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final DepartmentRepository departmentRepository;
    private final PropertyRepository propertyRepository;
    private final DistrictRepository districtRepository;
    private final UserDistrictAccessRepository userDistrictAccessRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenRepository tokenRepository;
    private final DepartmentAuthorizationService departmentAuthorizationService;

    public Page<UserResponse> getAllUsers(Integer departmentId, Boolean isActive, Pageable pageable) {
        Specification<User> spec = Specification.where(null);

        if (departmentId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("department").get("departmentId"), departmentId));
        }

        if (isActive != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("isActive"), isActive));
        }

        return userRepository.findAll(spec, pageable)
                .map(this::convertToUserResponse);
    }

    /**
     * Search users với authorization - Admin thấy tất cả, Manager chỉ thấy users trong department
     */
    public Page<UserResponse> searchUsersWithAuthorization(UserSearchRequest request, Pageable pageable) {
        log.debug("Searching users with authorization. Request: {}", request);
        
        try {
            // Get current user context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserEmail = authentication.getName();
            
            // Get accessible user IDs based on current user's role
            Set<Integer> accessibleUserIds = departmentAuthorizationService.getAccessibleUserIds();
            
            if (accessibleUserIds.isEmpty()) {
                log.warn("User {} has no accessible users", currentUserEmail);
                return Page.empty(pageable);
            }
            
            // Build specification with authorization filter
            Specification<User> spec = Specification.where((root, query, cb) -> 
                root.get("userId").in(accessibleUserIds));
            
            // Apply search filters
            if (request != null) {
                if (request.getName() != null && !request.getName().trim().isEmpty()) {
                    spec = spec.and((root, query, cb) -> 
                        cb.like(cb.lower(root.get("name")), "%" + request.getName().toLowerCase() + "%"));
                }
                
                if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
                    spec = spec.and((root, query, cb) -> 
                        cb.like(cb.lower(root.get("email")), "%" + request.getEmail().toLowerCase() + "%"));
                }
                
                if (request.getDepartmentId() != null) {
                    spec = spec.and((root, query, cb) -> 
                        cb.equal(root.get("department").get("departmentId"), request.getDepartmentId()));
                }
                
                if (request.getIsActive() != null) {
                    spec = spec.and((root, query, cb) -> 
                        cb.equal(root.get("isActive"), request.getIsActive()));
                }
                
                if (request.getRoleName() != null && !request.getRoleName().trim().isEmpty()) {
                    spec = spec.and((root, query, cb) -> 
                        cb.equal(root.join("userRoles").get("role").get("roleName"), request.getRoleName()));
                }
            }
            
            Page<UserResponse> result = userRepository.findAll(spec, pageable)
                    .map(this::convertToUserResponse);
            
            log.debug("Found {} users for user {}", result.getTotalElements(), currentUserEmail);
            return result;
            
        } catch (Exception e) {
            log.error("Error searching users with authorization", e);
            throw new RuntimeException("Failed to search users", e);
        }
    }

    /**
     * Update user status (activate/deactivate) với authorization check
     */
    @Transactional
    public UserResponse updateUserStatus(Integer userId, Boolean isActive) {
        log.debug("Updating user status. UserId: {}, isActive: {}", userId, isActive);
        
        try {
            // Authorization check sẽ được thực hiện ở controller level
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
            
            user.setIsActive(isActive);
            user.setUpdatedAt(LocalDateTime.now());
            
            User updatedUser = userRepository.save(user);
            
            // If deactivating, revoke all tokens
            if (!isActive) {
                tokenRepository.revokeAllUserTokens(userId);
                log.info("Revoked all tokens for deactivated user: {}", userId);
            }
            
            UserResponse response = convertToUserResponse(updatedUser);
            log.info("User status updated successfully. UserId: {}, isActive: {}", userId, isActive);
            
            return response;
            
        } catch (ResourceNotFoundException e) {
            log.error("User not found: {}", userId);
            throw e;
        } catch (Exception e) {
            log.error("Error updating user status", e);
            throw new RuntimeException("Failed to update user status", e);
        }
    }

    /**
     * Delete user - chỉ Admin mới có quyền và không thể xóa chính mình
     */
    @Transactional
    public void deleteUser(Integer userId) {
        log.debug("Deleting user with id: {}", userId);
        
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
            
            // Delete related data first
            // 1. Delete user roles
            userRoleRepository.deleteByUserUserId(userId);
            log.debug("Deleted user roles for user: {}", userId);
            
            // 2. Delete user district access
            userDistrictAccessRepository.deleteByUserUserId(userId);
            log.debug("Deleted user district access for user: {}", userId);
            
            // 3. Revoke all tokens
            tokenRepository.revokeAllUserTokens(userId);
            log.debug("Revoked all tokens for user: {}", userId);
            
            // 4. Delete user
            userRepository.delete(user);
            
            log.info("User deleted successfully: {}", userId);
            
        } catch (ResourceNotFoundException e) {
            log.error("User not found: {}", userId);
            throw e;
        } catch (Exception e) {
            log.error("Error deleting user", e);
            throw new RuntimeException("Failed to delete user", e);
        }
    }

    public UserResponse getUserById(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        return convertToUserResponse(user);
    }

    public UserResponse getUserByEmail(String email) {
        User user = User.builder().email(email).build();
        return convertToUserResponse(user);
    }

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException("Email already in use: " + request.getEmail());
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhoneNumber(request.getPhoneNumber());
        user.setAddress(request.getAddress());
        user.setDob(request.getDob());
        user.setIsActive(true);

        // Set department if provided
        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Department not found with id: " + request.getDepartmentId()));
            user.setDepartment(department);
        }

        User savedUser = userRepository.save(user);

        // Assign roles if provided
        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            for (Integer roleId : request.getRoleIds()) {
                Role role = roleRepository.findById(roleId)
                        .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));

                UserRole userRole = new UserRole();
                userRole.setUser(savedUser);
                userRole.setRole(role);
                userRole.setAssignedAt(LocalDateTime.now());

                userRoleRepository.save(userRole);
            }
        }

        return convertToUserResponse(savedUser);
    }

    @Transactional
    public UserResponse updateUser(Integer id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Check if new email is already used by another user
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail()) &&
                userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException("Email already in use: " + request.getEmail());
        }

        if (request.getName() != null) {
            user.setName(request.getName());
        }

        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }

        if (request.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }

        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }

        if (request.getDob() != null) {
            user.setDob(request.getDob());
        }

        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Department not found with id: " + request.getDepartmentId()));
            user.setDepartment(department);
        }

        user.setUpdatedAt(LocalDateTime.now());
        User updatedUser = userRepository.save(user);

        return convertToUserResponse(updatedUser);
    }

    @Transactional
    public UserResponse deactivateUser(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setIsActive(false);
        user.setUpdatedAt(LocalDateTime.now());

        User updatedUser = userRepository.save(user);
        return convertToUserResponse(updatedUser);
    }

    @Transactional
    public UserResponse activateUser(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setIsActive(true);
        user.setUpdatedAt(LocalDateTime.now());

        User updatedUser = userRepository.save(user);
        return convertToUserResponse(updatedUser);
    }

    @Transactional
    public UserResponse assignRole(Integer userId, Integer roleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));

        // Check if the user already has this role
        Optional<UserRole> existingRole = user.getUserRoles().stream()
                .filter(ur -> ur.getRole().getRoleId().equals(roleId))
                .findFirst();

        if (existingRole.isEmpty()) {
            UserRole userRole = new UserRole();
            userRole.setUser(user);
            userRole.setRole(role);
            userRole.setAssignedAt(LocalDateTime.now());

            userRoleRepository.save(userRole);
        }

        return convertToUserResponse(user);
    }

    @Transactional
    public UserResponse removeRole(Integer userId, Integer roleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Find the user role to remove
        UserRole userRoleToRemove = user.getUserRoles().stream()
                .filter(ur -> ur.getRole().getRoleId().equals(roleId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("User does not have role with id: " + roleId));

        userRoleRepository.delete(userRoleToRemove);

        // Refresh the user
        user = userRepository.findById(userId).orElseThrow();

        return convertToUserResponse(user);
    }

    @Transactional
    public void assignDistrictAccess(Integer userId, Integer districtId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        District district = districtRepository.findById(districtId)
                .orElseThrow(() -> new ResourceNotFoundException("District not found with id: " + districtId));

        // Check if access already exists
        if (!userDistrictAccessRepository.existsByUserUserIdAndDistrictId(userId, districtId)) {
            UserDistrictAccess access = UserDistrictAccess.builder()
                    .user(user)
                    .district(district)
                    .accessGrantedAt(LocalDateTime.now())
                    .build();

            userDistrictAccessRepository.save(access);
        }
    }

    @Transactional
    public void removeDistrictAccess(Integer userId, Integer districtId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        if (!districtRepository.existsById(districtId)) {
            throw new ResourceNotFoundException("District not found with id: " + districtId);
        }

        userDistrictAccessRepository.deleteByUserUserIdAndDistrictId(userId, districtId);
    }

    private UserResponse convertToUserResponse(User user) {
        Set<String> roles = user.getUserRoles().stream()
                .map(userRole -> userRole.getRole().getRoleName())
                .collect(Collectors.toSet());

        return UserResponse.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .address(user.getAddress())
                .dob(user.getDob())
                .isActive(user.getIsActive())
                .departmentId(user.getDepartment() != null ? user.getDepartment().getDepartmentId() : null)
                .departmentName(user.getDepartment() != null ? user.getDepartment().getName() : null)
                .roles(roles)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    @Transactional
    public void changePassword(Integer userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);

        // Revoke all refresh tokens for security
        tokenRepository.revokeAllUserTokens(userId);
    }

    @Transactional
    public List<RoleResponse> getUserRoles(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        return user.getUserRoles().stream()
                .map(userRole -> RoleResponse.builder()
                        .roleId(userRole.getRole().getRoleId())
                        .roleName(userRole.getRole().getRoleName())
                        .description(userRole.getRole().getDescription())
                        .createdAt(userRole.getRole().getCreatedAt())
                        .updatedAt(userRole.getRole().getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách role names của user theo email cho việc hiển thị trên màn hình login
     */
    public List<String> getUserRoles(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        return user.getUserRoles().stream()
                .map(userRole -> userRole.getRole().getRoleName())
                .collect(Collectors.toList());
    }

    /**
     * Lấy thông tin profile chi tiết của user hiện tại
     */
    public UserProfileResponse getCurrentUserProfile(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Lấy tất cả permissions từ các roles
        Set<String> permissions = user.getUserRoles().stream()
                .flatMap(userRole -> userRole.getRole().getRolePermissions().stream())
                .map(rolePermission -> rolePermission.getPermission().getPermissionName())
                .collect(Collectors.toSet());

        // Lấy statistics (có thể tối ưu bằng cách tạo query riêng)
        Integer totalProperties = Math.toIntExact(propertyRepository.countByUserUserId(userId));
        // Giả sử có CustomerRepository và InteractionRepository
        // Integer totalCustomers = customerRepository.countByAssignedUserId(userId);
        // Integer totalInteractions = interactionRepository.countByUserId(userId);

        return UserProfileResponse.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .address(user.getAddress())
                .dob(user.getDob())
                .isActive(user.getIsActive())
                .departmentId(user.getDepartment() != null ? user.getDepartment().getDepartmentId() : null)
                .departmentName(user.getDepartment() != null ? user.getDepartment().getName() : null)
                .roles(user.getUserRoles().stream()
                        .map(userRole -> userRole.getRole().getRoleName())
                        .collect(Collectors.toSet()))
                .permissions(permissions)
                .totalProperties(totalProperties)
                .totalCustomers(0) // Placeholder
                .totalInteractions(0) // Placeholder
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLoginAt(null) // Cần thêm field này vào User entity
                .build();
    }

    /**
     * Cập nhật profile của user hiện tại
     */
    @Transactional
    public UserProfileResponse updateCurrentUserProfile(Integer userId, ProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Kiểm tra email mới có bị trùng không
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail()) &&
                userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException("Email already in use: " + request.getEmail());
        }

        // Cập nhật các trường
        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }
        if (request.getDob() != null) {
            user.setDob(request.getDob());
        }

        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        return getCurrentUserProfile(userId);
    }

    /**
     * Đổi mật khẩu cho user hiện tại với validation
     */
    @Transactional
    public void changeCurrentUserPassword(Integer userId, ChangePasswordRequest request) {
        // Validate confirm password
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadCredentialsException("New password and confirm password do not match");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Revoke all refresh tokens for security
        tokenRepository.revokeAllUserTokens(userId);
    }

    /**
     * Lấy activity log của user hiện tại
     */
    public Page<UserActivityResponse> getCurrentUserActivityLog(Integer userId, Pageable pageable) {
        // Cần tạo query trong AuditLogRepository
        // Page<AuditLog> auditLogs = auditLogRepository.findByUserUserIdOrderByTimestampDesc(userId, pageable);
        
        // return auditLogs.map(log -> UserActivityResponse.builder()
        //         .logId(log.getLogId())
        //         .action(log.getAction())
        //         .entityType(log.getEntityType())
        //         .entityId(log.getEntityId())
        //         .description(log.getAction() + " on " + log.getEntityType())
        //         .ipAddress(log.getIpAddress())
        //         .timestamp(log.getTimestamp())
        //         .previousValue(log.getPreviousValue())
        //         .newValue(log.getNewValue())
        //         .build());
        
        // Placeholder implementation
        return Page.empty(pageable);
    }

}
