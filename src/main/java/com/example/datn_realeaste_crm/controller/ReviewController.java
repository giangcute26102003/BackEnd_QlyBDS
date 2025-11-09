package com.example.datn_realeaste_crm.controller;


import com.example.datn_realeaste_crm.audit.Auditable;
import com.example.datn_realeaste_crm.dto.request.ReviewRequest;
import com.example.datn_realeaste_crm.dto.response.ReviewResponse;
import com.example.datn_realeaste_crm.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {
    
    private final ReviewService reviewService;
    
    @GetMapping
    public ResponseEntity<Page<ReviewResponse>> getAllReviews(
            @RequestParam(required = false) Integer propertyId,
            Pageable pageable) {
        return ResponseEntity.ok(reviewService.getAllReviews(propertyId, pageable));
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('review_view') or isAuthenticated()")
    public ResponseEntity<ReviewResponse> getReview(@PathVariable Integer id) {
        return ResponseEntity.ok(reviewService.getReview(id));
    }
    
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Auditable(action = "CREATE_REVIEW", entityType = "Review", logResult = true)
    public ResponseEntity<ReviewResponse> createReview(@Valid @RequestBody ReviewRequest request) {
        return new ResponseEntity<>(reviewService.createReview(request), HttpStatus.CREATED);
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Auditable(action = "UPDATE_REVIEW", entityType = "Review", entityIdParam = "id", logResult = true)
    public ResponseEntity<ReviewResponse> updateReview(
            @PathVariable Integer id,
            @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(reviewService.updateReview(id, request));
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<ReviewResponse>> getUserReviews(
            @PathVariable Integer userId,
            Pageable pageable) {
        return ResponseEntity.ok(reviewService.getUserReviews(userId, pageable));
    }
    
    @GetMapping("/my-reviews")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ReviewResponse>> getMyReviews(
            @RequestParam(required = false) Integer propertyId,
            Pageable pageable) {
        return ResponseEntity.ok(reviewService.getMyReviews(propertyId, pageable));
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or @reviewAuthorizationService.isReviewOwner(authentication, #id)")
    @Auditable(action = "DELETE_REVIEW", entityType = "Review", entityIdParam = "id")
    public ResponseEntity<Void> deleteReview(@PathVariable Integer id) {
        reviewService.deleteReview(id);
        return ResponseEntity.ok().build();
    }
}