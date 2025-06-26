package com.example.datn_realeaste_crm.controller;


import com.example.datn_realeaste_crm.audit.Auditable;
import com.example.datn_realeaste_crm.dto.request.NotificationRequest;
import com.example.datn_realeaste_crm.dto.response.NotificationResponse;
import com.example.datn_realeaste_crm.entity.User;
import com.example.datn_realeaste_crm.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {
    
    private final NotificationService notificationService;
    
    @GetMapping
    @PreAuthorize("hasAuthority('notification_view')")
    public ResponseEntity<Page<NotificationResponse>> getUserNotifications(
            @RequestParam(required = false) String status,
            Pageable pageable) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Integer userId = ((User) authentication.getPrincipal()).getUserId();
        
        return ResponseEntity.ok(notificationService.getUserNotifications(userId, status, pageable));
    }
    
    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('MANAGER')")
    @Auditable(action = "CREATE_NOTIFICATION", entityType = "Notification", logResult = true)
    public ResponseEntity<NotificationResponse> createNotification(@Valid @RequestBody NotificationRequest request) {
        return new ResponseEntity<>(notificationService.createNotification(request), HttpStatus.CREATED);
    }
    
    @PutMapping("/{id}/read")
    @PreAuthorize("hasAuthority('notification_update')")
    @Auditable(action = "MARK_NOTIFICATION_READ", entityType = "Notification", entityIdParam = "id")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable Integer id) {
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }
    
    @PutMapping("/read-all")
    @PreAuthorize("hasAuthority('notification_update')")
    @Auditable(action = "MARK_ALL_NOTIFICATIONS_READ", entityType = "Notification")
    public ResponseEntity<Void> markAllAsRead() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Integer userId = ((User) authentication.getPrincipal()).getUserId();
        
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }
}