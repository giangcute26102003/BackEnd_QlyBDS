package com.example.datn_realeaste_crm.security;

import com.example.datn_realeaste_crm.dto.response.*;
import com.example.datn_realeaste_crm.entity.User;
import com.example.datn_realeaste_crm.exception.ResourceNotFoundException;
import com.example.datn_realeaste_crm.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final InteractionRepository interactionRepository;
    private final NotificationRepository notificationRepository;
    private final FavoriteRepository favoriteRepository;
    private final ReviewRepository reviewRepository;

    public DashboardStatisticsResponse getStatistics() {
        long totalProperties = propertyRepository.count();
        long pendingProperties = propertyRepository.countByAvailability("PENDING");
        long approvedProperties = propertyRepository.countByAvailability("APPROVED");
        long rejectedProperties = propertyRepository.countByAvailability("REJECTED");

        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByIsActiveTrue();

        long totalCustomers = customerRepository.count();
        long totalInteractions = interactionRepository.count();

        BigDecimal avgPropertyPrice = propertyRepository.findAveragePrice();

        return DashboardStatisticsResponse.builder()
                .totalProperties(totalProperties)
                .pendingProperties(pendingProperties)
                .approvedProperties(approvedProperties)
                .rejectedProperties(rejectedProperties)
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .totalCustomers(totalCustomers)
                .totalInteractions(totalInteractions)
                .averagePropertyPrice(avgPropertyPrice)
                .build();
    }

    public UserStatisticsResponse getUserStatistics(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        long myProperties = propertyRepository.countByUserUserId(userId);
        long myApprovedProperties = propertyRepository.countByUserUserIdAndAvailability(userId, "APPROVED");
        long myPendingProperties = propertyRepository.countByUserUserIdAndAvailability(userId, "PENDING");

        long myFavorites = favoriteRepository.countByUserUserId(userId);
        long myReviews = reviewRepository.countByUserUserId(userId);
        long unreadNotifications = notificationRepository.countByUserUserIdAndStatus(userId, "Chưa đọc");

        return UserStatisticsResponse.builder()
                .myProperties(myProperties)
                .myApprovedProperties(myApprovedProperties)
                .myPendingProperties(myPendingProperties)
                .myFavorites(myFavorites)
                .myReviews(myReviews)
                .unreadNotifications(unreadNotifications)
                .build();
    }
}