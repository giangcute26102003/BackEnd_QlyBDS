package com.example.datn_realeaste_crm.service;

import com.example.datn_realeaste_crm.dto.response.*;
import com.example.datn_realeaste_crm.entity.*;
import com.example.datn_realeaste_crm.exception.ResourceNotFoundException;
import com.example.datn_realeaste_crm.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManagerService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PropertyRepository propertyRepository;
    private final CustomerRepository customerRepository;
    private final InteractionRepository interactionRepository;
    private final ReviewRepository reviewRepository;
    private final UserDistrictAccessRepository userDistrictAccessRepository;
    private final DistrictRepository districtRepository;
    private final UserService userService;
    private final PropertyService propertyService;
    private final CustomerService customerService;
    private final InteractionService interactionService;
    private final ReviewService reviewService;

    /**
     * Lấy dashboard overview cho manager
     */
    public ManagerDashboardResponse getManagerDashboard(Integer managerId) {
        log.debug("Getting manager dashboard for managerId: {}", managerId);
        
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found with id: " + managerId));

        Department department = manager.getDepartment();
        if (department == null) {
            throw new IllegalStateException("Manager is not assigned to any department");
        }

        // Get all employees in the department
        List<User> employees = userRepository.findByDepartmentDepartmentId(department.getDepartmentId());
        long activeEmployees = employees.stream().filter(User::getIsActive).count();

        // Get all properties in the department
        List<Property> allProperties = propertyRepository.findByDepartment_DepartmentId(
                department.getDepartmentId(), Pageable.unpaged()).getContent();
        
        long availableProperties = allProperties.stream()
                .filter(p -> p.getAvailability() == AvailabilityStatus.AVAILABLE)
                .count();
        
        long soldProperties = allProperties.stream()
                .filter(p -> p.getAvailability() == AvailabilityStatus.SOLD)
                .count();
        
        long pendingProperties = allProperties.stream()
                .filter(p -> p.getAvailability() == AvailabilityStatus.PENDING)
                .count();

        // Calculate average property price
        Double averagePrice = allProperties.stream()
                .map(Property::getPrice)
                .filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue)
                .average()
                .orElse(0.0);

        // Get customers count (customers assigned to employees in this department)
        Set<Integer> employeeIds = employees.stream()
                .map(User::getUserId)
                .collect(Collectors.toSet());
        
        long totalCustomers = customerRepository.findAll().stream()
                .filter(c -> employeeIds.contains(c.getUser().getUserId()))
                .count();

        // Get interactions this month
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDate startOfMonthDate = LocalDate.now().withDayOfMonth(1);
        long interactionsThisMonth = interactionRepository.findAll().stream()
                .filter(i -> i.getDate().isAfter(startOfMonthDate) || i.getDate().isEqual(startOfMonthDate))
                .filter(i -> employeeIds.contains(i.getCustomer().getUser().getUserId()))
                .count();

        // Get new properties this month
        long newPropertiesThisMonth = allProperties.stream()
                .filter(p -> p.getCreatedAt().isAfter(startOfMonth))
                .count();

        // Calculate revenue this month (sum of sold properties this month)
        Double revenueThisMonth = allProperties.stream()
                .filter(p -> p.getAvailability() == AvailabilityStatus.SOLD)
                .filter(p -> p.getUpdatedAt().isAfter(startOfMonth))
                .map(Property::getPrice)
                .filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue)
                .sum();

        return ManagerDashboardResponse.builder()
                .departmentId(department.getDepartmentId())
                .departmentName(department.getName())
                .totalEmployees(employees.size())
                .activeEmployees((int) activeEmployees)
                .totalProperties(allProperties.size())
                .availableProperties((int) availableProperties)
                .soldProperties((int) soldProperties)
                .pendingProperties((int) pendingProperties)
                .totalCustomers((int) totalCustomers)
                .totalInteractionsThisMonth((int) interactionsThisMonth)
                .newPropertiesThisMonth((int) newPropertiesThisMonth)
                .averagePropertyPrice(averagePrice)
                .totalRevenueThisMonth(revenueThisMonth)
                .build();
    }

    /**
     * Lấy thống kê chi tiết của phòng ban
     */
    public DepartmentStatisticsResponse getDepartmentStatistics(
            Integer managerId, LocalDate startDate, LocalDate endDate) {
        
        log.debug("Getting department statistics for managerId: {}", managerId);
        
        User manager = getUserAndValidateDepartment(managerId);
        Department department = manager.getDepartment();

        // Set default date range if not provided
        if (startDate == null) {
            startDate = LocalDate.now().minusMonths(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        final LocalDate finalStartDate = startDate;
        final LocalDate finalEndDate = endDate;
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        // Get all employees and properties
        List<User> employees = userRepository.findByDepartmentDepartmentId(department.getDepartmentId());
        List<Property> allProperties = propertyRepository.findByDepartment_DepartmentId(
                department.getDepartmentId(), Pageable.unpaged()).getContent();

        // Filter properties by date range
        List<Property> periodProperties = allProperties.stream()
                .filter(p -> p.getCreatedAt().isBefore(endDateTime))
                .collect(Collectors.toList());

        long newProperties = allProperties.stream()
                .filter(p -> p.getCreatedAt().isAfter(startDateTime) && p.getCreatedAt().isBefore(endDateTime))
                .count();

        long soldProperties = allProperties.stream()
                .filter(p -> p.getAvailability() == AvailabilityStatus.SOLD)
                .filter(p -> p.getUpdatedAt().isAfter(startDateTime) && p.getUpdatedAt().isBefore(endDateTime))
                .count();

        long activeListings = allProperties.stream()
                .filter(p -> p.getAvailability() == AvailabilityStatus.AVAILABLE)
                .count();

        // Group by type and status
        Map<String, Integer> propertiesByType = periodProperties.stream()
                .collect(Collectors.groupingBy(
                        Property::getPropertyType,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        Map<String, Integer> propertiesByStatus = periodProperties.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getAvailability().name(),
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        // Financial statistics
        List<BigDecimal> prices = periodProperties.stream()
                .map(Property::getPrice)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        BigDecimal totalRevenue = allProperties.stream()
                .filter(p -> p.getAvailability() == AvailabilityStatus.SOLD)
                .filter(p -> p.getUpdatedAt().isAfter(startDateTime) && p.getUpdatedAt().isBefore(endDateTime))
                .map(Property::getPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgPrice = prices.isEmpty() ? BigDecimal.ZERO :
                prices.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(prices.size()), 2, RoundingMode.HALF_UP);

        BigDecimal highestPrice = prices.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal lowestPrice = prices.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

        // Customer statistics
        Set<Integer> employeeIds = employees.stream().map(User::getUserId).collect(Collectors.toSet());
        
        List<Customer> allCustomers = customerRepository.findAll().stream()
                .filter(c -> employeeIds.contains(c.getUser().getUserId()))
                .collect(Collectors.toList());

        long newCustomers = allCustomers.stream()
                .filter(c -> c.getCreatedAt().isAfter(startDateTime) && c.getCreatedAt().isBefore(endDateTime))
                .count();

        long totalInteractions = interactionRepository.findAll().stream()
                .filter(i -> (i.getDate().isAfter(finalStartDate) || i.getDate().isEqual(finalStartDate)) && 
                             (i.getDate().isBefore(finalEndDate) || i.getDate().isEqual(finalEndDate)))
                .filter(i -> employeeIds.contains(i.getCustomer().getUser().getUserId()))
                .count();

        // Performance metrics
        double conversionRate = periodProperties.isEmpty() ? 0.0 :
                (double) soldProperties / periodProperties.size() * 100;

        return DepartmentStatisticsResponse.builder()
                .departmentId(department.getDepartmentId())
                .departmentName(department.getName())
                .startDate(finalStartDate)
                .endDate(finalEndDate)
                .totalProperties(periodProperties.size())
                .newProperties((int) newProperties)
                .soldProperties((int) soldProperties)
                .activeListings((int) activeListings)
                .propertiesByType(propertiesByType)
                .propertiesByStatus(propertiesByStatus)
                .totalEmployees(employees.size())
                .activeEmployees((int) employees.stream().filter(User::getIsActive).count())
                .totalRevenue(totalRevenue)
                .averagePropertyPrice(avgPrice)
                .highestPropertyPrice(highestPrice)
                .lowestPropertyPrice(lowestPrice)
                .totalCustomers(allCustomers.size())
                .newCustomers((int) newCustomers)
                .totalInteractions((int) totalInteractions)
                .conversionRate(conversionRate)
                .averageResponseTime(0.0) // Placeholder
                .build();
    }

    /**
     * Lấy danh sách nhân viên trong phòng ban
     */
    public Page<UserResponse> getDepartmentEmployees(
            Integer managerId, Boolean isActive, String roleName, Pageable pageable) {
        
        User manager = getUserAndValidateDepartment(managerId);
        Department department = manager.getDepartment();

        Specification<User> spec = Specification.where(
                (root, query, cb) -> cb.equal(root.get("department").get("departmentId"), 
                        department.getDepartmentId()));

        if (isActive != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("isActive"), isActive));
        }

        if (roleName != null && !roleName.trim().isEmpty()) {
            spec = spec.and((root, query, cb) -> 
                    cb.equal(root.join("userRoles").get("role").get("roleName"), roleName));
        }

        return userRepository.findAll(spec, pageable)
                .map(this::convertToUserResponse);
    }

    /**
     * Lấy hiệu suất làm việc của một nhân viên
     */
    public EmployeePerformanceResponse getEmployeePerformance(
            Integer managerId, Integer employeeId, LocalDate startDate, LocalDate endDate) {
        
        User manager = getUserAndValidateDepartment(managerId);
        User employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));

        // Verify employee is in manager's department
        if (!employee.getDepartment().getDepartmentId().equals(manager.getDepartment().getDepartmentId())) {
            throw new AccessDeniedException("Employee is not in your department");
        }

        // Set default date range
        if (startDate == null) {
            startDate = LocalDate.now().minusMonths(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        final LocalDate finalStartDate = startDate;
        final LocalDate finalEndDate = endDate;
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        // Get employee's properties
        List<Property> allProperties = propertyRepository.findByUser_UserId(employeeId, Pageable.unpaged())
                .getContent();

        long newPropertiesAdded = allProperties.stream()
                .filter(p -> p.getCreatedAt().isAfter(startDateTime) && p.getCreatedAt().isBefore(endDateTime))
                .count();

        long soldProperties = allProperties.stream()
                .filter(p -> p.getAvailability() == AvailabilityStatus.SOLD)
                .filter(p -> p.getUpdatedAt().isAfter(startDateTime) && p.getUpdatedAt().isBefore(endDateTime))
                .count();

        long activeProperties = allProperties.stream()
                .filter(p -> p.getAvailability() == AvailabilityStatus.AVAILABLE)
                .count();

        // Get employee's customers
        Page<Customer> customersPage = customerRepository.findByUserUserId(employeeId, Pageable.unpaged());
        List<Customer> allCustomers = customersPage.getContent();

        long newCustomersAcquired = allCustomers.stream()
                .filter(c -> c.getCreatedAt().isAfter(startDateTime) && c.getCreatedAt().isBefore(endDateTime))
                .count();

        // Get interactions
        long totalInteractions = interactionRepository.findAll().stream()
                .filter(i -> i.getCustomer().getUser().getUserId().equals(employeeId))
                .filter(i -> (i.getDate().isAfter(finalStartDate) || i.getDate().isEqual(finalStartDate)) && 
                             (i.getDate().isBefore(finalEndDate) || i.getDate().isEqual(finalEndDate)))
                .count();

        // Calculate revenue
        BigDecimal totalRevenue = allProperties.stream()
                .filter(p -> p.getAvailability() == AvailabilityStatus.SOLD)
                .filter(p -> p.getUpdatedAt().isAfter(startDateTime) && p.getUpdatedAt().isBefore(endDateTime))
                .map(Property::getPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageDealValue = soldProperties > 0 ?
                totalRevenue.divide(BigDecimal.valueOf(soldProperties), 2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

        // Calculate conversion rate
        double conversionRate = allProperties.isEmpty() ? 0.0 :
                (double) soldProperties / allProperties.size() * 100;

        // Get recent interactions
        List<InteractionResponse> recentInteractions = interactionRepository.findAll().stream()
                .filter(i -> i.getCustomer().getUser().getUserId().equals(employeeId))
                .filter(i -> i.getDate().isAfter(finalStartDate) || i.getDate().isEqual(finalStartDate))
                .sorted((i1, i2) -> i2.getDate().compareTo(i1.getDate()))
                .limit(5)
                .map(this::convertToInteractionResponse)
                .collect(Collectors.toList());

        return EmployeePerformanceResponse.builder()
                .userId(employee.getUserId())
                .name(employee.getName())
                .email(employee.getEmail())
                .phoneNumber(employee.getPhoneNumber())
                .startDate(finalStartDate)
                .endDate(finalEndDate)
                .totalProperties(allProperties.size())
                .newPropertiesAdded((int) newPropertiesAdded)
                .soldProperties((int) soldProperties)
                .activeProperties((int) activeProperties)
                .totalCustomers(allCustomers.size())
                .newCustomersAcquired((int) newCustomersAcquired)
                .totalInteractions((int) totalInteractions)
                .totalRevenue(totalRevenue)
                .averageDealValue(averageDealValue)
                .conversionRate(conversionRate)
                .customerSatisfactionScore(0.0) // Placeholder
                .averageResponseTimeHours(0) // Placeholder
                .callsMade(0) // Placeholder
                .meetingsHeld(0) // Placeholder
                .emailsSent(0) // Placeholder
                .recentInteractions(recentInteractions)
                .build();
    }

    /**
     * Lấy danh sách properties của phòng ban
     */
    public Page<PropertyResponse> getDepartmentProperties(
            Integer managerId, String propertyType, Integer availability,
            Integer districtId, Integer assignedUserId, Pageable pageable) {
        
        User manager = getUserAndValidateDepartment(managerId);
        Department department = manager.getDepartment();

        Specification<Property> spec = Specification.where(
                (root, query, cb) -> cb.equal(root.get("department").get("departmentId"),
                        department.getDepartmentId()));

        if (propertyType != null && !propertyType.trim().isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("propertyType"), propertyType));
        }

        if (availability != null) {
            AvailabilityStatus status = AvailabilityStatus.values()[availability];
            spec = spec.and((root, query, cb) -> cb.equal(root.get("availability"), status));
        }

        if (districtId != null) {
            spec = spec.and((root, query, cb) -> 
                    cb.equal(root.get("district").get("id"), districtId));
        }

        if (assignedUserId != null) {
            spec = spec.and((root, query, cb) -> 
                    cb.equal(root.get("user").get("userId"), assignedUserId));
        }

        return propertyRepository.findAll(spec, pageable)
                .map(this::convertToPropertyResponse);
    }

    /**
     * Phân công districts cho nhân viên
     * Employee sẽ có quyền xem và quản lý các properties trong các districts được assign
     */
    @Transactional
    public void assignDistrictsToEmployee(Integer managerId, Integer employeeId, List<Integer> districtIds) {
        log.debug("Assigning districts to employee. ManagerId: {}, EmployeeId: {}, Districts: {}", 
                managerId, employeeId, districtIds);

        User manager = getUserAndValidateDepartment(managerId);
        User employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));

        // Verify employee is in manager's department
        if (!employee.getDepartment().getDepartmentId().equals(manager.getDepartment().getDepartmentId())) {
            throw new AccessDeniedException("Employee is not in your department");
        }

        // Xóa tất cả district access hiện tại của employee (nếu muốn override)
        // Hoặc có thể chỉ thêm mới không xóa
        // userDistrictAccessRepository.deleteByUserUserId(employeeId);

        // Assign các districts mới
        for (Integer districtId : districtIds) {
            District district = districtRepository.findById(districtId)
                    .orElseThrow(() -> new ResourceNotFoundException("District not found with id: " + districtId));

            // Kiểm tra xem đã tồn tại chưa để tránh duplicate
            if (!userDistrictAccessRepository.existsByUserUserIdAndDistrictId(employeeId, districtId)) {
                UserDistrictAccess access = UserDistrictAccess.builder()
                        .user(employee)
                        .district(district)
                        .accessGrantedAt(LocalDateTime.now())
                        .build();
                
                userDistrictAccessRepository.save(access);
                log.debug("Granted district access: employeeId={}, districtId={}", employeeId, districtId);
            } else {
                log.debug("District access already exists: employeeId={}, districtId={}", employeeId, districtId);
            }
        }

        log.info("Successfully assigned {} districts to employee {}", districtIds.size(), employeeId);
    }

    /**
     * Chuyển property từ nhân viên này sang nhân viên khác
     */
    @Transactional
    public PropertyResponse reassignProperty(Integer managerId, Integer propertyId, Integer newEmployeeId) {
        log.debug("Reassigning property. ManagerId: {}, PropertyId: {}, NewEmployeeId: {}", 
                managerId, propertyId, newEmployeeId);

        User manager = getUserAndValidateDepartment(managerId);
        
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + propertyId));

        User newEmployee = userRepository.findById(newEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + newEmployeeId));

        // Verify both property and employee are in manager's department
        if (!property.getDepartment().getDepartmentId().equals(manager.getDepartment().getDepartmentId())) {
            throw new AccessDeniedException("Property is not in your department");
        }

        if (!newEmployee.getDepartment().getDepartmentId().equals(manager.getDepartment().getDepartmentId())) {
            throw new AccessDeniedException("Employee is not in your department");
        }

        property.setUser(newEmployee);
        property.setUpdatedAt(LocalDateTime.now());
        Property updatedProperty = propertyRepository.save(property);

        log.info("Successfully reassigned property {} to employee {}", propertyId, newEmployeeId);
        
        return convertToPropertyResponse(updatedProperty);
    }

    /**
     * Lấy báo cáo hiệu suất của toàn bộ team
     */
    public TeamPerformanceReport getTeamPerformanceReport(
            Integer managerId, LocalDate startDate, LocalDate endDate) {
        
        User manager = getUserAndValidateDepartment(managerId);
        Department department = manager.getDepartment();

        if (startDate == null) {
            startDate = LocalDate.now().minusMonths(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        final LocalDate finalStartDate = startDate;
        final LocalDate finalEndDate = endDate;
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        List<User> employees = userRepository.findByDepartmentDepartmentId(department.getDepartmentId());
        
        // Calculate overall metrics
        int totalPropertiesSold = 0;
        int totalNewProperties = 0;
        int totalCustomersAcquired = 0;
        int totalInteractions = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;

        List<EmployeePerformanceSummary> employeePerformances = new ArrayList<>();

        for (User employee : employees) {
            List<Property> properties = propertyRepository.findByUser_UserId(
                    employee.getUserId(), Pageable.unpaged()).getContent();

            int soldProps = (int) properties.stream()
                    .filter(p -> p.getAvailability() == AvailabilityStatus.SOLD)
                    .filter(p -> p.getUpdatedAt().isAfter(startDateTime) && p.getUpdatedAt().isBefore(endDateTime))
                    .count();

            int newProps = (int) properties.stream()
                    .filter(p -> p.getCreatedAt().isAfter(startDateTime) && p.getCreatedAt().isBefore(endDateTime))
                    .count();

            BigDecimal revenue = properties.stream()
                    .filter(p -> p.getAvailability() == AvailabilityStatus.SOLD)
                    .filter(p -> p.getUpdatedAt().isAfter(startDateTime) && p.getUpdatedAt().isBefore(endDateTime))
                    .map(Property::getPrice)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            List<Customer> customers = customerRepository.findByUserUserId(
                    employee.getUserId(), Pageable.unpaged()).getContent();

            int interactions = (int) interactionRepository.findAll().stream()
                    .filter(i -> i.getCustomer().getUser().getUserId().equals(employee.getUserId()))
                    .filter(i -> (i.getDate().isAfter(finalStartDate) || i.getDate().isEqual(finalStartDate)) && 
                                 (i.getDate().isBefore(finalEndDate) || i.getDate().isEqual(finalEndDate)))
                    .count();

            double conversionRate = properties.isEmpty() ? 0.0 :
                    (double) soldProps / properties.size() * 100;

            totalPropertiesSold += soldProps;
            totalNewProperties += newProps;
            totalCustomersAcquired += customers.size();
            totalInteractions += interactions;
            totalRevenue = totalRevenue.add(revenue);

            employeePerformances.add(EmployeePerformanceSummary.builder()
                    .userId(employee.getUserId())
                    .name(employee.getName())
                    .email(employee.getEmail())
                    .totalProperties(properties.size())
                    .soldProperties(soldProps)
                    .totalCustomers(customers.size())
                    .totalInteractions(interactions)
                    .totalRevenue(revenue)
                    .conversionRate(conversionRate)
                    .performanceScore(calculatePerformanceScore(soldProps, interactions, revenue))
                    .performanceLevel(determinePerformanceLevel(soldProps, conversionRate))
                    .build());
        }

        // Sort by performance score
        employeePerformances.sort((e1, e2) -> e2.getPerformanceScore().compareTo(e1.getPerformanceScore()));

        double avgConversionRate = employeePerformances.stream()
                .mapToDouble(EmployeePerformanceSummary::getConversionRate)
                .average()
                .orElse(0.0);

        BigDecimal revenuePerEmployee = employees.isEmpty() ? BigDecimal.ZERO :
                totalRevenue.divide(BigDecimal.valueOf(employees.size()), 2, RoundingMode.HALF_UP);

        return TeamPerformanceReport.builder()
                .departmentId(department.getDepartmentId())
                .departmentName(department.getName())
                .startDate(finalStartDate)
                .endDate(finalEndDate)
                .totalEmployees(employees.size())
                .totalRevenue(totalRevenue)
                .totalPropertiesSold(totalPropertiesSold)
                .totalNewProperties(totalNewProperties)
                .totalCustomersAcquired(totalCustomersAcquired)
                .totalInteractions(totalInteractions)
                .averageConversionRate(avgConversionRate)
                .teamProductivity(calculateTeamProductivity(totalPropertiesSold, employees.size()))
                .revenuePerEmployee(revenuePerEmployee)
                .employeePerformances(employeePerformances)
                .dailyPerformance(new ArrayList<>()) // Placeholder
                .build();
    }

    /**
     * Lấy báo cáo về properties theo trạng thái
     */
    public PropertiesStatusReport getPropertiesStatusReport(Integer managerId) {
        User manager = getUserAndValidateDepartment(managerId);
        Department department = manager.getDepartment();

        List<Property> allProperties = propertyRepository.findByDepartment_DepartmentId(
                department.getDepartmentId(), Pageable.unpaged()).getContent();

        Map<String, Integer> propertiesByType = allProperties.stream()
                .collect(Collectors.groupingBy(
                        Property::getPropertyType,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        Map<String, Integer> propertiesByStatus = allProperties.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getAvailability().name(),
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        Map<String, Integer> propertiesByDistrict = allProperties.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getDistrict().getName(),
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        Map<String, Integer> propertiesByUser = allProperties.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getUser().getName(),
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        return PropertiesStatusReport.builder()
                .departmentId(department.getDepartmentId())
                .departmentName(department.getName())
                .totalProperties(allProperties.size())
                .availableProperties((int) allProperties.stream()
                        .filter(p -> p.getAvailability() == AvailabilityStatus.AVAILABLE).count())
                .soldProperties((int) allProperties.stream()
                        .filter(p -> p.getAvailability() == AvailabilityStatus.SOLD).count())
                .pendingProperties((int) allProperties.stream()
                        .filter(p -> p.getAvailability() == AvailabilityStatus.PENDING).count())
                .unavailableProperties(0) // Placeholder
                .propertiesByType(propertiesByType)
                .propertiesByStatus(propertiesByStatus)
                .propertiesByDistrict(propertiesByDistrict)
                .propertiesByUser(propertiesByUser)
                .build();
    }

    /**
     * Lấy danh sách các giao dịch gần đây của phòng ban
     */
    public Page<InteractionResponse> getDepartmentRecentActivities(
            Integer managerId, Integer employeeId, String interactionType, Pageable pageable) {
        
        User manager = getUserAndValidateDepartment(managerId);
        Department department = manager.getDepartment();

        List<User> employees = userRepository.findByDepartmentDepartmentId(department.getDepartmentId());
        Set<Integer> employeeIds = employees.stream().map(User::getUserId).collect(Collectors.toSet());

        // Filter interactions by department employees
        Page<Interaction> interactions;
        if (employeeId != null) {
            // Get interactions for specific employee
            Page<Customer> customers = customerRepository.findByUserUserId(employeeId, Pageable.unpaged());
            List<Integer> customerIds = customers.getContent().stream()
                    .map(Customer::getCustomerId)
                    .collect(Collectors.toList());
            
            if (customerIds.isEmpty()) {
                return Page.empty(pageable);
            }
            
            interactions = interactionRepository.findByCustomerCustomerId(customerIds.get(0), pageable);
        } else {
            // Get all interactions for department
            interactions = interactionRepository.findAll(pageable);
        }

        return interactions.map(this::convertToInteractionResponse);
    }

    /**
     * Lấy danh sách khách hàng của phòng ban
     */
    public Page<CustomerResponse> getDepartmentCustomers(
            Integer managerId, Integer assignedUserId, String searchKeyword, Pageable pageable) {
        
        User manager = getUserAndValidateDepartment(managerId);
        Department department = manager.getDepartment();

        List<User> employees = userRepository.findByDepartmentDepartmentId(department.getDepartmentId());

        if (assignedUserId != null) {
            // Get customers for specific user
            return customerRepository.findByUserUserId(assignedUserId, pageable)
                    .map(this::convertToCustomerResponse);
        } else {
            // Get all customers for department employees
            // Simple implementation - get all and filter
            List<Customer> allCustomers = new ArrayList<>();
            for (User employee : employees) {
                List<Customer> empCustomers = customerRepository.findByUserUserId(
                        employee.getUserId(), Pageable.unpaged()).getContent();
                
                if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
                    empCustomers = empCustomers.stream()
                            .filter(c -> c.getName().toLowerCase().contains(searchKeyword.toLowerCase()))
                            .collect(Collectors.toList());
                }
                
                allCustomers.addAll(empCustomers);
            }
            
            // Manual pagination
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), allCustomers.size());
            
            List<CustomerResponse> pageContent = allCustomers.subList(
                    Math.min(start, allCustomers.size()), 
                    Math.min(end, allCustomers.size()))
                    .stream()
                    .map(this::convertToCustomerResponse)
                    .collect(Collectors.toList());
            
            return new org.springframework.data.domain.PageImpl<>(
                    pageContent, pageable, allCustomers.size());
        }
    }

    /**
     * Lấy top performing employees
     */
    public List<EmployeePerformanceSummary> getTopPerformers(
            Integer managerId, Integer limit, LocalDate startDate, LocalDate endDate) {
        
        User manager = getUserAndValidateDepartment(managerId);
        Department department = manager.getDepartment();

        if (startDate == null) {
            startDate = LocalDate.now().minusMonths(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        final LocalDate finalStartDate = startDate;
        final LocalDate finalEndDate = endDate;
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        List<User> employees = userRepository.findByDepartmentDepartmentId(department.getDepartmentId());
        
        List<EmployeePerformanceSummary> performances = new ArrayList<>();

        for (User employee : employees) {
            List<Property> properties = propertyRepository.findByUser_UserId(
                    employee.getUserId(), Pageable.unpaged()).getContent();

            int soldProps = (int) properties.stream()
                    .filter(p -> p.getAvailability() == AvailabilityStatus.SOLD)
                    .filter(p -> p.getUpdatedAt().isAfter(startDateTime) && p.getUpdatedAt().isBefore(endDateTime))
                    .count();

            BigDecimal revenue = properties.stream()
                    .filter(p -> p.getAvailability() == AvailabilityStatus.SOLD)
                    .filter(p -> p.getUpdatedAt().isAfter(startDateTime) && p.getUpdatedAt().isBefore(endDateTime))
                    .map(Property::getPrice)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            List<Customer> customers = customerRepository.findByUserUserId(
                    employee.getUserId(), Pageable.unpaged()).getContent();

            int interactions = (int) interactionRepository.findAll().stream()
                    .filter(i -> i.getCustomer().getUser().getUserId().equals(employee.getUserId()))
                    .filter(i -> (i.getDate().isAfter(finalStartDate) || i.getDate().isEqual(finalStartDate)) && 
                                 (i.getDate().isBefore(finalEndDate) || i.getDate().isEqual(finalEndDate)))
                    .count();

            double conversionRate = properties.isEmpty() ? 0.0 :
                    (double) soldProps / properties.size() * 100;

            performances.add(EmployeePerformanceSummary.builder()
                    .userId(employee.getUserId())
                    .name(employee.getName())
                    .email(employee.getEmail())
                    .totalProperties(properties.size())
                    .soldProperties(soldProps)
                    .totalCustomers(customers.size())
                    .totalInteractions(interactions)
                    .totalRevenue(revenue)
                    .conversionRate(conversionRate)
                    .performanceScore(calculatePerformanceScore(soldProps, interactions, revenue))
                    .performanceLevel(determinePerformanceLevel(soldProps, conversionRate))
                    .build());
        }

        return performances.stream()
                .sorted((e1, e2) -> e2.getPerformanceScore().compareTo(e1.getPerformanceScore()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách reviews của properties trong phòng ban
     */
    public Page<ReviewResponse> getDepartmentReviews(
            Integer managerId, Integer propertyId, Integer minRating, Pageable pageable) {
        
        User manager = getUserAndValidateDepartment(managerId);
        Department department = manager.getDepartment();

        if (propertyId != null) {
            // Get reviews for specific property
            Property property = propertyRepository.findById(propertyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + propertyId));
            
            if (!property.getDepartment().getDepartmentId().equals(department.getDepartmentId())) {
                throw new AccessDeniedException("Property is not in your department");
            }
            
            return reviewRepository.findByPropertyPropertyId(propertyId, pageable)
                    .map(this::convertToReviewResponse);
        } else {
            // Get all reviews for department properties
            List<Property> properties = propertyRepository.findByDepartment_DepartmentId(
                    department.getDepartmentId(), Pageable.unpaged()).getContent();
            
            List<Review> allReviews = new ArrayList<>();
            for (Property prop : properties) {
                allReviews.addAll(reviewRepository.findByPropertyPropertyId(
                        prop.getPropertyId(), Pageable.unpaged()).getContent());
            }
            
            // Manual pagination
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), allReviews.size());
            
            List<ReviewResponse> pageContent = allReviews.subList(
                    Math.min(start, allReviews.size()), 
                    Math.min(end, allReviews.size()))
                    .stream()
                    .map(this::convertToReviewResponse)
                    .collect(Collectors.toList());
            
            return new org.springframework.data.domain.PageImpl<>(
                    pageContent, pageable, allReviews.size());
        }
    }

    /**
     * Phê duyệt property
     */
    @Transactional
    public void approveProperty(Integer managerId, Integer propertyId, String comment) {
        User manager = getUserAndValidateDepartment(managerId);
        
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + propertyId));

        if (!property.getDepartment().getDepartmentId().equals(manager.getDepartment().getDepartmentId())) {
            throw new AccessDeniedException("Property is not in your department");
        }

        property.setAvailability(AvailabilityStatus.AVAILABLE);
        property.setUpdatedAt(LocalDateTime.now());
        propertyRepository.save(property);

        log.info("Property {} approved by manager {}", propertyId, managerId);
    }

    /**
     * Từ chối property
     */
    @Transactional
    public void rejectProperty(Integer managerId, Integer propertyId, String reason) {
        User manager = getUserAndValidateDepartment(managerId);
        
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + propertyId));

        if (!property.getDepartment().getDepartmentId().equals(manager.getDepartment().getDepartmentId())) {
            throw new AccessDeniedException("Property is not in your department");
        }

        property.setAvailability(AvailabilityStatus.PENDING);
        property.setUpdatedAt(LocalDateTime.now());
        propertyRepository.save(property);

        log.info("Property {} rejected by manager {}. Reason: {}", propertyId, managerId, reason);
    }

    /**
     * Lấy thống kê theo từng nhân viên
     */
    public List<EmployeeStatisticsSummary> getEmployeesStatistics(
            Integer managerId, LocalDate startDate, LocalDate endDate) {
        
        User manager = getUserAndValidateDepartment(managerId);
        Department department = manager.getDepartment();

        if (startDate == null) {
            startDate = LocalDate.now().minusMonths(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        List<User> employees = userRepository.findByDepartmentDepartmentId(department.getDepartmentId());
        
        List<EmployeeStatisticsSummary> statistics = new ArrayList<>();

        for (User employee : employees) {
            List<Property> properties = propertyRepository.findByUser_UserId(
                    employee.getUserId(), Pageable.unpaged()).getContent();

            int activeProps = (int) properties.stream()
                    .filter(p -> p.getAvailability() == AvailabilityStatus.AVAILABLE)
                    .count();

            int soldProps = (int) properties.stream()
                    .filter(p -> p.getAvailability() == AvailabilityStatus.SOLD)
                    .count();

            List<Customer> customers = customerRepository.findByUserUserId(
                    employee.getUserId(), Pageable.unpaged()).getContent();

            int activeCustomers = (int) customers.stream()
                    .filter(c -> c.getCreatedAt().isAfter(startDateTime))
                    .count();

            List<Customer> empCustomers = customerRepository.findByUserUserId(
                    employee.getUserId(), Pageable.unpaged()).getContent();
            int totalInteractions = empCustomers.stream()
                    .mapToInt(c -> (int) interactionRepository.findByCustomerCustomerId(
                            c.getCustomerId(), Pageable.unpaged()).getTotalElements())
                    .sum();

            LocalDate startOfMonthDate = LocalDate.now().withDayOfMonth(1);
            int interactionsThisMonth = (int) interactionRepository.findAll().stream()
                    .filter(i -> i.getCustomer().getUser().getUserId().equals(employee.getUserId()))
                    .filter(i -> i.getDate().isAfter(startOfMonthDate) || i.getDate().isEqual(startOfMonthDate))
                    .count();

            BigDecimal totalRevenue = properties.stream()
                    .filter(p -> p.getAvailability() == AvailabilityStatus.SOLD)
                    .map(Property::getPrice)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal revenueThisMonth = properties.stream()
                    .filter(p -> p.getAvailability() == AvailabilityStatus.SOLD)
                    .filter(p -> p.getUpdatedAt().isAfter(startOfMonth))
                    .map(Property::getPrice)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            double conversionRate = properties.isEmpty() ? 0.0 :
                    (double) soldProps / properties.size() * 100;

            statistics.add(EmployeeStatisticsSummary.builder()
                    .userId(employee.getUserId())
                    .name(employee.getName())
                    .email(employee.getEmail())
                    .phoneNumber(employee.getPhoneNumber())
                    .totalProperties(properties.size())
                    .activeProperties(activeProps)
                    .soldProperties(soldProps)
                    .totalCustomers(customers.size())
                    .activeCustomers(activeCustomers)
                    .totalInteractions(totalInteractions)
                    .interactionsThisMonth(interactionsThisMonth)
                    .totalRevenue(totalRevenue)
                    .revenueThisMonth(revenueThisMonth)
                    .conversionRate(conversionRate)
                    .status(employee.getIsActive() ? "Active" : "Inactive")
                    .build());
        }

        return statistics;
    }

    // Helper methods

    private User getUserAndValidateDepartment(Integer managerId) {
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found with id: " + managerId));

        if (manager.getDepartment() == null) {
            throw new IllegalStateException("Manager is not assigned to any department");
        }

        return manager;
    }

    private int calculatePerformanceScore(int soldProperties, int interactions, BigDecimal revenue) {
        // Simple scoring algorithm: weighted sum
        int score = soldProperties * 10 + interactions * 2;
        if (revenue.compareTo(BigDecimal.valueOf(1000000)) > 0) {
            score += 50;
        } else if (revenue.compareTo(BigDecimal.valueOf(500000)) > 0) {
            score += 25;
        }
        return score;
    }

    private String determinePerformanceLevel(int soldProperties, double conversionRate) {
        if (soldProperties >= 10 && conversionRate >= 70) {
            return "Excellent";
        } else if (soldProperties >= 5 && conversionRate >= 50) {
            return "Good";
        } else if (soldProperties >= 2 && conversionRate >= 30) {
            return "Average";
        } else {
            return "Needs Improvement";
        }
    }

    private double calculateTeamProductivity(int totalPropertiesSold, int teamSize) {
        return teamSize > 0 ? (double) totalPropertiesSold / teamSize : 0.0;
    }

    // Conversion methods

    private UserResponse convertToUserResponse(User user) {
        Set<String> roles = user.getUserRoles().stream()
                .map(userRole -> userRole.getRole().getRoleName())
                .collect(Collectors.toSet());

        // Load assigned districts for this user
        List<Integer> assignedDistricts = userDistrictAccessRepository.findByUserUserId(user.getUserId())
                .stream()
                .map(access -> access.getDistrict().getId())
                .collect(Collectors.toList());

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
                .assignedDistricts(assignedDistricts)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private PropertyResponse convertToPropertyResponse(Property property) {
        return PropertyResponse.builder()
                .id(property.getPropertyId())
                .address(property.getAddressProperty())
                .propertyType(property.getPropertyType())
                .size(property.getSize())
                .floor(property.getFloor())
                .thumbnail(property.getThumbnail())
                .bedrooms(property.getBedrooms())
                .bathrooms(property.getBathrooms())
                .description(property.getDescription())
                .price(property.getPrice())
                .legalDocuments(property.getLegalDocuments())
                .availability(property.getAvailability())
                .availabilityText(property.getAvailability().name())
                .phoneOwner(property.getPhoneOwner())
                .districtId(property.getDistrict().getId())
                .districtName(property.getDistrict().getName())
                .departmentId(property.getDepartment() != null ? property.getDepartment().getDepartmentId() : null)
                .departmentName(property.getDepartment() != null ? property.getDepartment().getName() : null)
                .userId(property.getUser().getUserId())
                .userName(property.getUser().getName())
                .createdAt(property.getCreatedAt())
                .updatedAt(property.getUpdatedAt())
                .build();
    }

    private InteractionResponse convertToInteractionResponse(Interaction interaction) {
        return InteractionResponse.builder()
                .id(interaction.getInteractionId())
                .customerId(interaction.getCustomer().getCustomerId())
                .customerName(interaction.getCustomer().getName())
                .propertyId(interaction.getProperty() != null ? interaction.getProperty().getPropertyId() : null)
                .propertyAddress(interaction.getProperty() != null ? interaction.getProperty().getAddressProperty() : null)
                .propertyType(interaction.getProperty() != null ? interaction.getProperty().getPropertyType() : null)
                .date(interaction.getDate())
                .details(interaction.getDetails())
                .createdAt(interaction.getCreatedAt())
                .updatedAt(interaction.getUpdatedAt())
                .build();
    }

    private CustomerResponse convertToCustomerResponse(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getCustomerId())
                .name(customer.getName())
                .email(customer.getEmail())
                .phoneNumber(customer.getPhoneNumber())
                .address(customer.getAddress())
                .dob(customer.getDob())
                .userId(customer.getUser().getUserId())
                .userName(customer.getUser().getName())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }

    private ReviewResponse convertToReviewResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getReviewId())
                .propertyId(review.getProperty().getPropertyId())
                .propertyAddress(review.getProperty().getAddressProperty())
                .userId(review.getUser().getUserId())
                .userName(review.getUser().getName())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}

