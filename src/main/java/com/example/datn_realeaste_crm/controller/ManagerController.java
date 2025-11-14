package com.example.datn_realeaste_crm.controller;

import com.example.datn_realeaste_crm.audit.Auditable;
import com.example.datn_realeaste_crm.dto.request.AssignDistrictsRequest;
import com.example.datn_realeaste_crm.dto.response.*;
import com.example.datn_realeaste_crm.entity.User;
import com.example.datn_realeaste_crm.exception.ResourceNotFoundException;
import com.example.datn_realeaste_crm.repository.UserRepository;
import com.example.datn_realeaste_crm.service.ManagerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Controller cho các API dành cho Manager
 * Manager có thể quản lý nhân viên, properties, và xem báo cáo trong phòng ban của mình
 */
@RestController
@RequestMapping("/manager")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('customer_create')")
@Slf4j
public class ManagerController {

    private final ManagerService managerService;
    private final UserRepository userRepository;
    private final com.example.datn_realeaste_crm.security.crypto.DeterministicHasher deterministicHasher;

    /**
     * Lấy thông tin tổng quan của phòng ban
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ManagerDashboardResponse> getDashboard() {
        Integer managerId = getCurrentUserId();
        return ResponseEntity.ok(managerService.getManagerDashboard(managerId));
    }

    /**
     * Lấy thống kê chi tiết của phòng ban
     */
    @GetMapping("/department/statistics")
    public ResponseEntity<DepartmentStatisticsResponse> getDepartmentStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Integer managerId = getCurrentUserId();
        return ResponseEntity.ok(managerService.getDepartmentStatistics(managerId, startDate, endDate));
    }

    /**
     * Lấy danh sách nhân viên trong phòng ban
     */
    @GetMapping("/employees")
    public ResponseEntity<Page<UserResponse>> getDepartmentEmployees(
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String roleName,
            Pageable pageable) {
        Integer managerId = getCurrentUserId();
        return ResponseEntity.ok(managerService.getDepartmentEmployees(managerId, isActive, roleName, pageable));
    }

    /**
     * Xem hiệu suất làm việc của một nhân viên
     */
    @GetMapping("/employees/{employeeId}/performance")
    public ResponseEntity<EmployeePerformanceResponse> getEmployeePerformance(
            @PathVariable Integer employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Integer managerId = getCurrentUserId();
        return ResponseEntity.ok(managerService.getEmployeePerformance(managerId, employeeId, startDate, endDate));
    }

    /**
     * Lấy danh sách properties của phòng ban
     */
    @GetMapping("/department/properties")
    public ResponseEntity<Page<PropertyResponse>> getDepartmentProperties(
            @RequestParam(required = false) String propertyType,
            @RequestParam(required = false) Integer availability,
            @RequestParam(required = false) Integer districtId,
            @RequestParam(required = false) Integer assignedUserId,
            Pageable pageable) {
        Integer managerId = getCurrentUserId();
        return ResponseEntity.ok(managerService.getDepartmentProperties(
                managerId, propertyType, availability, districtId, assignedUserId, pageable));
    }

    /**
     * Phân công districts cho nhân viên trong phòng ban
     * Employee sẽ có quyền xem và quản lý properties trong các districts được assign
     */
    @PutMapping("/employees/{employeeId}/assign-districts")
    @Auditable(action = "ASSIGN_DISTRICTS", entityType = "UserDistrictAccess", logResult = true)
    public ResponseEntity<Map<String, String>> assignDistrictsToEmployee(
            @PathVariable Integer employeeId,
            @Valid @RequestBody AssignDistrictsRequest request) {
        Integer managerId = getCurrentUserId();
        managerService.assignDistrictsToEmployee(managerId, employeeId, request.getDistrictIds());
        return ResponseEntity.ok(Map.of(
                "message", "Districts assigned successfully", 
                "districtCount", String.valueOf(request.getDistrictIds().size())
        ));
    }

    /**
     * Chuyển property từ nhân viên này sang nhân viên khác
     */
    @PutMapping("/properties/{propertyId}/reassign")
    @Auditable(action = "REASSIGN_PROPERTY", entityType = "Property", entityIdParam = "propertyId")
    public ResponseEntity<PropertyResponse> reassignProperty(
            @PathVariable Integer propertyId,
            @RequestParam Integer newEmployeeId) {
        Integer managerId = getCurrentUserId();
        return ResponseEntity.ok(managerService.reassignProperty(managerId, propertyId, newEmployeeId));
    }

    /**
     * Lấy báo cáo hiệu suất của toàn bộ team
     */
    @GetMapping("/reports/team-performance")
    public ResponseEntity<TeamPerformanceReport> getTeamPerformanceReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Integer managerId = getCurrentUserId();
        return ResponseEntity.ok(managerService.getTeamPerformanceReport(managerId, startDate, endDate));
    }

    /**
     * Lấy báo cáo về properties theo trạng thái
     */
    @GetMapping("/reports/properties-status")
    public ResponseEntity<PropertiesStatusReport> getPropertiesStatusReport() {
        Integer managerId = getCurrentUserId();
        return ResponseEntity.ok(managerService.getPropertiesStatusReport(managerId));
    }

    /**
     * Lấy danh sách các giao dịch gần đây của phòng ban
     */
    @GetMapping("/department/recent-activities")
    public ResponseEntity<Page<InteractionResponse>> getRecentActivities(
            @RequestParam(required = false) Integer employeeId,
            @RequestParam(required = false) String interactionType,
            Pageable pageable) {
        Integer managerId = getCurrentUserId();
        return ResponseEntity.ok(managerService.getDepartmentRecentActivities(
                managerId, employeeId, interactionType, pageable));
    }

    /**
     * Lấy danh sách khách hàng của phòng ban
     */
    @GetMapping("/department/customers")
    public ResponseEntity<Page<CustomerResponse>> getDepartmentCustomers(
            @RequestParam(required = false) Integer assignedUserId,
            @RequestParam(required = false) String searchKeyword,
            Pageable pageable) {
        Integer managerId = getCurrentUserId();
        return ResponseEntity.ok(managerService.getDepartmentCustomers(
                managerId, assignedUserId, searchKeyword, pageable));
    }

    /**
     * Lấy top performing employees trong phòng ban
     */
    @GetMapping("/employees/top-performers")
    public ResponseEntity<List<EmployeePerformanceSummary>> getTopPerformers(
            @RequestParam(required = false, defaultValue = "5") Integer limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Integer managerId = getCurrentUserId();
        return ResponseEntity.ok(managerService.getTopPerformers(managerId, limit, startDate, endDate));
    }

    /**
     * Export báo cáo phòng ban (placeholder - có thể implement export Excel/PDF)
     */
    @GetMapping("/reports/export")
    public ResponseEntity<Map<String, String>> exportDepartmentReport(
            @RequestParam String reportType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Integer managerId = getCurrentUserId();
        // Placeholder for export functionality
        return ResponseEntity.ok(Map.of(
                "message", "Export functionality will be implemented",
                "reportType", reportType
        ));
    }

    /**
     * Lấy danh sách reviews của properties trong phòng ban
     */
    @GetMapping("/department/reviews")
    public ResponseEntity<Page<ReviewResponse>> getDepartmentReviews(
            @RequestParam(required = false) Integer propertyId,
            @RequestParam(required = false) Integer minRating,
            Pageable pageable) {
        Integer managerId = getCurrentUserId();
        return ResponseEntity.ok(managerService.getDepartmentReviews(
                managerId, propertyId, minRating, pageable));
    }

    /**
     * Phê duyệt hoặc từ chối một property (nếu cần workflow approval)
     */
    @PutMapping("/properties/{propertyId}/approve")
    @Auditable(action = "APPROVE_PROPERTY", entityType = "Property", entityIdParam = "propertyId")
    public ResponseEntity<Map<String, String>> approveProperty(
            @PathVariable Integer propertyId,
            @RequestParam(required = false) String comment) {
        Integer managerId = getCurrentUserId();
        managerService.approveProperty(managerId, propertyId, comment);
        return ResponseEntity.ok(Map.of("message", "Property approved successfully"));
    }

    @PutMapping("/properties/{propertyId}/reject")
    @Auditable(action = "REJECT_PROPERTY", entityType = "Property", entityIdParam = "propertyId")
    public ResponseEntity<Map<String, String>> rejectProperty(
            @PathVariable Integer propertyId,
            @RequestParam String reason) {
        Integer managerId = getCurrentUserId();
        managerService.rejectProperty(managerId, propertyId, reason);
        return ResponseEntity.ok(Map.of("message", "Property rejected", "reason", reason));
    }

    /**
     * Lấy thống kê theo từng nhân viên
     */
    @GetMapping("/employees/statistics")
    public ResponseEntity<List<EmployeeStatisticsSummary>> getEmployeesStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Integer managerId = getCurrentUserId();
        return ResponseEntity.ok(managerService.getEmployeesStatistics(managerId, startDate, endDate));
    }

    // Helper method
    private Integer getCurrentUserId() {
        User currentUser = getCurrentUser();
        return currentUser.getUserId();
    }

    /**
     * Lấy thông tin user hiện tại từ Authentication context
     * @return User entity của user hiện tại
     * @throws ResourceNotFoundException nếu không tìm thấy user
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("No authenticated user found in security context");
            throw new ResourceNotFoundException("No authenticated user found");
        }
        
        String email = authentication.getName();
        if (email == null || email.trim().isEmpty()) {
            log.error("No username found in authentication");
            throw new ResourceNotFoundException("No username found in authentication");
        }
        
        String normalizedEmail = email.trim().toLowerCase(java.util.Locale.ROOT);
        byte[] emailHash = deterministicHasher.emailHash(normalizedEmail);
        User user = userRepository.findByEmailHash(emailHash)
                .orElseThrow(() -> {
                    log.error("User not found with email: {}", email);
                    return new ResourceNotFoundException("User not found with email: " + email);
                });
        
        if (!user.getIsActive()) {
            log.error("User account is inactive: {}", email);
            throw new ResourceNotFoundException("User account is inactive");
        }
        
        return user;
    }
}

