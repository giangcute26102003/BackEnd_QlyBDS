package com.example.datn_realeaste_crm.service;


import com.example.datn_realeaste_crm.dto.request.ReviewRequest;
import com.example.datn_realeaste_crm.dto.response.ReviewResponse;
import com.example.datn_realeaste_crm.entity.Property;
import com.example.datn_realeaste_crm.entity.Review;
import com.example.datn_realeaste_crm.entity.User;
import com.example.datn_realeaste_crm.exception.ResourceNotFoundException;
import com.example.datn_realeaste_crm.repository.PropertyRepository;
import com.example.datn_realeaste_crm.repository.ReviewRepository;
import com.example.datn_realeaste_crm.repository.UserRepository;
import com.example.datn_realeaste_crm.security.crypto.DeterministicHasher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {
    
    private final ReviewRepository reviewRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final DeterministicHasher deterministicHasher;
    
    public Page<ReviewResponse> getAllReviews(Integer propertyId, Pageable pageable) {
        Page<Review> reviews;
        
        if (propertyId != null) {
            reviews = reviewRepository.findByPropertyPropertyId(propertyId, pageable);
        } else {
            reviews = reviewRepository.findAll(pageable);
        }
        
        return reviews.map(this::convertToReviewResponse);
    }
    
    public ReviewResponse getReview(Integer id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + id));
        
        return convertToReviewResponse(review);
    }
    
    @Transactional
    public ReviewResponse createReview(ReviewRequest request) {
        User currentUser = getCurrentUser();
        
        Property property = propertyRepository.findById(request.getPropertyId())
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + request.getPropertyId()));
        
        Review review = Review.builder()
                .user(currentUser)
                .property(property)
                .comment(request.getComment())
                .action(request.getAction())
                .build();
        
        Review savedReview = reviewRepository.save(review);
        log.info("Review created with ID: {} by user: {} with action: {}", 
                savedReview.getReviewId(), currentUser.getUserId(), savedReview.getAction());
        
        return convertToReviewResponse(savedReview);
    }
    
    @Transactional
    public ReviewResponse updateReview(Integer id, ReviewRequest request) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + id));
        
        User currentUser = getCurrentUser();
        
        // Only the review owner can update
        if (!review.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new AccessDeniedException("You can only update your own reviews");
        }
        
        // Update fields
        review.setComment(request.getComment());
        review.setAction(request.getAction());
        
        Review updatedReview = reviewRepository.save(review);
        log.info("Review updated with ID: {} by user: {} with action: {}", 
                id, currentUser.getUserId(), updatedReview.getAction());
        
        return convertToReviewResponse(updatedReview);
    }
    
    public Page<ReviewResponse> getUserReviews(Integer userId, Pageable pageable) {
        return reviewRepository.findByUserUserId(userId, pageable)
                .map(this::convertToReviewResponse);
    }
    
    public Page<ReviewResponse> getMyReviews(Integer propertyId, Pageable pageable) {
        User currentUser = getCurrentUser();
        
        Page<Review> reviews;
        if (propertyId != null) {
            // Filter by current user and specific property
            reviews = reviewRepository.findByUserUserIdAndPropertyPropertyId(
                    currentUser.getUserId(), propertyId, pageable);
            log.debug("Found {} reviews for user {} and property {}", 
                    reviews.getTotalElements(), currentUser.getUserId(), propertyId);
        } else {
            // Get all reviews by current user
            reviews = reviewRepository.findByUserUserId(currentUser.getUserId(), pageable);
            log.debug("Found {} reviews for user {}", 
                    reviews.getTotalElements(), currentUser.getUserId());
        }
        
        return reviews.map(this::convertToReviewResponse);
    }
    
    @Transactional
    public void deleteReview(Integer id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + id));
        
        User currentUser = getCurrentUser();
        
        // Only the review owner can delete (admins are handled by @PreAuthorize in controller)
        if (!review.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new AccessDeniedException("You can only delete your own reviews");
        }
        
        reviewRepository.deleteById(id);
        log.info("Review deleted with ID: {} by user: {}", id, currentUser.getUserId());
    }
    
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        
        byte[] emailHash = deterministicHasher.emailHash(email);
        
        return userRepository.findByEmailHash(emailHash)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }
    
    private ReviewResponse convertToReviewResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getReviewId())
                .userId(review.getUser().getUserId())
                .userName(review.getUser().getName())
                .propertyId(review.getProperty().getPropertyId())
                .propertyAddress(review.getProperty().getAddressProperty())
                .comment(review.getComment())
                .action(review.getAction())
                .createdAt(review.getCreatedAt())
                .build();
    }
}