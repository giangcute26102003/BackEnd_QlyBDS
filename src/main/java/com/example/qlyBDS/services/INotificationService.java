//package com.example.qlyBDS.services;
//
//import com.realestate.dto.request.CreateNotificationRequest;
//import com.realestate.dto.response.NotificationResponse;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//
//public interface INotificationService {
//    NotificationResponse createNotification(CreateNotificationRequest request);
//    void markAsRead(Integer notificationId);
//    void markAllAsRead(Integer userId);
//    void deleteNotification(Integer notificationId);
//    NotificationResponse getNotificationById(Integer notificationId);
//    Page<NotificationResponse> getUserNotifications(Integer userId, Pageable pageable);
//    Long getUnreadCount(Integer userId);
//}