package com.example.datn_realeaste_crm.service;


import com.example.datn_realeaste_crm.dto.request.DepartmentRequest;
import com.example.datn_realeaste_crm.dto.response.*;
import com.example.datn_realeaste_crm.entity.Department;
import com.example.datn_realeaste_crm.entity.User;
import com.example.datn_realeaste_crm.exception.ResourceAlreadyExistsException;
import com.example.datn_realeaste_crm.exception.ResourceNotFoundException;
import com.example.datn_realeaste_crm.repository.DepartmentRepository;
import com.example.datn_realeaste_crm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartmentService {
    
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    
    public Page<DepartmentResponse> getAllDepartments(Pageable pageable) {
        return departmentRepository.findAll(pageable)
                .map(this::convertToDepartmentResponse);
    }
    
    public DepartmentResponse getDepartment(Integer id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
        
        return convertToDepartmentResponse(department);
    }
    
    @Transactional
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        // Check if department name already exists
        Optional<Department> existingDepartment = departmentRepository.findByName(request.getName());
        if (existingDepartment.isPresent()) {
            throw new ResourceAlreadyExistsException("Department name already exists: " + request.getName());
        }
        
        Department department = new Department();
        department.setName(request.getName());
        department.setDescription(request.getDescription());
        
        // Set manager if provided
        if (request.getManagerId() != null) {
            User manager = userRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getManagerId()));
            department.setManager(manager);
        }
        
        Department savedDepartment = departmentRepository.save(department);
        
        return convertToDepartmentResponse(savedDepartment);
    }
    
    @Transactional
    public DepartmentResponse updateDepartment(Integer id, DepartmentRequest request) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
        
        // Check if updated name already exists for another department
        if (!department.getName().equals(request.getName())) {
            Optional<Department> existingDepartment = departmentRepository.findByName(request.getName());
            if (existingDepartment.isPresent() && !existingDepartment.get().getDepartmentId().equals(id)) {
                throw new ResourceAlreadyExistsException("Department name already exists: " + request.getName());
            }
            department.setName(request.getName());
        }
        
        department.setDescription(request.getDescription());
        
        // Update manager if provided
        if (request.getManagerId() != null) {
            User manager = userRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getManagerId()));
            department.setManager(manager);
        }
        
        Department updatedDepartment = departmentRepository.save(department);
        
        return convertToDepartmentResponse(updatedDepartment);
    }
    
    @Transactional
    public void deleteDepartment(Integer id) {
        if (!departmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Department not found with id: " + id);
        }
        
        departmentRepository.deleteById(id);
    }
    
    @Transactional
    public DepartmentResponse assignManager(Integer departmentId, Integer userId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + departmentId));
        
        User manager = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        department.setManager(manager);
        Department updatedDepartment = departmentRepository.save(department);
        
        return convertToDepartmentResponse(updatedDepartment);
    }
    
    public List<UserResponse> getDepartmentUsers(Integer departmentId) {
        if (!departmentRepository.existsById(departmentId)) {
            throw new ResourceNotFoundException("Department not found with id: " + departmentId);
        }
        
        return userRepository.findByDepartmentDepartmentId(departmentId)
                .stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
    }
    
    private DepartmentResponse convertToDepartmentResponse(Department department) {
        return DepartmentResponse.builder()
                .id(department.getDepartmentId())
                .name(department.getName())
                .description(department.getDescription())
                .managerId(department.getManager() != null ? department.getManager().getUserId() : null)
                .managerName(department.getManager() != null ? department.getManager().getName() : null)
                .createdAt(department.getCreatedAt())
                .updatedAt(department.getUpdatedAt())
                .build();
    }
    
    private UserResponse convertToUserResponse(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .isActive(user.getIsActive())
                .departmentId(user.getDepartment() != null ? user.getDepartment().getDepartmentId() : null)
                .departmentName(user.getDepartment() != null ? user.getDepartment().getName() : null)
                .build();
    }
}