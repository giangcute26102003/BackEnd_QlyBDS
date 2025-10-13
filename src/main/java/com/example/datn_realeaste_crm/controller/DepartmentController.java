package com.example.datn_realeaste_crm.controller;


import com.example.datn_realeaste_crm.audit.Auditable;
import com.example.datn_realeaste_crm.dto.request.DepartmentRequest;
import com.example.datn_realeaste_crm.dto.response.DepartmentResponse;
import com.example.datn_realeaste_crm.dto.response.UserResponse;
import com.example.datn_realeaste_crm.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/departments")
@RequiredArgsConstructor
public class DepartmentController {
    
    private final DepartmentService departmentService;
    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<DepartmentResponse>> getAllDepartments(Pageable pageable) {
        return ResponseEntity.ok(departmentService.getAllDepartments(pageable));
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<DepartmentResponse> getDepartment(@PathVariable Integer id) {
        return ResponseEntity.ok(departmentService.getDepartment(id));
    }
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Auditable(action = "CREATE_DEPARTMENT", entityType = "Department", logResult = true)
    public ResponseEntity<DepartmentResponse> createDepartment(@Valid @RequestBody DepartmentRequest request) {
        return new ResponseEntity<>(departmentService.createDepartment(request), HttpStatus.CREATED);
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Auditable(action = "UPDATE_DEPARTMENT", entityType = "Department", entityIdParam = "id")
    public ResponseEntity<DepartmentResponse> updateDepartment(
            @PathVariable Integer id, 
            @Valid @RequestBody DepartmentRequest request) {
        return ResponseEntity.ok(departmentService.updateDepartment(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Auditable(action = "DELETE_DEPARTMENT", entityType = "Department", entityIdParam = "id")
    public ResponseEntity<Void> deleteDepartment(@PathVariable Integer id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/{id}/manager/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Auditable(action = "ASSIGN_DEPARTMENT_MANAGER", entityType = "Department", entityIdParam = "id")
    public ResponseEntity<DepartmentResponse> assignManager(
            @PathVariable Integer id, 
            @PathVariable Integer userId) {
        return ResponseEntity.ok(departmentService.assignManager(id, userId));
    }
    
    @GetMapping("/{id}/users")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<List<UserResponse>> getDepartmentUsers(@PathVariable Integer id) {
        return ResponseEntity.ok(departmentService.getDepartmentUsers(id));
    }
}